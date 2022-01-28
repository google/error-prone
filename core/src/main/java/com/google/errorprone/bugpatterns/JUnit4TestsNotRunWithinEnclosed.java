/*
 * Copyright 2021 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.fixes.SuggestedFixes.updateAnnotationArgumentValues;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.JUnitMatchers.TEST_CASE;
import static com.google.errorprone.matchers.JUnitMatchers.isJUnit4TestRunnerOfType;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.hasArgumentWithValue;
import static com.google.errorprone.matchers.Matchers.isType;
import static com.google.errorprone.util.ASTHelpers.getAnnotationWithSimpleName;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.MultiMatcher;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;

/** Finds tests that won't run due to the enclosing runner. */
@BugPattern(
    summary =
        "This test is annotated @Test, but given it's within a class using the Enclosed runner,"
            + " will not run.",
    severity = ERROR)
public final class JUnit4TestsNotRunWithinEnclosed extends BugChecker
    implements CompilationUnitTreeMatcher {
  private static final MultiMatcher<ClassTree, AnnotationTree> ENCLOSED =
      annotations(
          AT_LEAST_ONE,
          allOf(
              isType("org.junit.runner.RunWith"),
              hasArgumentWithValue(
                  "value",
                  isJUnit4TestRunnerOfType(
                      ImmutableSet.of("org.junit.experimental.runners.Enclosed")))));

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    ImmutableSet<Type> extendedTypes = getExtendedTypes(state);

    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitClass(ClassTree classTree, Void unused) {
        if (!ENCLOSED.matches(classTree, state)) {
          return super.visitClass(classTree, null);
        }
        ClassType classType = getType(classTree);
        if (extendedTypes.stream().anyMatch(t -> isSameType(t, classType, state))) {
          return super.visitClass(classTree, null);
        }
        for (Tree member : classTree.getMembers()) {
          if (member instanceof MethodTree && TEST_CASE.matches((MethodTree) member, state)) {
            SuggestedFix.Builder fix = SuggestedFix.builder();
            String junit4 = SuggestedFixes.qualifyType(state, fix, "org.junit.runners.JUnit4");
            state.reportMatch(
                describeMatch(
                    member,
                    fix.merge(
                            updateAnnotationArgumentValues(
                                getAnnotationWithSimpleName(
                                    classTree.getModifiers().getAnnotations(), "RunWith"),
                                state,
                                "value",
                                ImmutableList.of(junit4 + ".class")))
                        .build()));
          }
        }
        return super.visitClass(classTree, unused);
      }
    }.scan(tree, null);
    return Description.NO_MATCH;
  }

  private static ImmutableSet<Type> getExtendedTypes(VisitorState state) {
    ImmutableSet.Builder<Type> extendedTypes = ImmutableSet.builder();
    new TreePathScanner<Void, Void>() {

      @Override
      public Void visitClass(ClassTree classTree, Void unused) {
        if (classTree.getExtendsClause() != null) {
          extendedTypes.add(getType(classTree.getExtendsClause()));
        }
        return super.visitClass(classTree, null);
      }
    }.scan(state.getPath().getCompilationUnit(), null);
    return extendedTypes.build();
  }
}
