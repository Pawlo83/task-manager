package top.skiba.task_manager_backend.dto;

public class AuthResponseDTO {
    private String username;
    private String email;
    private String csrfToken;

    public AuthResponseDTO() {}

    public AuthResponseDTO(String username, String email, String csrfToken) {
        this.username = username;
        this.email = email;
        this.csrfToken = csrfToken;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCsrfToken() { return csrfToken; }
    public void setCsrfToken(String csrfToken) { this.csrfToken = csrfToken; }
}