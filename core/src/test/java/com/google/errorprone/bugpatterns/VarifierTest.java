/*
 * Copyright 2022 Google Inc. All Rights Reserved.
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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link Varifier}. */
@RunWith(JUnit4.class)
public final class VarifierTest {
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(Varifier.class, getClass());

  @Test
  public void cast() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  public void t(Object o) {",
            "    Test t = (Test) o;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  public void t(Object o) {",
            "    var t = (Test) o;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void constructor() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  public void t() {",
            "    Test t = new Test();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  public void t() {",
            "    var t = new Test();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void constructor_usingDiamond() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.ArrayList;",
            "class Test {",
            "  public void t() {",
            "    ArrayList<Integer> xs = new ArrayList<>();",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void constructor_usingExplicitType() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.ArrayList;",
            "class Test {",
            "  public void t() {",
            "    ArrayList<Integer> xs = new ArrayList<Integer>();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.ArrayList;",
            "class Test {",
            "  public void t() {",
            "    var xs = new ArrayList<Integer>();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void alreadyVar() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  public void t() {",
            "    var t = new Test();",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void fromInstanceMethod() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "abstract class Test {",
            "  public void t() {",
            "    Test t = t2();",
            "  }",
            "  public abstract Test t2();",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void builder() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.protobuf.Duration;",
            "abstract class Test {",
            "  public void t() {",
            "    Duration duration = Duration.newBuilder().setSeconds(4).build();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.protobuf.Duration;",
            "abstract class Test {",
            "  public void t() {",
            "    var duration = Duration.newBuilder().setSeconds(4).build();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void fromFactoryMethod() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.time.Instant;",
            "class Test {",
            "  public void t() {",
            "    Instant now = Instant.ofEpochMilli(1);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.time.Instant;",
            "class Test {",
            "  public void t() {",
            "    var now = Instant.ofEpochMilli(1);",
            "  }",
            "}")
        .doTest();
  }
}
