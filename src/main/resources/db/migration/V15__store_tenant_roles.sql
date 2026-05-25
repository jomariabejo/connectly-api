-- Store-facing tenant roles: migrate legacy STAFF/EMPLOYEE to MANAGER/CASHIER

UPDATE tenant_users SET tenant_role = 'MANAGER' WHERE tenant_role = 'STAFF';
UPDATE tenant_users SET tenant_role = 'CASHIER' WHERE tenant_role = 'EMPLOYEE';

UPDATE tenant_invitations SET tenant_role = 'MANAGER' WHERE tenant_role = 'STAFF';
UPDATE tenant_invitations SET tenant_role = 'CASHIER' WHERE tenant_role = 'EMPLOYEE';
