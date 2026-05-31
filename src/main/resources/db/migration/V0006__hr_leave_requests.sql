-- HR module: leave requests.
CREATE TABLE leave_requests (
    id                uuid PRIMARY KEY,
    employee_id       uuid NOT NULL REFERENCES employees(id) ON DELETE RESTRICT,
    leave_type_id     uuid NOT NULL REFERENCES leave_types(id) ON DELETE RESTRICT,
    start_date        date NOT NULL,
    end_date          date NOT NULL,
    days              int NOT NULL,
    reason            text,
    status            text NOT NULL DEFAULT 'PENDING',
    decision_reason   text,
    decided_by        uuid REFERENCES users(uuid),
    decided_at        timestamp,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    requested_by      uuid NOT NULL REFERENCES users(uuid),
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    updated_at        timestamp,
    CONSTRAINT leave_requests_status_chk CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED'))
);
CREATE INDEX leave_requests_org_employee_idx ON leave_requests(organization_id, employee_id);
CREATE INDEX leave_requests_org_status_idx ON leave_requests(organization_id, status);
