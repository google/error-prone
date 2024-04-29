/*
 * Copyright 2023 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFix.emptyFix;
import static com.google.errorprone.fixes.SuggestedFix.merge;
import static com.google.errorprone.fixes.SuggestedFixes.addModifiers;
import static com.google.errorprone.fixes.SuggestedFixes.removeModifiers;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.canBeRemoved;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasDirectAnnotationWithSimpleName;
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.isStatic;
import static javax.lang.model.element.ElementKind.FIELD;
import static javax.lang.model.element.Modifier.FINAL;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.Objects;
import javax.lang.model.element.Modifier;

/** A BugPattern; see the summary. */
@BugPattern(summary = "Static fields should almost always be final.", severity = WARNING)
public final class NonFinalStaticField extends BugChecker implements VariableTreeMatcher {
  private static final ImmutableSet<String> ANNOTATIONS_TO_AVOID =
      ImmutableSet.of("Captor", "Inject", "Mock", "TestParameter");

  private static final ImmutableSet<String> BEFORE_ALL_METHOD_ANNOTATIONS =
      ImmutableSet.of("org.junit.BeforeClass", "org.junit.jupiter.api.BeforeAll");

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    var symbol = getSymbol(tree);
    if (!symbol.getKind().equals(FIELD)) {
      return NO_MATCH;
    }
    if (!isStatic(symbol)) {
      return NO_MATCH;
    }
    if (isConsideredFinal(symbol)) {
      return NO_MATCH;
    }
    if (ANNOTATIONS_TO_AVOID.stream()
        .anyMatch(anno -> hasDirectAnnotationWithSimpleName(tree, anno))) {
      return NO_MATCH;
    }
    IsMutated everMutatedInSameCompilationUnit = isEverMutatedInSameCompilationUnit(symbol, state);
    if (everMutatedInSameCompilationUnit == IsMutated.IN_BEFORE_METHOD) {
      return NO_MATCH;
    }
    if (!canBeRemoved(symbol, state) || everMutatedInSameCompilationUnit == IsMutated.TRUE) {
      return describeMatch(tree);
    }
    return describeMatch(
        tree,
        merge(
            addModifiers(tree, tree.getModifiers(), state, ImmutableSet.of(FINAL))
                .orElse(emptyFix()),
            removeModifiers(tree.getModifiers(), state, ImmutableSet.of(Modifier.VOLATILE))
                .orElse(emptyFix()),
            addDefaultInitializerIfNecessary(tree, state)));
  }

  private static SuggestedFix addDefaultInitializerIfNecessary(
      VariableTree tree, VisitorState state) {
    if (tree.getInitializer() != null) {
      return emptyFix();
    }
    int pos = state.getEndPosition(tree) - 1;
    return SuggestedFix.replace(pos, pos, " = " + getDefaultInitializer(tree, state));
  }

  private static String getDefaultInitializer(VariableTree tree, VisitorState state) {
    var type = getType(tree);
    var symtab = state.getSymtab();
    if (isSameType(type, symtab.booleanType, state)) {
      return "false";
    }
    if (isSameType(type, symtab.shortType, state)) {
      return "(short) 0";
    }
    if (isSameType(type, symtab.byteType, state)) {
      return "(byte) 0";
    }
    if (isSameType(type, symtab.charType, state)) {
      return "(char) 0";
    }
    if (isSameType(type, symtab.longType, state)
        || isSameType(type, symtab.intType, state)
        || isSameType(type, symtab.floatType, state)
        || isSameType(type, symtab.doubleType, state)) {
      return "0";
    }
    return "null";
  }

  enum IsMutated {
    TRUE,
    FALSE,
    IN_BEFORE_METHOD
  }

  private static IsMutated isEverMutatedInSameCompilationUnit(
      VarSymbol symbol, VisitorState state) {
    var scanner =
        new TreeScanner<Void, Void>() {
          IsMutated isMutated = IsMutated.FALSE;

          boolean inBeforeMethod = false;

          @Override
          public Void visitAssignment(AssignmentTree tree, Void unused) {
            if (Objects.equals(getSymbol(tree.getVariable()), symbol)) {
              isMutated();
            }
            return super.visitAssignment(tree, null);
          }

          @Override
          public Void visitCompoundAssignment(CompoundAssignmentTree tree, Void unused) {
            if (Objects.equals(getSymbol(tree.getVariable()), symbol)) {
              isMutated();
            }
            return super.visitCompoundAssignment(tree, null);
          }

          @Override
          public Void visitUnary(UnaryTree tree, Void unused) {
            if (Objects.equals(getSymbol(tree.getExpression()), symbol)
                && isMutating(tree.getKind())) {
              isMutated();
            }
            return super.visitUnary(tree, null);
          }

          private void isMutated() {
            if (inBeforeMethod) {
              isMutated = IsMutated.IN_BEFORE_METHOD;
            } else if (isMutated.equals(IsMutated.FALSE)) {
              isMutated = IsMutated.TRUE;
            }
          }

          private boolean isMutating(Kind kind) {
            switch (kind) {
              case POSTFIX_DECREMENT:
              case POSTFIX_INCREMENT:
              case PREFIX_DECREMENT:
              case PREFIX_INCREMENT:
                return true;
              default:
                return false;
            }
          }

          @Override
          public Void visitMethod(MethodTree tree, Void unused) {
            boolean prev = inBeforeMethod;
            try {
              inBeforeMethod |=
                  BEFORE_ALL_METHOD_ANNOTATIONS.stream()
                      .anyMatch(annotation -> ASTHelpers.hasAnnotation(tree, annotation, state));
              return super.visitMethod(tree, null);
            } finally {
              inBeforeMethod = prev;
            }
          }
        };
    scanner.scan(state.getPath().getCompilationUnit(), null);
    return scanner.isMutated;
  }
}
