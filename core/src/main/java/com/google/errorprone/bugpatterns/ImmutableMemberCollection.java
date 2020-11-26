/*
 * Copyright 2016 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.hasAnnotationWithSimpleName;
import static com.google.errorprone.matchers.Matchers.hasModifier;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.util.ASTHelpers.getConstructors;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import java.util.List;
import javax.lang.model.element.Modifier;

/** Refactoring to suggest Immutable types for member collection that are not mutated. */
@BugPattern(
    name = "ImmutableMemberCollection",
    summary = "If you don't intend to mutate a member collection prefer using Immutable types.",
    severity = SUGGESTION)
public final class ImmutableMemberCollection extends BugChecker implements ClassTreeMatcher {

  // TODO(ashishkedia) : Expand to other types of collection.
  private static final Matcher<Tree> IMMUTABLE_TYPE = isSameType(ImmutableList.class);

  private static final Matcher<Tree> PRIVATE_FINAL_LIST_MATCHER =
      allOf(
          kindIs(Kind.VARIABLE),
          hasModifier(Modifier.PRIVATE),
          hasModifier(Modifier.FINAL),
          isSameType(List.class));

  // TODO(ashishkedia) : Share this with ImmutableSetForContains.
  private static final Matcher<Tree> EXCLUSIONS =
      anyOf(
          hasAnnotationWithSimpleName("Bind"),
          hasAnnotationWithSimpleName("Inject"));

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    ImmutableSet<VariableTree> listMembers =
        classTree.getMembers().stream()
            .filter(member -> PRIVATE_FINAL_LIST_MATCHER.matches(member, state))
            .filter(member -> !EXCLUSIONS.matches(member, state))
            .map(VariableTree.class::cast)
            .collect(toImmutableSet());
    if (listMembers.isEmpty()) {
      return Description.NO_MATCH;
    }
    ArrayListMultimap<Symbol, Tree> initTree = ArrayListMultimap.create();
    ImmutableMap<Symbol, VariableTree> declarations =
        Maps.uniqueIndex(listMembers, ASTHelpers::getSymbol);
    listMembers.stream()
        .filter(var -> var.getInitializer() != null)
        .forEach(var -> initTree.put(getSymbol(var), var.getInitializer()));
    new TreeScanner<Void, VisitorState>() {
      @Override
      public Void visitAssignment(AssignmentTree assignmentTree, VisitorState visitorState) {
        Symbol varSymbol = getSymbol(assignmentTree.getVariable());
        if (declarations.containsKey(varSymbol) && assignmentTree.getExpression() != null) {
          initTree.put(varSymbol, assignmentTree.getExpression());
        }
        return super.visitAssignment(assignmentTree, visitorState);
      }
    }.scan(getConstructors(classTree), state);

    SuggestedFix.Builder suggestedFix = SuggestedFix.builder();
    initTree.keySet().stream()
        // TODO(ashishkedia) : Expand to non-immutable init classTree, but then also scan all usages
        // and look for any potential mutation.
        .filter(
            key -> initTree.get(key).stream().allMatch(tree -> IMMUTABLE_TYPE.matches(tree, state)))
        .map(key -> stripParameters(declarations.get(key).getType()))
        .forEach(type -> suggestedFix.replace(type, "ImmutableList"));
    if (suggestedFix.isEmpty()) {
      return Description.NO_MATCH;
    }
    return describeMatch(classTree, suggestedFix.addImport(ImmutableList.class.getName()).build());
  }

  private static Tree stripParameters(Tree tree) {
    return tree.getKind().equals(Kind.PARAMETERIZED_TYPE)
        ? ((ParameterizedTypeTree) tree).getType()
        : tree;
  }
}
