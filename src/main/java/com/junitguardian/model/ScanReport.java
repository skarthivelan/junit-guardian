package com.junitguardian.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.Instant;
import java.util.List;

/**
 * Aggregated result of a project coverage scan.
 */
public class ScanReport {
    private final int totalCount;
    private final int coveredCount;
    private final List<SourceFile> uncoveredFiles;
    private final String timestamp;

    public ScanReport(int totalCount, int coveredCount, List<SourceFile> uncoveredFiles) {
        this.totalCount     = totalCount;
        this.coveredCount   = coveredCount;
        this.uncoveredFiles = uncoveredFiles;
        this.timestamp      = Instant.now().toString();
    }

    public int  getTotalCount()    { return totalCount; }
    public int  getCoveredCount()  { return coveredCount; }
    public double getCoveragePercent() {
        return totalCount == 0 ? 100.0 : (coveredCount * 100.0 / totalCount);
    }
    public List<SourceFile> getUncoveredFiles() { return uncoveredFiles; }
    public String getTimestamp()   { return timestamp; }

    public String toJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(new JsonReport(this));
    }

    /** Serialization-friendly DTO */
    private static class JsonReport {
        final int total;
        final int covered;
        final int uncovered;
        final double coveragePercent;
        final List<String> uncoveredFiles;
        final String timestamp;

        JsonReport(ScanReport r) {
            this.total           = r.totalCount;
            this.covered         = r.coveredCount;
            this.uncovered       = r.uncoveredFiles.size();
            this.coveragePercent = r.getCoveragePercent();
            this.uncoveredFiles  = r.uncoveredFiles.stream()
                .map(f -> f.getRelativePath().toString()).toList();
            this.timestamp       = r.timestamp;
        }
    }
}
