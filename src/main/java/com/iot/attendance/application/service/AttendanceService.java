package com.iot.attendance.application.service;

import com.iot.attendance.application.dto.request.RfidAttendanceRequest;
import com.iot.attendance.application.dto.response.AttendanceResponse;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {

    AttendanceResponse recordCheckIn(RfidAttendanceRequest request);

    AttendanceResponse recordCheckOut(RfidAttendanceRequest request);

    AttendanceResponse getAttendanceById(Long id);

    List<AttendanceResponse> getAttendanceByDate(LocalDate date);

    List<AttendanceResponse> getAttendanceByWorkerAndDateRange(
            Long workerId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<AttendanceResponse> getLateAttendancesByDate(LocalDate date);

    AttendanceResponse getActiveAttendanceByWorker(Long workerId);

    long countLateAttendances(Long workerId, LocalDate startDate, LocalDate endDate);
}
