package com.app.aml.feature.ingestion.batch;

import com.app.aml.feature.ingestion.entity.CustomerProfile;
import com.app.aml.feature.ingestion.repository.CustomerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class CustomerIngestionBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final CustomerProfileRepository repository;
    private final CustomerProfileValidationProcessor processor;
    private final IngestionJobCompletionListener completionListener;
    private final ValidationSkipListener skipListener;

    @Bean
    public Job customerProfileIngestionJob() {
        return new JobBuilder("customerProfileIngestionJob", jobRepository)
                .listener(completionListener)
                .start(validationStep())
                // Only execute ingestion step if validation step generates NO errors
                .next(ingestionStep())
                .build();
    }

    @Bean
    public Step validationStep() {
        return new StepBuilder("validationStep", jobRepository)
                .<CustomerProfileCsvDto, CustomerProfile>chunk(1000, transactionManager)
                .reader(customerProfileReader(null))
                .processor(processor)
                .writer(items -> {
                    // No-Op Writer: We are just validating here. Don't write to DB yet.
                })
                .faultTolerant()
                .skip(ValidationException.class)
                .skip(org.springframework.batch.item.file.FlatFileParseException.class)
                .skipLimit(Integer.MAX_VALUE) // Keep validating whole file to find ALL errors
                .listener(skipListener)
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Step ingestionStep() {
        return new StepBuilder("ingestionStep", jobRepository)
                .<CustomerProfileCsvDto, CustomerProfile>chunk(1000, transactionManager)
                .reader(customerProfileReader(null))
                .processor(processor)
                .writer(customerProfileWriter())
                .taskExecutor(taskExecutor()) // Parallel database insertion
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<CustomerProfileCsvDto> customerProfileReader(
            @Value("#{jobParameters['filePath']}") String filePath) {

        // Define the exact expected header string (comma separated)
        String expectedHeader = "accountNumber,customerName,customerType,idType,idNumber,nationality,countryOfResidence,monthlyIncome,netWorth,riskRating,isPep,isDormant,accountOpenedOn";

        return new FlatFileItemReaderBuilder<CustomerProfileCsvDto>()
                .name("customerProfileReader")
                .resource(new FileSystemResource(filePath))
                .linesToSkip(1) // We still skip it so it doesn't map as a data DTO
                .skippedLinesCallback(headerLine -> {
                    // This callback fires exactly once for the skipped header row.
                    // If it doesn't match perfectly, we throw an exception to kill the job instantly.
                    if (!headerLine.trim().equalsIgnoreCase(expectedHeader)) {
                        throw new IllegalArgumentException("Header mismatch. Expected: [" + expectedHeader + "] but found: [" + headerLine + "]");
                    }
                })
                .delimited()
                .names(expectedHeader.split(",")) // Reusing the string to prevent typos
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

    @Bean
    public TaskExecutor taskExecutor() {
        SimpleAsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor("batch_worker_");
        asyncTaskExecutor.setConcurrencyLimit(10); // Process 10 chunks in parallel
        return asyncTaskExecutor;
    }
}