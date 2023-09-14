/*
 * Copyright 2013 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.refaster;

import static com.google.common.truth.Truth.assertThat;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.util.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link URepeated}. */
@RunWith(JUnit4.class)
public class URepeatedTest extends AbstractUTreeTest {

  @Test
  public void unifies() {
    CompilationTestHelper.newInstance(UnificationChecker.class, getClass())
        .addSourceLines(
            "A.java",
            "class A {",
            "  public void bar() {",
            "    int x = 0;",
            "    \"abcdefg\".charAt(x + 1);",
            "  }",
            "}")
        .doTest();
  }

  @BugPattern(
      summary = "Verify that unifying the expression results in the correct binding",
      explanation = "For test purposes only",
      severity = SUGGESTION)
  public static class UnificationChecker extends BugChecker implements MethodInvocationTreeMatcher {
    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      Unifier unifier = new Unifier(new Context());
      URepeated ident = URepeated.create("foo", UFreeIdent.create("foo"));

      assertThat(ident.unify(tree, unifier)).isNotNull();
      assertThat(unifier.getBindings()).containsExactly(new UFreeIdent.Key("foo"), tree);

      return Description.NO_MATCH;
    }
  }

  @Test
  public void equality() {
    new EqualsTester()
        .addEqualityGroup(URepeated.create("foo", UFreeIdent.create("foo")))
        .addEqualityGroup(URepeated.create("bar", UFreeIdent.create("bar")))
        .testEquals();
  }

  @Test
  public void serialization() {
    SerializableTester.reserializeAndAssert(URepeated.create("foo", UFreeIdent.create("foo")));
  }
}
