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

import static com.google.common.base.Ascii.toLowerCase;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFix.mergeFixes;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.SourceVersion.supportsPatternMatchingInstanceof;
import static com.google.errorprone.util.TargetType.targetType;
import static java.util.Collections.nCopies;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.InstanceOfTreeMatcher;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions.ConstantExpression;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.Reachability;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import java.util.HashSet;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.lang.model.SourceVersion;
import org.jspecify.annotations.Nullable;

/** A BugPattern; see the summary. */
@BugPattern(
    severity = WARNING,
    summary = "This code can be simplified to use a pattern-matching instanceof.")
public final class PatternMatchingInstanceof extends BugChecker implements InstanceOfTreeMatcher {
  private final ConstantExpressions constantExpressions;

  @Inject
  PatternMatchingInstanceof(ConstantExpressions constantExpressions) {
    this.constantExpressions = constantExpressions;
  }

  @Override
  public Description matchInstanceOf(InstanceOfTree instanceOfTree, VisitorState state) {
    if (!supportsPatternMatchingInstanceof(state.context)) {
      return NO_MATCH;
    }
    if (instanceOfTree.getPattern() != null) {
      return NO_MATCH;
    }
    var impliedStatements = findImpliedStatements(instanceOfTree, state);
    if (impliedStatements.isEmpty()) {
      return NO_MATCH;
    }
    var constant =
        constantExpressions.constantExpression(instanceOfTree.getExpression(), state).orElse(null);
    if (constant == null) {
      return NO_MATCH;
    }
    Type targetType = getType(instanceOfTree.getType());
    var allCasts = new HashSet<>(findAllCasts(constant, impliedStatements, targetType, state));
    String name = null;
    SuggestedFix.Builder fix = SuggestedFix.builder();

    int typeArgCount = getType(instanceOfTree.getType()).tsym.getTypeParameters().size();
    if (typeArgCount != 0
        && allCasts.stream()
            .flatMap(c -> Stream.ofNullable(targetType(state.withPath(c))))
            .anyMatch(t -> !t.type().isRaw())) {
      return NO_MATCH;
    }

    // If we find a variable tree which exists only to be assigned the cast result, use that as
    // the name and delete it.
    // NOTE: an even nicer approach would be to delete all such VariableTrees, and rename all
    // the names to one. That would require another scan, though.
    for (TreePath cast : allCasts) {
      VariableTree variableTree = isVariableAssignedFromCast(cast, instanceOfTree, state);
      if (variableTree != null) {
        allCasts.remove(cast);
        fix.delete(variableTree);
        name = variableTree.getName().toString();
        break;
      }
    }

    if (!allCasts.isEmpty() || !fix.isEmpty()) {
      if (name == null) {
        // This is a gamble as to an appropriate name. We could make sure it doesn't clash with
        // anything in scope, but that's effort.
        name = generateVariableName(targetType, state);
      }
      if (typeArgCount != 0 && !(instanceOfTree.getType() instanceof ParameterizedTypeTree)) {
        fix.postfixWith(
            instanceOfTree.getType(),
            nCopies(typeArgCount, "?").stream().collect(joining(",", "<", ">")));
      }
      String fn = name;
      return describeMatch(
          instanceOfTree,
          fix.postfixWith(instanceOfTree, " " + name)
              .merge(
                  allCasts.stream()
                      .map(c -> SuggestedFix.replace(c.getLeaf(), fn))
                      .collect(mergeFixes()))
              .build());
    }
    return NO_MATCH;
  }

  private static @Nullable VariableTree isVariableAssignedFromCast(
      TreePath treePath, InstanceOfTree instanceOfTree, VisitorState state) {
    var parent = treePath.getParentPath().getLeaf();
    if (!(parent instanceof VariableTree variableTree)) {
      return null;
    }
    if (!variableTree.getInitializer().equals(treePath.getLeaf())) {
      return null;
    }
    // Check that the type is exactly the same (not a subtypes), since refactoring cases where the
    // instanceof type is a supertype of the cast type could affect overload resolution.
    if (!state
        .getTypes()
        .isSameType(getType(instanceOfTree.getType()), getType(variableTree.getType()))) {
      return null;
    }
    return variableTree;
  }

  private static String generateVariableName(Type targetType, VisitorState state) {
    Type unboxed = state.getTypes().unboxedType(targetType);
    String simpleName = targetType.tsym.getSimpleName().toString();
    String lowerFirstLetter = toLowerCase(String.valueOf(simpleName.charAt(0)));
    String camelCased = lowerFirstLetter + simpleName.substring(1);
    if (SourceVersion.isKeyword(camelCased)
        || (unboxed != null && unboxed.getTag() != TypeTag.NONE)) {
      return lowerFirstLetter;
    }
    return camelCased;
  }

  /** Finds trees which are implied by the {@code instanceOfTree}. */
  private static ImmutableList<Tree> findImpliedStatements(
      InstanceOfTree tree, VisitorState state) {
    Tree last = tree;
    boolean negated = false;
    var impliedStatements = ImmutableList.<Tree>builder();
    for (TreePath parentPath = state.getPath().getParentPath();
        parentPath != null;
        parentPath = parentPath.getParentPath()) {
      Tree parent = parentPath.getLeaf();
      switch (parent.getKind()) {
        case CONDITIONAL_AND -> {
          if (negated) {
            return impliedStatements.build();
          }
          if (((BinaryTree) parent).getLeftOperand() == last) {
            impliedStatements.add(((BinaryTree) parent).getRightOperand());
          }
        }
        case CONDITIONAL_OR -> {
          if (!negated) {
            return impliedStatements.build();
          }
          if (((BinaryTree) parent).getLeftOperand() == last) {
            impliedStatements.add(((BinaryTree) parent).getRightOperand());
          }
        }
        case PARENTHESIZED -> {}
        case LOGICAL_COMPLEMENT -> {
          negated = !negated;
        }
        case IF -> {
          var ifTree = (IfTree) parent;
          if (ifTree.getCondition() != last) {
            return impliedStatements.build();
          }
          StatementTree positiveBranch =
              negated ? ifTree.getElseStatement() : ifTree.getThenStatement();
          if (positiveBranch != null) {
            impliedStatements.add(positiveBranch);
          }
          StatementTree negativeBranch =
              negated ? ifTree.getThenStatement() : ifTree.getElseStatement();
          if (negativeBranch != null && !Reachability.canCompleteNormally(negativeBranch)) {
            if (parentPath.getParentPath().getLeaf() instanceof BlockTree blockTree) {
              var index = blockTree.getStatements().indexOf(ifTree);
              impliedStatements.addAll(
                  blockTree.getStatements().subList(index + 1, blockTree.getStatements().size()));
            }
          }
          return impliedStatements.build();
        }
        case CONDITIONAL_EXPRESSION -> {
          var conditionalExpression = (ConditionalExpressionTree) parent;
          impliedStatements.add(
              negated
                  ? conditionalExpression.getFalseExpression()
                  : conditionalExpression.getTrueExpression());
          return impliedStatements.build();
        }
        default -> {
          return impliedStatements.build();
        }
      }
      last = parent;
    }
    return impliedStatements.build();
  }

  /**
   * Finds all casts of {@code symbol} which are cast to {@code targetType} within {@code trees}.
   */
  private ImmutableSet<TreePath> findAllCasts(
      ConstantExpression symbol, Iterable<Tree> trees, Type targetType, VisitorState state) {
    var usages = ImmutableSet.<TreePath>builder();
    var scanner =
        new TreePathScanner<Void, Void>() {
          @Override
          public Void visitTypeCast(TypeCastTree node, Void unused) {
            var castee = constantExpressions.constantExpression(node.getExpression(), state);
            if (castee.isPresent()
                && castee.get().equals(symbol)
                && state.getTypes().isSameType(getType(node.getType()), targetType)) {
              usages.add(getUsage(getCurrentPath()));
            }
            return super.visitTypeCast(node, null);
          }
        };
    for (Tree tree : trees) {
      scanner.scan(new TreePath(state.getPath(), tree), null);
    }
    return usages.build();
  }

  private static TreePath getUsage(TreePath currentPath) {
    TreePath parentPath = currentPath.getParentPath();
    return parentPath.getLeaf() instanceof ParenthesizedTree && !requiresParentheses(parentPath)
        ? parentPath
        : currentPath;
  }

  private static boolean requiresParentheses(TreePath path) {
    // This isn't ASTHelpers.requiresParentheses, because we want to know if parens are needed when
    // replacing a cast with the cast's expression, i.e. `((Foo) bar)` -> `(bar)`
    return switch (path.getParentPath().getLeaf().getKind()) {
      case IDENTIFIER,
          MEMBER_SELECT,
          METHOD_INVOCATION,
          ARRAY_ACCESS,
          PARENTHESIZED,
          NEW_CLASS,
          MEMBER_REFERENCE ->
          false;
      default -> true;
    };
  }
}
