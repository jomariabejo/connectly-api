package com.jomariabejo.connectly_api.ticketing_api.service;

import com.jomariabejo.connectly_api.crm_api.entity.CrmCustomer;
import com.jomariabejo.connectly_api.crm_api.repository.CrmCustomerRepository;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.repository.UserRepository;
import com.jomariabejo.connectly_api.ticketing_api.dto.*;
import com.jomariabejo.connectly_api.ticketing_api.entity.*;
import com.jomariabejo.connectly_api.ticketing_api.exception.TicketNotFoundException;
import com.jomariabejo.connectly_api.ticketing_api.repository.TicketCategoryRepository;
import com.jomariabejo.connectly_api.ticketing_api.repository.TicketCommentRepository;
import com.jomariabejo.connectly_api.ticketing_api.repository.TicketRepository;
import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import com.jomariabejo.connectly_api.tenant_api.service.TenantContextService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TicketService {
    private final TicketRepository ticketRepository;
    private final TicketCategoryRepository categoryRepository;
    private final TicketCommentRepository commentRepository;
    private final CrmCustomerRepository crmCustomerRepository;
    private final UserRepository userRepository;
    private final TenantContextService tenantContextService;

    public TicketService(
            TicketRepository ticketRepository,
            TicketCategoryRepository categoryRepository,
            TicketCommentRepository commentRepository,
            CrmCustomerRepository crmCustomerRepository,
            UserRepository userRepository,
            TenantContextService tenantContextService) {
        this.ticketRepository = ticketRepository;
        this.categoryRepository = categoryRepository;
        this.commentRepository = commentRepository;
        this.crmCustomerRepository = crmCustomerRepository;
        this.userRepository = userRepository;
        this.tenantContextService = tenantContextService;
    }

    @Transactional(readOnly = true)
    public Page<TicketDto> listTickets(int page, int size, String status) {
        Long tenantId = tenantContextService.requireTenantId();
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Ticket> result = status != null && !status.isBlank()
                ? ticketRepository.findByTenantIdAndStatus(tenantId, TicketStatus.valueOf(status.toUpperCase()), pageable)
                : ticketRepository.findByTenantId(tenantId, pageable);
        return result.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public TicketDto getTicket(Long id) {
        return toDto(requireTicket(id));
    }

    @Transactional
    public TicketDto createTicket(CreateTicketRequest request) {
        Tenant tenant = tenantContextService.requireTenant();
        Ticket.TicketBuilder builder = Ticket.builder()
                .tenant(tenant)
                .subject(request.getSubject())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : TicketPriority.MEDIUM)
                .requesterEmail(request.getRequesterEmail());
        if (request.getCategoryId() != null) {
            builder.category(categoryRepository.findById(request.getCategoryId()).orElse(null));
        }
        if (request.getCustomerId() != null) {
            CrmCustomer customer = crmCustomerRepository.findById(request.getCustomerId()).orElse(null);
            if (customer != null && customer.getTenant().getId().equals(tenant.getId())) {
                builder.customer(customer);
            }
        }
        return toDto(ticketRepository.save(builder.build()));
    }

    @Transactional
    public TicketDto updateTicket(Long id, UpdateTicketRequest request) {
        Ticket ticket = requireTicket(id);
        if (request.getSubject() != null) ticket.setSubject(request.getSubject());
        if (request.getDescription() != null) ticket.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            ticket.setStatus(request.getStatus());
            if (request.getStatus() == TicketStatus.RESOLVED || request.getStatus() == TicketStatus.CLOSED) {
                ticket.setResolvedAt(LocalDateTime.now());
            }
        }
        if (request.getPriority() != null) ticket.setPriority(request.getPriority());
        if (request.getCategoryId() != null) {
            ticket.setCategory(categoryRepository.findById(request.getCategoryId()).orElse(null));
        }
        if (request.getAssigneeId() != null) {
            ticket.setAssignee(userRepository.findById(request.getAssigneeId()).orElse(null));
        }
        return toDto(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public List<TicketCommentDto> getComments(Long ticketId) {
        requireTicket(ticketId);
        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(this::toCommentDto)
                .toList();
    }

    @Transactional
    public TicketCommentDto addComment(Long ticketId, CreateTicketCommentRequest request, User author) {
        Ticket ticket = requireTicket(ticketId);
        TicketComment comment = TicketComment.builder()
                .ticket(ticket)
                .author(author)
                .body(request.getBody())
                .internalNote(request.getInternalNote() != null && request.getInternalNote())
                .build();
        return toCommentDto(commentRepository.save(comment));
    }

    public long countTickets(Long tenantId) {
        return ticketRepository.countByTenantId(tenantId);
    }

    public long countOpenTickets(Long tenantId) {
        return ticketRepository.countByTenantIdAndStatus(tenantId, TicketStatus.OPEN);
    }

    private Ticket requireTicket(Long id) {
        Long tenantId = tenantContextService.requireTenantId();
        Ticket ticket = ticketRepository.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
        if (!ticket.getTenant().getId().equals(tenantId)) {
            throw new TicketNotFoundException(id);
        }
        return ticket;
    }

    private TicketDto toDto(Ticket t) {
        return TicketDto.builder()
                .id(t.getId())
                .subject(t.getSubject())
                .description(t.getDescription())
                .status(t.getStatus())
                .priority(t.getPriority())
                .categoryId(t.getCategory() != null ? t.getCategory().getId() : null)
                .categoryName(t.getCategory() != null ? t.getCategory().getName() : null)
                .customerId(t.getCustomer() != null ? t.getCustomer().getId() : null)
                .assigneeId(t.getAssignee() != null ? t.getAssignee().getId() : null)
                .requesterEmail(t.getRequesterEmail())
                .dueAt(t.getDueAt())
                .createdAt(t.getCreatedAt())
                .build();
    }

    private TicketCommentDto toCommentDto(TicketComment c) {
        return TicketCommentDto.builder()
                .id(c.getId())
                .ticketId(c.getTicket().getId())
                .body(c.getBody())
                .internalNote(c.getInternalNote())
                .authorEmail(c.getAuthor() != null ? c.getAuthor().getEmail() : null)
                .createdAt(c.getCreatedAt())
                .build();
    }
}
