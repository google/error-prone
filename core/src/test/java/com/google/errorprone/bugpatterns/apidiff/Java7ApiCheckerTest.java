/*
 * Copyright 2018 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.apidiff;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link Java7ApiChecker}Test */

@RunWith(JUnit4.class)
public class Java7ApiCheckerTest {

  protected final CompilationTestHelper compilationHelper;

  public Java7ApiCheckerTest() {
    this(Java7ApiChecker.class);
  }

  protected Java7ApiCheckerTest(Class<? extends ApiDiffChecker> checker) {
    compilationHelper = CompilationTestHelper.newInstance(checker, getClass());
  }

  @Test
  public void positiveClass() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  // BUG: Diagnostic contains: java.util.Optional",
            "  Optional<String> o;",
            "}")
        .doTest();
  }

  @Test
  public void positiveMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "class Test {",
            "  void f(List<Integer> xs) {",
            "    // BUG: Diagnostic contains: stream() is not available in java.util.List",
            "    xs.stream();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import javax.lang.model.SourceVersion;",
            "class Test {",
            "  // BUG: Diagnostic contains: RELEASE_8",
            "  SourceVersion version8 = SourceVersion.RELEASE_8;",
            "}")
        .doTest();
  }

  @Test
  public void negativeInherited() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.LinkedHashMap;",
            "import java.util.concurrent.ConcurrentHashMap;",
            "import java.util.Set;",
            "class Test {",
            "  Set<String> getKeySet(LinkedHashMap<String, String> map) {",
            "    return map.keySet();",
            "  }",
            "  Set<String> getKeySet(ConcurrentHashMap<String, String> map) {",
            "    return map.keySet();",
            "  }",
            "}")
        .doTest();
  }
}
