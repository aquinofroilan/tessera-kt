-- CRM activity log.
-- Calls, emails, meetings, notes, and tasks logged against any combination
-- of lead / opportunity / contact / customer. At least one related entity
-- is required so unattached activities don't accumulate as orphan rows.
-- TASK-type activities additionally carry a due_at, a completed flag, and
-- completed_at/by audit fields.

CREATE TABLE crm_activities (
    id                       uuid PRIMARY KEY,
    organization_id          uuid NOT NULL REFERENCES organizations(uuid),
    type                     text NOT NULL,
    subject                  text NOT NULL,
    body                     text,
    related_lead_id          uuid REFERENCES crm_leads(id) ON DELETE CASCADE,
    related_opportunity_id   uuid REFERENCES crm_opportunities(id) ON DELETE CASCADE,
    related_contact_id       uuid REFERENCES crm_contacts(id) ON DELETE SET NULL,
    related_customer_id      uuid REFERENCES customers(id) ON DELETE SET NULL,
    owner_user_id            uuid REFERENCES users(uuid),
    occurred_at              timestamp NOT NULL DEFAULT current_timestamp,
    due_at                   timestamp,
    completed                boolean NOT NULL DEFAULT false,
    completed_at             timestamp,
    completed_by             uuid REFERENCES users(uuid),
    created_by               uuid NOT NULL REFERENCES users(uuid),
    created_at               timestamp NOT NULL DEFAULT current_timestamp,
    updated_at               timestamp,
    CONSTRAINT crm_activities_type_chk
        CHECK (type IN ('CALL','EMAIL','MEETING','NOTE','TASK')),
    CONSTRAINT crm_activities_related_chk CHECK (
        related_lead_id IS NOT NULL OR
        related_opportunity_id IS NOT NULL OR
        related_contact_id IS NOT NULL OR
        related_customer_id IS NOT NULL
    )
);
CREATE INDEX crm_activities_org_lead_idx        ON crm_activities(organization_id, related_lead_id);
CREATE INDEX crm_activities_org_opportunity_idx ON crm_activities(organization_id, related_opportunity_id);
CREATE INDEX crm_activities_org_contact_idx     ON crm_activities(organization_id, related_contact_id);
CREATE INDEX crm_activities_org_customer_idx    ON crm_activities(organization_id, related_customer_id);
CREATE INDEX crm_activities_org_owner_idx       ON crm_activities(organization_id, owner_user_id);
CREATE INDEX crm_activities_org_due_open_idx
    ON crm_activities(organization_id, due_at)
    WHERE completed = false AND due_at IS NOT NULL;
