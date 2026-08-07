-- Project module: billing. Mark time entries as billed and link them to the
-- generated AR invoice so the same time is never billed twice.
ALTER TABLE time_entries
    ADD COLUMN invoiced boolean NOT NULL DEFAULT false,
    ADD COLUMN invoice_id uuid REFERENCES invoices(id) ON DELETE SET NULL;

CREATE INDEX time_entries_invoice_idx ON time_entries(invoice_id);
