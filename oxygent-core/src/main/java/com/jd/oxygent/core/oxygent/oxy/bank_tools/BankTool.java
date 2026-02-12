package com.jd.oxygent.core.oxygent.oxy.bank_tools;

import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyState;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * BankTool is a concrete implementation of BaseBank that handles HTTP requests
 * to external bank servers. It provides HTTP client functionality for interacting
 * with remote bank services within the OxyGent framework.
 *
 * <p>This class provides:
 * <ul>
 *   <li>HTTP client functionality for bank operations</li>
 *   <li>Support for GET and POST methods</li>
 *   <li>Configurable headers and timeouts</li>
 *   <li>Permission control for secure operations</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0
 * @since 1.0
 */
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class BankTool extends BaseBank {

    /**
     * URL of the remote bank server.
     */
    @Builder.Default
    private String serverUrl = "";

    @Builder.Default
    private String serverName="";

    /**
     * HTTP method to use for requests.
     * Default is "GET" as specified in Python version.
     */
    @Builder.Default
    private String method = "POST";

    /**
     * Whether permission is required for bank operations.
     * Default is true as specified in Python version.
     */
    @Builder.Default
    private boolean isPermissionRequired = true;

    /**
     * Extra HTTP headers to include in requests.
     */
    @Builder.Default
    private Map<String, String> headers = new HashMap<>();

    /**
     * Whether the bank is retrievable.
     * Default is false as specified in Python version.
     */
    @Builder.Default
    private boolean isRetrievable = false;

    /**
     * Executes the bank operation by making an HTTP request to the remote server.
     * This implementation always uses POST method as shown in the Python example.
     *
     * @param oxyRequest the request containing arguments for the bank operation
     * @return the response containing the HTTP response from the remote server
     */
    @Override
    protected OxyResponse _execute(OxyRequest oxyRequest) {
        try {
            if (serverUrl == null || serverUrl.trim().isEmpty()) {
                log.error("BankTool '{}': serverUrl is not configured", getName());
                return new OxyResponse(OxyState.FAILED, "Bank server URL is not configured", null, oxyRequest);
            }

            log.debug("BankTool '{}': Sending request to {}", getName(), serverUrl);

            // Prepare HTTP request
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl))
                    .timeout(Duration.ofSeconds((long)this.getTimeout()));

            // Add headers
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    requestBuilder.header(entry.getKey(), entry.getValue());
                }
            }

            // Prepare request body from arguments
            Map<String, Object> arguments = oxyRequest.getArguments();
            String requestBody = "";

            if (arguments != null && !arguments.isEmpty()) {
                // In a real implementation, you would serialize arguments to JSON
                // For now, we'll create a simple JSON string
                requestBody = JsonUtils.toJSONString(arguments);
                requestBuilder.header("Content-Type", "application/json");
            }

            // Build and send request
            HttpRequest request;
            if ("POST".equalsIgnoreCase(method)) {
                request = requestBuilder.POST(HttpRequest.BodyPublishers.ofString(requestBody)).build();
            } else if ("GET".equalsIgnoreCase(method)) {
                request = requestBuilder.GET().build();
            } else {
                log.error("BankTool '{}': Unsupported HTTP method: {}", getName(), method);
                return new OxyResponse(OxyState.FAILED, "Unsupported HTTP method: " + method, null, oxyRequest);
            }

            // Send HTTP request
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            log.debug("BankTool '{}': Received response with status {}", getName(), response.statusCode());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return new OxyResponse(OxyState.COMPLETED, response.body(), null, oxyRequest);
            } else {
                log.error("BankTool '{}': HTTP request failed with status {}", getName(), response.statusCode());
                return new OxyResponse(OxyState.FAILED,
                        String.format("HTTP request failed with status %d: %s",
                                response.statusCode(), response.body()), null, oxyRequest);
            }

        } catch (IOException e) {
            log.error("BankTool '{}': IO error during HTTP request", getName(), e);
            return new OxyResponse(OxyState.FAILED, "IO error: " + e.getMessage(), null, oxyRequest);
        } catch (InterruptedException e) {
            log.error("BankTool '{}': Request interrupted", getName(), e);
            Thread.currentThread().interrupt(); // Restore interrupt status
            return new OxyResponse(OxyState.FAILED, "Request interrupted", null, oxyRequest);
        } catch (Exception e) {
            log.error("BankTool '{}': Unexpected error", getName(), e);
            return new OxyResponse(OxyState.FAILED, "Unexpected error: " + e.getMessage(), null, oxyRequest);
        }
    }
    /**
     * Async version of execute method for non-blocking operations.
     * This is the equivalent of Python's async _execute method.
     */
    public CompletableFuture<OxyResponse> executeAsync(OxyRequest oxyRequest) {
        return CompletableFuture.supplyAsync(() -> _execute(oxyRequest));
    }
}