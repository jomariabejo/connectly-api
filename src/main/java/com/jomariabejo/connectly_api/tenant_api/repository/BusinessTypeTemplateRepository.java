package com.jomariabejo.connectly_api.tenant_api.repository;

import com.jomariabejo.connectly_api.tenant_api.entity.BusinessTypeTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusinessTypeTemplateRepository extends JpaRepository<BusinessTypeTemplate, Long> {
    Optional<BusinessTypeTemplate> findByBusinessType(String businessType);
}
