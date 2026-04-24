package com.app.aml.feature.ingestion.batch.transaction;

import com.app.aml.feature.ingestion.batch.ValidationException;
import com.app.aml.feature.ingestion.entity.Transaction;
import com.app.aml.feature.ingestion.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
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

@Configuration
@RequiredArgsConstructor
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class TransactionIngestionBatchConfig {

    private static final int CHUNK_SIZE = 1000;
    private static final int MAX_SKIP_LIMIT = Integer.MAX_VALUE;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final TransactionRepository repository;
    private final TransactionValidationProcessor processor;

    private final TransactionJobCompletionListener completionListener;
    private final TransactionValidationSkipListener skipListener;

    @Qualifier("batchTaskExecutor")
    private final TaskExecutor batchTaskExecutor;

    @Bean
    public Job transactionIngestionJob() {
        return new JobBuilder("transactionIngestionJob", jobRepository)
                .listener(completionListener)
                .start(transactionValidationStep())
                .on("COMPLETED_WITH_SKIPS").end()
                .from(transactionValidationStep())
                .on(ExitStatus.COMPLETED.getExitCode()).to(transactionIngestionStep())
                .end()
                .build();
    }

    @Bean
    public Step transactionValidationStep() {
        return new StepBuilder("transactionValidationStep", jobRepository)
                .<TransactionCsvDto, Transaction>chunk(CHUNK_SIZE, transactionManager)
                .reader(transactionReader(null)) // Single thread for validation
                .processor(processor)
                .writer(items -> { /* No-op: validation only phase */ })
                .faultTolerant()
                .skip(ValidationException.class)
                .skip(FlatFileParseException.class)
                .skipLimit(MAX_SKIP_LIMIT)
                .listener(skipListener)
                .listener(transactionValidationExitListener())
                .build();
    }

    @Bean
    public Step transactionIngestionStep() {
        return new StepBuilder("transactionIngestionStep", jobRepository)
                .<TransactionCsvDto, Transaction>chunk(CHUNK_SIZE, transactionManager)
                // Wrap reader for thread-safety during parallel ingestion
                .reader(new SynchronizedItemStreamReaderBuilder<TransactionCsvDto>()
                        .delegate(transactionReader(null))
                        .build())
                .processor(processor)
                .writer(transactionWriter())
                .taskExecutor(batchTaskExecutor)
                .build();
    }

    @Bean
    public StepExecutionListener transactionValidationExitListener() {
        return new StepExecutionListenerSupport() {
            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                if (stepExecution.getSkipCount() > 0) {
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
                        "amount,currencyCode,transactionType,channel,transactionTimestamp,referenceNote";

        return new FlatFileItemReaderBuilder<TransactionCsvDto>()
                .name("transactionReader")
                .resource(new FileSystemResource(filePath))
                .linesToSkip(1)
                .skippedLinesCallback(headerLine -> {
                    if (!headerLine.trim().equalsIgnoreCase(expectedHeader)) {
                        throw new IllegalArgumentException(
                                "Transaction Header mismatch. Expected: [" + expectedHeader + "] but found: [" + headerLine + "]"
                        );
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
                .methodName("saveAll")
                .build();
    }
}