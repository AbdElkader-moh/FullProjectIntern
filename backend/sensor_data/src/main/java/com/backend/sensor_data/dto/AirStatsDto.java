package com.backend.sensor_data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(name = "AirStatsDto", description = "Aggregated air pollution statistics for dashboard summary cards and charts")
public class AirStatsDto {

    @Schema(description = "Total number of air pollution records stored", example = "1240")
    private final long totalRecords;

    @Schema(description = "Average CO level across all records in ppm", example = "1.85")
    private final double averageCo;

    @Schema(description = "Average ozone level across all records in ppm", example = "0.52")
    private final double averageOzone;

    @Schema(description = "Highest recorded CO level in ppm", example = "9.4")
    private final double highestCo;

    @Schema(description = "Lowest recorded CO level in ppm", example = "0.1")
    private final double lowestCo;

    @Schema(description = "Highest recorded ozone level in ppm", example = "3.2")
    private final double highestOzone;

    @Schema(description = "Lowest recorded ozone level in ppm", example = "0.05")
    private final double lowestOzone;

    @Schema(description = "Total number of alert notifications triggered for air pollution sensors", example = "37")
    private final long totalAlerts;

    @Schema(
        description = "Count of records per pollution level. Keys: Good, Moderate, Unhealthy, Very_Unhealthy, Hazardous",
        example = "{\"Good\": 500, \"Moderate\": 400, \"Unhealthy\": 200, \"Very_Unhealthy\": 100, \"Hazardous\": 40}"
    )
    private final Map<String, Long> pollutionLevelBreakdown;

    public AirStatsDto(long totalRecords, double averageCo, double averageOzone,
            double highestCo, double lowestCo, double highestOzone, double lowestOzone,
            long totalAlerts, Map<String, Long> pollutionLevelBreakdown) {
        this.totalRecords = totalRecords;
        this.averageCo = averageCo;
        this.averageOzone = averageOzone;
        this.highestCo = highestCo;
        this.lowestCo = lowestCo;
        this.highestOzone = highestOzone;
        this.lowestOzone = lowestOzone;
        this.totalAlerts = totalAlerts;
        this.pollutionLevelBreakdown = pollutionLevelBreakdown;
    }

    public long getTotalRecords() { return totalRecords; }
    public double getAverageCo() { return averageCo; }
    public double getAverageOzone() { return averageOzone; }
    public double getHighestCo() { return highestCo; }
    public double getLowestCo() { return lowestCo; }
    public double getHighestOzone() { return highestOzone; }
    public double getLowestOzone() { return lowestOzone; }
    public long getTotalAlerts() { return totalAlerts; }
    public Map<String, Long> getPollutionLevelBreakdown() { return pollutionLevelBreakdown; }
}