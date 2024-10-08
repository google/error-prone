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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link TypeEqualsChecker}. */
@RunWith(JUnit4.class)
public class TypeEqualsCheckerTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(TypeEqualsChecker.class, getClass());

  @Test
  public void noMatch() {
    testHelper
        .addSourceLines(
            "ExampleChecker.java",
            """
            import com.google.errorprone.BugPattern;
            import com.google.errorprone.BugPattern.SeverityLevel;
            import com.google.errorprone.VisitorState;
            import com.google.errorprone.bugpatterns.BugChecker;
            import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
            import com.google.errorprone.matchers.Description;
            import com.sun.source.tree.ClassTree;
            import com.sun.tools.javac.code.Types;

            @BugPattern(name = "Example", summary = "", severity = SeverityLevel.ERROR)
            public class ExampleChecker extends BugChecker implements ClassTreeMatcher {
              @Override
              public Description matchClass(ClassTree t, VisitorState s) {
                return Description.NO_MATCH;
              }
            }
            """)
        .addModules("jdk.compiler/com.sun.tools.javac.code")
        .doTest();
  }

  @Test
  public void matchInABugChecker() {
    testHelper
        .addSourceLines(
            "ExampleChecker.java",
            """
            import static com.google.errorprone.util.ASTHelpers.getSymbol;
            import com.google.errorprone.BugPattern;
            import com.google.errorprone.BugPattern.SeverityLevel;
            import com.google.errorprone.VisitorState;
            import com.google.errorprone.bugpatterns.BugChecker;
            import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
            import com.google.errorprone.fixes.SuggestedFix;
            import com.google.errorprone.matchers.Description;
            import com.sun.source.tree.ClassTree;
            import com.sun.tools.javac.code.Symbol;
            import com.sun.tools.javac.code.Symbol.ClassSymbol;
            import com.sun.tools.javac.code.Type;
            import com.sun.tools.javac.code.Types;
            import java.util.Objects;

            @BugPattern(name = "Example", summary = "", severity = SeverityLevel.ERROR)
            public class ExampleChecker extends BugChecker implements ClassTreeMatcher {
              @Override
              public Description matchClass(ClassTree tree, VisitorState state) {
                Symbol sym = getSymbol(tree);
                Types types = state.getTypes();
                ClassSymbol owner = sym.enclClass();
                for (Type s : types.closure(owner.type)) {
                  // BUG: Diagnostic contains: TypeEquals
                  if (s.equals(owner.type)) {
                    return Description.NO_MATCH;
                  }
                  // BUG: Diagnostic contains: TypeEquals
                  if (Objects.equals(s, owner.type)) {
                    return Description.NO_MATCH;
                  }
                }
                return Description.NO_MATCH;
              }
            }
            """)
        .addModules(
            "jdk.compiler/com.sun.tools.javac.code", "jdk.compiler/com.sun.tools.javac.util")
        .doTest();
  }
}
