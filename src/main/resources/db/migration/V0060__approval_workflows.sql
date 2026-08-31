CREATE TABLE approval_workflows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(uuid) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    entity_type VARCHAR(100) NOT NULL, -- e.g., 'PURCHASE_ORDER', 'EXPENSE', 'LEAVE_REQUEST'
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(uuid),
    updated_by UUID REFERENCES users(uuid)
);

CREATE TABLE approval_workflow_steps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL REFERENCES approval_workflows(id) ON DELETE CASCADE,
    step_order INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    required_approvals INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(workflow_id, step_order)
);

CREATE TABLE workflow_step_approvers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    step_id UUID NOT NULL REFERENCES approval_workflow_steps(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(uuid) ON DELETE CASCADE,
    role_id UUID REFERENCES roles(uuid) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CHECK (user_id IS NOT NULL OR role_id IS NOT NULL)
);

CREATE TABLE approval_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(uuid) ON DELETE CASCADE,
    workflow_id UUID NOT NULL REFERENCES approval_workflows(id),
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED, CANCELLED
    current_step_order INTEGER NOT NULL DEFAULT 1,
    requester_id UUID NOT NULL REFERENCES users(uuid),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_approval_requests_entity ON approval_requests(entity_type, entity_id);

CREATE TABLE approval_actions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID NOT NULL REFERENCES approval_requests(id) ON DELETE CASCADE,
    step_order INTEGER NOT NULL,
    actor_id UUID NOT NULL REFERENCES users(uuid),
    action VARCHAR(50) NOT NULL, -- APPROVED, REJECTED
    comments TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
