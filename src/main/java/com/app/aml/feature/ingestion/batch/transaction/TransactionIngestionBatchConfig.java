package com.app.aml.feature.ingestion.batch.transaction;

import com.app.aml.feature.ingestion.batch.ValidationException;
import com.app.aml.feature.ingestion.entity.Transaction;
import com.app.aml.feature.ingestion.repository.TransactionRepository;
import com.app.aml.multitenency.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
@Slf4j
@Configuration
@RequiredArgsConstructor
public class TransactionIngestionBatchConfig {

    private static final int CHUNK_SIZE = 1000;
    private static final int MAX_SKIP_LIMIT = Integer.MAX_VALUE;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final TransactionRepository repository;
    private final TransactionValidationProcessor processor;
    private final TransactionValidationSkipListener skipListener;

    @Qualifier("batchTaskExecutor")
    private final TaskExecutor batchTaskExecutor;

    @Bean
    public StepExecutionListener tenantContextStepListener() {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                String tenantId = stepExecution.getJobParameters().getString("tenantId");
                String schemaName = stepExecution.getJobParameters().getString("schemaName");

                if (tenantId != null && schemaName != null) {
                    TenantContext.setTenantId(tenantId);
                    TenantContext.setSchemaName(schemaName);
                }
            }
            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                TenantContext.clear();
                return null;
            }
        };
    }

    @Bean
    public Job transactionIngestionJob(TransactionJobCompletionListener completionListener) {
        return new JobBuilder("transactionIngestionJob", jobRepository)
                .listener(completionListener)
                .start(transactionValidationStep())
                // Only proceed to ingestion if validation was SUCCESSFUL and had 0 skips
                .on(ExitStatus.COMPLETED.getExitCode()).to(transactionIngestionStep())
                // If validation finished but had skips, stop here
                .from(transactionValidationStep()).on("COMPLETED_WITH_SKIPS").end()
                // Fail if anything else happens
                .from(transactionValidationStep()).on("*").fail()
                .end()
                .build();
    }

    @Bean
    public Step transactionValidationStep() {
        return new StepBuilder("transactionValidationStep", jobRepository)
                .<TransactionCsvDto, Transaction>chunk(CHUNK_SIZE, transactionManager)
                .reader(transactionReader(null))
                .processor(processor)
                .writer(items -> { /* Just validation, no DB write yet */ })
                .faultTolerant()
                .skip(ValidationException.class)
                .skip(FlatFileParseException.class)
                .skipLimit(MAX_SKIP_LIMIT)
                .listener(skipListener)
                .listener(tenantContextStepListener())
                .listener(transactionValidationExitListener())
                .build();
    }

    @Bean
    public Step transactionIngestionStep() {
        return new StepBuilder("transactionIngestionStep", jobRepository)
                .<TransactionCsvDto, Transaction>chunk(CHUNK_SIZE, transactionManager)
                .reader(new SynchronizedItemStreamReaderBuilder<TransactionCsvDto>()
                        .delegate(transactionReader(null))
                        .build())
                .processor(processor)
                .writer(transactionWriter())
                // Use taskExecutor only if you are SURE your DB handles concurrent tenant writes
                .taskExecutor(batchTaskExecutor)
                .listener(tenantContextStepListener())
                .build();
    }

    @Bean
    public StepExecutionListener transactionValidationExitListener() {
        return new StepExecutionListenerSupport() {
            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                // If any records were skipped due to errors, we block the save step
                if (stepExecution.getSkipCount() > 0 || stepExecution.getProcessSkipCount() > 0) {
                    log.warn("Validation failed with {} skips. Saving aborted.", stepExecution.getSkipCount());
                    return new ExitStatus("COMPLETED_WITH_SKIPS");
                }
                return ExitStatus.COMPLETED;
            }
        };
    }

    @Bean
    @StepScope
    public FlatFileItemReader<TransactionCsvDto> transactionReader(
            @Value("#{jobParameters['filePath']}") String filePath) {

        final String expectedHeader =
                "transactionRef,originatorAccountNo,originatorName,originatorBankCode,originatorCountry," +
                        "beneficiaryAccountNo,beneficiaryName,beneficiaryBankCode,beneficiaryCountry," +
                        "amount,currencyCode,transactionType,channel,transactionTimestamp,referenceNote,status";

        return new FlatFileItemReaderBuilder<TransactionCsvDto>()
                .name("transactionReader")
                .resource(new FileSystemResource(filePath))
                .linesToSkip(1)
                .skippedLinesCallback(headerLine -> {
                    if (!headerLine.trim().equalsIgnoreCase(expectedHeader)) {
                        throw new IllegalArgumentException("Header mismatch.");
                    }
                })
                .delimited()
                .names(expectedHeader.split(","))
                .targetType(TransactionCsvDto.class)
                .build();
    }

    @Bean
    public RepositoryItemWriter<Transaction> transactionWriter() {
        return new RepositoryItemWriterBuilder<Transaction>()
                .repository(repository)
                .methodName("save") // Use "save" for RepositoryItemWriter (it calls it per item/list)
                .build();
    }
}