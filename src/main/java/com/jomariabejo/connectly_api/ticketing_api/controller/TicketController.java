package com.jomariabejo.connectly_api.ticketing_api.controller;

import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.ticketing_api.dto.*;
import com.jomariabejo.connectly_api.ticketing_api.service.TicketService;
import com.jomariabejo.connectly_api.tenant_api.annotation.RequiresProduct;
import com.jomariabejo.connectly_api.tenant_api.entity.ProductCode;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/tickets")
@RequiresProduct(ProductCode.TICKETING)
public class TicketController {
    private final TicketService ticketService;
    private final AuthenticationService authenticationService;

    public TicketController(TicketService ticketService, AuthenticationService authenticationService) {
        this.ticketService = ticketService;
        this.authenticationService = authenticationService;
    }

    @GetMapping
    public ResponseEntity<Page<TicketDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ticketService.listTickets(page, size, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicket(id));
    }

    @PostMapping
    public ResponseEntity<TicketDto> create(@RequestBody @Valid CreateTicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createTicket(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TicketDto> update(@PathVariable Long id, @RequestBody UpdateTicketRequest request) {
        return ResponseEntity.ok(ticketService.updateTicket(id, request));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<TicketCommentDto>> getComments(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getComments(id));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<TicketCommentDto> addComment(@PathVariable Long id, @RequestBody @Valid CreateTicketCommentRequest request) {
        User user = authenticationService.getAuthenticatedUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.addComment(id, request, user));
    }
}
