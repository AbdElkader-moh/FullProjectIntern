package com.backend.sensor_data.dto;

public class TrafficStatsDto {

    private long totalRecords;
    private double averageTrafficDensity;
    private double averageSpeed;
    private long highCongestionCount;
    private long severeCongestionCount;

    public TrafficStatsDto(
            long totalRecords,
            double averageTrafficDensity,
            double averageSpeed,
            long highCongestionCount,
            long severeCongestionCount) {
        this.totalRecords = totalRecords;
        this.averageTrafficDensity = averageTrafficDensity;
        this.averageSpeed = averageSpeed;
        this.highCongestionCount = highCongestionCount;
        this.severeCongestionCount = severeCongestionCount;
    }

    public long getTotalRecords() {
        return totalRecords;
    }

    public double getAverageTrafficDensity() {
        return averageTrafficDensity;
    }

    public double getAverageSpeed() {
        return averageSpeed;
    }

    public long getHighCongestionCount() {
        return highCongestionCount;
    }

    public long getSevereCongestionCount() {
        return severeCongestionCount;
    }
}