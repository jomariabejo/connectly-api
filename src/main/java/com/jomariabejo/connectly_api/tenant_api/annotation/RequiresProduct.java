package com.jomariabejo.connectly_api.tenant_api.annotation;

import com.jomariabejo.connectly_api.tenant_api.entity.ProductCode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresProduct {
    ProductCode value();
}
