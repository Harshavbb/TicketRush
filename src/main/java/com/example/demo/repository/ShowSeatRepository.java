package com.example.demo.repository;


import com.example.demo.model.ShowSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    List<ShowSeat> findByShowIdAndShowSeatIdInOrderByShowSeatIdAsc(Long showId, List<Long> showSeatIds);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE ShowSeat ss
        SET ss.status = 'LOCKED',
            ss.lockedByBookingId = :bookingId,
            ss.lockedUntil = :lockedUntil,
            ss.version = ss.version + 1
        WHERE ss.showId = :showId
          AND ss.showSeatId IN :showSeatIds
          AND (
              ss.status = 'AVAILABLE'
              OR (ss.status = 'LOCKED' AND ss.lockedUntil <= :now)
          )
        """)
    int lockSeatsAtomically(
            @Param("showId") Long showId,
            @Param("showSeatIds") List<Long> showSeatIds,
            @Param("bookingId") Long bookingId,
            @Param("lockedUntil") Instant lockedUntil,
            @Param("now") Instant now
    );

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE ShowSeat ss
        SET ss.status = 'AVAILABLE',
            ss.lockedByBookingId = NULL,
            ss.lockedUntil = NULL,
            ss.version = ss.version + 1
        WHERE ss.lockedByBookingId IN :bookingIds
          AND ss.status = 'LOCKED'
        """)
    int releaseSeatsForExpiredBookings(@Param("bookingIds") List<Long> bookingIds);
}
