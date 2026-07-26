package com.backend.sensor_data.controller;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.backend.sensor_data.dto.AirPollutionDataDto;
import com.backend.sensor_data.dto.StreetLightDataDto;
import com.backend.sensor_data.dto.TrafficDataDto;
import com.backend.sensor_data.entity.CongestionLevel;
import com.backend.sensor_data.entity.PollutionLevel;
import com.backend.sensor_data.entity.Status;
import com.backend.sensor_data.entity.TrafficData;
import com.backend.sensor_data.service.SensorDataService;

/**
 * Covers SensorController as a plain unit test (mocked SensorDataService,
 * no Spring context) -- this is deliberately NOT @WebMvcTest, since the
 * goal here is exercising the private validatePagination()/buildSort()
 * helpers (the exact lines/conditions flagged as uncovered), not the HTTP
 * serialization layer. If you also want a @WebMvcTest pass for the HTTP
 * contract itself (status codes as seen by a real request), that's a
 * separate, complementary test class.
 */
@ExtendWith(MockitoExtension.class)
class SensorControllerTest {

    @Mock
    private SensorDataService sensorDataService;

    @InjectMocks
    private SensorController controller;

    // ---------------- ingestion endpoints ----------------

    @Test
    void receiveTrafficData_delegatesToServiceAndReturns201() {
        TrafficDataDto dto = new TrafficDataDto();

        ResponseEntity<String> response = controller.receiveTrafficData(dto);

        verify(sensorDataService).saveTrafficData(dto);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo("Traffic data saved successfully.");
    }

    @Test
    void receiveAirData_delegatesToServiceAndReturns201() {
        AirPollutionDataDto dto = new AirPollutionDataDto();

        ResponseEntity<String> response = controller.receiveAirData(dto);

        verify(sensorDataService).saveAirPollutionData(dto);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo("Air pollution data saved successfully.");
    }

    @Test
    void receiveLightData_delegatesToServiceAndReturns201() {
        StreetLightDataDto dto = new StreetLightDataDto();

        ResponseEntity<String> response = controller.receiveLightData(dto);

        verify(sensorDataService).saveStreetLightData(dto);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo("Street light data saved successfully.");
    }

    // ---------------- validatePagination() via getTrafficData ----------------

    @Test
    void getTrafficData_negativePage_returns400BeforeCallingService() {
        ResponseEntity<Object> response = controller.getTrafficData(
                null, null, null, null, -1, 20, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body).containsEntry("details", "Page must be >= 0");
        verifyNoInteractions(sensorDataService);
    }

    @Test
    void getTrafficData_zeroSize_returns400BeforeCallingService() {
        ResponseEntity<Object> response = controller.getTrafficData(
                null, null, null, null, 0, 0, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body).containsEntry("details", "Page size must be greater than 0");
        verifyNoInteractions(sensorDataService);
    }

    @Test
    void getTrafficData_negativeSize_returns400() {
        ResponseEntity<Object> response = controller.getTrafficData(
                null, null, null, null, 0, -5, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(sensorDataService);
    }

    // ---------------- buildSort() via getTrafficData ----------------

    @Test
    void getTrafficData_invalidSortField_returns400WithFieldNameInMessage() {
        ResponseEntity<Object> response = controller.getTrafficData(
                null, null, null, null, 0, 20, "notARealField,asc");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body.get("details")).contains("notARealField");
        verifyNoInteractions(sensorDataService);
    }

    @Test
    void getTrafficData_nullSort_usesUnsortedAndCallsService() {
        Page<TrafficData> page = new PageImpl<>(List.of());
        when(sensorDataService.getTrafficData(any(), any(), any(), any(), any())).thenReturn(page);

        ResponseEntity<Object> response = controller.getTrafficData(
                null, null, null, null, 0, 20, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(sensorDataService).getTrafficData(any(), any(), any(), any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().isUnsorted()).isTrue();
    }

    @Test
    void getTrafficData_blankSort_usesUnsorted() {
        Page<TrafficData> page = new PageImpl<>(List.of());
        when(sensorDataService.getTrafficData(any(), any(), any(), any(), any())).thenReturn(page);

        controller.getTrafficData(null, null, null, null, 0, 20, "   ");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(sensorDataService).getTrafficData(any(), any(), any(), any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().isUnsorted()).isTrue();
    }

    @Test
    void getTrafficData_validSortFieldNoDirection_defaultsToAscending() {
        Page<TrafficData> page = new PageImpl<>(List.of());
        when(sensorDataService.getTrafficData(any(), any(), any(), any(), any())).thenReturn(page);

        controller.getTrafficData(null, null, null, null, 0, 20, "trafficDensity");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(sensorDataService).getTrafficData(any(), any(), any(), any(), pageableCaptor.capture());
        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("trafficDensity");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void getTrafficData_validSortFieldDescDirection_appliesDescending() {
        Page<TrafficData> page = new PageImpl<>(List.of());
        when(sensorDataService.getTrafficData(any(), any(), any(), any(), any())).thenReturn(page);

        controller.getTrafficData(null, null, null, null, 0, 20, "avgSpeed,desc");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(sensorDataService).getTrafficData(any(), any(), any(), any(), pageableCaptor.capture());
        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("avgSpeed");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getTrafficData_validParams_passesThroughLocationAndCongestionLevel() {
        Page<TrafficData> page = new PageImpl<>(List.of());
        when(sensorDataService.getTrafficData(any(), any(), any(), any(), any())).thenReturn(page);

        controller.getTrafficData("Main St", CongestionLevel.High, null, null, 1, 10, "timestamp,desc");

        verify(sensorDataService).getTrafficData(
                eq("Main St"), eq(CongestionLevel.High), eq(null), eq(null), any());
    }

    // ---------------- traffic read-only passthrough endpoints ----------------

    @Test
    void getTrafficStats_delegatesToService() {
        controller.getTrafficStats();
        verify(sensorDataService).getTrafficStats();
    }

    @Test
    void getTrafficTrends_delegatesToService() {
        controller.getTrafficTrends();
        verify(sensorDataService).getTrafficTrends();
    }

    @Test
    void getTrafficCongestionSummary_delegatesToService() {
        controller.getTrafficCongestionSummary();
        verify(sensorDataService).getTrafficCongestionSummary();
    }

    // ---------------- buildSort() with field aliases via getAirData
    // ----------------

    @Test
    void getAirData_aliasedSortField_resolvesToRealFieldName() {
        when(sensorDataService.getAirData(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        controller.getAirData(null, null, null, null, 0, 20, "pm2_5,asc");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(sensorDataService).getAirData(any(), any(), any(), any(), pageableCaptor.capture());
        // The alias map resolves the public "pm2_5" sort key to the entity's real
        // "pm25" field.
        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("pm25");
        assertThat(order).isNotNull();
    }

    @Test
    void getAirData_invalidSortField_returns400() {
        ResponseEntity<Object> response = controller.getAirData(
                null, null, null, null, 0, 20, "notAField");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(sensorDataService);
    }

    @Test
    void getAirData_negativePage_returns400() {
        ResponseEntity<Object> response = controller.getAirData(
                null, null, null, null, -1, 20, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(sensorDataService);
    }

    @Test
    void getAirData_validParams_passesThroughPollutionLevel() {
        when(sensorDataService.getAirData(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        controller.getAirData("Downtown", PollutionLevel.Unhealthy, null, null, 0, 20, null);

        verify(sensorDataService).getAirData(
                eq("Downtown"), eq(PollutionLevel.Unhealthy), eq(null), eq(null), any());
    }

    @Test
    void getAirStats_delegatesToService() {
        controller.getAirStats();
        verify(sensorDataService).getAirStats();
    }

    @Test
    void getAirTrends_delegatesToService() {
        controller.getAirTrends();
        verify(sensorDataService).getAirTrends();
    }

    // ---------------- light endpoints ----------------

    @Test
    void getLightData_zeroSize_returns400() {
        ResponseEntity<Object> response = controller.getLightData(
                null, null, null, null, 0, 0, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(sensorDataService);
    }

    @Test
    void getLightData_invalidSortField_returns400() {
        ResponseEntity<Object> response = controller.getLightData(
                null, null, null, null, 0, 20, "notAField,asc");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(sensorDataService);
    }

    @Test
    void getLightData_validParams_passesThroughStatusFilter() {
        when(sensorDataService.getLightData(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        controller.getLightData("Elm St", Status.ON, null, null, 0, 20, "brightnessLevel,desc");

        verify(sensorDataService).getLightData(
                eq("Elm St"), eq(Status.ON), eq(null), eq(null), any());
    }

    @Test
    void getLightStats_delegatesToService() {
        controller.getLightStats();
        verify(sensorDataService).getLightStats();
    }

    @Test
    void getLightTrends_delegatesToService() {
        controller.getLightTrends();
        verify(sensorDataService).getLightTrends();
    }
}
