
package com.example.demo.Controller;

import com.example.demo.DTO.SeatMapItem;
import com.example.demo.service.SeatMapService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shows")
public class ShowController {

    private final SeatMapService seatMapService;

    public ShowController(SeatMapService seatMapService) {
        this.seatMapService = seatMapService;
    }

    @GetMapping("/{showId}/seats")
    public List<SeatMapItem> getShowSeats(@PathVariable Long showId) {
        return seatMapService.getSeatMap(showId);
    }
}