/*
 * Copyright 2017 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.overloading;

import static com.google.common.collect.ImmutableList.sortedCopyOf;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.groupingBy;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.tree.JCTree;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * A {@link BugChecker} that detects inconsistently overloaded methods in Java classes.
 *
 * <p>The bug checker works in several stages. First, it group the class methods by name. Then each
 * group is processed separately (and violations are reported on per-group basis).
 *
 * <p>The group is processed by building a {@link ParameterTrie}. This trie is an archetype built
 * from methods of lower arity used to determine ordering of methods of higher arity. After ordering
 * of arguments of particular method has been determined (using the archetype) it is then added to
 * the trie and serves as basis for methods of even higher arity.
 *
 * <p>If the determined ordering is different the original parameter ordering a {@link
 * ParameterOrderingViolation} is reported. It is essentially a list of expected parameter and list
 * of actual parameters then used to build a {@link SuggestedFix} object.
 *
 * @author hanuszczak@google.com (Łukasz Hanuszczak)
 */
@BugPattern(
    name = "InconsistentOverloads",
    summary =
        "The ordering of parameters in overloaded methods should be as consistent as possible (when"
            + " viewed from left to right)",
    generateExamplesFromTestCases = false,
    category = Category.JDK,
    severity = SeverityLevel.WARNING)
public final class InconsistentOverloads extends BugChecker implements ClassTreeMatcher {

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    processClassMethods(getClassTreeMethods(classTree), state);

    /*
     * We want to report inconsistencies per method group. There is no "method group" unit in the
     * Java AST, so we match on the class, group its methods to method groups and process each group
     * separately.
     *
     * Because of this we return no match for the class itself but we report policy violations for
     * each group after it is processed.
     */
    return Description.NO_MATCH;
  }

  private void processClassMethods(List<MethodTree> classMethodTrees, VisitorState state) {
    for (List<MethodTree> groupMethods : getMethodGroups(classMethodTrees)) {
      processGroupMethods(groupMethods, state);
    }
  }

  private void processGroupMethods(List<MethodTree> groupMethodTrees, VisitorState state) {
    Preconditions.checkArgument(!groupMethodTrees.isEmpty());
    for (ParameterOrderingViolation violation : getViolations(groupMethodTrees)) {
      MethodSymbol methodSymbol = getSymbol(violation.methodTree());
      if (ASTHelpers.findSuperMethods(methodSymbol, state.getTypes()).isEmpty()) {
        Description.Builder description = buildDescription(violation.methodTree());
        description.setMessage(violation.getDescription());
        state.reportMatch(description.build());
      }
    }
  }

  private static ImmutableList<ParameterOrderingViolation> getViolations(
      List<MethodTree> groupMethodTrees) {
    ImmutableList.Builder<ParameterOrderingViolation> result = ImmutableList.builder();

    ParameterTrie trie = new ParameterTrie();
    for (MethodTree methodTree : sortedByArity(groupMethodTrees)) {
      Optional<ParameterOrderingViolation> violation = trie.extendAndComputeViolation(methodTree);
      violation.ifPresent(result::add);
    }

    return result.build();
  }

  private static ImmutableList<MethodTree> sortedByArity(Iterable<MethodTree> methodTrees) {
    return sortedCopyOf(comparingArity().thenComparing(comparingPositions()), methodTrees);
  }

  private static Comparator<MethodTree> comparingPositions() {
    return comparingInt(InconsistentOverloads::getStartPosition);
  }

  private static Comparator<MethodTree> comparingArity() {
    return comparingInt(ParameterTrie::getMethodTreeArity);
  }

  /**
   * Returns a collection of method groups for given list of {@code classMethods}.
   *
   * <p>A <i>method group</i> is a list of methods with the same name.
   *
   * <p>It is assumed that given {@code classMethods} really do belong to the same class. The
   * returned collection does not guarantee any particular group ordering.
   */
  private static Collection<List<MethodTree>> getMethodGroups(List<MethodTree> classMethods) {
    return classMethods.stream().collect(groupingBy(MethodTree::getName)).values();
  }

  /**
   * Returns a list of {@link MethodTree} declared in the given {@code classTree}.
   *
   * <p>Only method trees that belong to the {@code classTree} are returned, so methods declared in
   * nested classes are not going to be considered.
   */
  private ImmutableList<MethodTree> getClassTreeMethods(ClassTree classTree) {
    List<? extends Tree> members = classTree.getMembers();
    return members.stream()
        .filter(MethodTree.class::isInstance)
        .map(MethodTree.class::cast)
        .filter(m -> !isSuppressed(m))
        .collect(toImmutableList());
  }

  /**
   * Returns a start position of given {@code tree}.
   *
   * <p>The only purpose of this method is to avoid doing a hacky casting to {@link JCTree}.
   */
  private static int getStartPosition(Tree tree) {
    return ((JCTree) tree).getStartPosition();
  }
}
