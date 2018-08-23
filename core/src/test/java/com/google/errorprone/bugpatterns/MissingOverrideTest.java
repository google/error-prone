/*
 * Copyright 2015 The Error Prone Authors.
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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link MissingOverride}Test */
@RunWith(JUnit4.class)
public class MissingOverrideTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(MissingOverride.class, getClass());
  }

  @Test
  public void simple() {
    compilationHelper
        .addSourceLines("Super.java", "public class Super {", "  void f() {}", "}")
        .addSourceLines(
            "Test.java",
            "public class Test extends Super {",
            "  // BUG: Diagnostic contains: f overrides method in Super; expected @Override",
            "  public void f() {}",
            "}")
        .doTest();
  }

  @Test
  public void abstractMethod() {
    compilationHelper
        .addSourceLines("Super.java", "public abstract class Super {", "  abstract void f();", "}")
        .addSourceLines(
            "Test.java",
            "public class Test extends Super {",
            "  // BUG: Diagnostic contains: f implements method in Super; expected @Override",
            "  public void f() {}",
            "}")
        .doTest();
  }

  @Test
  public void interfaceMethod() {
    compilationHelper
        .addSourceLines("Super.java", "interface Super {", "  void f();", "}")
        .addSourceLines(
            "Test.java",
            "public class Test implements Super {",
            "  // BUG: Diagnostic contains: f implements method in Super; expected @Override",
            "  public void f() {}",
            "}")
        .doTest();
  }

  @Test
  public void bothStatic() {
    compilationHelper
        .addSourceLines("Super.java", "public class Super {", "  static void f() {}", "}")
        .addSourceLines(
            "Test.java", "public class Test extends Super {", "  static public void f() {}", "}")
        .doTest();
  }

  @Test
  public void deprecatedMethod() {
    compilationHelper
        .addSourceLines("Super.java", "public class Super {", "  @Deprecated void f() {}", "}")
        .addSourceLines(
            "Test.java", "public class Test extends Super {", "  public void f() {}", "}")
        .doTest();
  }

  @Test
  public void interfaceOverride() {
    compilationHelper
        .addSourceLines("Super.java", "interface Super {", "  void f();", "}")
        .addSourceLines(
            "Test.java",
            "public interface Test extends Super {",
            "  // BUG: Diagnostic contains: f implements method in Super; expected @Override",
            "  void f();",
            "}")
        .doTest();
  }

  @Test
  public void ignoreInterfaceOverride() {
    compilationHelper
        .setArgs(ImmutableList.of("-XepOpt:MissingOverride:IgnoreInterfaceOverrides=true"))
        .addSourceLines("Super.java", "interface Super {", "  void f();", "}")
        .addSourceLines("Test.java", "public interface Test extends Super {", "  void f();", "}")
        .doTest();
  }
}
