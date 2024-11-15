/*
 * Copyright 2017 The Error Prone Authors.
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@link ShouldHaveEvenArgs} bug pattern.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@RunWith(JUnit4.class)
public class ShouldHaveEvenArgsTest {
  CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(ShouldHaveEvenArgs.class, getClass());
  }

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "ShouldHaveEvenArgsPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import static com.google.common.truth.Truth.assertThat;

            import com.google.common.truth.Correspondence;
            import java.util.HashMap;
            import java.util.Map;

            /**
             * Positive test cases for {@link ShouldHaveEvenArgs} check.
             *
             * @author bhagwani@google.com (Sumit Bhagwani)
             */
            public class ShouldHaveEvenArgsPositiveCases {

              private static final Map map = new HashMap<String, String>();

              public void testWithOddArgs() {
                // BUG: Diagnostic contains: even number of arguments
                assertThat(map).containsExactly("hello", "there", "rest");

                // BUG: Diagnostic contains: even number of arguments
                assertThat(map).containsExactly("hello", "there", "hello", "there", "rest");

                // BUG: Diagnostic contains: even number of arguments
                assertThat(map).containsExactly(null, null, null, null, new Object[] {});
              }

              public void testWithArrayArgs() {
                String key = "hello";
                Object[] value = new Object[] {};
                Object[][] args = new Object[][] {};

                // BUG: Diagnostic contains: even number of arguments
                assertThat(map).containsExactly(key, value, (Object) args);
              }

              public void testWithOddArgsWithCorrespondence() {
                assertThat(map)
                    .comparingValuesUsing(Correspondence.from((a, b) -> true, "dummy"))
                    // BUG: Diagnostic contains: even number of arguments
                    .containsExactly("hello", "there", "rest");

                assertThat(map)
                    .comparingValuesUsing(Correspondence.from((a, b) -> true, "dummy"))
                    // BUG: Diagnostic contains: even number of arguments
                    .containsExactly("hello", "there", "hello", "there", "rest");
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "ShouldHaveEvenArgsNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import static com.google.common.truth.Truth.assertThat;

            import java.util.HashMap;
            import java.util.Map;

            /**
             * Negative test cases for {@link ShouldHaveEvenArgs} check.
             *
             * @author bhagwani@google.com (Sumit Bhagwani)
             */
            public class ShouldHaveEvenArgsNegativeCases {

              private static final Map<String, String> map = new HashMap<String, String>();

              public void testWithNoArgs() {
                assertThat(map).containsExactly();
              }

              public void testWithMinimalArgs() {
                assertThat(map).containsExactly("hello", "there");
              }

              public void testWithEvenArgs() {
                assertThat(map).containsExactly("hello", "there", "hello", "there");
              }

              public void testWithVarargs(Object... args) {
                assertThat(map).containsExactly("hello", args);
                assertThat(map).containsExactly("hello", "world", args);
              }

              public void testWithArray() {
                String[] arg = {"hello", "there"};
                assertThat(map).containsExactly("yolo", arg);

                String key = "hello";
                Object[] value = new Object[] {};
                Object[][] args = new Object[][] {};

                assertThat(map).containsExactly(key, value);
                assertThat(map).containsExactly(key, value, (Object[]) args);
                assertThat(map).containsExactly(key, value, key, value, key, value);
              }
            }\
            """)
        .doTest();
  }

  @org.junit.Ignore("Public truth doesn't contain this method")
  @Test
  public void positiveCase_multimap() {
    compilationHelper
        .addSourceLines(
            "ShouldHaveEvenArgsMultimapPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import static com.google.common.truth.Truth.assertThat;

            import com.google.common.collect.ImmutableMultimap;
            import com.google.common.collect.Multimap;
            import com.google.common.truth.Correspondence;

            /**
             * Positive test cases for {@link ShouldHaveEvenArgs} check.
             *
             * @author monnoroch@google.com (Max Strakhov)
             */
            public class ShouldHaveEvenArgsMultimapPositiveCases {

              private static final Multimap<String, String> multimap = ImmutableMultimap.of();

              public void testWithOddArgs() {
                // BUG: Diagnostic contains: even number of arguments
                assertThat(multimap).containsExactly("hello", "there", "rest");

                // BUG: Diagnostic contains: even number of arguments
                assertThat(multimap).containsExactly("hello", "there", "hello", "there", "rest");

                // BUG: Diagnostic contains: even number of arguments
                assertThat(multimap).containsExactly(null, null, null, null, new Object[] {});
              }

              public void testWithArrayArgs() {
                String key = "hello";
                Object[] value = new Object[] {};
                Object[][] args = new Object[][] {};

                // BUG: Diagnostic contains: even number of arguments
                assertThat(multimap).containsExactly(key, value, (Object) args);
              }

              public void testWithOddArgsWithCorrespondence() {
                assertThat(multimap)
                    .comparingValuesUsing(Correspondence.from((a, b) -> true, "dummy"))
                    // BUG: Diagnostic contains: even number of arguments
                    .containsExactly("hello", "there", "rest");

                assertThat(multimap)
                    .comparingValuesUsing(Correspondence.from((a, b) -> true, "dummy"))
                    // BUG: Diagnostic contains: even number of arguments
                    .containsExactly("hello", "there", "hello", "there", "rest");
              }
            }\
            """)
        .doTest();
  }

  @org.junit.Ignore("Public truth doesn't contain this method")
  @Test
  public void negativeCase_multimap() {
    compilationHelper
        .addSourceLines(
            "ShouldHaveEvenArgsMultimapNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import static com.google.common.truth.Truth.assertThat;

            import com.google.common.collect.ImmutableMultimap;
            import com.google.common.collect.Multimap;

            /**
             * Negative test cases for {@link ShouldHaveEvenArgs} check.
             *
             * @author monnoroch@google.com (Max Strakhov)
             */
            public class ShouldHaveEvenArgsMultimapNegativeCases {

              private static final Multimap<String, String> multimap = ImmutableMultimap.of();

              public void testWithMinimalArgs() {
                assertThat(multimap).containsExactly("hello", "there");
              }

              public void testWithEvenArgs() {
                assertThat(multimap).containsExactly("hello", "there", "hello", "there");
              }

              public void testWithVarargs(Object... args) {
                assertThat(multimap).containsExactly("hello", args);
                assertThat(multimap).containsExactly("hello", "world", args);
              }

              public void testWithArray() {
                String[] arg = {"hello", "there"};
                assertThat(multimap).containsExactly("yolo", arg);

                String key = "hello";
                Object[] value = new Object[] {};
                Object[][] args = new Object[][] {};

                assertThat(multimap).containsExactly(key, value);
                assertThat(multimap).containsExactly(key, value, (Object[]) args);
                assertThat(multimap).containsExactly(key, value, key, value, key, value);
              }
            }\
            """)
        .doTest();
  }
}
