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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.junit.Ignore;

/**
 * Tests for {@link ModifiedButNotUsed} bugpattern.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@RunWith(JUnit4.class)
public final class ModifiedButNotUsedTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ModifiedButNotUsed.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(new ModifiedButNotUsed(), getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "class Test {",
            "  void test() {",
            "    // BUG: Diagnostic contains:",
            "    List<Integer> foo = new ArrayList<>();",
            "    foo.add(1);",
            "    List<Integer> bar;",
            "    // BUG: Diagnostic contains:",
            "    bar = new ArrayList<>();",
            "    bar.add(1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void sideEffectFreeRefactoring() throws Exception {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "class Test {",
            "  void test() {",
            "    List<Integer> foo = new ArrayList<>();",
            "    foo.add(1);",
            "    List<Integer> bar = new ArrayList<>();",
            "    bar.add(sideEffects());",
            "    List<Integer> baz;",
            "    baz = new ArrayList<>();",
            "    bar.add(sideEffects());",
            "  }",
            "  int sideEffects() { return 1; }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "class Test {",
            "  void test() {",
            "    sideEffects();",
            "    sideEffects();",
            "  }",
            "  int sideEffects() { return 1; }",
            "}")
        .doTest();
  }

  @Test
  public void negatives() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "abstract class Test {",
            "  void test() {",
            "    List<Integer> foo = new ArrayList<>();",
            "    foo.add(1);",
            "    if (false) { foo = new ArrayList<>(); }",
            "    int a = foo.get(0);",
            "  }",
            "  void test2() {",
            "    List<Integer> foo = new ArrayList<>();",
            "    foo.add(1);",
            "    incoming(foo);",
            "  }",
            "  abstract void incoming(List<Integer> l);",
            "}")
        .doTest();
  }

  @Test
  public void usedByAssignment() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "class Test {",
            "  private List<Integer> bar;",
            "  void test() {",
            "    List<Integer> foo = new ArrayList<>();",
            "    foo.add(1);",
            "    bar = foo;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void usedDuringAssignment() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "abstract class Test {",
            "  private List<Integer> bar;",
            "  void test() {",
            "    List<Integer> foo = new ArrayList<>();",
            "    foo.add(1);",
            "    foo = frobnicate(foo);",
            "  }",
            "  abstract List<Integer> frobnicate(List<Integer> a);",
            "}")
        .doTest();
  }

  @Test
  public void negativeAfterReassignment() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "class Test {",
            "  void test() {",
            "    List<Integer> foo = new ArrayList<>();",
            "    foo.add(1);",
            "    foo.get(0);",
            "    foo = new ArrayList<>();",
            "    foo.add(1);",
            "    foo.get(0);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  @Ignore("b/74365407 test proto sources are broken")
  public void proto() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;",
            "class Test {",
            "  static void foo() {",
            "    // BUG: Diagnostic contains:",
            "    TestProtoMessage.Builder proto = TestProtoMessage.newBuilder();",
            "    proto.setMessage(TestFieldProtoMessage.newBuilder());",
            "    TestProtoMessage.Builder proto2 =",
            "        // BUG: Diagnostic contains:",
            "        TestProtoMessage.newBuilder().setMessage(TestFieldProtoMessage.newBuilder());",
            "    TestProtoMessage.Builder proto3 =",
            "        // BUG: Diagnostic contains:",
            "        TestProtoMessage.getDefaultInstance().toBuilder().clearMessage();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  @Ignore("b/74365407 test proto sources are broken")
  public void protoSideEffects() throws Exception {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;",
            "class Test {",
            "  void foo() {",
            "    TestProtoMessage.Builder proto = TestProtoMessage.newBuilder();",
            "    TestFieldProtoMessage.Builder builder = TestFieldProtoMessage.newBuilder();",
            "    proto.setMessage(builder).setMessage(sideEffects());",
            "  }",
            "  TestFieldProtoMessage sideEffects() { throw new UnsupportedOperationException(); }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;",
            "class Test {",
            "  void foo() {",
            "    TestFieldProtoMessage.Builder builder = TestFieldProtoMessage.newBuilder();",
            "    sideEffects();",
            "  }",
            "  TestFieldProtoMessage sideEffects() { throw new UnsupportedOperationException(); }",
            "}")
        .doTest();
  }

  @Test
  @Ignore("b/74365407 test proto sources are broken")
  public void protoNegative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;",
            "class Test {",
            "  static TestProtoMessage foo() {",
            "    TestProtoMessage.Builder proto = TestProtoMessage.newBuilder();",
            "    return proto.setMessage(TestFieldProtoMessage.newBuilder()).build();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void immutableCollection() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "class Test {",
            "  static void foo() {",
            "    // BUG: Diagnostic contains:",
            "    ImmutableList.Builder<Integer> a = ImmutableList.builder();",
            "    a.add(1);",
            "    // BUG: Diagnostic contains:",
            "    ImmutableList.Builder<Integer> b = ImmutableList.<Integer>builder().add(1);",
            "    b.add(1);",
            "  }",
            "}")
        .doTest();
  }
}
