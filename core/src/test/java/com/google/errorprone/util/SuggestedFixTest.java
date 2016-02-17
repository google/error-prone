/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.errorprone.util;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.common.base.Verify;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.BugPattern.MaturityLevel;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.tools.javac.code.Type;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.annotation.Retention;

@RunWith(JUnit4.class)
public class SuggestedFixTest {

  @Retention(RUNTIME)
  public @interface TypeToCast {
    String value();
  }

  @BugPattern(
    category = Category.ONE_OFF,
    maturity = MaturityLevel.EXPERIMENTAL,
    name = "CastReturn",
    severity = SeverityLevel.ERROR,
    summary = "Adds casts to returned expressions"
  )
  public static class CastReturn extends BugChecker implements ReturnTreeMatcher {

    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      if (tree.getExpression() == null) {
        return Description.NO_MATCH;
      }
      TypeToCast typeToCast =
          ASTHelpers.getAnnotation(
              ASTHelpers.findEnclosingNode(state.getPath(), MethodTree.class), TypeToCast.class);
      SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
      Type type = state.getTypeFromString(typeToCast.value());
      Verify.verifyNotNull(type, "could not find type: %s", typeToCast.value());
      String qualifiedTargetType = SuggestedFix.qualifyType(state, fixBuilder, type.tsym);
      fixBuilder.prefixWith(tree.getExpression(), String.format("(%s) ", qualifiedTargetType));
      return describeMatch(tree, fixBuilder.build());
    }
  }

  @Test
  public void qualifiedName_Object() {
    CompilationTestHelper.newInstance(CastReturn.class, getClass())
        .addSourceLines(
            "Test.java",
            String.format("import %s;", TypeToCast.class.getCanonicalName()),
            "class Test {",
            "  @TypeToCast(\"java.lang.Object\")",
            "  Object f() {",
            "    // BUG: Diagnostic contains: return (Object) null;",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void qualifiedName_imported() {
    CompilationTestHelper.newInstance(CastReturn.class, getClass())
        .addSourceLines(
            "Test.java",
            "import java.util.Map.Entry;",
            String.format("import %s;", TypeToCast.class.getCanonicalName()),
            "class Test {",
            "  @TypeToCast(\"java.util.Map.Entry\")",
            "  Object f() {",
            "    // BUG: Diagnostic contains: return (Entry) null;",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void qualifiedName_notImported() {
    CompilationTestHelper.newInstance(CastReturn.class, getClass())
        .addSourceLines(
            "Test.java",
            String.format("import %s;", TypeToCast.class.getCanonicalName()),
            "class Test {",
            "  @TypeToCast(\"java.util.Map.Entry\")",
            "  Object f() {",
            "    // BUG: Diagnostic contains: return (Map.Entry) null;",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }
}
