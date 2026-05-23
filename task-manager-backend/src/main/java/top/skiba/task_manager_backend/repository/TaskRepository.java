package top.skiba.task_manager_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import top.skiba.task_manager_backend.model.Task;
import top.skiba.task_manager_backend.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByUser(User user);
    Optional<Task> findByIdAndUser(Long id, User user);
}
