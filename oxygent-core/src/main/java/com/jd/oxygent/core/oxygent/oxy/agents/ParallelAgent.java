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
package com.jd.oxygent.core.oxygent.oxy.agents;

import com.jd.oxygent.core.oxygent.schemas.memory.Memory;
import com.jd.oxygent.core.oxygent.schemas.memory.Message;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Parallel Agent - Manages multiple agents calling LLM operations simultaneously
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@ToString(callSuper = true)
@Slf4j
public class ParallelAgent extends LocalAgent {
    private static class TaskMetric {
        OxyResponse response;
        long durationMs;
        boolean success;

        TaskMetric(OxyResponse response, long durationMs, boolean success) {
            this.response = response;
            this.durationMs = durationMs;
            this.success = success;
        }
    }

    // Parallel thread pool configuration (adjustable via builder)
    @lombok.Builder.Default
    private int coreParallelThreads = 4;
    @lombok.Builder.Default
    private long keepAliveSeconds = 60;
    @lombok.Builder.Default
    private int queueCapacity = 128;

    // Concurrent executor (concurrently calls sub-agents)
    private transient ExecutorService parallelExecutor;

    // Asynchronous logging executor (avoid blocking in business threads)
    private transient ExecutorService loggingExecutor;

    @lombok.Builder.Default
    private boolean enableAsyncLogging = true;

    @lombok.Builder.Default
    private boolean logTimings = true;

    /**
     * Initialize parallel agent
     *
     * <p>Execute agent initialization process, including calling parent class initialization logic
     * and setting default prompts. This method ensures the chat agent has complete conversational capabilities.</p>
     */
    @Override
    public void init() {
        super.init();
        // Initialize parallel executor and asynchronous logging executor
        if (parallelExecutor == null) {
            parallelExecutor = new ThreadPoolExecutor(
                    Math.max(1, coreParallelThreads),
                    Math.max(Math.max(1, coreParallelThreads), this.getSemaphoreCount()),
                    Math.max(0, keepAliveSeconds),
                    TimeUnit.SECONDS,
                    new SynchronousQueue<>(),
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );
        }
        if (loggingExecutor == null) {
            loggingExecutor = Executors.newSingleThreadExecutor();
        }

        log.debug("ParallelAgent initialization completed: {}, coreThreads={}, maxThreads={}, keepAlive={}s, queueCapacity={}, rejectionPolicy=CallerRunsPolicy, asyncLogging={}, logTimings={}",
                this.getName(), coreParallelThreads, this.getSemaphoreCount(), keepAliveSeconds, queueCapacity, enableAsyncLogging, logTimings);
    }


    public OxyResponse _execute(OxyRequest oxyRequest) {
        // 1. Generate parallelId
        String parallelId = UUID.randomUUID().toString();

        // 2. Concurrently execute calls for all team members
        List<CompletableFuture<OxyResponse>> futureList = new ArrayList<>();
        for (String permittedToolName : this.getPermittedToolNameList()) {

            Map<String, Object> callRequest = new HashMap<>();
            callRequest.put("callee", permittedToolName);
            callRequest.put("arguments", oxyRequest.getArguments());
            callRequest.put("parallelId", parallelId);

            CompletableFuture<OxyResponse> future = CompletableFuture.supplyAsync(() -> {
                OxyResponse resp;
                try {
                    resp = oxyRequest.call(callRequest);
                } catch (Exception ex) {
                    log.warn("[Task Exception] tool={}, error={}", permittedToolName, ex.toString());
                    resp = new OxyResponse();
                    resp.setOutput("");
                }
                return resp;
            }, parallelExecutor);
            futureList.add(future);
        }

        // 3. Wait for all concurrent tasks to complete
        CompletableFuture<Void> allDoneFuture = CompletableFuture.allOf(
                futureList.toArray(new CompletableFuture[0])
        );

        // 4. Process results
        List<OxyResponse> oxyResponses = futureList.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        // 5. Construct Memory and Message
        Memory tempMemory = new Memory(this.shortMemorySize);
        tempMemory.addMessage(
                Message.systemMessage(
                        "You are a helpful assistant, the user's question is:" + oxyRequest.getQuery() +
                                ".\nPlease summarize the results of the parallel execution of the above tasks."
                )
        );

        StringBuilder sb = new StringBuilder("The parallel results are as following:\n");
        for (int i = 0; i < oxyResponses.size(); i++) {
            sb.append(i + 1)
                    .append(". ")
                    .append(oxyResponses.get(i).getOutput())
                    .append("\n");
        }

        tempMemory.addMessage(
                Message.userMessage(sb.toString())
        );

        // 6. Call llmModel again
        Map<String, Object> arguments = new HashMap<>();
        arguments.put(
                "messages",
                tempMemory.toDictList()
        );
        Map<String, Object> callRequest = Map.of("callee", this.llmModel, "arguments", Map.of("messages", tempMemory));
        return oxyRequest.call(callRequest);

    }

}
