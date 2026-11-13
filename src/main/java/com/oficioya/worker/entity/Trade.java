package com.oficioya.worker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "trades")
@Getter
@Setter
@NoArgsConstructor
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(nullable = false, length = 60)
    private String name;

    @Column(length = 200)
    private String description;

    @Column(nullable = false)
    private boolean active;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private TradeCategory category;
}