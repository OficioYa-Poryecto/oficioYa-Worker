package com.oficioya.worker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "work_photos")
@Getter
@Setter
@NoArgsConstructor
public class WorkPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @NotBlank
    @Column(nullable = false, length = 500)
    private String url;

    @Column(length = 200)
    private String description;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
}