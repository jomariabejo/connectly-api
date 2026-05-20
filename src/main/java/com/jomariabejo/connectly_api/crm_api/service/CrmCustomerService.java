package com.jomariabejo.connectly_api.crm_api.service;

import com.jomariabejo.connectly_api.crm_api.dto.*;
import com.jomariabejo.connectly_api.crm_api.entity.CrmCustomer;
import com.jomariabejo.connectly_api.crm_api.entity.CrmCustomerInteraction;
import com.jomariabejo.connectly_api.crm_api.entity.CrmCustomerNote;
import com.jomariabejo.connectly_api.crm_api.entity.CrmCustomerStatus;
import com.jomariabejo.connectly_api.crm_api.exception.CrmCustomerNotFoundException;
import com.jomariabejo.connectly_api.crm_api.repository.CrmCustomerInteractionRepository;
import com.jomariabejo.connectly_api.crm_api.repository.CrmCustomerNoteRepository;
import com.jomariabejo.connectly_api.crm_api.repository.CrmCustomerRepository;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import com.jomariabejo.connectly_api.tenant_api.service.TenantContextService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CrmCustomerService {
    private final CrmCustomerRepository customerRepository;
    private final CrmCustomerNoteRepository noteRepository;
    private final CrmCustomerInteractionRepository interactionRepository;
    private final TenantContextService tenantContextService;

    public CrmCustomerService(
            CrmCustomerRepository customerRepository,
            CrmCustomerNoteRepository noteRepository,
            CrmCustomerInteractionRepository interactionRepository,
            TenantContextService tenantContextService) {
        this.customerRepository = customerRepository;
        this.noteRepository = noteRepository;
        this.interactionRepository = interactionRepository;
        this.tenantContextService = tenantContextService;
    }

    @Transactional(readOnly = true)
    public Page<CrmCustomerDto> listCustomers(int page, int size, String status) {
        Long tenantId = tenantContextService.requireTenantId();
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CrmCustomer> result;
        if (status != null && !status.isBlank()) {
            result = customerRepository.findByTenantIdAndStatus(tenantId, CrmCustomerStatus.valueOf(status.toUpperCase()), pageable);
        } else {
            result = customerRepository.findByTenantId(tenantId, pageable);
        }
        return result.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public CrmCustomerDto getCustomer(Long id) {
        return toDto(requireCustomer(id));
    }

    @Transactional
    public CrmCustomerDto createCustomer(CreateCrmCustomerRequest request) {
        Tenant tenant = tenantContextService.requireTenant();
        CrmCustomer customer = CrmCustomer.builder()
                .tenant(tenant)
                .email(request.getEmail())
                .phone(request.getPhone())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .company(request.getCompany())
                .status(request.getStatus() != null ? request.getStatus() : CrmCustomerStatus.ACTIVE)
                .build();
        return toDto(customerRepository.save(customer));
    }

    @Transactional
    public CrmCustomerDto updateCustomer(Long id, UpdateCrmCustomerRequest request) {
        CrmCustomer customer = requireCustomer(id);
        if (request.getEmail() != null) customer.setEmail(request.getEmail());
        if (request.getPhone() != null) customer.setPhone(request.getPhone());
        if (request.getFirstName() != null) customer.setFirstName(request.getFirstName());
        if (request.getLastName() != null) customer.setLastName(request.getLastName());
        if (request.getCompany() != null) customer.setCompany(request.getCompany());
        if (request.getStatus() != null) customer.setStatus(request.getStatus());
        return toDto(customerRepository.save(customer));
    }

    @Transactional
    public void deleteCustomer(Long id) {
        CrmCustomer customer = requireCustomer(id);
        customerRepository.delete(customer);
    }

    @Transactional(readOnly = true)
    public List<CrmCustomerNoteDto> getNotes(Long customerId) {
        requireCustomer(customerId);
        return noteRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::toNoteDto)
                .toList();
    }

    @Transactional
    public CrmCustomerNoteDto addNote(Long customerId, CreateCrmNoteRequest request, User author) {
        CrmCustomer customer = requireCustomer(customerId);
        CrmCustomerNote note = CrmCustomerNote.builder()
                .customer(customer)
                .author(author)
                .content(request.getContent())
                .build();
        return toNoteDto(noteRepository.save(note));
    }

    @Transactional(readOnly = true)
    public List<CrmInteractionDto> getInteractions(Long customerId) {
        requireCustomer(customerId);
        return interactionRepository.findByCustomerIdOrderByOccurredAtDesc(customerId).stream()
                .map(this::toInteractionDto)
                .toList();
    }

    @Transactional
    public CrmInteractionDto addInteraction(Long customerId, CreateCrmInteractionRequest request, User author) {
        CrmCustomer customer = requireCustomer(customerId);
        CrmCustomerInteraction interaction = CrmCustomerInteraction.builder()
                .customer(customer)
                .interactionType(request.getInteractionType())
                .subject(request.getSubject())
                .description(request.getDescription())
                .occurredAt(request.getOccurredAt() != null ? request.getOccurredAt() : java.time.LocalDateTime.now())
                .createdBy(author)
                .build();
        return toInteractionDto(interactionRepository.save(interaction));
    }

    public long countCustomers(Long tenantId) {
        return customerRepository.countByTenantId(tenantId);
    }

    private CrmCustomer requireCustomer(Long id) {
        Long tenantId = tenantContextService.requireTenantId();
        CrmCustomer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CrmCustomerNotFoundException(id));
        if (!customer.getTenant().getId().equals(tenantId)) {
            throw new CrmCustomerNotFoundException(id);
        }
        return customer;
    }

    private CrmCustomerDto toDto(CrmCustomer c) {
        return CrmCustomerDto.builder()
                .id(c.getId())
                .email(c.getEmail())
                .phone(c.getPhone())
                .firstName(c.getFirstName())
                .lastName(c.getLastName())
                .company(c.getCompany())
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private CrmCustomerNoteDto toNoteDto(CrmCustomerNote n) {
        return CrmCustomerNoteDto.builder()
                .id(n.getId())
                .customerId(n.getCustomer().getId())
                .content(n.getContent())
                .authorEmail(n.getAuthor() != null ? n.getAuthor().getEmail() : null)
                .createdAt(n.getCreatedAt())
                .build();
    }

    private CrmInteractionDto toInteractionDto(CrmCustomerInteraction i) {
        return CrmInteractionDto.builder()
                .id(i.getId())
                .customerId(i.getCustomer().getId())
                .interactionType(i.getInteractionType())
                .subject(i.getSubject())
                .description(i.getDescription())
                .occurredAt(i.getOccurredAt())
                .build();
    }
}
