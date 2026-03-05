package com.basebox.ridelite.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Getter@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "drivers", indexes = {
    @Index(name = "idx_driver_availability", columnList = "is_available")
})
public class Driver {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull(message = "User is required")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = false;

    @Column(name = "vehicle_type", length = 50)
    private String vehicleType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Relationship with trips
    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Trip> trips = new ArrayList<>();

    @Version
    @Column(name = "version")
    private Long version; // Optimistic locking for availability updates
}
