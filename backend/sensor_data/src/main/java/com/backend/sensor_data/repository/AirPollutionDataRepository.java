package com.backend.sensor_data.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.backend.sensor_data.entity.AirPollutionData;
import com.backend.sensor_data.entity.PollutionLevel;

@Repository
public interface AirPollutionDataRepository extends 
        JpaRepository<AirPollutionData, String>,
        JpaSpecificationExecutor<AirPollutionData> {

    // trend data — last 50 records for charts
    List<AirPollutionData> findTop50ByOrderByTimestampDesc();

    // aggregation — pollution level breakdown for stats
    long countByPollutionLevel(PollutionLevel pollutionLevel);

    // aggregation — highest and lowest values for stats
    @Query("SELECT MAX(a.co) FROM AirPollutionData a")
    Double findMaxCo();

    @Query("SELECT MIN(a.co) FROM AirPollutionData a")
    Double findMinCo();

    @Query("SELECT MAX(a.ozone) FROM AirPollutionData a")
    Double findMaxOzone();

    @Query("SELECT MIN(a.ozone) FROM AirPollutionData a")
    Double findMinOzone();

    @Query("SELECT AVG(a.co) FROM AirPollutionData a")
    Double findAvgCo();

    @Query("SELECT AVG(a.ozone) FROM AirPollutionData a")
    Double findAvgOzone();
}