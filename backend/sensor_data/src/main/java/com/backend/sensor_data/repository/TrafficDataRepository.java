package com.backend.sensor_data.repository;

import com.backend.sensor_data.entity.TrafficData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrafficDataRepository extends JpaRepository<TrafficData, String> {
}
