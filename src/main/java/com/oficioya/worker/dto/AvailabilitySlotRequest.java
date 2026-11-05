package com.oficioya.worker.dto;

import com.oficioya.worker.model.RecurrenceType;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

// dayOfWeek y endDate solo aplican a RECURRING; el service valida esa combinación
public record AvailabilitySlotRequest(
        @NotNull RecurrenceType recurrenceType,
        DayOfWeek dayOfWeek,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime) {
}
