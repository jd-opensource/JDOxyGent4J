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
package com.jd.oxygent.core.oxygent.tools;

import com.jd.oxygent.core.oxygent.oxy.function_tools.FunctionHub;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Map;

public class FhMathTools extends FunctionHub {

    public FhMathTools() {
        super("math_tools");
        this.setDesc("A tool that can calculate the value of pi.");
    }

    @Tool(
            name = "calcPi",
            description = "A tool that can calculate the value of pi.",
            paramMetas = {
                    @ParamMetaAuto(
                            name = "prec",
                            type = "int",
                            description = "how many decimal places"
                    ),
                    @ParamMetaAuto(
                            name = "oxy_request",
                            type = "OxyRequest",
                            description = "The oxy request"
                    )
            }
    )
    public String calcPi(int prec, OxyRequest oxyRequest) {
        if (prec <= 0) {
            throw new IllegalArgumentException("Precision must be positive");
        }

        // Set calculation precision (a few more digits than required to avoid rounding errors)
        MathContext mathContext = new MathContext(prec + 10, RoundingMode.HALF_UP);

        BigDecimal x = BigDecimal.ZERO;
        int iterations = prec / 8 + 1;

        for (int k = 0; k <= iterations; k++) {
            // a = 2 * sqrt(2) / 9801
            BigDecimal a = BigDecimal.valueOf(2)
                    .multiply(sqrt(BigDecimal.valueOf(2), mathContext))
                    .divide(BigDecimal.valueOf(9801), mathContext);

            // b = factorial(4 * k) * (1103 + 26390 * k)
            BigDecimal b = factorial(4 * k)
                    .multiply(BigDecimal.valueOf(1103 + 26390L * k));

            // c = factorial(k)^4 * 396^(4*k)
            BigDecimal factorialK = factorial(k);
            BigDecimal c = factorialK.pow(4)
                    .multiply(BigDecimal.valueOf(396).pow(4 * k));

            // x = x + a * b / c
            BigDecimal term = a.multiply(b).divide(c, mathContext);
            x = x.add(term);
        }

        // result = 1 / x
        BigDecimal result = BigDecimal.ONE.divide(x, mathContext);

        oxyRequest.sendMessage(Map.of("type", "answer", "content", result.toString()));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        oxyRequest.breakTask();

        // Return result with specified precision
        return result.setScale(prec, RoundingMode.HALF_UP).toString();
    }

    // Factorial calculation
    private static BigDecimal factorial(int n) {
        if (n == 0) {
            return BigDecimal.ONE;
        }

        BigDecimal result = BigDecimal.ONE;
        for (int i = 1; i <= n; i++) {
            result = result.multiply(BigDecimal.valueOf(i));
        }
        return result;
    }

    // Square root calculation (using Newton's method)
    private static BigDecimal sqrt(BigDecimal value, MathContext mathContext) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ArithmeticException("Square root of negative number");
        }

        BigDecimal x = new BigDecimal(Math.sqrt(value.doubleValue()), mathContext);
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Newton's method for square root calculation
        for (int i = 0; i < mathContext.getPrecision(); i++) {
            BigDecimal x2 = x.multiply(x);
            BigDecimal delta = value.subtract(x2);
            if (delta.abs().compareTo(BigDecimal.valueOf(1, mathContext.getPrecision())) < 0) {
                break;
            }
            x = x.add(delta.divide(x.multiply(BigDecimal.valueOf(2)), mathContext));
        }

        return x;
    }
}
