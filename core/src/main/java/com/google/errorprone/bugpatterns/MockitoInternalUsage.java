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

import static com.google.errorprone.matchers.Matchers.packageStartsWith;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MemberSelectTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;

/**
 * {@link com.google.errorprone.bugpatterns.BugChecker} that detects usages of
 * org.mockito.internal.*
 */
@BugPattern(
    name = "MockitoInternalUsage",
    summary = "org.mockito.internal.* is a private API and should not be used by clients",
    severity = SeverityLevel.WARNING,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class MockitoInternalUsage extends BugChecker implements MemberSelectTreeMatcher {

  private static final Matcher<Tree> INSIDE_MOCKITO = packageStartsWith("org.mockito");

  @Override
  public Description matchMemberSelect(MemberSelectTree tree, VisitorState state) {
    if (INSIDE_MOCKITO.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    Symbol symbol = ASTHelpers.getSymbol(tree);
    if (symbol != null && symbol.getQualifiedName().toString().startsWith("org.mockito.internal")) {
      return describeMatch(tree);
    }
    return Description.NO_MATCH;
  }
}
