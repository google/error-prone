/*
 * Copyright 2016 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.collectionincompatibletype;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link IncompatibleArgumentType} */
@RunWith(JUnit4.class)
public class IncompatibleArgumentTypeTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(IncompatibleArgumentType.class, getClass());

  @Test
  public void genericMethod() {
    compilationHelper.addSourceFile("IncompatibleArgumentTypeGenericMethod.java").doTest();
  }

  @Test
  public void owningTypes() {
    compilationHelper.addSourceFile("IncompatibleArgumentTypeEnclosingTypes.java").doTest();
  }

  @Test
  public void multimapIntegration() {
    compilationHelper.addSourceFile("IncompatibleArgumentTypeMultimapIntegration.java").doTest();
  }

  @Test
  public void intersectionTypes() {
    compilationHelper.addSourceFile("IncompatibleArgumentTypeIntersectionTypes.java").doTest();
  }

  @Test
  public void typeWithinLambda() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.errorprone.annotations.CompatibleWith;",
            "import java.util.Map;",
            "import java.util.Optional;",
            "abstract class Test {",
            "  abstract <K, V> Optional<V> getOrEmpty(Map<K, V> map, @CompatibleWith(\"K\") Object"
                + " key);",
            "  void test(Map<Long, String> map, ImmutableList<Long> xs) {",
            "    // BUG: Diagnostic contains:",
            "    getOrEmpty(map, xs);",
            "    Optional<String> x = Optional.empty().flatMap(k -> getOrEmpty(map, xs));",
            "  }",
            "}")
        .doTest();
  }
}
