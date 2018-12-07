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

package com.google.errorprone.bugpatterns.threadsafety;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Type;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link WellKnownMutability} test. */
@RunWith(JUnit4.class)
public final class WellKnownMutabilityTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(TestChecker.class, getClass());

  /** Test checker. */
  @BugPattern(
      name = "TestChecker",
      documentSuppression = false,
      summary = "This is not proto2",
      severity = SeverityLevel.ERROR,
      providesFix = ProvidesFix.NO_FIX)
  public static final class TestChecker extends BugChecker implements VariableTreeMatcher {
    public TestChecker(ErrorProneFlags flags) {}

    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
      Type type = ASTHelpers.getSymbol(tree).type;
      boolean isProto2 = WellKnownMutability.isProto2MessageClass(state, type);
      if (!isProto2) {
        return describeMatch(tree);
      }
      return Description.NO_MATCH;
    }
  }

  @Test
  public void basicFields() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.testdata.proto.proto1api.User;",
            "class Test {",
            "  // BUG: Diagnostic contains: This is not proto2",
            "  final User user = null;",
            "}")
        .doTest();
  }
}
