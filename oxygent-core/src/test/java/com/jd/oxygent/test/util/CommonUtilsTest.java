package com.jd.oxygent.test.util;

import com.jd.oxygent.core.oxygent.utils.CommonUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommonUtilsTest {

    @Test
    void cleanAnsiCodes() {
        String rawLog = "\u001B[31mError occurred\u001B[0m";
        String cleanedLog = CommonUtils.cleanAnsiCodes(rawLog);

        System.out.println("Original: " + rawLog);
        System.out.println("Cleaned: " + cleanedLog);
    }
}
