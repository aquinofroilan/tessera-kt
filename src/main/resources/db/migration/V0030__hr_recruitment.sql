-- HR: recruitment / applicant tracking (ATS).
-- A job posting drives applications; each application accumulates interviews
-- and progresses through APPLIED -> SCREENING -> INTERVIEW -> OFFERED -> HIRED
-- or REJECTED/WITHDRAWN at any point. Postings link optionally to a department
-- and/or a position so headcount planning later can reconcile against
-- hired-but-not-yet-onboarded counts.

CREATE TABLE hr_recruitment_job_postings (
    id              uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES organizations(uuid),
    title           text NOT NULL,
    description     text,
    department_id   uuid REFERENCES departments(id) ON DELETE SET NULL,
    position_id     uuid REFERENCES positions(id) ON DELETE SET NULL,
    status          text NOT NULL DEFAULT 'DRAFT',
    posted_at       timestamp,
    closed_at       timestamp,
    owner_user_id   uuid REFERENCES users(uuid),
    notes           text,
    created_by      uuid NOT NULL REFERENCES users(uuid),
    created_at      timestamp NOT NULL DEFAULT current_timestamp,
    updated_at      timestamp,
    CONSTRAINT hr_rec_jobs_status_chk CHECK (status IN ('DRAFT','OPEN','CLOSED','CANCELLED'))
);
CREATE INDEX hr_rec_jobs_org_status_idx ON hr_recruitment_job_postings(organization_id, status);

CREATE TABLE hr_recruitment_applications (
    id                   uuid PRIMARY KEY,
    organization_id      uuid NOT NULL REFERENCES organizations(uuid),
    job_posting_id       uuid NOT NULL REFERENCES hr_recruitment_job_postings(id) ON DELETE CASCADE,
    candidate_full_name  text NOT NULL,
    email                text,
    phone                text,
    resume_url           text,
    source               text,
    status               text NOT NULL DEFAULT 'APPLIED',
    applied_at           timestamp NOT NULL DEFAULT current_timestamp,
    owner_user_id        uuid REFERENCES users(uuid),
    notes                text,
    created_by           uuid NOT NULL REFERENCES users(uuid),
    created_at           timestamp NOT NULL DEFAULT current_timestamp,
    updated_at           timestamp,
    CONSTRAINT hr_rec_app_status_chk
        CHECK (status IN ('APPLIED','SCREENING','INTERVIEW','OFFERED','HIRED','REJECTED','WITHDRAWN'))
);
CREATE INDEX hr_rec_app_org_job_idx    ON hr_recruitment_applications(organization_id, job_posting_id);
CREATE INDEX hr_rec_app_org_status_idx ON hr_recruitment_applications(organization_id, status);

CREATE TABLE hr_recruitment_interviews (
    id                  uuid PRIMARY KEY,
    organization_id     uuid NOT NULL REFERENCES organizations(uuid),
    application_id      uuid NOT NULL REFERENCES hr_recruitment_applications(id) ON DELETE CASCADE,
    scheduled_at        timestamp NOT NULL,
    interviewer_user_id uuid REFERENCES users(uuid),
    mode                text NOT NULL DEFAULT 'VIDEO',
    status              text NOT NULL DEFAULT 'SCHEDULED',
    outcome             text,
    notes               text,
    created_by          uuid NOT NULL REFERENCES users(uuid),
    created_at          timestamp NOT NULL DEFAULT current_timestamp,
    updated_at          timestamp,
    CONSTRAINT hr_rec_int_mode_chk    CHECK (mode IN ('PHONE','VIDEO','ONSITE')),
    CONSTRAINT hr_rec_int_status_chk  CHECK (status IN ('SCHEDULED','COMPLETED','CANCELLED')),
    CONSTRAINT hr_rec_int_outcome_chk CHECK (outcome IS NULL OR outcome IN ('PASS','FAIL','UNDECIDED'))
);
CREATE INDEX hr_rec_int_org_app_idx ON hr_recruitment_interviews(organization_id, application_id);
