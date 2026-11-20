package com.example.demo.model;

import com.example.demo.utils.seatStatus;
import jakarta.persistence.*;


@Entity
@Table(name = "seat")
public class seats {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seat_id;

    private String seat_number;

    @Column(name = "locked_by_user", nullable = true)
    private Long locked_by_user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private seatStatus status;

    public Long getSeat_id() {
        return seat_id;
    }

    public void setSeat_id(Long seat_id) {
        this.seat_id = seat_id;
    }

    public String getSeat_number() {
        return seat_number;
    }

    public void setSeat_number(String seat_number) {
        this.seat_number = seat_number;
    }

    public Long getLocked_by_user() {
        return locked_by_user;
    }

    public void setLocked_by_user(Long locked_by_user) {
        this.locked_by_user = locked_by_user;
    }

    public seatStatus getStatus() {
        return status;
    }

    public void setStatus(seatStatus status) {
        this.status = status;
    }
}
