-- HR module: attendance records (clock-in / clock-out + daily timesheets).
-- One record per employee per work date.
CREATE TABLE attendance_records (
    id                uuid PRIMARY KEY,
    employee_id       uuid NOT NULL REFERENCES employees(id) ON DELETE RESTRICT,
    work_date         date NOT NULL,
    clock_in          timestamp,
    clock_out         timestamp,
    worked_minutes    int,
    status            text NOT NULL DEFAULT 'PRESENT',
    notes             text,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    updated_at        timestamp,
    CONSTRAINT attendance_records_status_chk CHECK (status IN ('PRESENT','ABSENT','ON_LEAVE')),
    CONSTRAINT attendance_records_unique_per_day UNIQUE (organization_id, employee_id, work_date)
);
CREATE INDEX attendance_records_org_employee_date_idx ON attendance_records(organization_id, employee_id, work_date);
