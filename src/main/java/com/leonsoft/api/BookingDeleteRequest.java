package com.leonsoft.api;

import jakarta.validation.constraints.NotBlank;

public record BookingDeleteRequest(@NotBlank String id) {
}
