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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.FixChoosers;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link ProtoBuilderReturnValueIgnored} bugpattern.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@RunWith(JUnit4.class)
public final class ProtoBuilderReturnValueIgnoredTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(ProtoBuilderReturnValueIgnored.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(ProtoBuilderReturnValueIgnored.class, getClass());

  @Test
  public void refactoring() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.protobuf.Duration;",
            "final class Test {",
            "  private void singleField(Duration.Builder proto) {",
            "    proto.clearSeconds().build();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.protobuf.Duration;",
            "final class Test {",
            "  private void singleField(Duration.Builder proto) {",
            "    proto.clearSeconds();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void toBuilderExempted() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.protobuf.Duration;",
            "final class Test {",
            "  private void singleField(Duration proto) {",
            "    proto.toBuilder().clearSeconds().build();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringReceiverIsOnlyIdentifier() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.protobuf.Duration;",
            "final class Test {",
            "  private void singleField(Duration.Builder proto) {",
            "    proto.build();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.protobuf.Duration;",
            "final class Test {",
            "  private void singleField(Duration.Builder proto) {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringSecondFix() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.protobuf.Duration;",
            "final class Test {",
            "  private void singleField(Duration.Builder proto) {",
            "    proto.build();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import static com.google.common.base.Preconditions.checkState;",
            "import com.google.protobuf.Duration;",
            "final class Test {",
            "  private void singleField(Duration.Builder proto) {",
            "    checkState(proto.isInitialized());",
            "  }",
            "}")
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }

  @Test
  public void testFoo() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.protobuf.Duration;",
            "final class Test {",
            "  public void proto_build() {",
            // TODO(b/9467048): this will later be flagged after the depot is scrubbed
            "    Duration.newBuilder().setSeconds(4).build();",
            "    Duration duration = Duration.newBuilder().setSeconds(4).build();",
            "  }",
            "  public void proto_buildPartial() {",
            // TODO(b/9467048): this will later be flagged after the depot is scrubbed
            "    Duration.newBuilder().setSeconds(4).buildPartial();",
            "    Duration duration = Duration.newBuilder().setSeconds(4).buildPartial();",
            "  }",
            "}")
        .doTest();
  }
}
