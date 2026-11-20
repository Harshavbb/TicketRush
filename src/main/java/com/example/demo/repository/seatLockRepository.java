package com.example.demo.repository;

import com.example.demo.model.seats;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface seatLockRepository extends JpaRepository<seats, Long> {

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT s from seats s WHERE s.seat_id IN :ids")
        List<seats> findAllByIdwithLock(@Param("ids") List<Long> ids);

}
