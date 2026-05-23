package top.skiba.task_manager_backend.model;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "task_manager_users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public User() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }

    public Long getId()                    { return id; }
    public void setId(Long id)             { this.id = id; }
    @Override public String getUsername()  { return username; }
    public void setUsername(String u)      { this.username = u; }
    public String getEmail()               { return email; }
    public void setEmail(String e)         { this.email = e; }
    @Override public String getPassword()  { return password; }
    public void setPassword(String p)      { this.password = p; }
    public LocalDateTime getCreatedAt()    { return createdAt; }
}
