package com.backend.sensor_data.repository;

import com.backend.sensor_data.entity.TrafficData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.backend.sensor_data.entity.CongestionLevel;
import java.util.List;

@Repository
public interface TrafficDataRepository
        extends JpaRepository<TrafficData, String>, JpaSpecificationExecutor<TrafficData> {
    long countByCongestionLevel(CongestionLevel congestionLevel);

    List<TrafficData> findTop50ByOrderByTimestampDesc();

}