package com.example.apiservice.execution.renderprobe;

public class RenderSummary {

    private String renderEventsPath;
    private Long swapEventCount;
    private Double durationSec;
    private Double swapsPerSecAvg;
    private String error;

    public String getRenderEventsPath() {
        return renderEventsPath;
    }

    public Long getSwapEventCount() {
        return swapEventCount;
    }

    public Double getDurationSec() {
        return durationSec;
    }

    public Double getSwapsPerSecAvg() {
        return swapsPerSecAvg;
    }

    public String getError() {
        return error;
    }

    public void setRenderEventsPath(String renderEventsPath) {
        this.renderEventsPath = renderEventsPath;
    }

    public void setSwapEventCount(Long swapEventCount) {
        this.swapEventCount = swapEventCount;
    }

    public void setDurationSec(Double durationSec) {
        this.durationSec = durationSec;
    }

    public void setSwapsPerSecAvg(Double swapsPerSecAvg) {
        this.swapsPerSecAvg = swapsPerSecAvg;
    }

    public void setError(String error) {
        this.error = error;
    }
}