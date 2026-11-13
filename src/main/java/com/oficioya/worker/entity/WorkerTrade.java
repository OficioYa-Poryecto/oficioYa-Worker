package com.oficioya.worker.entity;

import com.oficioya.worker.model.RateUnit;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "worker_trades")
@Getter
@Setter
@NoArgsConstructor
public class WorkerTrade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @ManyToOne
    @JoinColumn(name = "trade_id", nullable = false)
    private Trade trade;

    // true = oficio principal; el mapper/service separa principal y secundarios
    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "approx_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal approxRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_unit", nullable = false, length = 10)
    private RateUnit rateUnit;
}