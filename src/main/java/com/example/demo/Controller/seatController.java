package com.example.demo.Controller;

import com.example.demo.DTO.bookingRequest;
import com.example.demo.model.seats;
import com.example.demo.service.seatService;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/seats")
public class seatController {


    public seatService seatservice;

    seatController(seatService seatservice){
        this.seatservice = seatservice;
    }

    @GetMapping
    public List<seats> getSeats(){
        return seatservice.getSeats();
    }

    @PostMapping("/bookSeat")
    @Transactional
    public ResponseEntity<String> bookSeat(@RequestBody bookingRequest request){

        String result = seatservice.bookSeat(request.getUserId(), request.getSeatIds());

        return ResponseEntity.ok(result);
    }

}
