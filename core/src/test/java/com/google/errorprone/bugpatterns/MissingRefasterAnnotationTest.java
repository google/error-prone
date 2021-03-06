/*
 * Copyright 2021 The Error Prone Authors.
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

import com.google.common.base.Predicates;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link MissingRefasterAnnotation}. */
@RunWith(JUnit4.class)
public final class MissingRefasterAnnotationTest {
  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(MissingRefasterAnnotation.class, getClass())
          .expectErrorMessage(
              "X",
              Predicates.containsPattern(
                  "The Refaster template contains a method without any Refaster annotations"));

  @Test
  public void testIdentification() {
    compilationTestHelper
        .addSourceLines(
            "A.java",
            "import com.google.errorprone.refaster.annotation.AfterTemplate;",
            "import com.google.errorprone.refaster.annotation.AlsoNegation;",
            "import com.google.errorprone.refaster.annotation.BeforeTemplate;",
            "import java.util.Map;",
            "",
            "class A {",
            "  // BUG: Diagnostic matches: X",
            "  static final class MethodLacksBeforeTemplateAnnotation {",
            "    @BeforeTemplate",
            "    boolean before1(String string) {",
            "      return string.equals(\"\");",
            "    }",
            "",
            "    // @BeforeTemplate is missing",
            "    boolean before2(String string) {",
            "      return string.length() == 0;",
            "    }",
            "",
            "    @AfterTemplate",
            "    @AlsoNegation",
            "    boolean after(String string) {",
            "      return string.isEmpty();",
            "    }",
            "  }",
            "",
            "  // BUG: Diagnostic matches: X",
            "  static final class MethodLacksAfterTemplateAnnotation {",
            "    @BeforeTemplate",
            "    boolean before(String string) {",
            "      return string.equals(\"\");",
            "    }",
            "",
            "    // @AfterTemplate is missing",
            "    boolean after(String string) {",
            "      return string.isEmpty();",
            "    }",
            "  }",
            "",
            "  // BUG: Diagnostic matches: X",
            "  abstract class MethodLacksPlaceholderAnnotation<K, V> {",
            "    // @Placeholder is missing",
            "    abstract V function(K key);",
            "",
            "    @BeforeTemplate",
            "    void before(Map<K, V> map, K key) {",
            "      if (!map.containsKey(key)) {",
            "        map.put(key, function(key));",
            "      }",
            "    }",
            "",
            "    @AfterTemplate",
            "    void after(Map<K, V> map, K key) {",
            "      map.computeIfAbsent(key, k -> function(k));",
            "    }",
            "  }",
            "",
            "  static final class ValidRefasterTemplate {",
            "    @BeforeTemplate",
            "    void unusedPureFunctionCall(Object o) {",
            "      o.toString();",
            "    }",
            "  }",
            "",
            "  static final class NotARefasterTemplate {",
            "    @Override",
            "    public String toString() {",
            "      return \"This is not a Refaster template\";",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
