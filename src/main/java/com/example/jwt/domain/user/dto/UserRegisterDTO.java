package com.example.jwt.domain.user.dto;

import com.example.jwt.core.generic.ExtendedDTO;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public class UserRegisterDTO extends ExtendedDTO {

  // Without these the request reaches the database and a missing value comes
  // back as a 500 from a not-null constraint instead of a 400.
  @NotBlank
  private String firstName;

  @NotBlank
  private String lastName;

  @NotBlank
  @Email
  private String email;

  @NotBlank
  private String password;


  public UserRegisterDTO() {
  }

  public UserRegisterDTO(UUID id, String firstName, String lastName, String email,
      String password) {
    super(id);
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.password = password;
  }

  public String getFirstName() {
    return firstName;
  }

  public UserRegisterDTO setFirstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  public String getLastName() {
    return lastName;
  }

  public UserRegisterDTO setLastName(String lastName) {
    this.lastName = lastName;
    return this;
  }

  public String getEmail() {
    return email;
  }

  public UserRegisterDTO setEmail(String email) {
    this.email = email;
    return this;
  }

  public String getPassword() {
    return password;
  }

  public UserRegisterDTO setPassword(String password) {
    this.password = password;
    return this;
  }
}
