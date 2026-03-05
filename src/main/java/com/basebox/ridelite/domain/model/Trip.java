package com.basebox.ridelite.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.basebox.ridelite.domain.enums.TripStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "trips", indexes = {
    @Index(name = "idx_trip_status", columnList = "status"),
    @Index(name = "idx_trip_client", columnList = "client_id"),
    @Index(name = "idx_trip_driver", columnList = "driver_id")
})
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull(message = "Client is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @NotNull(message = "Trip status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TripStatus status = TripStatus.REQUESTED;

    @DecimalMin(value = "0.0", inclusive = false, message = "Fare must be positive")
    @Column(name = "fare", precision = 10, scale = 2)
    private BigDecimal fare;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Relationship with payment
    @OneToOne(mappedBy = "trip", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Payment payment;

    @Version
    @Column(name = "version")
    private Long version; // Optimistic locking for state transitions
}
