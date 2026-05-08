package com.backend.sensor_data.repository;

import com.backend.sensor_data.entity.AirPollutionData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AirPollutionDataRepository extends JpaRepository<AirPollutionData, String> {
}
