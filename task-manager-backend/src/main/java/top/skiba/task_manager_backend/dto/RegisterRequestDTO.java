package top.skiba.task_manager_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequestDTO {

    @NotBlank(message = "Username is required.")
    @Size(min = 3, max = 30, message = "Username must be 3-30 characters.")
    private String username;

    @NotBlank(message = "Email is required.")
    @Email(message = "Invalid email address.")
    @Size(max = 254, message = "Email must not exceed 254 characters.")
    private String email;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 128, message = "Password must be 8-128 characters.")
    private String password;

    public RegisterRequestDTO() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}