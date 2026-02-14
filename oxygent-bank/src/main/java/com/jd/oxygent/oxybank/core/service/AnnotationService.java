package com.jd.oxygent.oxybank.core.service;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.jd.oxygent.core.oxygent.utils.DateUtils;
import com.jd.oxygent.oxybank.core.config.ServiceConfig;
import com.jd.oxygent.oxybank.core.model.annotation.AnnotationModel;
import com.jd.oxygent.oxybank.core.model.annotation.DepositModel;
import com.jd.oxygent.oxybank.core.model.annotation.QADataModel;
import com.jd.oxygent.oxybank.core.model.annotation.QueryModel;
import com.jd.oxygent.oxybank.core.model.annotation.StatsModel;
import lombok.extern.slf4j.Slf4j;

import com.jd.oxygent.oxybank.core.storer.docmanager.AnnotationManager;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Annotation service - Core business logic
 *
 * Handles all annotation platform business logic:
 * - Data deposit and deduplication
 * - Data query and filtering
 * - Annotation management
 * - KB ingestion (method calls)
 * - Statistics and analysis
 */
@Slf4j
public class AnnotationService {

    private final AnnotationManager annotationManager;

    private final ServiceConfig.AnnotationConfig config;

    private String _current_batchId;

    private final com.jd.oxygent.oxybank.core.config.ServiceConfig settings = com.jd.oxygent.oxybank.core.config.ServiceConfig.getInstance();

    private final OkHttpClient httpClient = new OkHttpClient.Builder().build();

    public AnnotationService(AnnotationManager annotationManager, ServiceConfig.AnnotationConfig config) {
        this.annotationManager = annotationManager;
        this.config = config;
    }

    private String generateDataId() {
        return UUID.randomUUID().toString();
    }

    private String generateBatchId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(timestamp.getBytes());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8 && i < digest.length; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("生成 batchId 失败", e);
            return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
    }

    private String computeDataHash(String question, String answer) {
        String content = question.trim() + "|||" + answer.trim();
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(content.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("计算数据哈希失败", e);
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> buildEsQueryFromFilters(Map<String, Object> filters) {
        Map<String, Object> query = new HashMap<>();
        Map<String, Object> bool = new HashMap<>();
        List<Object> must = new ArrayList<>();
        bool.put("must", must);
        query.put("bool", bool);

        if (filters.containsKey("status")) {
            must.add(Map.of("term", Map.of("status", filters.get("status"))));
        }
        if (filters.containsKey("priority")) {
            must.add(Map.of("term", Map.of("priority", filters.get("priority"))));
        }
        if (filters.containsKey("dataType")) {
            must.add(Map.of("term", Map.of("dataType", filters.get("dataType"))));
        }
        if (filters.containsKey("caller")) {
            must.add(
                    Map.of(
                            "wildcard",
                            Map.of(
                                    "caller",
                                    Map.of("value", "*" + filters.get("caller") + "*")
                            )
                    )
            );
        }
        if (filters.containsKey("callee")) {
            must.add(
                    Map.of(
                            "wildcard",
                            Map.of(
                                    "callee",
                                    Map.of("value", "*" + filters.get("callee") + "*")
                            )
                    )
            );
        }
        if (filters.containsKey("category")) {
            must.add(Map.of("term", Map.of("category", filters.get("category"))));
        }
        if (filters.containsKey("tags")) {
            must.add(Map.of("terms", Map.of("tags", filters.get("tags"))));
        }
        if (filters.containsKey("created_after")) {
            must.add(
                    Map.of(
                            "range",
                            Map.of(
                                    "created_at",
                                    Map.of("gte", filters.get("created_after"))
                            )
                    )
            );
        }
        if (filters.containsKey("created_before")) {
            must.add(
                    Map.of(
                            "range",
                            Map.of(
                                    "created_at",
                                    Map.of("lte", filters.get("created_before"))
                            )
                    )
            );
        }
        if (filters.containsKey("search_text")) {
            must.add(
                    Map.of(
                            "multi_match",
                            Map.of(
                                    "query", filters.get("search_text"),
                                    "fields", List.of("question", "answer"),
                                    "type", "best_fields"
                            )
                    )
            );
        }
        if (filters.containsKey("trace_id")) {
            must.add(
                    Map.of(
                            "wildcard",
                            Map.of(
                                    "source_trace_id",
                                    Map.of("value", "*" + filters.get("trace_id") + "*")
                            )
                    )
            );
        }
        if (filters.containsKey("group_id")) {
            must.add(
                    Map.of(
                            "wildcard",
                            Map.of(
                                    "source_group_id",
                                    Map.of("value", "*" + filters.get("group_id") + "*")
                            )
                    )
            );
        }

        if (must.isEmpty()) {
            return Map.of("match_all", Map.of());
        }
        return Map.of("bool", Map.of("must", must));
    }

    private String inferDataType(
            String caller,
            String callee,
            String callerType,
            String calleeType
    ) {
        if ((caller == null || caller.isEmpty()) && (callee == null || callee.isEmpty())) {
            return config.getDefaultDataType();
        }
        if ("user".equals(caller)) {
            return "e2e";
        }
        if ((caller == null || caller.isEmpty()) && callee != null && !callee.isEmpty()) {
            return "tool";
        }
        if (caller != null && !caller.isEmpty() && (callee == null || callee.isEmpty())) {
            return "agent";
        }
        if ("llm".equals(calleeType) || "llm".equals(callerType)) {
            return "llm";
        }
        return "agent";
    }

    private int inferPriority(String caller, String dataType) {
        if ("e2e".equals(dataType)) {
            return 0;
        }
        if (caller == null || caller.isEmpty()) {
            return config.getDefaultPriority();
        }
        return 4;
    }

    /**
     * Deposit single data.
     */
    public DepositModel.DepositResponse depositData(DepositModel.DepositRequest request) {
        try {
            String dataHash = computeDataHash(request.getQuestion(), request.getAnswer());
            boolean isDuplicate = annotationManager.existsByHash(dataHash);
            if (isDuplicate) {
                Map<String, Object> existing = getExistingByHash(dataHash);
                if (existing != null) {
                    log.info("Duplicate found in ES: hash={}...", dataHash.substring(0, 16));
                    return new DepositModel.DepositResponse(
                            (String) existing.get("data_id"),
                            dataHash,
                            "pending",
                            true,
                            "Data already exists (ES)"
                    );
                }
            }

            String dataType = request.getDataType() != null
                    ? request.getDataType()
                    : inferDataType(
                    request.getCaller(),
                    request.getCallee(),
                    request.getCallerType(),
                    request.getCalleeType()
            );

            Integer priorityVal = request.getPriority();
            int priority = priorityVal != null
                    ? priorityVal
                    : inferPriority(request.getCaller(), dataType);

            if (_current_batchId == null) {
                _current_batchId = generateBatchId();
            }

            String now = DateUtils.formatDate(new Date(), DateUtils.DEFAULT_DATE_TIME_FORMAT);
            QADataModel.QADataItem qaData = new QADataModel.QADataItem();
            qaData.setDataId(generateDataId());
            qaData.setDataHash(dataHash);
            qaData.setQuestion(request.getQuestion());
            qaData.setAnswer(request.getAnswer());
            qaData.setSourceTraceId(
                    request.getSourceTraceId() != null
                            ? request.getSourceTraceId()
                            : generateDataId()
            );
            qaData.setSourceRequestId(
                    request.getSourceRequestId() != null
                            ? request.getSourceRequestId()
                            : generateDataId()
            );
            qaData.setSourceGroupId(
                    request.getSourceGroupId() != null
                            ? request.getSourceGroupId()
                            : generateBatchId()
            );
            qaData.setCaller(
                    request.getCaller() != null && !request.getCaller().isEmpty()
                            ? request.getCaller()
                            : "unknown"
            );
            qaData.setCallee(
                    request.getCallee() != null && !request.getCallee().isEmpty()
                            ? request.getCallee()
                            : "unknown"
            );
            qaData.setCallerType(request.getCallerType());
            qaData.setCalleeType(request.getCalleeType());
            qaData.setDataType(dataType);
            qaData.setPriority(priority);
            qaData.setCategory(request.getCategory());
            qaData.setTags(
                    request.getTags() != null
                            ? new ArrayList<>(request.getTags())
                            : new ArrayList<>()
            );
            qaData.setStatus("pending");
            qaData.setAnnotation(new HashMap<>());
            qaData.setScores(new HashMap<>());
            qaData.setRejectReason(null);
            qaData.setKbStatus("pending");
            qaData.setKbIngestedAt(null);
            qaData.setKbErrorMessage(null);
            qaData.setKbExtra(new HashMap<>());
            qaData.setBatchId(_current_batchId);
            qaData.setCreatedAt(now);
            qaData.setUpdatedAt(now);
            qaData.setExtra(
                    request.getExtra() != null
                            ? new HashMap<>(request.getExtra())
                            : new HashMap<>()
            );

            annotationManager.create(qaData);

            log.info(
                    "Data deposited: data_id={}, trace_id={}, priority={}, type={}, caller={}, callee={}",
                    qaData.getDataId(),
                    qaData.getSourceTraceId(),
                    qaData.getPriority(),
                    qaData.getDataType(),
                    qaData.getCaller(),
                    qaData.getCallee()
            );

            return new DepositModel.DepositResponse(
                    qaData.getDataId(),
                    dataHash,
                    "pending",
                    false,
                    "Deposit successful"
            );
        } catch (Exception e) {
            log.error("Deposit failed", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Batch deposit data.
     */
    public DepositModel.DepositBatchResponse depositBatch(DepositModel.DepositBatchRequest request) {
        try {
            String batchId = request.getBatchId() != null
                    ? request.getBatchId()
                    : generateBatchId();
            _current_batchId = batchId;

            List<DepositModel.DepositResponse> results = new ArrayList<>();
            int successCount = 0;
            int duplicateCount = 0;
            int failedCount = 0;

            for (DepositModel.DepositRequest depositRequest : request.getDataList()) {
                try {
                    DepositModel.DepositResponse response = depositData(depositRequest);
                    results.add(response);
                    if (response.isDuplicate()) {
                        duplicateCount += 1;
                    } else {
                        successCount += 1;
                    }
                } catch (Exception e) {
                    log.error("Batch deposit item failed", e);
                    failedCount += 1;
                }
            }

            log.info(
                    "Batch deposit completed: batchId={}, total={}, success={}, duplicate={}, failed={}",
                    batchId,
                    request.getDataList().size(),
                    successCount,
                    duplicateCount,
                    failedCount
            );

            return new DepositModel.DepositBatchResponse(
                    batchId,
                    request.getDataList().size(),
                    successCount,
                    duplicateCount,
                    failedCount,
                    results
            );
        } catch (Exception e) {
            log.error("Batch deposit failed", e);
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> getExistingByHash(String dataHash) {
        try {
            Map<String, Object> query =
                    Map.of(
                            "query",
                            Map.of("term", Map.of("dataHash", dataHash)),
                            "size",
                            1
                    );
            return annotationManager.searchOneByQuery(query);
        } catch (Exception e) {
            log.error("Query existing data failed", e);
            return null;
        }
    }

    public Map<String, Object> getDataList(QueryModel.DataListQueryParams params) {
        try {
            Map<String, Object> filters = new HashMap<>();
            if (params.getStatus() != null && !params.getStatus().isEmpty()) {
                filters.put("status", params.getStatus());
            }
            if (params.getPriority() != null) {
                filters.put("priority", params.getPriority());
            }
            if (params.getDataType() != null && !params.getDataType().isEmpty()) {
                filters.put("dataType", params.getDataType());
            }
            if (params.getCaller() != null && !params.getCaller().isEmpty()) {
                filters.put("caller", params.getCaller());
            }
            if (params.getCallee() != null && !params.getCallee().isEmpty()) {
                filters.put("callee", params.getCallee());
            }
            if (params.getCategory() != null && !params.getCategory().isEmpty()) {
                filters.put("category", params.getCategory());
            }
            if (params.getTags() != null && !params.getTags().isEmpty()) {
                filters.put("tags", params.getTags());
            }
            if (params.getCreatedAfter() != null && !params.getCreatedAfter().isEmpty()) {
                filters.put("created_after", params.getCreatedAfter());
            }
            if (params.getCreatedBefore() != null && !params.getCreatedBefore().isEmpty()) {
                filters.put("created_before", params.getCreatedBefore());
            }
            if (params.getSearchText() != null && !params.getSearchText().isEmpty()) {
                filters.put("search_text", params.getSearchText());
            }
            if (params.getTraceId() != null && !params.getTraceId().isEmpty()) {
                filters.put("trace_id", params.getTraceId());
            }
            if (params.getGroupId() != null && !params.getGroupId().isEmpty()) {
                filters.put("group_id", params.getGroupId());
            }

            Map<String, Object> pagination = Map.of(
                    "page", params.getPage(),
                    "page_size", params.getPageSize()
            );

            Map<String, Object> sortItem = Map.of(
                    "field",
                    params.getSortBy() != null && !params.getSortBy().isEmpty()
                            ? params.getSortBy()
                            : "created_at",
                    "order",
                    params.getSortOrder() != null && !params.getSortOrder().isEmpty()
                            ? params.getSortOrder()
                            : "desc"
            );
            List<Map<String, Object>> sorting = List.of(sortItem);

            return annotationManager.listQuery(filters, pagination, sorting);
        } catch (Exception e) {
            log.error("Data list query failed", e);
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> getDataById(String dataId) {
        try {
            return annotationManager.getById(dataId);
        } catch (Exception e) {
            log.error("Get data failed: {}", dataId, e);
            throw new RuntimeException(e);
        }
    }

    public List<Map<String, Object>> getByTraceId(String trace_id) {
        try {
            return annotationManager.getByTraceId(trace_id);
        } catch (Exception e) {
            log.error("Query by trace_id failed: {}", trace_id, e);
            throw new RuntimeException(e);
        }
    }

    public List<Map<String, Object>> getByGroupId(String groupId) {
        try {
            return annotationManager.getByGroupId(groupId);
        } catch (Exception e) {
            log.error("Query by groupId failed: {}", groupId, e);
            throw new RuntimeException(e);
        }
    }

    public List<Map<String, Object>> getGroupsSummary() {
        try {
            return annotationManager.getGroupsSummary();
        } catch (Exception e) {
            log.error("Get groups summary failed", e);
            throw new RuntimeException(e);
        }
    }

    public boolean updateAnnotation(String dataId, AnnotationModel.AnnotationUpdateRequest request) {
        try {
            Map<String, Object> updateData = new HashMap<>();

            if (request.getAnnotation() != null) {
                updateData.put("annotation", request.getAnnotation());
            }
            if (request.getCategory() != null) {
                updateData.put("category", request.getCategory());
            }
            if (request.getTags() != null) {
                updateData.put("tags", request.getTags());
            }
            if (request.getScores() != null) {
                updateData.put("scores", request.getScores());
            }

            Map<String, Object> annotationUpdates = new HashMap<>();
            if (request.getComment() != null && !request.getComment().isEmpty()) {
                annotationUpdates.put("comment", request.getComment());
            }
            if (request.getRemark() != null && !request.getRemark().isEmpty()) {
                annotationUpdates.put("remark", request.getRemark());
            }
            if (request.getAnnotationData() != null && !request.getAnnotationData().isEmpty()) {
                annotationUpdates.putAll(request.getAnnotationData());
            }

            if (!annotationUpdates.isEmpty() && request.getAnnotation() == null) {
                Object existing = updateData.get("annotation");
                Map<String, Object> merged;
                if (existing instanceof Map) {
                    merged = new HashMap<>((Map) existing);
                } else {
                    merged = new HashMap<>();
                }
                merged.putAll(annotationUpdates);
                updateData.put("annotation", merged);
            }

            updateData.put("status", "annotated");

            annotationManager.update(dataId, updateData);
            log.info("Annotation updated: dataId={}", dataId);
            return true;
        } catch (Exception e) {
            log.error("Annotation update failed: {}", dataId, e);
            throw new RuntimeException(e);
        }
    }

    public boolean approveData(String dataId, String reason) {
        try {
            Map<String, Object> data = annotationManager.getById(dataId);
            if (data == null || data.isEmpty()) {
                throw new IllegalArgumentException("Data not found: " + dataId);
            }

            Map<String, Object> updateData = new HashMap<>();
            updateData.put("status", "approved");
            updateData.put(
                    "annotation.approval_reason",
                    reason != null && !reason.isEmpty() ? reason : "Approved"
            );
            updateData.put(
                    "annotation.approved_at",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"))
            );

            annotationManager.update(dataId, updateData);
            log.info("Data approved: dataId={}, reason={}", dataId, reason);

            if (config.isKbAutoIngest()) {
                log.info("Auto ingest to KB: dataId={}", dataId);
                ingestToKb(dataId, null, null);
            }
            return true;
        } catch (Exception e) {
            log.error("Approval failed: {}", dataId, e);
            throw new RuntimeException(e);
        }
    }

    public boolean rejectData(String dataId, String reason, String rejectCategory) {
        try {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("status", "rejected");
            updateData.put("reject_reason", reason);
            updateData.put(
                    "annotation.rejection_category",
                    rejectCategory != null && !rejectCategory.isEmpty()
                            ? rejectCategory
                            : "other"
            );
            updateData.put(
                    "annotation.rejected_at",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"))
            );
            annotationManager.update(dataId, updateData);
            log.info("Data rejected: dataId={}, reason={}", dataId, reason);
            return true;
        } catch (Exception e) {
            log.error("Rejection failed: {}", dataId, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Ingest annotation data to knowledge base.
     *
     * Calls KB HTTP API endpoint.
     */
    public Map<String, Object> ingestToKb(String dataId, Double score, String remark) {
        try {
            if (!config.isKbEnabled()) {
                throw new IllegalArgumentException("KB ingestion not enabled, check ANNOTATION_KB_ENABLED config");
            }

            Map<String, Object> data = annotationManager.getById(dataId);
            if (data == null || data.isEmpty()) {
                throw new IllegalArgumentException("Data not found: " + dataId);
            }

            Object statusObj = data.get("status");
            String status = statusObj == null ? "" : statusObj.toString();
            if (!List.of("approved", "annotated", "kb_failed").contains(status)) {
                throw new IllegalArgumentException(
                        String.format(
                                "Only approved, annotated or kb_failed data can be ingested to KB, current status: %s",
                                status
                        )
                );
            }

            Map<String, Object> kb_data = new HashMap<>();
            kb_data.put("question", Objects.toString(data.get("question"), ""));
            kb_data.put("answer", Objects.toString(data.get("answer"), ""));
            kb_data.put("caller", Objects.toString(data.get("caller"), ""));
            kb_data.put("callee", Objects.toString(data.get("callee"), ""));
            if (score != null) {
                kb_data.put("score", score);
            }
            if (remark != null) {
                kb_data.put("remark", remark);
            }
            kb_data.put("source_trace_id", data.get("source_trace_id"));
            kb_data.put("source_request_id", data.get("source_request_id"));
            kb_data.put("dataType", data.get("dataType"));
            kb_data.put("priority", data.get("priority"));
            kb_data.put("category", data.get("category"));

            String api_url = settings.getApiBaseUrl()
                    + "/api/v1/kb_base/"
                    + config.getKbId()
                    + "/ingest_data";

            String last_error = null;
            for (int attempt = 0; attempt < config.getKbRetryTimes(); attempt++) {
                try {
                    String jsonBody = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                            .build()
                            .writeValueAsString(kb_data);

                    okhttp3.Request request = new okhttp3.Request.Builder()
                            .url(api_url)
                            .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                            .build();

                    try (Response response = httpClient.newCall(request).execute()) {
                        if (response.isSuccessful()) {
                            String respBody = response.body() != null ? response.body().string() : "";
                            Map<?, ?> result = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                                    .build()
                                    .readValue(respBody, Map.class);

                            log.info(
                                    "KB ingestion successful via HTTP API: data_id={}, kb_id={}, response={}",
                                    dataId,
                                    config.getKbId(),
                                    result
                            );

                            Map<String, Object> kbExtra = new HashMap<>();
                            kbExtra.put("ingest_method", "http_api");
                            kbExtra.put("api_response", result);
                            kbExtra.put("quality_score", score);
                            kbExtra.put("remark", remark);

                            annotationManager.updateKbStatus(
                                    dataId,
                                    "ingested",
                                    LocalDateTime.now(),
                                    null,
                                    kbExtra
                            );

                            return Map.of(
                                    "success", Boolean.TRUE,
                                    "kb_id", config.getKbId(),
                                    "data_id", dataId,
                                    "message", "KB ingestion successful",
                                    "kb_result", result
                            );
                        } else {
                            String errorText = response.body() != null ? response.body().string() : "";
                            last_error = String.format(
                                    "API error %d: %s",
                                    response.code(),
                                    errorText
                            );
                            log.warn("KB ingestion API returned {}: {}", response.code(), errorText);
                        }
                    }
                } catch (Exception e) {
                    last_error = e.toString();
                    log.warn(
                            "KB ingestion HTTP request failed (attempt {}/{})",
                            attempt + 1,
                            config.getKbRetryTimes(),
                            e
                    );
                }
                if (attempt < config.getKbRetryTimes() - 1) {
                    try {
                        Thread.sleep(config.getKbRetryInterval() * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            String errorMsg = String.format(
                    "KB ingestion failed, retried %d times: %s",
                    config.getKbRetryTimes(),
                    last_error
            );
            log.error(errorMsg);

            annotationManager.updateKbStatus(
                    dataId,
                    "failed",
                    null,
                    errorMsg,
                    null
            );
            throw new RuntimeException(errorMsg);
        } catch (IllegalArgumentException e) {
            log.error("KB ingestion failed (invalid params): {}", dataId, e);
            annotationManager.updateKbStatus(
                    dataId,
                    "failed",
                    null,
                    e.getMessage(),
                    null
            );
            throw e;
        } catch (Exception e) {
            log.error("KB ingestion failed: {}", dataId, e);
            annotationManager.updateKbStatus(
                    dataId,
                    "failed",
                    null,
                    e.getMessage(),
                    null
            );
            throw new RuntimeException(e);
        }
    }

    public StatsModel.OverallStatsResponse getOverallStats(QueryModel.DataListQueryParams filters) {
        // fixme: 按照 Python 中的 ES 聚合逻辑实现统计查询
        throw new UnsupportedOperationException("getOverallStats not implemented yet // fixme");
    }

    public Map<String, Object> get_pending_p0_stats(int limit) {
        // fixme: 实现 pending P0 统计查询
        throw new UnsupportedOperationException("get_pending_p0_stats not implemented yet // fixme");
    }

    public StatsModel.TypeStatsResponse get_stats_by_type(QueryModel.DataListQueryParams filters) {
        // fixme: 按类型聚合统计
        throw new UnsupportedOperationException("get_stats_by_type not implemented yet // fixme");
    }
}