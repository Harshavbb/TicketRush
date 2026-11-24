package com.example.demo.model;



import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "booking_seat")
public class BookingSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_seat_id")
    private Long bookingSeatId;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "show_seat_id", nullable = false)
    private Long showSeatId;

    @Column(name = "price_at_booking", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtBooking;

    public BookingSeat() {}

    public BookingSeat(Long bookingId, Long showSeatId, BigDecimal priceAtBooking) {
        this.bookingId = bookingId;
        this.showSeatId = showSeatId;
        this.priceAtBooking = priceAtBooking;
    }

    public Long getBookingSeatId() { return bookingSeatId; }
    public Long getBookingId() { return bookingId; }
    public Long getShowSeatId() { return showSeatId; }
    public BigDecimal getPriceAtBooking() { return priceAtBooking; }
}
