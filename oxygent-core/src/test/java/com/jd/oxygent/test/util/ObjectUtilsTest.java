package com.jd.oxygent.test.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import com.jd.oxygent.core.oxygent.utils.ObjectUtils;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class ObjectUtilsTest {

    @Test
    void deepCopy() {
        Map original = JsonUtils.parseObject("""
                {
                  "type" : "tool_call",
                  "content" : {
                    "caller_category" : "agent",
                    "current_trace_id" : "current_trace_id",
                    "from_trace_id" : "from_trace_id",
                    "caller" : "master_agent",
                    "group_id" : "group_id",
                    "callee" : "time_agent",
                    "callee_category" : "agent",
                    "arguments" : {
                      "query" : "What time is it now?"
                    },
                    "call_stack" : [ "user", "master_agent", "time_agent" ],
                    "request_id" : "request_id",
                    "shared_data" : {
                      "_headers" : {
                        "sec-fetch-mode" : "cors",
                        "referer" : "referer",
                        "cache-control" : "no-cache",
                        "user-agent" : "user-agent",
                        "accept" : "text/event-stream",
                        "sec-fetch-dest" : "empty"
                      },
                      "extra" : "extra argument",
                      "query" : "What time is it now?"
                    },
                    "node_id" : "node_id"
                  }
                }
                """, Map.class);
        assertEquals(JsonUtils.toJSONString(ObjectUtils.deepCopy(original)), JsonUtils.toJSONString(original));
    }

    @Test
    void deepCopyPerformance() {
        Map original = JsonUtils.parseObject("""
                {
                  "type" : "tool_call",
                  "content" : {
                    "caller_category" : "agent",
                    "current_trace_id" : "current_trace_id",
                    "from_trace_id" : "from_trace_id",
                    "caller" : "master_agent",
                    "group_id" : "group_id",
                    "callee" : "time_agent",
                    "callee_category" : "agent",
                    "arguments" : {
                      "query" : "What time is it now?"
                    },
                    "call_stack" : [ "user", "master_agent", "time_agent" ],
                    "request_id" : "request_id",
                    "shared_data" : {
                      "_headers" : {
                        "sec-fetch-mode" : "cors",
                        "referer" : "referer",
                        "cache-control" : "no-cache",
                        "user-agent" : "user-agent",
                        "accept" : "text/event-stream",
                        "sec-fetch-dest" : "empty"
                      },
                      "extra" : "extra argument",
                      "query" : "What time is it now?"
                    },
                    "node_id" : "node_id"
                  }
                }
                """, Map.class);
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1_000_000; i++) {
            ObjectUtils.deepCopy(original);
        }
        double cost = (System.currentTimeMillis() - start) / 1000.0;
        System.out.println("Time: " + cost + "s");
        assertTrue(cost < 2);
    }
}
