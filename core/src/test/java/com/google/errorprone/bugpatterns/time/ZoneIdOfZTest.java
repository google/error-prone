/*
 * Copyright 2020 The Error Prone Authors.
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
package com.google.errorprone.bugpatterns.time;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ZoneIdOfZ}. */
@RunWith(JUnit4.class)
public class ZoneIdOfZTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(ZoneIdOfZ.class, getClass());

  @Test
  public void zoneIdOfNyc() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.ZoneId;",
            "public class TestClass {",
            "  private static final ZoneId NYC = ZoneId.of(\"America/New_York\");",
            "}")
        .doTest();
  }

  @Test
  public void zoneIdOfZ() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.ZoneId;",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: private static final ZoneId UTC = ZoneOffset.UTC;",
            "  private static final ZoneId UTC = ZoneId.of(\"Z\");",
            "}")
        .doTest();
  }

  @Test
  public void zoneIdOfLowerCaseZ() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.ZoneId;",
            "public class TestClass {",
            "  private static final ZoneId UTC = ZoneId.of(\"z\");",
            "}")
        .doTest();
  }

  @Test
  public void zoneIdOfConstant() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.ZoneId;",
            "public class TestClass {",
            "  private static final String ZONE = \"Z\";",
            "  // BUG: Diagnostic contains: private static final ZoneId UTC = ZoneOffset.UTC;",
            "  private static final ZoneId UTC = ZoneId.of(ZONE);",
            "}")
        .doTest();
  }

  @Test
  public void zoneIdOfNonConstant() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import java.time.ZoneId;",
            "public class TestClass {",
            "  private String zone = \"z\";",
            "  private ZoneId utc = ZoneId.of(zone);",
            "}")
        .doTest();
  }
}
