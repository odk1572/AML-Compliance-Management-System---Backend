-- This removes the NOT NULL constraint from your existing table
ALTER TABLE alerts ALTER COLUMN triggering_transaction_id DROP NOT NULL;