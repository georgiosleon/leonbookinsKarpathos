package com.leonsoft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.leonsoft.models.BookingResource;
import org.junit.jupiter.api.Test;

class BookingResourceResponseTest {

    @Test
    void fromMapsResourceFields() {
        BookingResource resource = new BookingResource("r1", "rooms", "APT 2");

        BookingResourceResponse response = BookingResourceResponse.from(resource);

        assertEquals("r1", response.id());
        assertEquals("rooms", response.type());
        assertEquals("APT 2", response.name());
        assertTrue(response.active());
    }
}
