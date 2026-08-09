-- CRM leads and opportunities.
-- Leads are unqualified prospects -- typically arriving via marketing, an
-- inbound form, or a list import -- that don't yet have a Customer record.
-- Once a lead is QUALIFIED it is CONVERTED into an Opportunity (and
-- typically into a Customer + Contact), at which point lead.status flips
-- and lead.converted_to_opportunity_id back-references the new opportunity.
--
-- Opportunities are active deals in the pipeline. They reference a
-- Customer (the eventual billing target) and optionally a primary Contact;
-- their position in the funnel is determined by stage_id, and their value
-- by (amount, currency). status OPEN advances by stage_id; closing the
-- opportunity stamps WON or LOST and freezes the amount.

CREATE TABLE crm_leads (
    id                          uuid PRIMARY KEY,
    organization_id             uuid NOT NULL REFERENCES organizations(uuid),
    full_name                   text NOT NULL,
    company                     text,
    email                       text,
    phone                       text,
    source                      text,
    status                      text NOT NULL DEFAULT 'NEW',
    owner_user_id               uuid REFERENCES users(uuid),
    notes                       text,
    converted_to_opportunity_id uuid,
    converted_at                timestamp,
    created_by                  uuid NOT NULL REFERENCES users(uuid),
    created_at                  timestamp NOT NULL DEFAULT current_timestamp,
    updated_at                  timestamp,
    CONSTRAINT crm_leads_status_chk CHECK (status IN ('NEW','QUALIFIED','CONVERTED','DISQUALIFIED'))
);
CREATE INDEX crm_leads_org_status_idx ON crm_leads(organization_id, status);
CREATE INDEX crm_leads_org_owner_idx  ON crm_leads(organization_id, owner_user_id);

CREATE TABLE crm_opportunities (
    id                  uuid PRIMARY KEY,
    organization_id     uuid NOT NULL REFERENCES organizations(uuid),
    name                text NOT NULL,
    customer_id         uuid NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    primary_contact_id  uuid REFERENCES crm_contacts(id) ON DELETE SET NULL,
    stage_id            uuid NOT NULL REFERENCES crm_pipeline_stages(id) ON DELETE RESTRICT,
    amount              numeric(18,4) NOT NULL DEFAULT 0,
    currency            char(3) NOT NULL,
    expected_close_date date,
    status              text NOT NULL DEFAULT 'OPEN',
    owner_user_id       uuid REFERENCES users(uuid),
    source_lead_id      uuid REFERENCES crm_leads(id) ON DELETE SET NULL,
    notes               text,
    closed_at           timestamp,
    closed_by           uuid REFERENCES users(uuid),
    created_by          uuid NOT NULL REFERENCES users(uuid),
    created_at          timestamp NOT NULL DEFAULT current_timestamp,
    updated_at          timestamp,
    CONSTRAINT crm_opp_status_chk CHECK (status IN ('OPEN','WON','LOST','ABANDONED')),
    CONSTRAINT crm_opp_amount_chk CHECK (amount >= 0)
);
CREATE INDEX crm_opp_org_status_idx   ON crm_opportunities(organization_id, status);
CREATE INDEX crm_opp_org_customer_idx ON crm_opportunities(organization_id, customer_id);
CREATE INDEX crm_opp_org_stage_idx    ON crm_opportunities(organization_id, stage_id);
CREATE INDEX crm_opp_org_owner_idx    ON crm_opportunities(organization_id, owner_user_id);

ALTER TABLE crm_leads
    ADD CONSTRAINT crm_leads_converted_opp_fk
    FOREIGN KEY (converted_to_opportunity_id)
    REFERENCES crm_opportunities(id) ON DELETE SET NULL;
