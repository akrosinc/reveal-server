ALTER TABLE plan_assignment
    add COLUMN IF NOT EXISTS selected boolean;
ALTER TABLE plan_assignment_aud
    add COLUMN IF NOT EXISTS selected boolean;