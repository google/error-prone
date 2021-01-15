/*
 * Copyright 2019 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.nullness;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.sun.source.tree.Tree.Kind.STRING_LITERAL;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks for unnecessarily performing null checks on expressions which can't be null.
 *
 * @author awturner@google.com (Andy Turner)
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    name = "UnnecessaryCheckNotNull",
    summary = "This null check is unnecessary; the expression can never be null",
    severity = ERROR,
    altNames = {"PreconditionsCheckNotNull", "PreconditionsCheckNotNullPrimitive"})
public class UnnecessaryCheckNotNull extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<MethodInvocationTree> CHECK_NOT_NULL_MATCHER =
      Matchers.<MethodInvocationTree>anyOf(
          staticMethod().onClass("com.google.common.base.Preconditions").named("checkNotNull"),
          staticMethod().onClass("com.google.common.base.Verify").named("verifyNotNull"),
          staticMethod().onClass("java.util.Objects").named("requireNonNull"));

  private static final Matcher<MethodInvocationTree> NEW_INSTANCE_MATCHER =
      argument(
          0, Matchers.<ExpressionTree>kindAnyOf(ImmutableSet.of(Kind.NEW_CLASS, Kind.NEW_ARRAY)));

  private static final Matcher<MethodInvocationTree> STRING_LITERAL_ARG_MATCHER =
      argument(0, Matchers.<ExpressionTree>kindIs(STRING_LITERAL));

  private static final Matcher<MethodInvocationTree> PRIMITIVE_ARG_MATCHER =
      argument(0, Matchers.<ExpressionTree>isPrimitiveType());

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!CHECK_NOT_NULL_MATCHER.matches(tree, state) || tree.getArguments().isEmpty()) {
      return Description.NO_MATCH;
    }
    if (NEW_INSTANCE_MATCHER.matches(tree, state)) {
      return matchNewInstance(tree, state);
    }
    if (STRING_LITERAL_ARG_MATCHER.matches(tree, state)) {
      return matchStringLiteral(tree, state);
    }
    if (PRIMITIVE_ARG_MATCHER.matches(tree, state)) {
      return describePrimitiveMatch(tree, state);
    }
    return Description.NO_MATCH;
  }

  private Description matchNewInstance(MethodInvocationTree tree, VisitorState state) {
    Fix fix = SuggestedFix.replace(tree, state.getSourceForNode(tree.getArguments().get(0)));
    return describeMatch(tree, fix);
  }

  private Description matchStringLiteral(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    List<? extends ExpressionTree> arguments = methodInvocationTree.getArguments();
    ExpressionTree stringLiteralValue = arguments.get(0);
    Fix fix;
    if (arguments.size() == 2) {
      fix = SuggestedFix.swap(arguments.get(0), arguments.get(1));
    } else {
      fix = SuggestedFix.delete(state.getPath().getParentPath().getLeaf());
    }
    return describeMatch(stringLiteralValue, fix);
  }

  /**
   * If the call to Preconditions.checkNotNull is part of an expression (assignment, return, etc.),
   * we substitute the argument for the method call. E.g.: {@code bar =
   * Preconditions.checkNotNull(foo); ==> bar = foo;}
   *
   * <p>If the argument to Preconditions.checkNotNull is a comparison using == or != and one of the
   * operands is null, we call checkNotNull on the non-null operand. E.g.: {@code checkNotNull(a ==
   * null); ==> checkNotNull(a);}
   *
   * <p>If the argument is a method call or binary tree and its return type is boolean, change it to
   * a checkArgument/checkState. E.g.: {@code Preconditions.checkNotNull(foo.hasFoo()) ==>
   * Preconditions.checkArgument(foo.hasFoo())}
   *
   * <p>Otherwise, delete the checkNotNull call. E.g.: {@code Preconditions.checkNotNull(foo); ==>
   * [delete the line]}
   */
  private Description describePrimitiveMatch(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    ExpressionTree arg1 = methodInvocationTree.getArguments().get(0);
    Tree parent = state.getPath().getParentPath().getLeaf();

    // Assignment, return, etc.
    if (parent.getKind() != Kind.EXPRESSION_STATEMENT) {
      return describeMatch(
          arg1, SuggestedFix.replace(methodInvocationTree, state.getSourceForNode(arg1)));
    }

    // Comparison to null
    if (arg1.getKind() == Kind.EQUAL_TO || arg1.getKind() == Kind.NOT_EQUAL_TO) {
      BinaryTree binaryExpr = (BinaryTree) arg1;
      if (binaryExpr.getLeftOperand().getKind() == Kind.NULL_LITERAL) {
        return describeMatch(
            arg1, SuggestedFix.replace(arg1, state.getSourceForNode(binaryExpr.getRightOperand())));
      }
      if (binaryExpr.getRightOperand().getKind() == Kind.NULL_LITERAL) {
        return describeMatch(
            arg1, SuggestedFix.replace(arg1, state.getSourceForNode(binaryExpr.getLeftOperand())));
      }
    }

    if ((arg1 instanceof BinaryTree
            || arg1.getKind() == Kind.METHOD_INVOCATION
            || arg1.getKind() == Kind.LOGICAL_COMPLEMENT)
        && state.getTypes().isSameType(ASTHelpers.getType(arg1), state.getSymtab().booleanType)) {
      return describeMatch(arg1, createCheckArgumentOrStateCall(methodInvocationTree, state, arg1));
    }

    return describeMatch(arg1, SuggestedFix.delete(parent));
  }

  /**
   * Creates a SuggestedFix that replaces the checkNotNull call with a checkArgument or checkState
   * call.
   */
  private static Fix createCheckArgumentOrStateCall(
      MethodInvocationTree methodInvocationTree, VisitorState state, ExpressionTree arg1) {
    String replacementMethod = "checkState";
    if (hasMethodParameter(state.getPath(), arg1)) {
      replacementMethod = "checkArgument";
    }

    SuggestedFix.Builder fix = SuggestedFix.builder();
    String name =
        SuggestedFixes.qualifyStaticImport(
            "com.google.common.base.Preconditions." + replacementMethod, fix, state);

    fix.replace(methodInvocationTree.getMethodSelect(), name);
    return fix.build();
  }

  /**
   * Determines whether the expression contains a reference to one of the enclosing method's
   * parameters.
   *
   * <p>TODO(eaftan): Extract this to ASTHelpers.
   *
   * @param path the path to the current tree node
   * @param tree the node to compare against the parameters
   * @return whether the argument is a parameter to the enclosing method
   */
  private static boolean hasMethodParameter(TreePath path, ExpressionTree tree) {
    Set<Symbol> symbols = new HashSet<>();
    for (IdentifierTree ident : getVariableUses(tree)) {
      Symbol sym = ASTHelpers.getSymbol(ident);
      if (ASTHelpers.isLocal(sym)) {
        symbols.add(sym);
      }
    }

    // Find enclosing method declaration.
    while (path != null && !(path.getLeaf() instanceof MethodTree)) {
      path = path.getParentPath();
    }
    if (path == null) {
      throw new IllegalStateException("Should have an enclosing method declaration");
    }
    MethodTree methodDecl = (MethodTree) path.getLeaf();
    for (VariableTree param : methodDecl.getParameters()) {
      if (symbols.contains(ASTHelpers.getSymbol(param))) {
        return true;
      }
    }

    return false;
  }

  /**
   * Find the root variable identifiers from an arbitrary expression.
   *
   * <p>Examples: a.trim().intern() ==> {a} a.b.trim().intern() ==> {a} this.intValue.foo() ==>
   * {this} this.foo() ==> {this} intern() ==> {} String.format() ==> {} java.lang.String.format()
   * ==> {} x.y.z(s.t) ==> {x,s}
   */
  static List<IdentifierTree> getVariableUses(ExpressionTree tree) {
    final List<IdentifierTree> freeVars = new ArrayList<>();

    new TreeScanner<Void, Void>() {
      @Override
      public Void visitIdentifier(IdentifierTree node, Void v) {
        if (((JCIdent) node).sym instanceof VarSymbol) {
          freeVars.add(node);
        }
        return super.visitIdentifier(node, v);
      }
    }.scan(tree, null);

    return freeVars;
  }
}
