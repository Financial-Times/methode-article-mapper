package com.ft.methodearticlemapper.health;

import com.ft.platform.dropwizard.AdvancedHealthCheck;
import com.ft.platform.dropwizard.AdvancedResult;

public class StandaloneHealthCheck extends AdvancedHealthCheck {
    private final String panicGuideUrl;
    
    public StandaloneHealthCheck(String panicGuideUrl) {
        super("Service is up and running");
        
        this.panicGuideUrl = panicGuideUrl;
    }

    public AdvancedResult checkAdvanced() throws Exception {
        return AdvancedResult.healthy();
    }

    @Override
    protected int severity() {
        return 1;
    }

    @Override
    protected String businessImpact() {
        return "No business impact";
    }

    @Override
    protected String technicalSummary() {
        return "This service is running without external dependencies.";
    }

    @Override
    protected String panicGuideUrl() {
        return panicGuideUrl;
    }
}
