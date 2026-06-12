/*
 * Copyright 2026 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.constructor;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.containsComments;
import static com.google.errorprone.util.ASTHelpers.findEnclosingMethod;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.stripParentheses;

import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.IfTreeMatcher;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import java.util.List;
import javax.inject.Inject;
import javax.lang.model.element.ElementKind;
import org.jspecify.annotations.Nullable;

/**
 * A check that suggests using {@link com.google.common.base.Preconditions} instead of explicit
 * if-throw checks on method parameters.
 */
@BugPattern(
    summary = "Consider using Preconditions instead of explicit if-throw for parameter validation.",
    severity = WARNING)
public final class PreferPreconditions extends BugChecker implements IfTreeMatcher {

  private final ConstantExpressions constantExpressions;

  @Inject
  PreferPreconditions(ErrorProneFlags flags) {
    this.constantExpressions = ConstantExpressions.fromFlags(flags);
  }

  @Override
  public Description matchIf(IfTree tree, VisitorState state) {
    if (tree.getElseStatement() != null) {
      return NO_MATCH;
    }
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (parent instanceof IfTree) {
      return NO_MATCH;
    }

    ThrowTree throwTree = getSingleThrow(tree.getThenStatement());
    if (throwTree == null) {
      return NO_MATCH;
    }

    NewClassTree newClass = getThrownNewClass(throwTree.getExpression());
    if (newClass == null) {
      return NO_MATCH;
    }

    ExpressionTree condition = stripParentheses(tree.getCondition());
    if (!usesMethodParameter(condition)) {
      return NO_MATCH;
    }

    if (!areArgumentsSideEffectFree(newClass, state)) {
      return NO_MATCH;
    }

    // Don't refactor conditions with logical operators, as they can become quite complex and
    // difficult to read when negated.
    if (isHardToNegate(condition)) {
      return NO_MATCH;
    }

    // Keep this as late as possible to avoid scanning the tree for comments unnecessarily.
    if (containsComments(tree, state)) {
      return NO_MATCH;
    }

    ExpressionTree checkedExpr = getNullCheckedExpression(condition);
    if (NEW_NPE.matches(newClass, state) && checkedExpr != null) {
      return suggest(tree, "checkNotNull", state.getSourceForNode(checkedExpr), newClass, state);
    }

    if (NEW_IAE.matches(newClass, state)) {
      return suggest(tree, "checkArgument", negate(condition, state), newClass, state);
    }

    if (NEW_ISE.matches(newClass, state)) {
      return suggest(tree, "checkState", negate(condition, state), newClass, state);
    }

    return NO_MATCH;
  }

  /**
   * Returns the {@link ThrowTree} if {@code then} is a single throw statement, or a block
   * containing a single throw statement; otherwise returns {@code null}.
   */
  private static @Nullable ThrowTree getSingleThrow(StatementTree then) {
    if (then instanceof ThrowTree throwTree) {
      return throwTree;
    }
    if (then instanceof BlockTree block) {
      List<? extends StatementTree> statements = block.getStatements();
      if (statements.size() == 1 && statements.getFirst() instanceof ThrowTree throwTree) {
        return throwTree;
      }
    }
    return null;
  }

  /**
   * Returns {@code expr} as a {@link NewClassTree} if it's a constructor call of the thrown
   * exception; otherwise returns {@code null}.
   */
  private static @Nullable NewClassTree getThrownNewClass(ExpressionTree expr) {
    return stripParentheses(expr) instanceof NewClassTree newClassTree ? newClassTree : null;
  }

  /** Returns true if {@code tree} contains any references to method parameters. */
  private static boolean usesMethodParameter(Tree tree) {
    var scanner =
        new TreeScanner<Void, Void>() {
          private boolean foundMethodParameter;

          @Override
          public Void scan(Tree tree, Void unused) {
            return foundMethodParameter ? null : super.scan(tree, null);
          }

          @Override
          public Void visitIdentifier(IdentifierTree node, Void unused) {
            Symbol sym = getSymbol(node);
            if (sym != null && sym.getKind() == ElementKind.PARAMETER) {
              foundMethodParameter = true;
            }
            return super.visitIdentifier(node, null);
          }
        };
    scanner.scan(tree, null);
    return scanner.foundMethodParameter;
  }

  /**
   * If {@code condition} is of the form {@code a == null} or {@code null == a}, returns the
   * expression {@code a}; otherwise returns {@code null}.
   */
  private static @Nullable ExpressionTree getNullCheckedExpression(ExpressionTree condition) {
    if (condition instanceof BinaryTree binary && binary.getKind() == Tree.Kind.EQUAL_TO) {
      if (binary.getLeftOperand().getKind() == Tree.Kind.NULL_LITERAL) {
        return binary.getRightOperand();
      }
      if (binary.getRightOperand().getKind() == Tree.Kind.NULL_LITERAL) {
        return binary.getLeftOperand();
      }
    }
    return null;
  }

  /**
   * Returns a {@link Description} with a {@link SuggestedFix} to replace {@code ifTree} with a call
   * to {@code Preconditions.method}.
   */
  private Description suggest(
      IfTree ifTree,
      String method,
      String conditionSource,
      NewClassTree newClass,
      VisitorState state) {
    // Don't suggest refactoring to checkX if we are already inside a method named checkX,
    // as this would introduce infinite recursion (e.g. if the user is implementing their
    // own checkNotNull to avoid dependencies).
    // This check relies purely on the method name. Since this checker specifically targets Guava's
    // Preconditions, it's unlikely to cause issues, but a more robust check might also verify that
    // the method belongs to a utility class or has a specific signature. Given this is a
    // SUGGESTION, the current heuristic is acceptable.
    MethodTree enclosingMethod = findEnclosingMethod(state);
    if (enclosingMethod != null && enclosingMethod.getName().contentEquals(method)) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    fix.addStaticImport("com.google.common.base.Preconditions." + method);

    StringBuilder replacement = new StringBuilder(method).append("(").append(conditionSource);
    appendArguments(replacement, newClass, state);
    replacement.append(");");

    return describeMatch(ifTree, fix.replace(ifTree, replacement.toString()).build());
  }

  /**
   * Returns true if all arguments to the exception constructor are side-effect-free.
   *
   * <p>This is to avoid hoisting side-effects, which could change behavior. For example, changing
   * {@code if (foo == null) { throw new NPE(bar()); }} to {@code checkNotNull(foo, bar())} would
   * cause {@code bar()} to be evaluated even if {@code foo} is not null.
   */
  private boolean areArgumentsSideEffectFree(NewClassTree newClass, VisitorState state) {
    List<? extends ExpressionTree> args = newClass.getArguments();
    if (args.isEmpty()) {
      return true;
    }
    ExpressionTree firstArg = args.getFirst();
    List<? extends ExpressionTree> formatArgs = getSafeStringFormatArgs(firstArg, state);
    if (formatArgs != null) {
      return formatArgs.stream()
          .allMatch(arg -> constantExpressions.constantExpression(arg, state).isPresent());
    }
    return args.stream()
        .allMatch(arg -> constantExpressions.constantExpression(arg, state).isPresent());
  }

  /**
   * If {@code tree} is a call to {@code String.format} or {@code Strings.lenientFormat} with a
   * literal format string that's safe for Preconditions, returns its arguments. Otherwise returns
   * null.
   */
  private static @Nullable List<? extends ExpressionTree> getSafeStringFormatArgs(
      ExpressionTree tree, VisitorState state) {
    if (STRING_FORMAT.matches(tree, state)) {
      MethodInvocationTree formatCall = (MethodInvocationTree) tree;
      ExpressionTree template = formatCall.getArguments().getFirst();
      if (template instanceof LiteralTree literal
          && literal.getValue() instanceof String templateStr
          && isSafeForPreconditions(templateStr)) {
        return formatCall.getArguments();
      }
    }
    return null;
  }

  /**
   * Appends {@code newClass}'s arguments to {@code sb} as arguments to a Preconditions call.
   *
   * <p>If the only argument is a safe call to {@code String.format}, its arguments are unpacked to
   * become arguments to the Preconditions method.
   */
  private void appendArguments(StringBuilder sb, NewClassTree newClass, VisitorState state) {
    List<? extends ExpressionTree> args = newClass.getArguments();
    if (args.isEmpty()) {
      return;
    }
    List<? extends ExpressionTree> formatArgs = getSafeStringFormatArgs(args.getFirst(), state);
    if (formatArgs != null) {
      formatArgs.forEach(arg -> sb.append(", ").append(state.getSourceForNode(arg)));
      return;
    }
    args.forEach(arg -> sb.append(", ").append(state.getSourceForNode(arg)));
  }

  /**
   * Returns true if the template string is safe to be passed to Preconditions varargs methods,
   * which only support {@code %s} placeholders.
   *
   * <p>Preconditions' format methods (e.g., {@code checkArgument}) only support the {@code %s}
   * format specifier. Other specifiers, including {@code %%} for a literal percent sign, are not
   * supported and would lead to runtime errors if passed to Preconditions.
   */
  private static boolean isSafeForPreconditions(String template) {
    int index = 0;
    while ((index = template.indexOf('%', index)) != -1) {
      if (index + 1 >= template.length()) {
        return false;
      }
      if (template.charAt(index + 1) != 's') {
        return false;
      }
      index += 2;
    }
    return true;
  }

  /**
   * Returns a source representation of {@code condition}, heuristically negated.
   *
   * <p>If {@code condition} is a binary operator, we swap it for its negated counterpart (e.g.,
   * {@code ==} becomes {@code !=}). If it's a unary negation, we strip the negation. Otherwise, we
   * prefix it with {@code !}.
   */
  private static String negate(ExpressionTree condition, VisitorState state) {
    condition = stripParentheses(condition);
    if (condition instanceof BinaryTree binary) {
      // Avoid swapping operators for floating point types because !(a < b) and a >= b are not
      // equivalent when NaN is involved. The fallback to !(...) preserves behavior.
      if (!isFloatingPoint(binary, state)) {
        String op =
            switch (condition.getKind()) {
              case EQUAL_TO -> "!=";
              case NOT_EQUAL_TO -> "==";
              case LESS_THAN -> ">=";
              case LESS_THAN_EQUAL -> ">";
              case GREATER_THAN -> "<=";
              case GREATER_THAN_EQUAL -> "<";
              default -> null;
            };
        if (op != null) {
          return String.format(
              "%s %s %s",
              state.getSourceForNode(binary.getLeftOperand()),
              op,
              state.getSourceForNode(binary.getRightOperand()));
        }
      }
    }
    return switch (condition.getKind()) {
      case LOGICAL_COMPLEMENT ->
          state.getSourceForNode(stripParentheses(((UnaryTree) condition).getExpression()));
      case METHOD_INVOCATION, IDENTIFIER, MEMBER_SELECT -> "!" + state.getSourceForNode(condition);
      default -> "!(" + state.getSourceForNode(condition) + ")";
    };
  }

  /** Returns true if either operand of {@code binary} is a float or double. */
  private static boolean isFloatingPoint(BinaryTree binary, VisitorState state) {
    return isFloatingPoint(getType(binary.getLeftOperand()), state)
        || isFloatingPoint(getType(binary.getRightOperand()), state);
  }

  /** Returns true if {@code type} is a float or double. */
  private static boolean isFloatingPoint(@Nullable Type type, VisitorState state) {
    if (type == null) {
      return false;
    }
    Type unboxed = state.getTypes().unboxedTypeOrType(type);
    return unboxed.hasTag(TypeTag.FLOAT) || unboxed.hasTag(TypeTag.DOUBLE);
  }

  private static boolean isHardToNegate(ExpressionTree condition) {
    var scanner =
        new TreeScanner<Void, Void>() {
          private boolean foundComplex;

          @Override
          public Void scan(Tree tree, Void unused) {
            if (foundComplex) {
              return null;
            }
            if (tree != null) {
              switch (tree.getKind()) {
                case CONDITIONAL_AND,
                    CONDITIONAL_OR,
                    XOR,
                    CONDITIONAL_EXPRESSION,
                    SWITCH_EXPRESSION ->
                    foundComplex = true;
                default -> {}
              }
            }
            return super.scan(tree, null);
          }
        };
    scanner.scan(condition, null);
    return scanner.foundComplex;
  }

  private static final Matcher<ExpressionTree> STRING_FORMAT =
      anyOf(
          staticMethod().onClass("java.lang.String").named("format"),
          staticMethod().onClass("com.google.common.base.Strings").named("lenientFormat"));

  private static final Matcher<ExpressionTree> NEW_NPE =
      anyOf(
          constructor().forClass("java.lang.NullPointerException").withNoParameters(),
          constructor()
              .forClass("java.lang.NullPointerException")
              .withParameters("java.lang.String"));
  private static final Matcher<ExpressionTree> NEW_IAE =
      anyOf(
          constructor().forClass("java.lang.IllegalArgumentException").withNoParameters(),
          constructor()
              .forClass("java.lang.IllegalArgumentException")
              .withParameters("java.lang.String"));
  private static final Matcher<ExpressionTree> NEW_ISE =
      anyOf(
          constructor().forClass("java.lang.IllegalStateException").withNoParameters(),
          constructor()
              .forClass("java.lang.IllegalStateException")
              .withParameters("java.lang.String"));
}
