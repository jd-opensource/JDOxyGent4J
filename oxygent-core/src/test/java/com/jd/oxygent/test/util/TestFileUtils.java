package com.jd.oxygent.test.util;

import com.jd.oxygent.core.oxygent.utils.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * set working directory to your project root
 */
public class TestFileUtils {

    @ParameterizedTest
    @ValueSource(strings = {"classpath:raw.json", "D:/temp/temp.json"})
    void findAllPaths(String allPaths) throws IOException, URISyntaxException {
        List<Path> paths = FileUtils.findAllPaths(allPaths, null, null);
        for (Path path : paths) {
            String content = Files.readString(path);
            System.out.println(content);
        }
    }

    @Test
    void findAllPaths2() throws IOException, URISyntaxException {
        List<Path> paths = FileUtils.findAllPaths(Path.of(System.getProperty("user.home"), ".cursor/skills-cursor/create-skill/SKILL.md").toString(), null, null);
        for (Path path : paths) {
            String content = Files.readString(path);
            System.out.println(content);
        }
    }
}
