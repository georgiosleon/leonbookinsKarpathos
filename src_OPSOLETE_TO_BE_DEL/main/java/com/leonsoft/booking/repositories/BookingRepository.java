package com.leonsoft.booking.repositories;

import com.leonsoft.booking.models.Booking;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends CrudRepository<Booking, String> {

    //public List<Booking> findAllByOrderByIdAsc();
    List<Booking> findAllByOrderByStartDateAsc();




//    select start_date, end_date, name from Booking
//    WHERE ( DATE(start_date) BETWEEN '2024-02-18' AND '2024-02-19')   OR  ( DATE(end_date) BETWEEN '2024-02-18' AND '2024-02-19' )
//    OR    (DATE(start_date) <  '2024-02-18'  AND DATE(end_date) >  '2024-02-19'   )
//    order by start_date ASC;

    @Query(value = "select * from Booking "
          + " WHERE "
          + "    DATE(start_date) BETWEEN :startDate AND :endDate "
          + " OR DATE(end_date)   BETWEEN :startDate AND :endDate "
          + " OR ( DATE(start_date) < :startDate AND DATE(end_date) >  :endDate ) "
          + " ORDER BY start_date ASC",
          nativeQuery = true)
    List<Booking> findAllBookingsForReport( @Param("startDate") String startDate, @Param("endDate") String endDate);




//    List<Booking> findByNameLike( String name);

    List<Booking> findByNameContainingIgnoreCase( String name);


//
//    @Query(value = "select * from Booking "
//          + " WHERE "
//          + "    DATE(start_date) BETWEEN :startDate AND :endDate "
//          + " OR DATE(end_date)   BETWEEN :startDate AND :endDate "
//          + " OR ( DATE(start_date) < :startDate AND DATE(end_date) >  :endDate ) "
//          + " AND name like %:name% "
//          + " ORDER BY  start_date ASC",
//          nativeQuery = true)
//    List<Booking> findAllBookingsForReportWithName( @Param("name") String name ,  @Param("startDate") String startDate, @Param("endDate") String endDate);



}
