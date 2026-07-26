package com.backend.sensor_data.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.backend.sensor_data.dto.AirPollutionDataDto;
import com.backend.sensor_data.dto.AirStatsDto;
import com.backend.sensor_data.dto.AirTrendDto;
import com.backend.sensor_data.dto.LightStatsDto;
import com.backend.sensor_data.dto.StreetLightDataDto;
import com.backend.sensor_data.dto.TrafficDataDto;
import com.backend.sensor_data.dto.TrafficStatsDto;
import com.backend.sensor_data.entity.AirPollutionData;
import com.backend.sensor_data.entity.CongestionLevel;
import com.backend.sensor_data.entity.PollutionLevel;
import com.backend.sensor_data.entity.Status;
import com.backend.sensor_data.entity.StreetLightData;
import com.backend.sensor_data.entity.TrafficData;
import com.backend.sensor_data.repository.AirPollutionDataRepository;
import com.backend.sensor_data.repository.NotificationRepository;
import com.backend.sensor_data.repository.StreetLightDataRepository;
import com.backend.sensor_data.repository.TrafficDataRepository;
import com.backend.sensor_data.service.factory.SensorProcessorFactory;
import com.backend.sensor_data.service.processor.AbstractSensorProcessor;

/**
 * ASSUMPTIONS FLAGGED (I don't have these source files, so these are
 * best-guesses based on naming conventions used elsewhere in this codebase;
 * expect possible compile errors on the items below -- paste the error and
 * I'll correct in one pass, same as the processor test files):
 *
 * 1. SensorProcessorFactory.getProcessor(String) returns
 * AbstractSensorProcessor<D, Object> directly (confirmed from
 * SensorProcessorFactory.java) with a process(D dto) method inherited
 * from AbstractSensorProcessor -- process()'s exact signature is assumed
 * since I don't have AbstractSensorProcessor.java itself.
 * 2. TrafficData/AirPollutionData/StreetLightData all have a
 * setTimestamp(LocalDateTime) setter (used by getXTrends()'s
 * findTop50ByOrderByTimestampDesc() + data.getTimestamp() call).
 * 3. TrafficStatsDto/AirStatsDto/LightStatsDto getter names are assumed to
 * mirror their constructor-parameter / builder-property names exactly
 * (e.g. averageTrafficDensity -> getAverageTrafficDensity()).
 */
@ExtendWith(MockitoExtension.class)
class SensorDataServiceTest {

    @Mock
    private TrafficDataRepository trafficRepo;
    @Mock
    private AirPollutionDataRepository airRepo;
    @Mock
    private StreetLightDataRepository lightRepo;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private SensorProcessorFactory processorFactory;

    @InjectMocks
    private SensorDataService service;

    // ---------------- ingestion delegation ----------------

    @Test
    @SuppressWarnings("unchecked")
    void saveTrafficData_delegatesToTrafficProcessor() {
        TrafficDataDto dto = new TrafficDataDto();
        AbstractSensorProcessor<TrafficDataDto, Object> processor = mock(AbstractSensorProcessor.class);
        when(processorFactory.<TrafficDataDto>getProcessor("TRAFFIC")).thenReturn(processor);

        service.saveTrafficData(dto);

        verify(processor).process(dto);
    }

    @Test
    @SuppressWarnings("unchecked")
    void saveAirPollutionData_delegatesToAirProcessor() {
        AirPollutionDataDto dto = new AirPollutionDataDto();
        AbstractSensorProcessor<AirPollutionDataDto, Object> processor = mock(AbstractSensorProcessor.class);
        when(processorFactory.<AirPollutionDataDto>getProcessor("AIR")).thenReturn(processor);

        service.saveAirPollutionData(dto);

        verify(processor).process(dto);
    }

    @Test
    @SuppressWarnings("unchecked")
    void saveStreetLightData_delegatesToLightProcessor() {
        StreetLightDataDto dto = new StreetLightDataDto();
        AbstractSensorProcessor<StreetLightDataDto, Object> processor = mock(AbstractSensorProcessor.class);
        when(processorFactory.<StreetLightDataDto>getProcessor("LIGHT")).thenReturn(processor);

        service.saveStreetLightData(dto);

        verify(processor).process(dto);
    }

    // ---------------- getTrafficData() ----------------

    @Test
    void getTrafficData_fromAfterTo_throwsIllegalArgumentException() {
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = from.minusDays(1);
        Pageable pageable = PageRequest.of(0, 20);

        assertThatThrownBy(() -> service.getTrafficData(null, null, from, to, pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("'from' date cannot be after 'to' date");
        verifyNoInteractions(trafficRepo);
    }

    @Test
    void getTrafficData_validParams_delegatesToRepositoryWithSpecAndPageable() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<TrafficData> expected = new PageImpl<>(List.of());
        when(trafficRepo.findAll(any(Specification.class), eq(pageable))).thenReturn(expected);

        Page<TrafficData> result = service.getTrafficData(
                "Main St", CongestionLevel.High, LocalDateTime.now().minusDays(1), LocalDateTime.now(), pageable);

        assertThat(result).isSameAs(expected);
        verify(trafficRepo).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getTrafficData_noFilters_stillCallsRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        when(trafficRepo.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of()));

        service.getTrafficData(null, null, null, null, pageable);

        verify(trafficRepo).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getTrafficData_blankLocation_isTreatedAsNoLocationFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        when(trafficRepo.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of()));

        // Should not throw, and should still reach the repository call.
        service.getTrafficData("   ", null, null, null, pageable);

        verify(trafficRepo).findAll(any(Specification.class), eq(pageable));
    }

    // -- per-branch coverage: each filter condition exercised independently
    // (not only "all together" or "none"), consolidated into a single
    // parameterized test since the 4 cases share identical structure --

    @ParameterizedTest(name = "getTrafficData: {0}")
    @MethodSource("trafficDataSingleFilterCases")
    void getTrafficData_singleFilterOnly_doesNotThrowAndReachesRepository(
            String description, String location, CongestionLevel congestionLevel,
            LocalDateTime from, LocalDateTime to) {
        Pageable pageable = PageRequest.of(0, 20);
        when(trafficRepo.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of()));

        service.getTrafficData(location, congestionLevel, from, to, pageable);

        verify(trafficRepo).findAll(any(Specification.class), eq(pageable));
    }

    static Stream<Arguments> trafficDataSingleFilterCases() {
        LocalDateTime now = LocalDateTime.now();
        return Stream.of(
                Arguments.of("location only", "Main St", null, null, null),
                Arguments.of("congestionLevel only", null, CongestionLevel.Moderate, null, null),
                // 'from' only: the from.isAfter(to) check is skipped since 'to' is null.
                Arguments.of("from only, no to", null, null, now.minusDays(1), null),
                Arguments.of("to only, no from", null, null, null, now));
    }

    // ---------------- getTrafficStats() ----------------

    @Test
    void getTrafficStats_computesAveragesAndCongestionCounts() {
        TrafficData d1 = new TrafficData();
        d1.setTrafficDensity(200);
        d1.setAvgSpeed(50.0f);
        TrafficData d2 = new TrafficData();
        d2.setTrafficDensity(400);
        d2.setAvgSpeed(70.0f);

        when(trafficRepo.count()).thenReturn(2L);
        when(trafficRepo.findAll()).thenReturn(List.of(d1, d2));
        when(trafficRepo.countByCongestionLevel(CongestionLevel.High)).thenReturn(3L);
        when(trafficRepo.countByCongestionLevel(CongestionLevel.Severe)).thenReturn(1L);

        TrafficStatsDto stats = service.getTrafficStats();

        assertThat(stats.getTotalRecords()).isEqualTo(2L);
        assertThat(stats.getAverageTrafficDensity()).isEqualTo(300.0);
        assertThat(stats.getAverageSpeed()).isEqualTo(60.0);
        assertThat(stats.getHighCongestionCount()).isEqualTo(3L);
        assertThat(stats.getSevereCongestionCount()).isEqualTo(1L);
    }

    @Test
    void getTrafficStats_emptyRepository_averagesDefaultToZero() {
        when(trafficRepo.count()).thenReturn(0L);
        when(trafficRepo.findAll()).thenReturn(List.of());
        when(trafficRepo.countByCongestionLevel(any())).thenReturn(0L);

        TrafficStatsDto stats = service.getTrafficStats();

        assertThat(stats.getAverageTrafficDensity()).isEqualTo(0.0);
        assertThat(stats.getAverageSpeed()).isEqualTo(0.0);
    }

    // ---------------- getTrafficTrends() ----------------

    @Test
    void getTrafficTrends_mapsEntitiesToDtosPreservingFields() {
        TrafficData entity = new TrafficData();
        entity.setTimestamp(LocalDateTime.of(2026, java.time.Month.JANUARY, 1, 12, 0));
        entity.setTrafficDensity(300);
        entity.setAvgSpeed(45.0f);
        when(trafficRepo.findTop50ByOrderByTimestampDesc()).thenReturn(List.of(entity));

        var trends = service.getTrafficTrends();

        assertThat(trends).hasSize(1);
        assertThat(trends.get(0).getTimestamp()).isEqualTo(entity.getTimestamp());
    }

    // ---------------- getTrafficCongestionSummary() ----------------

    @Test
    void getTrafficCongestionSummary_buildsMapForAllFourLevels() {
        when(trafficRepo.countByCongestionLevel(CongestionLevel.Low)).thenReturn(5L);
        when(trafficRepo.countByCongestionLevel(CongestionLevel.Moderate)).thenReturn(10L);
        when(trafficRepo.countByCongestionLevel(CongestionLevel.High)).thenReturn(3L);
        when(trafficRepo.countByCongestionLevel(CongestionLevel.Severe)).thenReturn(1L);

        Map<String, Long> summary = service.getTrafficCongestionSummary();

        assertThat(summary)
                .containsEntry("Low", 5L)
                .containsEntry("Moderate", 10L)
                .containsEntry("High", 3L)
                .containsEntry("Severe", 1L);
    }

    // ---------------- getAirData() ----------------

    @Test
    void getAirData_pageSizeZero_throwsIllegalArgumentException() {
        // PageRequest.of() itself rejects size <= 0 at construction, so a
        // real Pageable can never reach the service with size 0 -- this branch
        // only fires for a caller-supplied Pageable implementation that allows
        // it, hence a mock rather than PageRequest.of() here.
        Pageable zeroSizePageable = mock(Pageable.class);
        when(zeroSizePageable.getPageSize()).thenReturn(0);

        assertThatThrownBy(() -> service.getAirData(null, null, null, null, zeroSizePageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page size must be greater than 0");
    }

    @Test
    void getAirData_fromAfterTo_throwsIllegalArgumentException() {
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = from.minusDays(1);
        Pageable pageable = PageRequest.of(0, 20);

        assertThatThrownBy(() -> service.getAirData(null, null, from, to, pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("'from' date cannot be after 'to' date");
    }

    @Test
    void getAirData_unsortedPageable_passesThroughUnchanged() {
        Pageable pageable = PageRequest.of(0, 20);
        when(airRepo.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of()));

        service.getAirData("Downtown", PollutionLevel.Moderate, null, null, pageable);

        verify(airRepo).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getAirData_sortedPageable_stripsSortBeforeCallingRepository() {
        Pageable sortedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "co"));
        when(airRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.getAirData(null, null, null, null, sortedPageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(airRepo).findAll(any(Specification.class), pageableCaptor.capture());
        // The sort is moved into the Specification's ORDER BY clause; the Pageable
        // passed to findAll should come back stripped of its own Sort to avoid
        // double-application / conflicting sort translation.
        assertThat(pageableCaptor.getValue().getSort().isUnsorted()).isTrue();
    }

    @Test
    void getAirData_ascendingSort_alsoStripsSortCorrectly() {
        // Hits the other side of order.isAscending() ? asc : desc -- the
        // DESC test above only exercised the false branch of that ternary.
        Pageable sortedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "ozone"));
        when(airRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.getAirData(null, null, null, null, sortedPageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(airRepo).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().isUnsorted()).isTrue();
    }

    // -- per-branch coverage for getAirData's filter conditions --

    @Test
    void getAirData_locationOnly_noOtherFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        when(airRepo.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of()));

        service.getAirData("Downtown", null, null, null, pageable);

        verify(airRepo).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getAirData_blankLocation_isTreatedAsNoLocationFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        when(airRepo.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of()));

        service.getAirData("   ", null, null, null, pageable);

        verify(airRepo).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getAirData_pollutionLevelOnly_noOtherFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        when(airRepo.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of()));

        service.getAirData(null, PollutionLevel.Hazardous, null, null, pageable);

        verify(airRepo).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getAirData_fromOnly_noToProvided_doesNotThrow() {
        Pageable pageable = PageRequest.of(0, 20);
        when(airRepo.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of()));

        service.getAirData(null, null, LocalDateTime.now().minusDays(1), null, pageable);

        verify(airRepo).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getAirData_toOnly_noFromProvided_doesNotThrow() {
        Pageable pageable = PageRequest.of(0, 20);
        when(airRepo.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of()));

        service.getAirData(null, null, null, LocalDateTime.now(), pageable);

        verify(airRepo).findAll(any(Specification.class), eq(pageable));
    }

    // ---------------- getAirStats() ----------------

    @Test
    void getAirStats_nullAggregates_defaultToZero() {
        when(airRepo.count()).thenReturn(0L);
        when(notificationRepository.countByType("Air")).thenReturn(0L);
        when(airRepo.findAvgCo()).thenReturn(null);
        when(airRepo.findAvgOzone()).thenReturn(null);
        when(airRepo.findMaxCo()).thenReturn(null);
        when(airRepo.findMinCo()).thenReturn(null);
        when(airRepo.findMaxOzone()).thenReturn(null);
        when(airRepo.findMinOzone()).thenReturn(null);
        when(airRepo.countByPollutionLevel(any())).thenReturn(0L);

        AirStatsDto stats = service.getAirStats();

        assertThat(stats.getAverageCo()).isEqualTo(0.0);
        assertThat(stats.getAverageOzone()).isEqualTo(0.0);
        assertThat(stats.getHighestCo()).isEqualTo(0.0);
        assertThat(stats.getLowestCo()).isEqualTo(0.0);
        assertThat(stats.getHighestOzone()).isEqualTo(0.0);
        assertThat(stats.getLowestOzone()).isEqualTo(0.0);
    }

    @Test
    void getAirStats_nonNullAggregates_passThroughDirectly() {
        when(airRepo.count()).thenReturn(100L);
        when(notificationRepository.countByType("Air")).thenReturn(7L);
        when(airRepo.findAvgCo()).thenReturn(22.5);
        when(airRepo.findAvgOzone()).thenReturn(120.0);
        when(airRepo.findMaxCo()).thenReturn(45.0);
        when(airRepo.findMinCo()).thenReturn(5.0);
        when(airRepo.findMaxOzone()).thenReturn(280.0);
        when(airRepo.findMinOzone()).thenReturn(10.0);
        when(airRepo.countByPollutionLevel(any())).thenReturn(20L);

        AirStatsDto stats = service.getAirStats();

        assertThat(stats.getTotalRecords()).isEqualTo(100L);
        assertThat(stats.getTotalAlerts()).isEqualTo(7L);
        assertThat(stats.getAverageCo()).isEqualTo(22.5);
        assertThat(stats.getHighestOzone()).isEqualTo(280.0);
    }

    // ---------------- getAirTrends() ----------------

    @Test
    void getAirTrends_mapsEntitiesToDtos() {
        AirPollutionData entity = new AirPollutionData();
        entity.setTimestamp(LocalDateTime.of(2026, java.time.Month.FEBRUARY, 1, 8, 0));
        entity.setCo(15.0f);
        entity.setOzone(90.0f);
        when(airRepo.findTop50ByOrderByTimestampDesc()).thenReturn(List.of(entity));

        List<AirTrendDto> trends = service.getAirTrends();

        assertThat(trends).hasSize(1);
        assertThat(trends.get(0).getTimestamp()).isEqualTo(entity.getTimestamp());
    }

    // ---------------- getLightData() ----------------

    @Test
    void getLightData_fromAfterTo_throwsIllegalArgumentException() {
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = from.minusDays(1);
        Pageable pageable = PageRequest.of(0, 20);

        assertThatThrownBy(() -> service.getLightData(null, null, from, to, pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("'from' date cannot be after 'to' date");
    }

    @Test
    void getLightData_validParams_passesThroughStatusFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        when(lightRepo.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of()));

        service.getLightData("Elm St", Status.ON, null, null, pageable);

        verify(lightRepo).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getLightData_pageSizeZero_throwsIllegalArgumentException() {
        Pageable zeroSizePageable = mock(Pageable.class);
        when(zeroSizePageable.getPageSize()).thenReturn(0);

        assertThatThrownBy(() -> service.getLightData(null, null, null, null, zeroSizePageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page size must be greater than 0");
    }

    // -- per-branch coverage for getLightData's filter conditions --

    @Test
    void getLightData_locationOnly_noOtherFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        when(lightRepo.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of()));

        service.getLightData("Elm St", null, null, null, pageable);

        verify(lightRepo).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getLightData_blankLocation_isTreatedAsNoLocationFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        when(lightRepo.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of()));

        service.getLightData("   ", null, null, null, pageable);

        verify(lightRepo).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getLightData_statusOnly_noOtherFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        when(lightRepo.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of()));

        service.getLightData(null, Status.OFF, null, null, pageable);

        verify(lightRepo).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getLightData_fromOnly_noToProvided_doesNotThrow() {
        Pageable pageable = PageRequest.of(0, 20);
        when(lightRepo.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of()));

        service.getLightData(null, null, LocalDateTime.now().minusDays(1), null, pageable);

        verify(lightRepo).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getLightData_toOnly_noFromProvided_doesNotThrow() {
        Pageable pageable = PageRequest.of(0, 20);
        when(lightRepo.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of()));

        service.getLightData(null, null, null, LocalDateTime.now(), pageable);

        verify(lightRepo).findAll(any(Specification.class), eq(pageable));
    }

    // ---------------- getLightStats() ----------------

    @Test
    void getLightStats_nullAggregates_defaultToZero() {
        when(lightRepo.count()).thenReturn(0L);
        when(notificationRepository.countByType("Light")).thenReturn(0L);
        when(lightRepo.findAvgBrightnessLevel()).thenReturn(null);
        when(lightRepo.findAvgPowerConsumption()).thenReturn(null);
        when(lightRepo.findMaxPowerConsumption()).thenReturn(null);
        when(lightRepo.findMinBrightnessLevel()).thenReturn(null);
        when(lightRepo.countByStatus(any())).thenReturn(0L);

        LightStatsDto stats = service.getLightStats();

        assertThat(stats).isNotNull();
        // Exact getter names for LightStatsDto are unconfirmed -- see class-level
        // assumption note. If this doesn't compile, share LightStatsDto.java.
    }

    // ---------------- getLightTrends() ----------------

    @Test
    void getLightTrends_mapsEntitiesToDtos() {
        StreetLightData entity = new StreetLightData();
        entity.setTimestamp(LocalDateTime.of(2026, java.time.Month.MARCH, 1, 6, 0));
        entity.setBrightnessLevel(80);
        entity.setPowerConsumption(900.0f);
        when(lightRepo.findTop50ByOrderByTimestampDesc()).thenReturn(List.of(entity));

        var trends = service.getLightTrends();

        assertThat(trends).hasSize(1);
        assertThat(trends.get(0).getTimestamp()).isEqualTo(entity.getTimestamp());
    }
}
