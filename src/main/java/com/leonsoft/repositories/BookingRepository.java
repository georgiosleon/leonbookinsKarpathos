package com.leonsoft.repositories;

import com.leonsoft.models.Booking;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface BookingRepository extends CrudRepository<Booking, String> {

    //public List<Booking> findAllByOrderByIdAsc();
    List<Booking> findAllByOrderByStartDateAsc();

    List<Booking> findByStartDateBetweenOrderByStartDateAsc(String startDate, String endDate);

}
