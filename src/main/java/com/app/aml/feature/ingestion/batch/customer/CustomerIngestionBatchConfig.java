package com.app.aml.feature.ingestion.batch.customer;

import com.app.aml.feature.ingestion.batch.*;
import com.app.aml.feature.ingestion.entity.CustomerProfile;
import com.app.aml.feature.ingestion.repository.CustomerProfileRepository;
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
public class CustomerIngestionBatchConfig {

    private static final int CHUNK_SIZE = 1000;
    private static final int MAX_SKIP_LIMIT = Integer.MAX_VALUE;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final CustomerProfileRepository repository;
    private final CustomerProfileValidationProcessor processor;
    private final CustomerIngestionJobCompletionListener completionListener;
    private final CustomerValidationSkipListener skipListener;

    @Qualifier("batchTaskExecutor")
    private final TaskExecutor batchTaskExecutor;

    @Bean
    public Job customerProfileIngestionJob() {

        Step validation = validationStep();
        Step ingestion = ingestionStep();

        return new JobBuilder("customerProfileIngestionJob", jobRepository)
                .listener(completionListener)
                .start(validation)
                .on("COMPLETED_WITH_SKIPS").end()     // stop job if validation found errors
                .from(validation)
                .on(ExitStatus.COMPLETED.getExitCode()).to(ingestion)
                .end()
                .build();
    }

    @Bean
    public Step validationStep() {
        return new StepBuilder("validationStep", jobRepository)
                .<CustomerProfileCsvDto, CustomerProfile>chunk(CHUNK_SIZE, transactionManager)
                .reader(customerProfileReader(null))
                .processor(processor)
                .writer(items -> {
                    // No-op: validation only
                })
                .faultTolerant()
                .skip(ValidationException.class)
                .skip(FlatFileParseException.class)
                .skipLimit(MAX_SKIP_LIMIT)
                .listener(skipListener)
                .listener(validationExitStatusListener())
                // IMPORTANT: no taskExecutor here (reader is not thread-safe)
                .build();
    }

    @Bean
    public StepExecutionListener validationExitStatusListener() {
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
    public Step ingestionStep() {
        return new StepBuilder("ingestionStep", jobRepository)
                .<CustomerProfileCsvDto, CustomerProfile>chunk(CHUNK_SIZE, transactionManager)
                // Wrap the reader to make it thread-safe for the TaskExecutor
                .reader(new org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder<CustomerProfileCsvDto>()
                        .delegate(customerProfileReader(null))
                        .build())
                .processor(processor)
                .writer(customerProfileWriter())
                .taskExecutor(batchTaskExecutor)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<CustomerProfileCsvDto> customerProfileReader(
            @Value("#{jobParameters['filePath']}") String filePath) {

        final String expectedHeader =
                "accountNumber,customerName,customerType,idType,idNumber,nationality," +
                        "countryOfResidence,monthlyIncome,netWorth,riskRating,isPep,isDormant,accountOpenedOn";

        return new FlatFileItemReaderBuilder<CustomerProfileCsvDto>()
                .name("customerProfileReader")
                .resource(new FileSystemResource(filePath))
                .linesToSkip(1)
                .skippedLinesCallback(headerLine -> {
                    if (!headerLine.trim().equalsIgnoreCase(expectedHeader)) {
                        throw new IllegalArgumentException(
                                "Header mismatch. Expected: [" + expectedHeader + "] but found: [" + headerLine + "]"
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
                .methodName("saveAll")
                .build();
    }

}