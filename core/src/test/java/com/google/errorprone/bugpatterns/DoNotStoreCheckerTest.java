/*
 * Copyright 2011 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class DoNotStoreCheckerTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(DoNotStoreChecker.class, getClass());

  @Test
  public void doNotStore() throws Exception {
    testHelper
        .addSourceLines(
            "Test.java",
            "package test;",
            "import com.google.errorprone.annotations.DoNotStore;",
            "public class Test {",
            "  // BUG: Diagnostic contains: [DoNotStore]",
            "  private boolean value = trueValue();",
            "  public void setTrueValue() {",
            "    // BUG: Diagnostic contains: [DoNotStore]",
            "    value = trueValue();",
            "    // BUG: Diagnostic contains: [DoNotStore]",
            "    boolean value2 = trueValue();",
            "  }",
            "  public boolean proxyValue(boolean value) {",
            "    return value;",
            "  }",
            "  public boolean getTrueValueViaProxy() {",
            "    return proxyValue(trueValue());",
            "  }",
            "  @DoNotStore boolean trueValue() {",
            "    return true;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void doNotStoreWithValue() throws Exception {
    testHelper
        .addSourceLines(
            "Test.java",
            "package test;",
            "import com.google.errorprone.annotations.DoNotStore;",
            "public class Test {",
            "  // BUG: Diagnostic contains: plz plz",
            "  private boolean value = trueValue();",
            "  public void setTrueValue() {",
            "    // BUG: Diagnostic contains: plz plz",
            "    value = trueValue();",
            "    // BUG: Diagnostic contains: plz plz",
            "    boolean value2 = trueValue();",
            "  }",
            "  public boolean proxyValue(boolean value) {",
            "    return value;",
            "  }",
            "  public boolean getTrueValueViaProxy() {",
            "    return proxyValue(trueValue());",
            "  }",
            "  @DoNotStore(\"plz plz\") boolean trueValue() {",
            "    return true;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void doNotStoreIsIgnoredWithoutAnnotation() throws Exception {
    testHelper
        .addSourceLines(
            "Test.java",
            "package test;",
            "public class Test {",
            "  private boolean value = trueValue();",
            "  public void setTrueValue() {",
            "    value = trueValue();",
            "    boolean value2 = trueValue();",
            "  }",
            "  public boolean proxyValue(boolean value) {",
            "    return value;",
            "  }",
            "  public boolean getTrueValueViaProxy() {",
            "    return proxyValue(trueValue());",
            "  }",
            "  boolean trueValue() {",
            "    return true;",
            "  }",
            "}")
        .doTest();
  }
}
