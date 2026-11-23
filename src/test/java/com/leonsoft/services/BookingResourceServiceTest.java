package com.leonsoft.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leonsoft.api.BookingResourceRequest;
import com.leonsoft.api.BookingResourceUpdateRequest;
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
        BookingResourceRequest request = new BookingResourceRequest("rooms", "  Suite 1  ", new java.math.BigDecimal("2.50"), "AMA-1", "Addr 1", "climate_fee");
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
              () -> resourceService.create(new BookingResourceRequest("cars", "Car 1", java.math.BigDecimal.ZERO, null, null, null)));

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

    @Test
    void requireExistingReturnsInactiveResource() {
        BookingResource inactive = new BookingResource("inactive", "rooms", "APT 1");
        inactive.setActive(false);
        when(resourceRepository.findById("inactive")).thenReturn(Optional.of(inactive));

        BookingResource result = resourceService.requireExisting("inactive");

        assertEquals("inactive", result.getId());
        assertEquals(false, result.isActive());
    }

    @Test
    void updatesResourceNameTaxAndActive() {
        BookingResource resource = new BookingResource("r1", "rooms", "APT 1");
        resource.setRentalTaxPerNight(new java.math.BigDecimal("1.00"));
        when(resourceRepository.findById("r1")).thenReturn(Optional.of(resource));
        when(resourceRepository.existsByTypeAndNameIgnoreCaseAndIdNot("rooms", "APT 2", "r1")).thenReturn(false);
        when(resourceRepository.save(any(BookingResource.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResource updated = resourceService.update("r1",
                new BookingResourceUpdateRequest("  APT 2  ", new java.math.BigDecimal("2.50"), false, "REG-2", "Addr 2", "climate_fee"));

        assertEquals("APT 2", updated.getName());
        assertEquals(new java.math.BigDecimal("2.50"), updated.getRentalTaxPerNight());
        assertEquals(false, updated.isActive());
    }

    @Test
    void rejectsDuplicateNameOnUpdate() {
        BookingResource resource = new BookingResource("r1", "rooms", "APT 1");
        when(resourceRepository.findById("r1")).thenReturn(Optional.of(resource));
        when(resourceRepository.existsByTypeAndNameIgnoreCaseAndIdNot("rooms", "APT 2", "r1")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> resourceService.update("r1", new BookingResourceUpdateRequest("APT 2", java.math.BigDecimal.ZERO, true, null, null, null)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }
}
