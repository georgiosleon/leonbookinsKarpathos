package com.leonsoft.repositories;

import com.leonsoft.models.BookingResource;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface BookingResourceRepository extends CrudRepository<BookingResource, String> {

    List<BookingResource> findAllByOrderByTypeAscNameAsc();

    boolean existsByTypeAndNameIgnoreCase(String type, String name);
}
