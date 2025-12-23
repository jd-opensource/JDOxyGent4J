package com.jd.oxygent.infra.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.oxygent.core.oxygent.infra.rag.Knowledge;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
@Component
public class KonwledgeImpl implements Knowledge {

    private OkHttpClient httpClient = new OkHttpClient();
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<String> getChunkList(String query,String app,String key) {
        return List.of();
    }
}
