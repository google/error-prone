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

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MemberSelectTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.tools.javac.code.Symbol;

/**
 * {@link com.google.errorprone.bugpatterns.BugChecker} that detects usages of
 * org.mockito.internal.*
 */
@BugPattern(
    name = "MockitoInternalUsage",
    summary = "org.mockito.internal.* is a private API and should not be used by clients",
    category = Category.MOCKITO,
    severity = SeverityLevel.WARNING,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class MockitoInternalUsage extends BugChecker implements MemberSelectTreeMatcher {

  @Override
  public Description matchMemberSelect(MemberSelectTree tree, VisitorState state) {
    Symbol symbol = ASTHelpers.getSymbol(tree);
    if (symbol != null
        && symbol.getQualifiedName().toString().startsWith("org.mockito.internal")
        && !insideMockitoPackage(state)) {
      return describeMatch(tree);
    }
    return Description.NO_MATCH;
  }

  private static boolean insideMockitoPackage(VisitorState state) {
    ExpressionTree packageName = state.getPath().getCompilationUnit().getPackageName();

    // Classes in the default packages do not have a packageName
    if (packageName == null) {
      return false;
    }

    return packageName.toString().startsWith("org.mockito");
  }
}
