package com.iot.attendanceaccesscontrolapi.domain.valueobjects;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalTime;

@Getter
@EqualsAndHashCode
@ToString
public class TimeRange {

    private final LocalTime startTime;
    private final LocalTime endTime;

    public TimeRange(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start and end time cannot be null");
        }
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static TimeRange of(LocalTime startTime, LocalTime endTime) {
        return new TimeRange(startTime, endTime);
    }

    public boolean contains(LocalTime time) {
        return !time.isBefore(startTime) && !time.isAfter(endTime);
    }
}
