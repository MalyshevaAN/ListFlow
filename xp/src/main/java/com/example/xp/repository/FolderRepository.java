package com.example.xp.repository;

import com.example.xp.entity.Folder;
import com.example.xp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {
    List<Folder> findByUserOrderByIsDefaultDescIdAsc(User user);
    Optional<Folder> findByUserAndIsDefault(User user, boolean isDefault);
}
