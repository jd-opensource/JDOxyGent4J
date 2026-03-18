package com.jd.oxygent.test.util;

import com.jd.oxygent.core.oxygent.config.Prompts;
import com.jd.oxygent.core.oxygent.tools.FileTool;
import org.junit.jupiter.api.Test;

import java.util.List;

public class FileToolTest {

    @Test
    void test() {
        var fileTool = new FileTool();

        System.out.println("=== File Tool Test ===");
        System.out.println("1. Write file:");
        System.out.println(fileTool.call("write_file", "test.txt", Prompts.SYSTEM_PROMPT));

        System.out.println("\n2. Read file:");
        System.out.println(fileTool.call("read_file", "test.txt"));

        System.out.println("\n3. View text file:");
        System.out.println(fileTool.call("view_text_file", "test.txt", List.of(10, 15)));

        System.out.println("\n4. Delete file:");
        System.out.println(fileTool.call("delete_file", "test.txt"));

    }
}
