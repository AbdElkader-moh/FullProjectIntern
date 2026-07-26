package com.backend.sensor_data.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.backend.sensor_data.dto.AirPollutionDataDto;
import com.backend.sensor_data.dto.TrafficDataDto;
import com.backend.sensor_data.entity.AirPollutionData;
import com.backend.sensor_data.entity.TrafficData;
import com.backend.sensor_data.repository.AirPollutionDataRepository;
import com.backend.sensor_data.repository.NotificationRepository;
import com.backend.sensor_data.repository.StreetLightDataRepository;
import com.backend.sensor_data.repository.TrafficDataRepository;
import com.backend.sensor_data.service.factory.SensorProcessorFactory;
import com.backend.sensor_data.service.processor.AbstractSensorProcessor;

class SensorDataServiceTest {

    private TrafficDataRepository trafficRepo;
    private AirPollutionDataRepository airRepo;
    private StreetLightDataRepository lightRepo;
    private NotificationRepository notificationRepository;
    private SensorProcessorFactory processorFactory;
    private SensorDataService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        trafficRepo = mock(TrafficDataRepository.class);
        airRepo = mock(AirPollutionDataRepository.class);
        lightRepo = mock(StreetLightDataRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        processorFactory = mock(SensorProcessorFactory.class);
        service = new SensorDataService(trafficRepo, airRepo, lightRepo, notificationRepository, processorFactory);
    }

    @Test
@SuppressWarnings("unchecked")
void saveTrafficData_delegatesToTrafficProcessor() {
    AbstractSensorProcessor<TrafficDataDto, ?> mockProcessor = mock(AbstractSensorProcessor.class);
    doReturn(mockProcessor).when(processorFactory).<TrafficDataDto>getProcessor("TRAFFIC");

    TrafficDataDto dto = new TrafficDataDto();
    service.saveTrafficData(dto);

    verify(mockProcessor).process(dto);
}

@Test
@SuppressWarnings("unchecked")
void saveAirPollutionData_delegatesToAirProcessor() {
    AbstractSensorProcessor<AirPollutionDataDto, ?> mockProcessor = mock(AbstractSensorProcessor.class);
    doReturn(mockProcessor).when(processorFactory).<AirPollutionDataDto>getProcessor("AIR");

    AirPollutionDataDto dto = new AirPollutionDataDto();
    service.saveAirPollutionData(dto);

    verify(mockProcessor).process(dto);
}

    @Test
    void getTrafficData_fromAfterTo_throwsIllegalArgumentException() {
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = from.minusDays(1);

        assertThrows(IllegalArgumentException.class, () ->
                service.getTrafficData(null, null, from, to, PageRequest.of(0, 20)));
    }

    @Test
    void getTrafficData_validParams_returnsPage() {
        Page<TrafficData> emptyPage = new PageImpl<>(Collections.emptyList());
        when(trafficRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        Page<TrafficData> result = service.getTrafficData(
                "Downtown", null, null, null, PageRequest.of(0, 20));

        assertNotNull(result);
        verify(trafficRepo).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getAirData_pageSizeZero_throwsIllegalArgumentException() {
        Pageable invalidPageable = mock(Pageable.class);
        when(invalidPageable.getPageSize()).thenReturn(0);

        assertThrows(IllegalArgumentException.class, () ->
                service.getAirData(null, null, null, null, invalidPageable));
    }

    @Test
    void getAirData_fromAfterTo_throwsIllegalArgumentException() {
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = from.minusDays(1);

        assertThrows(IllegalArgumentException.class, () ->
                service.getAirData(null, null, from, to, PageRequest.of(0, 20)));
    }

    @Test
    void getAirData_withSort_appliesManualOrdering() {
        Page<AirPollutionData> emptyPage = new PageImpl<>(Collections.emptyList());
        when(airRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        Pageable sortedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "pm25"));
        Page<AirPollutionData> result = service.getAirData(null, null, null, null, sortedPageable);

        assertNotNull(result);
        verify(airRepo).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getTrafficStats_returnsAggregatedStats() {
        when(trafficRepo.count()).thenReturn(5L);
        when(trafficRepo.findAll()).thenReturn(Collections.emptyList());
        when(trafficRepo.countByCongestionLevel(any())).thenReturn(1L);

        assertNotNull(service.getTrafficStats());
    }

    @Test
    void getTrafficTrends_returnsEmptyListWhenNoData() {
        when(trafficRepo.findTop50ByOrderByTimestampDesc()).thenReturn(Collections.emptyList());

        assertTrue(service.getTrafficTrends().isEmpty());
    }

    @Test
    void getTrafficCongestionSummary_returnsAllLevels() {
        when(trafficRepo.countByCongestionLevel(any())).thenReturn(2L);

        assertEquals(4, service.getTrafficCongestionSummary().size());
    }
}