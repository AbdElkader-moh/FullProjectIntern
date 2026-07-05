package com.backend.sensor_data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(name = "LightStatsDto", description = "Aggregated street light statistics for dashboard summary cards and charts")
public class LightStatsDto {

    @Schema(description = "Total number of street light records stored", example = "980")
    private final long totalRecords;

    @Schema(description = "Average brightness level across all records (0–100)", example = "72.4")
    private final double averageBrightness;

    @Schema(description = "Average power consumption across all records in watts", example = "43.8")
    private final double averagePowerConsumption;

    @Schema(description = "Highest recorded power consumption in watts", example = "98.5")
    private final double highestPowerConsumption;

    @Schema(description = "Lowest recorded brightness level (0–100)", example = "5.0")
    private final double lowestBrightness;

    @Schema(description = "Total number of alert notifications triggered for street light sensors", example = "21")
    private final long totalAlerts;

    @Schema(
        description = "Count of records per operational status. Keys: ON, OFF",
        example = "{\"ON\": 750, \"OFF\": 230}"
    )
    private final Map<String, Long> statusBreakdown;

    public LightStatsDto(long totalRecords, double averageBrightness,
            double averagePowerConsumption, double highestPowerConsumption,
            double lowestBrightness, long totalAlerts, Map<String, Long> statusBreakdown) {
        this.totalRecords = totalRecords;
        this.averageBrightness = averageBrightness;
        this.averagePowerConsumption = averagePowerConsumption;
        this.highestPowerConsumption = highestPowerConsumption;
        this.lowestBrightness = lowestBrightness;
        this.totalAlerts = totalAlerts;
        this.statusBreakdown = statusBreakdown;
    }

    public long getTotalRecords() { return totalRecords; }
    public double getAverageBrightness() { return averageBrightness; }
    public double getAveragePowerConsumption() { return averagePowerConsumption; }
    public double getHighestPowerConsumption() { return highestPowerConsumption; }
    public double getLowestBrightness() { return lowestBrightness; }
    public long getTotalAlerts() { return totalAlerts; }
    public Map<String, Long> getStatusBreakdown() { return statusBreakdown; }
}