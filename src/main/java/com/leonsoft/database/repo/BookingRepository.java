package com.leonsoft.database.repo;

import com.leonsoft.businesslogic.pojos.Booking;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface BookingRepository extends CrudRepository<Booking, String> {

    //public List<Booking> findAllByOrderByIdAsc();
    List<Booking> findAllByOrderByStartDateAsc();

    List<Booking> findByStartDateBetweenOrderByStartDateDesc(String startDate, String endDate);

}
