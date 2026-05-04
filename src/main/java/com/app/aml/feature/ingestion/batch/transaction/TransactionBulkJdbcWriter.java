package com.app.aml.feature.ingestion.batch.transaction;


import com.app.aml.feature.ingestion.entity.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import java.sql.Types;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.util.List;

@RequiredArgsConstructor
public class TransactionBulkJdbcWriter implements ItemWriter<Transaction> {

    private final JdbcTemplate jdbcTemplate;

    public TransactionBulkJdbcWriter(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    private static final String INSERT_SQL = """
        INSERT INTO transactions (
            id, batch_id, customer_id, transaction_ref, originator_account_no,
            originator_name, originator_bank_code, originator_country,
            beneficiary_account_no, beneficiary_name, beneficiary_bank_code,
            beneficiary_country, amount, currency_code, transaction_type,
            channel, transaction_timestamp, reference_note, status
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (transaction_ref) DO NOTHING
    """;

    @Override
    public void write(Chunk<? extends Transaction> chunk) {
        List<? extends Transaction> items = chunk.getItems();

        jdbcTemplate.batchUpdate(INSERT_SQL, items, items.size(), (ps, txn) -> {
            ps.setObject(1, txn.getId());
            ps.setObject(2, txn.getBatch() != null ? txn.getBatch().getId() : null);
            ps.setObject(3, txn.getCustomer() != null ? txn.getCustomer().getId() : null);
            ps.setString(4, txn.getTransactionRef());
            ps.setString(5, txn.getOriginatorAccountNo());
            ps.setString(6, txn.getOriginatorName());
            ps.setString(7, txn.getOriginatorBankCode());
            ps.setString(8, txn.getOriginatorCountry());
            ps.setString(9, txn.getBeneficiaryAccountNo());
            ps.setString(10, txn.getBeneficiaryName());
            ps.setString(11, txn.getBeneficiaryBankCode());
            ps.setString(12, txn.getBeneficiaryCountry());
            ps.setBigDecimal(13, txn.getAmount());
            ps.setString(14, txn.getCurrencyCode());
            ps.setString(15, txn.getTransactionType() != null ? txn.getTransactionType().name() : null);
            ps.setString(16, txn.getChannel() != null ? txn.getChannel().name() : null);
            ps.setObject(17, txn.getTransactionTimestamp() != null
                    ? txn.getTransactionTimestamp().atOffset(ZoneOffset.UTC)
                    : null, Types.TIMESTAMP_WITH_TIMEZONE);

            ps.setString(18, txn.getReferenceNote());
            ps.setString(19, txn.getStatus() != null ? txn.getStatus().name() : null);
        });
    }
}