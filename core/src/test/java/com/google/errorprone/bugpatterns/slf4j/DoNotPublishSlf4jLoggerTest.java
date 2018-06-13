package com.google.errorprone.bugpatterns.slf4j;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.errorprone.CompilationTestHelper;

@RunWith(JUnit4.class)
public class DoNotPublishSlf4jLoggerTest {

    private CompilationTestHelper compilationHelper;

    @Before
    public void setup() {
      compilationHelper = CompilationTestHelper.newInstance(DoNotPublishSlf4jLogger.class, getClass());
    }

    @Test
    public void doNotReturnNullPositiveCases() {
      compilationHelper.addSourceFile("DoNotPublishSlf4jLoggerPositiveCases.java").doTest();
    }

    @Test
    public void doNotReturnNullNegativeCases() {
      compilationHelper.addSourceFile("DoNotPublishSlf4jLoggerNegativeCases.java").doTest();
    }
}
