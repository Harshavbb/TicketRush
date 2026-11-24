package com.example.demo.model;



import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "show_seat")
public class ShowSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "show_seat_id")
    private Long showSeatId;

    @Column(name = "show_id", nullable = false)
    private Long showId;

    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ShowSeatStatus status;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "locked_by_booking_id")
    private Long lockedByBookingId;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    // Getters & Setters
    public Long getShowSeatId() { return showSeatId; }
    public Long getShowId() { return showId; }
    public Long getSeatId() { return seatId; }
    public ShowSeatStatus getStatus() { return status; }
    public BigDecimal getPrice() { return price; }
    public Long getLockedByBookingId() { return lockedByBookingId; }
    public Instant getLockedUntil() { return lockedUntil; }
}
