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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.fixes.SuggestedFixes.addMembers;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.createPrivateConstructor;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isGeneratedConstructor;
import static com.sun.source.tree.Tree.Kind.CLASS;
import static com.sun.source.tree.Tree.Kind.METHOD;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import javax.lang.model.element.Modifier;

/**
 * @author gak@google.com (Gregory Kick)
 */
@BugPattern(
    summary =
        "Classes which are not intended to be instantiated should be made non-instantiable with a"
            + " private constructor. This includes utility classes (classes with only static"
            + " members), and the main class.",
    severity = SUGGESTION)
public final class PrivateConstructorForUtilityClass extends BugChecker
    implements ClassTreeMatcher {

  private static final ImmutableSet<String> ANNOTATIONS_TO_IGNORE =
      ImmutableSet.of(
          "org.junit.runner.RunWith",
          "org.robolectric.annotation.Implements",
          "org.springframework.boot.autoconfigure.SpringBootApplication",
          "com.google.auto.value.AutoValue");

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    if (!classTree.getKind().equals(CLASS)
        || classTree.getModifiers().getFlags().contains(Modifier.ABSTRACT)
        || classTree.getExtendsClause() != null
        || !classTree.getImplementsClause().isEmpty()
        || isInPrivateScope(state)) {
      return NO_MATCH;
    }
    ClassSymbol sym = getSymbol(classTree);
    for (String annotation : ANNOTATIONS_TO_IGNORE) {
      if (hasAnnotation(sym, annotation, state)) {
        return NO_MATCH;
      }
    }

    ImmutableList<Tree> nonSyntheticMembers =
        classTree.getMembers().stream()
            .filter(
                tree ->
                    !(tree instanceof MethodTree methodTree && isGeneratedConstructor(methodTree)))
            .collect(toImmutableList());
    if (nonSyntheticMembers.isEmpty()
        || nonSyntheticMembers.stream().anyMatch(PrivateConstructorForUtilityClass::isInstance)) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fix =
        addMembers(classTree, state, createPrivateConstructor(classTree)).toBuilder();
    SuggestedFixes.addModifiers(classTree, state, Modifier.FINAL).ifPresent(fix::merge);
    return describeMatch(classTree, fix.build());
  }

  private static boolean isInPrivateScope(VisitorState state) {
    return stream(state.getPath())
        .anyMatch(
            currentLeaf ->
                // Checking instanceof rather than Kind given (e.g.) enums are ClassTrees.
                currentLeaf instanceof ClassTree classTree
                    && classTree.getModifiers().getFlags().contains(PRIVATE));
  }

  private static boolean isInstance(Tree tree) {
    return switch (tree.getKind()) {
      case CLASS -> !((ClassTree) tree).getModifiers().getFlags().contains(STATIC);
      case METHOD -> !((MethodTree) tree).getModifiers().getFlags().contains(STATIC);
      case VARIABLE -> !((VariableTree) tree).getModifiers().getFlags().contains(STATIC);
      case BLOCK -> !((BlockTree) tree).isStatic();
      case ENUM, ANNOTATION_TYPE, INTERFACE, RECORD -> false;
      default -> throw new AssertionError("unknown member type:" + tree.getKind());
    };
  }
}
