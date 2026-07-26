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

    private AirStatsDto(Builder builder) {
        this.totalRecords = builder.totalRecords;
        this.averageCo = builder.averageCo;
        this.averageOzone = builder.averageOzone;
        this.highestCo = builder.highestCo;
        this.lowestCo = builder.lowestCo;
        this.highestOzone = builder.highestOzone;
        this.lowestOzone = builder.lowestOzone;
        this.totalAlerts = builder.totalAlerts;
        this.pollutionLevelBreakdown = builder.pollutionLevelBreakdown;
    }

    public static Builder builder() {
        return new Builder();
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

    public static final class Builder {
        private long totalRecords;
        private double averageCo;
        private double averageOzone;
        private double highestCo;
        private double lowestCo;
        private double highestOzone;
        private double lowestOzone;
        private long totalAlerts;
        private Map<String, Long> pollutionLevelBreakdown;

        private Builder() {}

        public Builder totalRecords(long totalRecords) { this.totalRecords = totalRecords; return this; }
        public Builder averageCo(double averageCo) { this.averageCo = averageCo; return this; }
        public Builder averageOzone(double averageOzone) { this.averageOzone = averageOzone; return this; }
        public Builder highestCo(double highestCo) { this.highestCo = highestCo; return this; }
        public Builder lowestCo(double lowestCo) { this.lowestCo = lowestCo; return this; }
        public Builder highestOzone(double highestOzone) { this.highestOzone = highestOzone; return this; }
        public Builder lowestOzone(double lowestOzone) { this.lowestOzone = lowestOzone; return this; }
        public Builder totalAlerts(long totalAlerts) { this.totalAlerts = totalAlerts; return this; }
        public Builder pollutionLevelBreakdown(Map<String, Long> pollutionLevelBreakdown) {
            this.pollutionLevelBreakdown = pollutionLevelBreakdown;
            return this;
        }

        public AirStatsDto build() {
            return new AirStatsDto(this);
        }
    }
}
