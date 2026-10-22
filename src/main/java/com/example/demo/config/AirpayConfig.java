package com.example.demo.config;



import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Mirrors all context-param values from the original web.xml
 */
@Configuration
public class AirpayConfig {

    @Value("${airpay.username}")
    private String username;

    @Value("${airpay.password}")
    private String password;

    @Value("${airpay.secret}")
    private String secret;

    @Value("${airpay.mercid}")
    private String mercId;

    @Value("${airpay.clientid}")
    private String clientId;

    @Value("${airpay.clientsecret}")
    private String clientSecret;

    @Value("${airpay.tokenUrl}")
    private String tokenUrl;

    @Value("${airpay.paymentUrl}")
    private String paymentUrl;

    @Value("${airpay.kitVersion}")
    private String kitVersion;

    @Value("${airpay.hiddenMode:}")
    private String hiddenMode;
    
 // Add field
    private String frontendBaseUrl;

    // Add getter/setter
    public String getFrontendBaseUrl() { return frontendBaseUrl; }
    public void setFrontendBaseUrl(String frontendBaseUrl) { this.frontendBaseUrl = frontendBaseUrl; }

    // ---------- Getters ----------

    public String getUsername()    { return username; }
    public String getPassword()    { return password; }
    public String getSecret()      { return secret; }
    public String getMercId()      { return mercId; }
    public String getClientId()    { return clientId; }
    public String getClientSecret(){ return clientSecret; }
    public String getTokenUrl()    { return tokenUrl; }
    public String getPaymentUrl()  { return paymentUrl; }
    public String getKitVersion()  { return kitVersion; }
    public String getHiddenMode()  { return hiddenMode; }
}