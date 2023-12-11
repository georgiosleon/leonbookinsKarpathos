package com.leonsoft.booking.repositories;

import com.leonsoft.booking.models.Booking;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface BookingRepository extends CrudRepository<Booking, String> {

    //public List<Booking> findAllByOrderByIdAsc();
    List<Booking> findAllByOrderByStartDateAsc();

    List<Booking> findByStartDateBetweenOrderByStartDateDesc(String startDate, String endDate);

}
