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

import static com.google.common.collect.Sets.immutableEnumSet;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.suppliers.Suppliers.JAVA_LANG_VOID_TYPE;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static javax.lang.model.element.ElementKind.BINDING_VARIABLE;
import static javax.lang.model.element.ElementKind.FIELD;
import static javax.lang.model.element.ElementKind.LOCAL_VARIABLE;
import static javax.lang.model.element.ElementKind.PARAMETER;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.IdentifierTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MemberSelectTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import javax.lang.model.element.ElementKind;

/** A BugPattern; see the summary. */
@BugPattern(
    summary =
        "Using a Void-typed variable is potentially confusing, and can be replaced with a literal"
            + " `null`.",
    severity = WARNING)
public final class VoidUsed extends BugChecker
    implements IdentifierTreeMatcher, MemberSelectTreeMatcher {
  @Override
  public Description matchIdentifier(IdentifierTree tree, VisitorState state) {
    return handle(tree, state);
  }

  @Override
  public Description matchMemberSelect(MemberSelectTree tree, VisitorState state) {
    return handle(tree, state);
  }

  private Description handle(Tree tree, VisitorState state) {
    var parent = state.getPath().getParentPath().getLeaf();
    if (parent instanceof AssignmentTree assignmentTree
        && assignmentTree.getVariable().equals(tree)) {
      return NO_MATCH;
    }
    var symbol = getSymbol(tree);
    if (symbol == null
        || !KINDS.contains(symbol.getKind())
        || !isSameType(symbol.type, JAVA_LANG_VOID_TYPE.get(state), state)) {
      return NO_MATCH;
    }
    return describeMatch(tree, SuggestedFix.replace(tree, "null"));
  }

  private static final ImmutableSet<ElementKind> KINDS =
      immutableEnumSet(PARAMETER, LOCAL_VARIABLE, FIELD, BINDING_VARIABLE);
}
