package com.msb.supsale.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "greennode")
public class GreenNodeConfig {
    private String apiKey;
    private String baseUrl;
    private String modelCustomer;
    private String modelStaff;
    private boolean mock;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModelCustomer() { return modelCustomer; }
    public void setModelCustomer(String modelCustomer) { this.modelCustomer = modelCustomer; }
    public String getModelStaff() { return modelStaff; }
    public void setModelStaff(String modelStaff) { this.modelStaff = modelStaff; }
    public boolean isMock() { return mock; }
    public void setMock(boolean mock) { this.mock = mock; }
}
