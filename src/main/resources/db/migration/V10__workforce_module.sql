-- Flyway Migration V10: Workforce Management

CREATE TABLE workforce_shifts (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE workforce_schedules (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    employee_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    shift_id BIGINT REFERENCES workforce_shifts(id) ON DELETE SET NULL,
    scheduled_date DATE NOT NULL,
    notes VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (tenant_id, employee_id, scheduled_date)
);

CREATE TABLE workforce_time_entries (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    employee_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    clock_in TIMESTAMP NOT NULL,
    clock_out TIMESTAMP,
    notes VARCHAR(512)
);

CREATE TABLE workforce_leave_requests (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    employee_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    leave_type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_workforce_schedules_tenant_date ON workforce_schedules(tenant_id, scheduled_date);
