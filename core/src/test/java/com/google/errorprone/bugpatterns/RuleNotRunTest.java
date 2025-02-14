/*
 * Copyright 2025 The Error Prone Authors.
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
public class RuleNotRunTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(RuleNotRun.class, getClass());

  @Test
  public void positive() {
    helper
        .addSourceLines(
            "TestTest.java",
            """
            import org.junit.Test;
            import org.junit.rules.TemporaryFolder;
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public final class TestTest {
              // BUG: Diagnostic contains:
              public final TemporaryFolder folder = new TemporaryFolder();
            }
            """)
        .doTest();
  }

  @Test
  public void negative() {
    helper
        .addSourceLines(
            "TestTest.java",
            """
            import org.junit.Rule;
            import org.junit.Test;
            import org.junit.rules.TemporaryFolder;
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public final class TestTest {
              @Rule public final TemporaryFolder folder = new TemporaryFolder();
            }
            """)
        .doTest();
  }

  @Test
  public void negative_ruleChain() {
    helper
        .addSourceLines(
            "TestTest.java",
            """
            import org.junit.Rule;
            import org.junit.Test;
            import org.junit.rules.RuleChain;
            import org.junit.rules.TemporaryFolder;
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public final class TestTest {
              public final TemporaryFolder folder = new TemporaryFolder();
              @Rule public final RuleChain rules = RuleChain.outerRule(folder);
            }
            """)
        .doTest();
  }

  @Test
  public void negative_injected() {
    helper
        .addSourceLines(
            "TestTest.java",
            """
            import javax.inject.Inject;
            import org.junit.Rule;
            import org.junit.Test;
            import org.junit.rules.TemporaryFolder;
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public final class TestTest {
              @Inject public final TemporaryFolder folder = new TemporaryFolder();
            }
            """)
        .doTest();
  }

  @Test
  public void negative_private() {
    helper
        .addSourceLines(
            "TestTest.java",
            """
            import org.junit.Rule;
            import org.junit.Test;
            import org.junit.rules.TemporaryFolder;
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public final class TestTest {
              private final TemporaryFolder folder = new TemporaryFolder();
            }
            """)
        .doTest();
  }
}
