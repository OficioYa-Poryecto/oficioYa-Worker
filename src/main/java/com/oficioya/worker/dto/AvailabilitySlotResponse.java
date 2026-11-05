package com.oficioya.worker.dto;

import com.oficioya.worker.model.RecurrenceType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public record AvailabilitySlotResponse(Long id, RecurrenceType recurrenceType, DayOfWeek dayOfWeek,
                                       LocalDate startDate, LocalDate endDate,
                                       LocalTime startTime, LocalTime endTime) {
}
