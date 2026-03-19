package com.jd.oxygent.test;

import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("JDOxygent4J JUnit Platform Suite")
@SelectPackages("com.jd.oxygent.test")
public class TestSuite {
}
