package com.leonsoft.repositories;

import com.leonsoft.models.Booking;
import com.leonsoft.models.User;
import java.util.Collection;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface BookingRepository extends CrudRepository<Booking, String> {

    //public List<Booking> findAllByOrderByIdAsc();
    List<Booking> findAllByOrderByStartDateAsc();

    List<Booking> findByStartDateBetweenOrderByStartDateDesc(String startDate, String endDate);

    List<Booking> findByRoomAndStatusIn(String room, Collection<String> statuses);

    List<Booking> findByUserIdOrderByStartDateAsc(Integer userId);

}
