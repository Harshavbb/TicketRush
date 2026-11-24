package com.example.demo.service;



import com.example.demo.DTO.BookingHoldRequest;
import com.example.demo.DTO.BookingHoldResponse;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.BookingSeatRepository;
import com.example.demo.repository.ShowSeatRepository;

import com.example.demo.model.*;
import com.example.demo.utils.SeatUnavailableException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private static final Duration HOLD_DURATION = Duration.ofMinutes(7);

    private final BookingRepository bookingRepository;
    private final ShowSeatRepository showSeatRepository;
    private final BookingSeatRepository bookingSeatRepository;

    public BookingService(BookingRepository bookingRepository,
                          ShowSeatRepository showSeatRepository,
                          BookingSeatRepository bookingSeatRepository) {
        this.bookingRepository = bookingRepository;
        this.showSeatRepository = showSeatRepository;
        this.bookingSeatRepository = bookingSeatRepository;
    }

    @Transactional
    public BookingHoldResponse holdSeats(Long userId, String idempotencyKey, BookingHoldRequest request) {
        // 1. Idempotency check for client retries
        var existing = bookingRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        // 2. Sort & deduplicate IDs ascending to guarantee deadlock-free InnoDB row locking
        List<Long> sortedSeatIds = request.showSeatIds().stream()
                .distinct()
                .sorted()
                .toList();

        if (sortedSeatIds.isEmpty()) {
            throw new IllegalArgumentException("At least one seat must be selected.");
        }

        // 3. Fetch authoritative prices from DB (never trust prices from frontend)
        List<ShowSeat> seats = showSeatRepository
                .findByShowIdAndShowSeatIdInOrderByShowSeatIdAsc(request.showId(), sortedSeatIds);

        if (seats.size() != sortedSeatIds.size()) {
            throw new SeatUnavailableException("One or more selected seats do not exist for this show.");
        }

        BigDecimal totalAmount = seats.stream()
                .map(ShowSeat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(HOLD_DURATION);

        // 4. Insert PENDING booking envelope
        Booking booking = new Booking();
        booking.setBookingReference("TR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        booking.setUserId(userId);
        booking.setShowId(request.showId());
        booking.setIdempotencyKey(idempotencyKey);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(totalAmount);
        booking.setExpiresAt(expiresAt);

        try {
            booking = bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException ex) {
            // Handles two identical Idempotency-Key requests arriving at the exact same millisecond
            return bookingRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                    .map(this::toResponse)
                    .orElseThrow(() -> ex);
        }

        // 5. Atomically claim seats; lazily reclaims expired LOCKED seats
        int lockedCount = showSeatRepository.lockSeatsAtomically(
                request.showId(), sortedSeatIds, booking.getBookingId(), expiresAt, now
        );

        if (lockedCount != sortedSeatIds.size()) {
            throw new SeatUnavailableException("One or more seats were just taken by another user.");
        }

        // 6. Save permanent order line items
        Long finalBookingId = booking.getBookingId();
        List<BookingSeat> lineItems = seats.stream()
                .map(s -> new BookingSeat(finalBookingId, s.getShowSeatId(), s.getPrice()))
                .toList();
        bookingSeatRepository.saveAll(lineItems);

        return new BookingHoldResponse(
                booking.getBookingId(),
                booking.getBookingReference(),
                booking.getStatus().name(),
                booking.getTotalAmount(),
                booking.getExpiresAt(),
                sortedSeatIds
        );
    }

    private BookingHoldResponse toResponse(Booking booking) {
        List<Long> seatIds = bookingSeatRepository.findByBookingId(booking.getBookingId()).stream()
                .map(BookingSeat::getShowSeatId)
                .toList();
        return new BookingHoldResponse(
                booking.getBookingId(),
                booking.getBookingReference(),
                booking.getStatus().name(),
                booking.getTotalAmount(),
                booking.getExpiresAt(),
                seatIds
        );
    }
}
