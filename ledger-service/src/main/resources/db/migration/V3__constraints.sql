ALTER TABLE ledger_entries
    ADD CONSTRAINT chk_debit_credit
    CHECK (
        (debit = 0 AND credit > 0)
        OR (credit = 0 AND debit > 0)
    );