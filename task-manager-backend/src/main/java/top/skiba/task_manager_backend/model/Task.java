package top.skiba.task_manager_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_manager_tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Task() {}

    public Task(String title, String description, TaskStatus status) {
        this.title = title;
        this.description = description;
        this.status = status;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = TaskStatus.NEW;
    }

    public Long getId()                       { return id; }
    public void setId(Long id)                { this.id = id; }
    public String getTitle()                  { return title; }
    public void setTitle(String title)        { this.title = title; }
    public String getDescription()            { return description; }
    public void setDescription(String d)      { this.description = d; }
    public TaskStatus getStatus()             { return status; }
    public void setStatus(TaskStatus status)  { this.status = status; }
    public LocalDateTime getCreatedAt()       { return createdAt; }
    public User getUser()                     { return user; }
    public void setUser(User user)            { this.user = user; }
}