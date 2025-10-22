package com.example.xp.repository;

import com.example.xp.entity.Task;
import com.example.xp.entity.User;
import com.example.xp.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByUserOrderByIdDesc(User user);
    List<Task> findByFolderOrderByIdDesc(Folder folder);
}
