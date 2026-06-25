package com.backend.sensor_data.dto;

import java.util.Map;

public class LightStatsDto {

    private final long totalRecords;
    private final double averageBrightness;
    private final double averagePowerConsumption;
    private final double highestPowerConsumption;
    private final double lowestBrightness;
    private final long totalAlerts;
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