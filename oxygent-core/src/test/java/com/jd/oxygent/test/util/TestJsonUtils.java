package com.jd.oxygent.test.util;

import com.jd.oxygent.core.oxygent.utils.FileUtils;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestJsonUtils {

    @Test
    void readJson() throws IOException, URISyntaxException {
        List<Path> paths = FileUtils.findAllPaths("classpath:raw.json", null, null);
        for (Path path : paths) {
            String content = Files.readString(path);
            Map map = JsonUtils.readValue(content, Map.class);
            assertNotNull(map.get("arguments"));
        }
    }

}
