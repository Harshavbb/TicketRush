package com.example.demo.service;



import com.example.demo.model.BookingStatus;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.ShowSeatRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class HoldExpirationScheduler {

    private final BookingRepository bookingRepository;
    private final ShowSeatRepository showSeatRepository;

    public HoldExpirationScheduler(BookingRepository bookingRepository,
                                   ShowSeatRepository showSeatRepository) {
        this.bookingRepository = bookingRepository;
        this.showSeatRepository = showSeatRepository;
    }

    @Scheduled(fixedDelay = 15000)
    @Transactional
    public void releaseExpiredHolds() {
        List<Long> expiredIds = bookingRepository.findExpiredPendingBookingIds(
                BookingStatus.PENDING, Instant.now()
        );
        if (expiredIds.isEmpty()) {
            return;
        }
        bookingRepository.markBookingsExpired(expiredIds);
        showSeatRepository.releaseSeatsForExpiredBookings(expiredIds);
    }
}
