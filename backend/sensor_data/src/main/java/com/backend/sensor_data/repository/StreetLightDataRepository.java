package com.backend.sensor_data.repository;

import com.backend.sensor_data.entity.StreetLightData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StreetLightDataRepository extends JpaRepository<StreetLightData, String> {
}
