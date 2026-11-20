package com.example.demo.service;

import com.example.demo.model.seats;
import com.example.demo.repository.seatLockRepository;
import com.example.demo.utils.seatStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class seatService {

    private seatLockRepository seatlockrepository;

    seatService(seatLockRepository seatlockrepository){
        this.seatlockrepository = seatlockrepository;
    }

    public List<seats> getSeats(){

        return seatlockrepository.findAll();
    }

    public String bookSeat(Long userId, List<Long> seatsIds){

            List<seats> seat = seatlockrepository.findAllByIdwithLock(seatsIds);

            if(seat.size() != seatsIds.size()){
                return "Failed";
            }

            for(seats i: seat){
                if(i.getStatus() != seatStatus.AVAILABLE){
                    return "Failed to book "+ i.getSeat_number();
                }
            }

            for(seats j :seat){
                j.setStatus(seatStatus.BOOKED);
                j.setLocked_by_user(userId);
                seatlockrepository.saveAll(seat);
            }
        System.out.println(" BOOKED BY USER: " + userId);

            return "Succesfully booked Seats  by user " + userId;

    }
}
