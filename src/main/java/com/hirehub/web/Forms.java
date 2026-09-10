package com.hirehub.web;

import jakarta.validation.constraints.*;

public class Forms {
  public record Registration(
      @NotBlank @Size(max = 100) String name,
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank
          @Size(min = 12, max = 72)
          @Pattern(regexp = "[\\x20-\\x7E]+", message = "Use printable ASCII characters")
          String password) {}

  public record JobInput(
      @NotBlank @Size(max = 150) String title,
      @NotBlank @Size(max = 150) String company,
      @NotBlank @Size(max = 150) String location,
      @NotBlank @Size(max = 10000) String description,
      boolean active) {}

  public record ApplicationInput(@NotBlank @Size(max = 5000) String coverLetter) {}

  public record StatusInput(@NotNull com.hirehub.domain.JobApplication.Status status) {}
}
