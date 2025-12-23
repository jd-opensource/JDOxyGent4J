/*
 * Copyright 2025 JD.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this project except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.oxygent.core.oxygent.infra.databases;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Database Infrastructure Abstract Class
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public abstract class BaseDB {

    private static final Logger logger = Logger.getLogger(BaseDB.class.getName());

    /**
     * Execute operation with retry mechanism
     *
     * @param operation  The operation to execute
     * @param maxRetries Maximum number of retries
     * @param <T>        Return type
     * @return Operation result
     */
    protected <T> T executeWithRetry(Supplier<T> operation, int maxRetries) {
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries) {
                    logger.warning(String.format("Operation failed, attempt %d/%d: %s",
                            attempt + 1, maxRetries + 1, e.getMessage()));

                    try {
                        Thread.sleep(1000 * (attempt + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Operation interrupted", ie);
                    }
                } else {
                    logger.severe(String.format("Operation failed after %d attempts: %s",
                            maxRetries + 1, e.getMessage()));
                }
            }
        }

        throw new RuntimeException("Operation failed after retries", lastException);
    }

    /**
     * Execute operation asynchronously with retry mechanism
     *
     * @param operation  The operation to execute
     * @param maxRetries Maximum number of retries
     * @param <T>        Return type
     * @return CompletableFuture with operation result
     */
    protected <T> CompletableFuture<T> executeAsyncWithRetry(Supplier<T> operation, int maxRetries) {
        return CompletableFuture.supplyAsync(() -> executeWithRetry(operation, maxRetries));
    }

    /**
     * Close database connection and clean up resources
     */
    public abstract void close();

}