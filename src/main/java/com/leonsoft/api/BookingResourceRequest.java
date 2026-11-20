package com.leonsoft.api;

import com.leonsoft.models.BookingResource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BookingResourceRequest(
      @NotBlank @Pattern(regexp = "rooms|cars") String type,
      @NotBlank @Size(max = 80) String name) {

    public BookingResource toResource(String id) {
        return new BookingResource(id, type, name.trim());
    }
}
