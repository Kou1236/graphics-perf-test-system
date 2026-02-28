package com.example.apiservice.execution.renderprobe;

public class RenderProbePlan {

    private boolean enabled;
    private String injectorExePath;
    private String hookDllPath;
    private String renderEventsPath;

    public boolean isEnabled() {
        return enabled;
    }

    public String getInjectorExePath() {
        return injectorExePath;
    }

    public String getHookDllPath() {
        return hookDllPath;
    }

    public String getRenderEventsPath() {
        return renderEventsPath;
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

    public void setRenderEventsPath(String renderEventsPath) {
        this.renderEventsPath = renderEventsPath;
    }
}