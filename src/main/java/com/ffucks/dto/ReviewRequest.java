package com.ffucks.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewRequest(@NotBlank String movie) {}
