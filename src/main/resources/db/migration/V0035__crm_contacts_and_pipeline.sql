-- CRM foundation: contacts and sales-pipeline stages.
-- Contacts are people we sell to or talk to. They optionally belong to a
-- Customer (the billing entity) -- this lets us track multiple contacts for
-- one customer organisation (buyer, AP clerk, technical lead, etc.) without
-- forcing every conversation through the Customer record.
-- Pipeline stages are an org-defined ordered catalog (Prospect, Qualified,
-- Proposal, Won, Lost...). Opportunities reference them in a later stack.

CREATE TABLE crm_contacts (
    id                uuid PRIMARY KEY,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    customer_id       uuid REFERENCES customers(id) ON DELETE SET NULL,
    first_name        text NOT NULL,
    last_name         text NOT NULL,
    email             text,
    phone             text,
    job_title         text,
    department        text,
    notes             text,
    is_active         boolean NOT NULL DEFAULT true,
    created_by        uuid NOT NULL REFERENCES users(uuid),
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    updated_at        timestamp,
    CONSTRAINT crm_contacts_name_chk CHECK (length(first_name) > 0 OR length(last_name) > 0)
);
CREATE INDEX crm_contacts_org_customer_idx ON crm_contacts(organization_id, customer_id);
CREATE INDEX crm_contacts_org_active_idx   ON crm_contacts(organization_id, is_active);
CREATE INDEX crm_contacts_org_email_idx    ON crm_contacts(organization_id, lower(email));

CREATE TABLE crm_pipeline_stages (
    id                uuid PRIMARY KEY,
    organization_id   uuid NOT NULL REFERENCES organizations(uuid),
    code              text NOT NULL,
    name              text NOT NULL,
    description       text,
    sort_order        int  NOT NULL DEFAULT 0,
    probability_pct   numeric(5,2) NOT NULL DEFAULT 0,
    is_won            boolean NOT NULL DEFAULT false,
    is_lost           boolean NOT NULL DEFAULT false,
    is_active         boolean NOT NULL DEFAULT true,
    created_at        timestamp NOT NULL DEFAULT current_timestamp,
    updated_at        timestamp,
    CONSTRAINT crm_pipeline_probability_chk CHECK (probability_pct >= 0 AND probability_pct <= 100),
    CONSTRAINT crm_pipeline_terminal_chk    CHECK (NOT (is_won AND is_lost)),
    CONSTRAINT crm_pipeline_unique_code_per_org UNIQUE (organization_id, code)
);
CREATE INDEX crm_pipeline_org_sort_idx ON crm_pipeline_stages(organization_id, sort_order);
