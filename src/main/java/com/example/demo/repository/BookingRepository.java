package com.example.demo.repository;




import com.example.demo.model.Booking;
import com.example.demo.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    @Query("SELECT b.bookingId FROM Booking b WHERE b.status = :status AND b.expiresAt <= :now")
    List<Long> findExpiredPendingBookingIds(@Param("status") BookingStatus status, @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Booking b SET b.status = 'EXPIRED' WHERE b.bookingId IN :bookingIds AND b.status = 'PENDING'")
    int markBookingsExpired(@Param("bookingIds") List<Long> bookingIds);
}
