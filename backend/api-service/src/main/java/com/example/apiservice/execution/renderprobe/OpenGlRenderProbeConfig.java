package com.example.apiservice.execution.renderprobe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenGlRenderProbeConfig {

    @JsonProperty("enabled")
    private boolean enabled;

    @JsonProperty("injectorExePath")
    private String injectorExePath;

    @JsonProperty("hookDllPath")
    private String hookDllPath;

    public boolean isEnabled() {
        return enabled;
    }

    public String getInjectorExePath() {
        return injectorExePath;
    }

    public String getHookDllPath() {
        return hookDllPath;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setInjectorExePath(String injectorExePath) {
        this.injectorExePath = injectorExePath;
    }

    public void setHookDllPath(String hookDllPath) {
        this.hookDllPath = hookDllPath;
    }
}