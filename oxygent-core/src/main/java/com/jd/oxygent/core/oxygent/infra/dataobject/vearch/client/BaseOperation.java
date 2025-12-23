package com.jd.oxygent.core.oxygent.infra.dataobject.vearch.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
public abstract class BaseOperation {
    private String baseUrl;
    private String userName = "root";
    private String token = "token";

    public BaseOperation() {
    }

    public BaseOperation(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public BaseOperation(String baseUrl, String userName, String token) {
        this.baseUrl = baseUrl;
        this.userName = userName;
        this.token = token;
    }

    public String sendGetRequest(String endpoint) {
        return this.sendGetRequest(endpoint, null);
    }

    public String sendGetRequest(String endpoint, Map<String, Object> params) {
        StringBuilder urlBuilder = new StringBuilder(this.baseUrl + endpoint);
        if (params != null && !params.isEmpty()) {
            urlBuilder.append("?");

            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                urlBuilder.append(key).append("=").append(value).append("&");
            }

            urlBuilder.deleteCharAt(urlBuilder.length() - 1);
        }

        try {
            URL url = new URL(urlBuilder.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            if (this.userName != null && !this.userName.isEmpty() && this.token != null && !this.token.isEmpty()) {
                String auth = this.userName + ":" + this.token;
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    throw new IOException("Failed to send GET request. Response code: " + responseCode);
                } else {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();

                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    reader.close();
                    return response.toString();
                }
            } else {
                throw new VearchException("auth header is empty");
            }
        } catch (IOException e) {
            throw new VearchException("Failed to send GET request. Response code: " + e.getMessage());
        }
    }

    public String sendDeleteRequest(String endpoint) {
        return this.sendDeleteRequest(endpoint, null);
    }

    public String sendDeleteRequest(String endpoint, Map<String, String> params) {
        StringBuilder urlBuilder = new StringBuilder(this.baseUrl + endpoint);
        if (params != null && !params.isEmpty()) {
            urlBuilder.append("?");

            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                urlBuilder.append(key).append("=").append(value).append("&");
            }

            urlBuilder.deleteCharAt(urlBuilder.length() - 1);
        }

        try {
            URL url = new URL(urlBuilder.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("DELETE");
            if (this.userName != null && !this.userName.isEmpty() && this.token != null && !this.token.isEmpty()) {
                String auth = this.userName + ":" + this.token;
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    throw new VearchException("Failed to send GET request. Response code: " + responseCode);
                } else {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();

                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    reader.close();
                    return response.toString();
                }
            } else {
                throw new VearchException("auth header is empty");
            }
        } catch (Exception e) {
            throw new VearchException("Failed to send GET request. Response code: " + e.getMessage());
        }
    }

    public String sendPostRequest(String endpoint, Map<String, Object> body) {
        return this.sendPostRequest(endpoint, JsonUtils.writeValueAsString(body));
    }

    public String sendPostRequest(String endpoint, String body) {
        StringBuilder urlBuilder = new StringBuilder(this.baseUrl + endpoint);
        StringBuilder response = new StringBuilder();

        try {
            URL url = new URL(urlBuilder.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            if (this.userName != null && !this.userName.isEmpty() && this.token != null && !this.token.isEmpty()) {
                String auth = this.userName + ":" + this.token;
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
                connection.setDoOutput(true);
                connection.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);
                int responseCode = connection.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();
                if (responseCode == 200) {
                    return response.toString();
                } else {
                    log.info(response.toString());
                    return response.toString();
                }
            } else {
                throw new VearchException("auth header is empty");
            }
        } catch (Exception e) {
            log.error("response=====", response, e);
            throw new VearchException("Failed to send POST request. Response code: " + e.getMessage());
        }
    }

    public String sendPutRequest(String endpoint, String body) {
        log.info(body);

        try {
            URL url = new URL(this.baseUrl + endpoint);
            log.info(url.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/json");
            if (this.userName != null && !this.userName.isEmpty() && this.token != null && !this.token.isEmpty()) {
                String auth = this.userName + ":" + this.token;
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
                connection.setDoOutput(true);
                connection.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();

                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    reader.close();
                    return response.toString();
                } else {
                    log.warn("Failed to send POST request. Response code: " + connection.getResponseMessage());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    StringBuilder response = new StringBuilder();

                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    reader.close();
                    log.warn(response.toString());
                    throw new VearchException("Failed to send POST request. Response code: " + responseCode);
                }
            } else {
                throw new VearchException("auth header is empty");
            }
        } catch (VearchException e) {
            throw e;
        } catch (Exception e) {
            throw new VearchException("Failed to send POST request. Response code: " + e.getMessage());
        }
    }
}
