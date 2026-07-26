package com.backend.sensor_data.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.backend.sensor_data.entity.AirPollutionData;
import com.backend.sensor_data.entity.CongestionLevel;
import com.backend.sensor_data.entity.TrafficData;
import com.backend.sensor_data.service.SensorDataService;

import java.util.Collections;

class SensorControllerTest {

    private SensorDataService sensorDataService;
    private SensorController controller;

    @BeforeEach
    void setUp() {
        sensorDataService = mock(SensorDataService.class);
        controller = new SensorController(sensorDataService);
    }

    @Test
    void getAirData_sortByPm2_5_resolvesToPm25Property() {
        Page<AirPollutionData> emptyPage = new PageImpl<>(Collections.emptyList());
        when(sensorDataService.getAirData(any(), any(), any(), any(), any())).thenReturn(emptyPage);

        ResponseEntity<?> response = controller.getAirData(
                null, null, null, null, 0, 20, "pm2_5,desc");

        assertEquals(HttpStatus.OK, response.getStatusCode());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(sensorDataService).getAirData(any(), any(), any(), any(), pageableCaptor.capture());

        Sort resultSort = pageableCaptor.getValue().getSort();
        Sort.Order order = resultSort.getOrderFor("pm25");
        assertNotNull(order, "Expected sort to resolve to the 'pm25' Java property");
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    @Test
    void getAirData_sortByUnknownField_returns400() {
        ResponseEntity<?> response = controller.getAirData(
                null, null, null, null, 0, 20, "notARealField,asc");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(sensorDataService);
    }

    @Test
    void getAirData_blankSort_returnsUnsorted() {
        Page<AirPollutionData> emptyPage = new PageImpl<>(Collections.emptyList());
        when(sensorDataService.getAirData(any(), any(), any(), any(), any())).thenReturn(emptyPage);

        ResponseEntity<?> response = controller.getAirData(
                null, null, null, null, 0, 20, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(sensorDataService).getAirData(any(), any(), any(), any(), pageableCaptor.capture());
        assertTrue(pageableCaptor.getValue().getSort().isUnsorted());
    }

    @Test
    void getAirData_sortByTimestamp_noAliasNeeded() {
        Page<AirPollutionData> emptyPage = new PageImpl<>(Collections.emptyList());
        when(sensorDataService.getAirData(any(), any(), any(), any(), any())).thenReturn(emptyPage);

        controller.getAirData(null, null, null, null, 0, 20, "timestamp,asc");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(sensorDataService).getAirData(any(), any(), any(), any(), pageableCaptor.capture());

        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("timestamp");
        assertNotNull(order);
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    void getTrafficData_invalidPage_returns400() {
        ResponseEntity<?> response = controller.getTrafficData(
                null, null, null, null, -1, 20, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(sensorDataService);
    }

    @Test
    void getTrafficData_sortByCongestionLevel_noAliasNeeded() {
        Page<TrafficData> emptyPage = new PageImpl<>(Collections.emptyList());
        when(sensorDataService.getTrafficData(any(), any(), any(), any(), any())).thenReturn(emptyPage);

        ResponseEntity<?> response = controller.getTrafficData(
                null, CongestionLevel.High, null, null, 0, 20, "congestionLevel,desc");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getTrafficStats_returns200() {
        when(sensorDataService.getTrafficStats()).thenReturn(mock(com.backend.sensor_data.dto.TrafficStatsDto.class));

        ResponseEntity<?> response = controller.getTrafficStats();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getTrafficTrends_returns200() {
        when(sensorDataService.getTrafficTrends()).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = controller.getTrafficTrends();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getTrafficCongestionSummary_returns200() {
        when(sensorDataService.getTrafficCongestionSummary()).thenReturn(java.util.Map.of());

        ResponseEntity<?> response = controller.getTrafficCongestionSummary();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getAirStats_returns200() {
        when(sensorDataService.getAirStats()).thenReturn(mock(com.backend.sensor_data.dto.AirStatsDto.class));

        ResponseEntity<?> response = controller.getAirStats();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getAirTrends_returns200() {
        when(sensorDataService.getAirTrends()).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = controller.getAirTrends();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getLightStats_returns200() {
        when(sensorDataService.getLightStats()).thenReturn(mock(com.backend.sensor_data.dto.LightStatsDto.class));

        ResponseEntity<?> response = controller.getLightStats();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getLightTrends_returns200() {
        when(sensorDataService.getLightTrends()).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = controller.getLightTrends();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getLightData_sortByInvalidField_returns400() {
        ResponseEntity<?> response = controller.getLightData(
                null, null, null, null, 0, 20, "notARealField,asc");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(sensorDataService);
    }

    @Test
    void receiveTrafficData_returns201() {
        com.backend.sensor_data.dto.TrafficDataDto dto = new com.backend.sensor_data.dto.TrafficDataDto();

        ResponseEntity<?> response = controller.receiveTrafficData(dto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(sensorDataService).saveTrafficData(dto);
    }

    @Test
    void receiveAirData_returns201() {
        com.backend.sensor_data.dto.AirPollutionDataDto dto = new com.backend.sensor_data.dto.AirPollutionDataDto();

        ResponseEntity<?> response = controller.receiveAirData(dto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(sensorDataService).saveAirPollutionData(dto);
    }

    @Test
    void receiveLightData_returns201() {
        com.backend.sensor_data.dto.StreetLightDataDto dto = new com.backend.sensor_data.dto.StreetLightDataDto();

        ResponseEntity<?> response = controller.receiveLightData(dto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(sensorDataService).saveStreetLightData(dto);
    }
}