package com.backend.sensor_data.dto;

import java.util.Map;

public class AirStatsDto {

    private final long totalRecords;
    private final double averageCo;
    private final double averageOzone;
    private final double highestCo;
    private final double lowestCo;
    private final double highestOzone;
    private final double lowestOzone;
    private final long totalAlerts;
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