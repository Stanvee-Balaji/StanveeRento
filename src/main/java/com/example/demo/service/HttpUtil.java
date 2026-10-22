package com.example.demo.service;



import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HTTP utility — mirrors sendPostData() from sendtoairpay.jsp
 */
@Component
public class HttpUtil {

    /**
     * Sends a POST request with application/x-www-form-urlencoded body.
     *
     * @param targetUrl URL to POST to
     * @param params    Key-value pairs to send as form data
     * @return Raw response body as String
     */
    public String sendPostData(String targetUrl, Map<String, String> params) throws IOException {
        String postBody = params.entrySet().stream()
                .map(e -> {
                    try {
                        return URLEncoder.encode(e.getKey(), "UTF-8")
                                + "="
                                + URLEncoder.encode(e.getValue(), "UTF-8");
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                })
                .collect(Collectors.joining("&"));

        HttpURLConnection conn = null;
        try {
            URL url = new URL(targetUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(15_000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Accept-Charset", "UTF-8");

            try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8")) {
                writer.write(postBody);
                writer.flush();
            }

            int responseCode = conn.getResponseCode();
            InputStream is = (responseCode >= 200 && responseCode < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

//            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
//                StringBuilder response = new StringBuilder();
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    response.append(line);
//                }
//                return response.toString();
//            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                String rawResponse = response.toString();
                System.out.println("[HttpUtil] POST to " + targetUrl + " → HTTP " + responseCode + " | Body: " + rawResponse);
                return rawResponse;
            }
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}