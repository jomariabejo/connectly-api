package com.jomariabejo.connectly_api.workforce_api.controller;

import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.tenant_api.annotation.RequiresProduct;
import com.jomariabejo.connectly_api.tenant_api.entity.ProductCode;
import com.jomariabejo.connectly_api.workforce_api.dto.*;
import com.jomariabejo.connectly_api.workforce_api.entity.WorkforceLeaveRequest;
import com.jomariabejo.connectly_api.workforce_api.service.WorkforceService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/workforce")
@RequiresProduct(ProductCode.WORKFORCE)
public class WorkforceController {
    private final WorkforceService workforceService;
    private final AuthenticationService authenticationService;

    public WorkforceController(WorkforceService workforceService, AuthenticationService authenticationService) {
        this.workforceService = workforceService;
        this.authenticationService = authenticationService;
    }

    @GetMapping("/schedules")
    public ResponseEntity<List<WorkforceScheduleDto>> getSchedules(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(workforceService.getSchedules(from, to));
    }

    @PostMapping("/schedules")
    public ResponseEntity<WorkforceScheduleDto> createSchedule(@RequestBody @Valid CreateScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workforceService.createSchedule(request));
    }

    @PostMapping("/time/clock-in")
    public ResponseEntity<WorkforceTimeEntryDto> clockIn() {
        User user = authenticationService.getAuthenticatedUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(workforceService.clockIn(user));
    }

    @PostMapping("/time/clock-out")
    public ResponseEntity<WorkforceTimeEntryDto> clockOut() {
        User user = authenticationService.getAuthenticatedUser();
        return ResponseEntity.ok(workforceService.clockOut(user));
    }

    @PostMapping("/leave")
    public ResponseEntity<WorkforceLeaveRequest> createLeave(@RequestBody @Valid CreateLeaveRequest request) {
        User user = authenticationService.getAuthenticatedUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(workforceService.createLeave(request, user));
    }
}
