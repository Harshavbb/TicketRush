
package com.example.demo.service;

import com.example.demo.DTO.SeatMapItem;
import com.example.demo.repository.ShowSeatRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class SeatMapService {

    private static final Duration CACHE_TTL = Duration.ofSeconds(5);

    private final ShowSeatRepository showSeatRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public SeatMapService(ShowSeatRepository showSeatRepository,
                          StringRedisTemplate redisTemplate,
                          ObjectMapper objectMapper) {
        this.showSeatRepository = showSeatRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public List<SeatMapItem> getSeatMap(Long showId) {
        String cacheKey = "seatmap:show:" + showId;

        // 1. Try reading from Redis first
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                return objectMapper.readValue(cachedJson, new TypeReference<>() {});
            }
        } catch (Exception ignored) {
            // Fail open: if Redis is unreachable, fall back to MySQL
        }

        // 2. Cache miss -> query MySQL
        List<SeatMapItem> seatMap = showSeatRepository.findSeatMapByShowId(showId, Instant.now())
                .stream()
                .map(row -> new SeatMapItem(
                        row.getShowSeatId(),
                        row.getSeatNumber(),
                        row.getSeatType(),
                        row.getPrice(),
                        row.getEffectiveStatus()
                ))
                .toList();

        // 3. Populate Redis with short TTL
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(seatMap), CACHE_TTL);
        } catch (Exception ignored) {
        }

        return seatMap;
    }

    public void evictSeatMapCache(Long showId) {
        try {
            redisTemplate.delete("seatmap:show:" + showId);
        } catch (Exception ignored) {
        }
    }
}