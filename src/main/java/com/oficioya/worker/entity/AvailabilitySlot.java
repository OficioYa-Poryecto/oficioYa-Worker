package com.oficioya.worker.entity;

import com.oficioya.worker.model.RecurrenceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "availability_slots")
@Getter
@Setter
@NoArgsConstructor
public class AvailabilitySlot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", nullable = false, length = 10)
    private RecurrenceType recurrenceType;

    // Solo para disponibilidad repetitiva (puede ser null si es fecha fija)
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
}