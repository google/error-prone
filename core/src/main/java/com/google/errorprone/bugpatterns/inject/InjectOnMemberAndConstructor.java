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

package com.google.errorprone.bugpatterns.inject;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.InjectMatchers.hasInjectAnnotation;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.constructor;
import static com.google.errorprone.matchers.Matchers.isField;
import static com.google.errorprone.util.ASTHelpers.isStatic;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.InjectMatchers;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.ElementKind;

/**
 * Checks if class constructor and members are both annotated as @Inject.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    summary =
        "Members shouldn't be annotated with @Inject if constructor is already annotated @Inject",
    severity = ERROR)
public class InjectOnMemberAndConstructor extends BugChecker implements ClassTreeMatcher {

  private static final Matcher<ClassTree> HAS_CONSTRUCTORS_WITH_INJECT =
      constructor(AT_LEAST_ONE, hasInjectAnnotation());

  private static final Matcher<VariableTree> INSTANCE_FIELD_WITH_INJECT =
      allOf(isField(), hasInjectAnnotation());

  @Override
  public final Description matchClass(ClassTree classTree, VisitorState state) {
    if (!HAS_CONSTRUCTORS_WITH_INJECT.matches(classTree, state)) {
      return Description.NO_MATCH;
    }

    List<MethodTree> ctors = ASTHelpers.getConstructors(classTree);
    ImmutableList<MethodTree> ctorsWithInject =
        ctors.stream()
            .filter(c -> hasInjectAnnotation().matches(c, state))
            .collect(toImmutableList());

    if (ctorsWithInject.size() != 1) {
      // Injection frameworks don't support multiple @Inject ctors.
      // There is already an ERROR check for it.
      // https://errorprone.info/bugpattern/MoreThanOneInjectableConstructor
      return Description.NO_MATCH;
    }

    // collect the assignments in ctor
    Set<Symbol> variablesAssigned = new HashSet<>();
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitAssignment(AssignmentTree tree, Void unused) {
        Symbol symbol = ASTHelpers.getSymbol(tree.getVariable());
        // check if it is instance field.
        if (symbol != null && symbol.getKind() == ElementKind.FIELD && !isStatic(symbol)) {
          variablesAssigned.add(symbol);
        }
        return super.visitAssignment(tree, null);
      }
    }.scan(getOnlyElement(ctorsWithInject), null);

    SuggestedFix.Builder fix = SuggestedFix.builder();
    VariableTree variableTreeFirstMatch = null;
    for (Tree member : classTree.getMembers()) {
      if (!(member instanceof VariableTree variableTree)) {
        continue;
      }
      if (!INSTANCE_FIELD_WITH_INJECT.matches(variableTree, state)) {
        continue;
      }
      if (!variablesAssigned.contains(ASTHelpers.getSymbol(variableTree))) {
        continue;
      }
      variableTreeFirstMatch = variableTree;
      removeInjectAnnotationFromVariable(variableTree, state).ifPresent(fix::merge);
    }
    if (variableTreeFirstMatch == null) {
      return Description.NO_MATCH;
    }
    if (fix.isEmpty()) {
      return describeMatch(variableTreeFirstMatch);
    }
    return describeMatch(variableTreeFirstMatch, fix.build());
  }

  private static Optional<SuggestedFix> removeInjectAnnotationFromVariable(
      VariableTree variableTree, VisitorState state) {
    for (AnnotationTree annotation : variableTree.getModifiers().getAnnotations()) {
      if (InjectMatchers.IS_APPLICATION_OF_AT_INJECT.matches(annotation, state)) {
        return Optional.of(SuggestedFix.replace(annotation, ""));
      }
    }
    return Optional.empty();
  }
}
