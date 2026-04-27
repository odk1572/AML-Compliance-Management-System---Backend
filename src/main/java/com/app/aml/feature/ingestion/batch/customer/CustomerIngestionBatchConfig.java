package com.app.aml.feature.ingestion.batch.customer;

import com.app.aml.feature.ingestion.batch.ValidationException;
import com.app.aml.feature.ingestion.entity.CustomerProfile;
import com.app.aml.feature.ingestion.repository.CustomerProfileRepository;
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
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class CustomerIngestionBatchConfig {

    private static final int CHUNK_SIZE = 1000;
    private static final int MAX_SKIP_LIMIT = Integer.MAX_VALUE;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final CustomerProfileRepository repository;
    private final CustomerProfileValidationProcessor processor;
    private final CustomerValidationSkipListener skipListener;

    @Qualifier("batchTaskExecutor")
    private final TaskExecutor batchTaskExecutor;

    @Bean
    public StepExecutionListener customerTenantStepListener() {
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
    public Job customerProfileIngestionJob(CustomerIngestionJobCompletionListener completionListener) {
        return new JobBuilder("customerProfileIngestionJob", jobRepository)
                .listener(completionListener)
                .start(validationStep())
                .on("COMPLETED_WITH_SKIPS").end()
                .from(validationStep())
                .on(ExitStatus.COMPLETED.getExitCode()).to(ingestionStep())
                .end()
                .build();
    }

    @Bean
    public Step validationStep() {
        return new StepBuilder("validationStep", jobRepository)
                .<CustomerProfileCsvDto, CustomerProfile>chunk(CHUNK_SIZE, transactionManager)
                .reader(customerProfileReader(null))
                .processor(processor)
                .writer(items -> { /* Validation Only */ })
                .faultTolerant()
                .skip(ValidationException.class)
                .skip(FlatFileParseException.class)
                .skipLimit(MAX_SKIP_LIMIT)
                .listener(skipListener)
                .listener(validationExitStatusListener())
                .listener(customerTenantStepListener())
                .build();
    }

    @Bean
    public Step ingestionStep() {
        return new StepBuilder("ingestionStep", jobRepository)
                .<CustomerProfileCsvDto, CustomerProfile>chunk(CHUNK_SIZE, transactionManager)
                .reader(new SynchronizedItemStreamReaderBuilder<CustomerProfileCsvDto>()
                        .delegate(customerProfileReader(null))
                        .build())
                .processor(processor)
                .writer(customerProfileWriter())
                .taskExecutor(batchTaskExecutor)
                .listener(customerTenantStepListener())
                .build();
    }

    @Bean
    public StepExecutionListener validationExitStatusListener() {
        return new StepExecutionListenerSupport() {
            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                if (stepExecution.getSkipCount() > 0) {
                    log.warn("Customer validation found {} skips. Aborting ingestion.", stepExecution.getSkipCount());
                    return new ExitStatus("COMPLETED_WITH_SKIPS");
                }
                return ExitStatus.COMPLETED;
            }
        };
    }

    @Bean
    @StepScope
    public FlatFileItemReader<CustomerProfileCsvDto> customerProfileReader(
            @Value("#{jobParameters['filePath']}") String filePath) {

        final String expectedHeader =
                "accountNumber,customerName,customerType,idType,idNumber,nationality," +
                        "countryOfResidence,monthlyIncome,netWorth,riskRating,riskScore," +
                        "isPep,isDormant,accountOpenedOn,lastActivityDate,kycStatus";

        return new FlatFileItemReaderBuilder<CustomerProfileCsvDto>()
                .name("customerProfileReader")
                .resource(new FileSystemResource(filePath))
                .linesToSkip(1)
                .skippedLinesCallback(headerLine -> {
                    if (!headerLine.trim().equalsIgnoreCase(expectedHeader)) {
                        throw new IllegalArgumentException(
                                "Customer Header mismatch. Expected: [" + expectedHeader + "] but found: [" + headerLine + "]"
                        );
                    }
                })
                .delimited()
                .names(expectedHeader.split(","))
                .targetType(CustomerProfileCsvDto.class)
                .build();
    }
    @Bean
    public RepositoryItemWriter<CustomerProfile> customerProfileWriter() {
        return new RepositoryItemWriterBuilder<CustomerProfile>()
                .repository(repository)
                .methodName("save")
                .build();
    }
}