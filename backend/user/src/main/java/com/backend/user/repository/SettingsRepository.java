package com.backend.user.repository;

import com.backend.user.entity.Settings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import java.util.Optional;

@Repository
public interface SettingsRepository extends JpaRepository<Settings, String> {
    List<Settings> findByUserId(Long userId);
    Optional<Settings> findByUserIdAndTypeAndMetric(Long userId, String type, String metric);
}
