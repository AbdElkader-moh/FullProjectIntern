package com.backend.sensor_data.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.backend.sensor_data.entity.Status;
import com.backend.sensor_data.entity.StreetLightData;

@Repository
public interface StreetLightDataRepository extends
        JpaRepository<StreetLightData, String>,
        JpaSpecificationExecutor<StreetLightData> {

    // trend data — last 50 records for charts
    List<StreetLightData> findTop50ByOrderByTimestampDesc();

    // aggregation — ON/OFF breakdown for stats
    long countByStatus(Status status);

    // aggregation — highest and lowest values for stats
    @Query("SELECT MAX(s.powerConsumption) FROM StreetLightData s")
    Double findMaxPowerConsumption();

    @Query("SELECT MIN(s.brightnessLevel) FROM StreetLightData s")
    Double findMinBrightnessLevel();

    @Query("SELECT AVG(s.brightnessLevel) FROM StreetLightData s")
    Double findAvgBrightnessLevel();

    @Query("SELECT AVG(s.powerConsumption) FROM StreetLightData s")
    Double findAvgPowerConsumption();
}