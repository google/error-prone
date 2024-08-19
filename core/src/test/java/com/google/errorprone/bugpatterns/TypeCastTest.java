/*
 * Copyright 2024 The Error Prone Authors.
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

import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.TypeCastTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.TypeCastTree;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.errorprone.util.ASTHelpers.getType;

@RunWith(JUnit4.class)
public class TypeCastTest {

  @BugPattern(summary = "", severity = BugPattern.SeverityLevel.ERROR)
  public static final class TypeCastChecker extends BugChecker implements TypeCastTreeMatcher {

    @Override
    public Description matchTypeCast(TypeCastTree tree, VisitorState state) {
      return buildDescription(tree)
          .setMessage(getType(tree) + " " + getType(tree.getType()))
          .build();
    }
  }

  @Test
  public void typeCast() {
    CompilationTestHelper.newInstance(TypeCastChecker.class, getClass())
        .addSourceLines(
            "T.java",
            "import java.lang.annotation.ElementType;",
            "import java.lang.annotation.Target;",
            "class T {",
            "    @Target(ElementType.TYPE_USE)",
            "    @interface Nullable {}",
            "    interface A<T> {}",
            "    void f(Object o) {",
            "        // BUG: Diagnostic contains: T.A<java.lang.@T.Nullable String> T.A<java.lang.@T.Nullable String>",
            "        var x = (A<@Nullable String>) o;",
            "    }",
            "}")
        .doTest();
  }
}
