package com.leonsoft.services;

import com.leonsoft.api.BookingResourceRequest;
import com.leonsoft.api.BookingResourceUpdateRequest;
import com.leonsoft.models.BookingResource;
import com.leonsoft.repositories.BookingResourceRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookingResourceService {

    private final BookingResourceRepository resourceRepository;

    public BookingResourceService(BookingResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    public List<BookingResource> getAll() {
        return resourceRepository.findAllByOrderByTypeAscNameAsc();
    }

    public BookingResource create(BookingResourceRequest request) {
        String name = request.name().trim();
        if (resourceRepository.existsByTypeAndNameIgnoreCase(request.type(), name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A resource with this name already exists.");
        }
        return resourceRepository.save(request.toResource(UUID.randomUUID().toString()));
    }

    public BookingResource update(String id, BookingResourceUpdateRequest request) {
        BookingResource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found."));
        String name = request.name().trim();
        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resource name is required.");
        }
        if (resourceRepository.existsByTypeAndNameIgnoreCaseAndIdNot(resource.getType(), name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A resource with this name already exists.");
        }
        resource.setName(name);
        resource.setRentalTaxPerNight(request.rentalTaxPerNight() == null ? BigDecimal.ZERO : request.rentalTaxPerNight());
        resource.setActive(Boolean.TRUE.equals(request.active()));
        return resourceRepository.save(resource);
    }

    public BookingResource requireActive(String id) {
        BookingResource resource = resourceRepository.findById(id)
              .filter(BookingResource::isActive)
              .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "The selected room or vehicle is not available."));
        return resource;
    }

    public BookingResource requireExisting(String id) {
        return resourceRepository.findById(id)
              .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "The selected room or vehicle is not available."));
    }

    public Map<String, String> namesById() {
        return resourceRepository.findAllByOrderByTypeAscNameAsc().stream()
              .collect(Collectors.toMap(BookingResource::getId, BookingResource::getName));
    }
}
