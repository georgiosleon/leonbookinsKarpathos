package com.leonsoft.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leonsoft.api.BookingResourceRequest;
import com.leonsoft.models.BookingResource;
import com.leonsoft.repositories.BookingResourceRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class BookingResourceServiceTest {

    @Mock
    private BookingResourceRepository resourceRepository;

    @InjectMocks
    private BookingResourceService resourceService;

    @Test
    void createsNamedResourceWithGeneratedId() {
        BookingResourceRequest request = new BookingResourceRequest("rooms", "  Suite 1  ", new java.math.BigDecimal("2.50"));
        when(resourceRepository.save(any(BookingResource.class)))
              .thenAnswer(invocation -> invocation.getArgument(0));

        BookingResource result = resourceService.create(request);

        assertEquals("rooms", result.getType());
        assertEquals("Suite 1", result.getName());
        assertEquals(new java.math.BigDecimal("2.50"), result.getRentalTaxPerNight());
        assertNotNull(result.getId());
        verify(resourceRepository).save(any(BookingResource.class));
    }

    @Test
    void rejectsDuplicateNameWithinResourceType() {
        when(resourceRepository.existsByTypeAndNameIgnoreCase("cars", "Car 1")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
              ResponseStatusException.class,
              () -> resourceService.create(new BookingResourceRequest("cars", "Car 1", java.math.BigDecimal.ZERO)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void bookingCanOnlyUseAnActiveManagedResource() {
        BookingResource inactive = new BookingResource("inactive", "rooms", "Old room");
        inactive.setActive(false);
        when(resourceRepository.findById("inactive")).thenReturn(Optional.of(inactive));

        ResponseStatusException exception = assertThrows(
              ResponseStatusException.class, () -> resourceService.requireActive("inactive"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}
