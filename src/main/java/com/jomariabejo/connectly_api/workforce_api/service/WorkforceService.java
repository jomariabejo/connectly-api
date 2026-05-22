package com.jomariabejo.connectly_api.workforce_api.service;

import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.repository.UserRepository;
import com.jomariabejo.connectly_api.tenant_api.entity.Tenant;
import com.jomariabejo.connectly_api.tenant_api.service.TenantContextService;
import com.jomariabejo.connectly_api.workforce_api.dto.*;
import com.jomariabejo.connectly_api.workforce_api.entity.*;
import com.jomariabejo.connectly_api.workforce_api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WorkforceService {
    private final WorkforceScheduleRepository scheduleRepository;
    private final WorkforceTimeEntryRepository timeEntryRepository;
    private final WorkforceLeaveRequestRepository leaveRepository;
    private final WorkforceShiftRepository shiftRepository;
    private final UserRepository userRepository;
    private final TenantContextService tenantContextService;

    public WorkforceService(
            WorkforceScheduleRepository scheduleRepository,
            WorkforceTimeEntryRepository timeEntryRepository,
            WorkforceLeaveRequestRepository leaveRepository,
            WorkforceShiftRepository shiftRepository,
            UserRepository userRepository,
            TenantContextService tenantContextService) {
        this.scheduleRepository = scheduleRepository;
        this.timeEntryRepository = timeEntryRepository;
        this.leaveRepository = leaveRepository;
        this.shiftRepository = shiftRepository;
        this.userRepository = userRepository;
        this.tenantContextService = tenantContextService;
    }

    @Transactional(readOnly = true)
    public List<WorkforceScheduleDto> getSchedules(LocalDate from, LocalDate to) {
        Long tenantId = tenantContextService.requireTenantId();
        return scheduleRepository.findByTenantIdAndScheduledDateBetween(tenantId, from, to).stream()
                .map(this::toScheduleDto)
                .toList();
    }

    @Transactional
    public WorkforceScheduleDto createSchedule(CreateScheduleRequest request) {
        Tenant tenant = tenantContextService.requireTenant();
        User employee = userRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        WorkforceShift shift = request.getShiftId() != null
                ? shiftRepository.findById(request.getShiftId()).orElse(null) : null;
        WorkforceSchedule schedule = WorkforceSchedule.builder()
                .tenant(tenant)
                .employee(employee)
                .shift(shift)
                .scheduledDate(request.getScheduledDate())
                .notes(request.getNotes())
                .build();
        return toScheduleDto(scheduleRepository.save(schedule));
    }

    @Transactional
    public WorkforceTimeEntryDto clockIn(User employee) {
        Long tenantId = tenantContextService.requireTenantId();
        timeEntryRepository.findFirstByTenantIdAndEmployeeIdAndClockOutIsNullOrderByClockInDesc(tenantId, employee.getId())
                .ifPresent(e -> { throw new IllegalStateException("Already clocked in"); });
        Tenant tenant = tenantContextService.requireTenant();
        WorkforceTimeEntry entry = WorkforceTimeEntry.builder()
                .tenant(tenant)
                .employee(employee)
                .clockIn(LocalDateTime.now())
                .build();
        return toTimeDto(timeEntryRepository.save(entry));
    }

    @Transactional
    public WorkforceTimeEntryDto clockOut(User employee) {
        Long tenantId = tenantContextService.requireTenantId();
        WorkforceTimeEntry entry = timeEntryRepository
                .findFirstByTenantIdAndEmployeeIdAndClockOutIsNullOrderByClockInDesc(tenantId, employee.getId())
                .orElseThrow(() -> new IllegalStateException("No active clock-in found"));
        entry.setClockOut(LocalDateTime.now());
        return toTimeDto(timeEntryRepository.save(entry));
    }

    @Transactional
    public WorkforceLeaveRequest createLeave(CreateLeaveRequest request, User employee) {
        Tenant tenant = tenantContextService.requireTenant();
        WorkforceLeaveRequest leave = WorkforceLeaveRequest.builder()
                .tenant(tenant)
                .employee(employee)
                .leaveType(request.getLeaveType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reason(request.getReason())
                .build();
        return leaveRepository.save(leave);
    }

    public long countSchedules(Long tenantId) {
        return scheduleRepository.countByTenantId(tenantId);
    }

    public long countPendingLeave(Long tenantId) {
        return leaveRepository.countByTenantIdAndStatus(tenantId, LeaveStatus.PENDING);
    }

    private WorkforceScheduleDto toScheduleDto(WorkforceSchedule s) {
        return WorkforceScheduleDto.builder()
                .id(s.getId())
                .employeeId(s.getEmployee().getId())
                .employeeEmail(s.getEmployee().getEmail())
                .shiftId(s.getShift() != null ? s.getShift().getId() : null)
                .shiftName(s.getShift() != null ? s.getShift().getName() : null)
                .scheduledDate(s.getScheduledDate())
                .notes(s.getNotes())
                .build();
    }

    private WorkforceTimeEntryDto toTimeDto(WorkforceTimeEntry e) {
        return WorkforceTimeEntryDto.builder()
                .id(e.getId())
                .employeeId(e.getEmployee().getId())
                .clockIn(e.getClockIn())
                .clockOut(e.getClockOut())
                .notes(e.getNotes())
                .build();
    }
}
