package com.example.demo;

import com.example.demo.DTO.BookingHoldRequest;
import com.example.demo.service.BookingService;
import com.example.demo.utils.SeatUnavailableException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class BookingConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Test
    void fiftyConcurrentUsersCompetingForSameSeat_onlyOneSucceeds() throws InterruptedException {
        int threadCount = 50;
        Long contestedShowSeatId = 1L;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger unexpectedErrorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final long userId = 1000L + i;
            final String idempotencyKey = "stress-test-" + UUID.randomUUID();

            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    // All 50 threads pause here until startGun fires simultaneously
                    startGun.await();
                    bookingService.holdSeats(
                            userId,
                            idempotencyKey,
                            new BookingHoldRequest(userId, 1L, List.of(contestedShowSeatId))
                    );
                    successCount.incrementAndGet();
                } catch (SeatUnavailableException ex) {
                    conflictCount.incrementAndGet();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    unexpectedErrorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(); // Wait until all 50 threads are lined up
        startGun.countDown(); // Fire!
        doneLatch.await(); // Wait for all 50 transactions to finish
        executor.shutdown();

        System.out.printf("Results -> Winner: %d | Rejected (409): %d | Errors: %d%n",
                successCount.get(), conflictCount.get(), unexpectedErrorCount.get());

        assertEquals(1, successCount.get(), "Exactly 1 user must win the seat");
        assertEquals(49, conflictCount.get(), "The other 49 users must fail cleanly with SeatUnavailableException");
        assertEquals(0, unexpectedErrorCount.get(), "Zero deadlocks or connection timeouts allowed");
    }
}