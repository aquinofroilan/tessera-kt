-- Customer portal user mappings
CREATE TABLE IF NOT EXISTS customer_portal_users (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES organizations(uuid) ON DELETE CASCADE,
    customer_id uuid NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    user_id uuid NOT NULL REFERENCES users(uuid) ON DELETE CASCADE,
    is_primary boolean NOT NULL DEFAULT false,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_cust_portal_user UNIQUE (organization_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_cust_portal_users_cust ON customer_portal_users(organization_id, customer_id);
CREATE INDEX IF NOT EXISTS idx_cust_portal_users_user ON customer_portal_users(user_id);

-- Support tickets
CREATE TABLE IF NOT EXISTS support_tickets (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES organizations(uuid) ON DELETE CASCADE,
    ticket_number text NOT NULL,
    customer_id uuid NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    contact_id uuid REFERENCES crm_contacts(id) ON DELETE SET NULL,
    subject text NOT NULL,
    description text NOT NULL,
    status text NOT NULL DEFAULT 'OPEN',
    priority text NOT NULL DEFAULT 'MEDIUM',
    category text NOT NULL DEFAULT 'GENERAL_INQUIRY',
    assigned_to_user_id uuid REFERENCES users(uuid) ON DELETE SET NULL,
    created_by_user_id uuid NOT NULL REFERENCES users(uuid) ON DELETE CASCADE,
    resolved_at timestamp with time zone,
    closed_at timestamp with time zone,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_support_tickets_org_num UNIQUE (organization_id, ticket_number),
    CONSTRAINT chk_support_ticket_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'WAITING_FOR_CUSTOMER', 'RESOLVED', 'CLOSED')),
    CONSTRAINT chk_support_ticket_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT chk_support_ticket_category CHECK (category IN ('BILLING', 'ORDER_INQUIRY', 'TECHNICAL_SUPPORT', 'PRODUCT_DEFECT', 'GENERAL_INQUIRY', 'FEATURE_REQUEST', 'OTHER'))
);

CREATE INDEX IF NOT EXISTS idx_support_tickets_org ON support_tickets(organization_id);
CREATE INDEX IF NOT EXISTS idx_support_tickets_cust ON support_tickets(organization_id, customer_id);
CREATE INDEX IF NOT EXISTS idx_support_tickets_status ON support_tickets(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_support_tickets_assignee ON support_tickets(assigned_to_user_id);

-- Support ticket messages / comments
CREATE TABLE IF NOT EXISTS support_ticket_messages (
    id uuid PRIMARY KEY,
    organization_id uuid NOT NULL REFERENCES organizations(uuid) ON DELETE CASCADE,
    ticket_id uuid NOT NULL REFERENCES support_tickets(id) ON DELETE CASCADE,
    sender_id uuid NOT NULL REFERENCES users(uuid) ON DELETE CASCADE,
    sender_type text NOT NULL,
    message text NOT NULL,
    is_internal_note boolean NOT NULL DEFAULT false,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT chk_ticket_msg_sender_type CHECK (sender_type IN ('CUSTOMER', 'AGENT', 'SYSTEM'))
);

CREATE INDEX IF NOT EXISTS idx_support_ticket_msgs_ticket ON support_ticket_messages(ticket_id);
CREATE INDEX IF NOT EXISTS idx_support_ticket_msgs_org ON support_ticket_messages(organization_id);
