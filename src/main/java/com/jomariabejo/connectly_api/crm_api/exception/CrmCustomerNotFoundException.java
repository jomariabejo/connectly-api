package com.jomariabejo.connectly_api.crm_api.exception;

public class CrmCustomerNotFoundException extends RuntimeException {
    public CrmCustomerNotFoundException(Long id) {
        super("CRM customer not found: " + id);
    }
}
