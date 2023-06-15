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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.constructor;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static com.google.errorprone.util.ASTHelpers.requiresParentheses;
import static com.google.errorprone.util.ASTHelpers.targetType;
import static java.util.stream.Collectors.joining;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.ASTHelpers.TargetType;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Position;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.ElementKind;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Prefer string concatenation over explicitly using `StringBuilder#append`, since `+` reads"
            + " better and has equivalent or better performance.",
    severity = BugPattern.SeverityLevel.WARNING)
public class UnnecessaryStringBuilder extends BugChecker implements NewClassTreeMatcher {
  private static final Matcher<ExpressionTree> MATCHER =
      constructor().forClass("java.lang.StringBuilder");

  private static final Matcher<ExpressionTree> APPEND =
      instanceMethod().onExactClass("java.lang.StringBuilder").named("append");

  private static final Matcher<ExpressionTree> TO_STRING =
      instanceMethod().onExactClass("java.lang.StringBuilder").named("toString");

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    List<ExpressionTree> parts = new ArrayList<>();
    switch (tree.getArguments().size()) {
      case 0:
        break;
      case 1:
        ExpressionTree argument = getOnlyElement(tree.getArguments());
        if (isSubtype(getType(argument), JAVA_LANG_CHARSEQUENCE.get(state), state)) {
          parts.add(argument);
        }
        break;
      default:
        return NO_MATCH;
    }
    TreePath path = state.getPath();
    while (true) {
      TreePath parentPath = path.getParentPath();
      if (!(parentPath.getLeaf() instanceof MemberSelectTree)) {
        break;
      }
      TreePath grandParent = parentPath.getParentPath();
      if (!(grandParent.getLeaf() instanceof MethodInvocationTree)) {
        break;
      }
      MethodInvocationTree methodInvocationTree = (MethodInvocationTree) grandParent.getLeaf();
      if (!methodInvocationTree.getMethodSelect().equals(parentPath.getLeaf())) {
        break;
      }
      if (APPEND.matches(methodInvocationTree, state)) {
        if (methodInvocationTree.getArguments().size() != 1) {
          // an append method that doesn't transliterate to concat
          return NO_MATCH;
        }
        parts.add(getOnlyElement(methodInvocationTree.getArguments()));
        path = parentPath.getParentPath();
      } else if (TO_STRING.matches(methodInvocationTree, state)) {
        return describeMatch(
            methodInvocationTree,
            SuggestedFix.replace(methodInvocationTree, replacement(state, parts)));
      } else {
        // another instance method on StringBuilder
        return NO_MATCH;
      }
    }
    ASTHelpers.TargetType target = ASTHelpers.targetType(state.withPath(path));
    if (!isUsedAsStringBuilder(state, target)) {
      return describeMatch(
          path.getLeaf(), SuggestedFix.replace(path.getLeaf(), replacement(state, parts)));
    }
    Tree leaf = target.path().getLeaf();
    if (leaf instanceof VariableTree) {
      VariableTree variableTree = (VariableTree) leaf;
      if (isRewritableVariable(variableTree, state)) {
        SuggestedFix.Builder fix = SuggestedFix.builder();
        if (state.getEndPosition(variableTree.getType()) != Position.NOPOS) {
          // If the variable is declared with `var`, there's no declaration type to change
          fix.replace(variableTree.getType(), "String");
        }
        fix.replace(variableTree.getInitializer(), replacement(state, parts));
        return describeMatch(variableTree, fix.build());
      }
    }
    return NO_MATCH;
  }

  /**
   * Returns true if the StringBuilder is assigned to a variable, and the type of the variable can
   * safely be refactored to be a String.
   */
  boolean isRewritableVariable(VariableTree variableTree, VisitorState state) {
    Symbol sym = getSymbol(variableTree);
    if (!sym.getKind().equals(ElementKind.LOCAL_VARIABLE)) {
      return false;
    }
    boolean[] ok = {true};
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitIdentifier(IdentifierTree tree, Void unused) {
        if (sym.equals(getSymbol(tree))) {
          TargetType target = targetType(state.withPath(getCurrentPath()));
          if (isUsedAsStringBuilder(state, target)) {
            ok[0] = false;
          }
        }
        return super.visitIdentifier(tree, unused);
      }
    }.scan(state.getPath().getCompilationUnit(), null);
    return ok[0];
  }

  private static boolean isUsedAsStringBuilder(VisitorState state, TargetType target) {
    if (target.path().getLeaf().getKind().equals(Tree.Kind.MEMBER_REFERENCE)) {
      // e.g. sb::append
      return true;
    }
    return ASTHelpers.isSubtype(target.type(), JAVA_LANG_APPENDABLE.get(state), state);
  }

  private static String replacement(VisitorState state, List<ExpressionTree> parts) {
    if (parts.isEmpty()) {
      return "\"\"";
    }
    return parts.stream()
        .map(
            x -> {
              String source = state.getSourceForNode(x);
              if (requiresParentheses(x, state)) {
                source = String.format("(%s)", source);
              }
              return source;
            })
        .collect(joining(" + "));
  }

  private static final Supplier<Type> JAVA_LANG_APPENDABLE =
      VisitorState.memoize(state -> state.getTypeFromString("java.lang.Appendable"));

  private static final Supplier<Type> JAVA_LANG_CHARSEQUENCE =
      VisitorState.memoize(state -> state.getTypeFromString("java.lang.CharSequence"));
}
