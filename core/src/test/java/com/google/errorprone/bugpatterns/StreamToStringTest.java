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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link StreamToString}Test */
@RunWith(JUnit4.class)
public class StreamToStringTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(StreamToString.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Arrays;

            class Test {
              public static void main(String[] args) {
                // BUG: Diagnostic contains:
                System.err.println(Arrays.asList(42).stream());
                // BUG: Diagnostic contains:
                Arrays.asList(42).stream().toString();
                // BUG: Diagnostic contains:
                String.valueOf(Arrays.asList(42).stream());
                // BUG: Diagnostic contains:
                String s = "" + Arrays.asList(42).stream();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
"""
import java.util.Arrays;
import java.util.stream.Collectors;

class Test {
  public static void main(String[] args) {
    System.err.println(
        Arrays.asList(42).stream().map(String::valueOf).collect(Collectors.joining(", ")));
    String.valueOf(
        Arrays.asList(42).stream().map(String::valueOf).collect(Collectors.joining(", ")));
    String s =
        "" + Arrays.asList(42).stream().map(String::valueOf).collect(Collectors.joining(", "));
    String.format("%s %s", null, null);
  }
}
""")
        .doTest();
  }

  @Test
  public void withinStreamClass() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            public abstract class Test implements java.util.stream.Stream {
              public String s() {
                return toString();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void viaJoiner() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.base.Joiner;
            import java.util.stream.Stream;

            class Test {
              public void s(Stream<String> xs) {
                // BUG: Diagnostic contains:
                var x = Joiner.on("foo").join(xs, xs);
                // BUG: Diagnostic contains:
                var y = Joiner.on("foo").join(null, null, new Stream[] {xs});
              }
            }
            """)
        .doTest();
  }
}
