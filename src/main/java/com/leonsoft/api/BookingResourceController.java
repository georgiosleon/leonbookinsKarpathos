package com.leonsoft.api;

import com.leonsoft.models.BookingResource;
import com.leonsoft.services.BookingResourceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resources")
public class BookingResourceController {

    private final BookingResourceService resourceService;

    public BookingResourceController(BookingResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    List<BookingResource> getAll() {
        return resourceService.getAll();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    BookingResource create(@Valid @RequestBody BookingResourceRequest request) {
        return resourceService.create(request);
    }
}
