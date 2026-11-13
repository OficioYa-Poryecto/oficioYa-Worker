package com.oficioya.worker.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "trade_categories")
@Getter
@Setter
@NoArgsConstructor
public class TradeCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(nullable = false, length = 60)
    private String name;

    @Column(length = 200)
    private String detail;
}