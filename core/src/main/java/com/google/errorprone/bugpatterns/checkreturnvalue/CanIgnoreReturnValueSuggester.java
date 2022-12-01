/*
 * Copyright 2022 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.checkreturnvalue;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.util.ASTHelpers.getAnnotationWithSimpleName;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getReturnType;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isAbstract;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static com.google.errorprone.util.ASTHelpers.stripParentheses;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;

/**
 * Checker that recommends annotating a method with {@code @CanIgnoreReturnValue} if the method
 * returns {@code this} (or other methods that are likely to also just return {@code this}).
 */
@BugPattern(
    summary =
        "Methods that always 'return this' should be annotated with"
            + " @com.google.errorprone.annotations.CanIgnoreReturnValue",
    severity = WARNING)
public final class CanIgnoreReturnValueSuggester extends BugChecker implements MethodTreeMatcher {
  private static final String CRV = "com.google.errorprone.annotations.CheckReturnValue";
  private static final String CIRV = "com.google.errorprone.annotations.CanIgnoreReturnValue";

  private static final Supplier<Type> PROTO_BUILDER =
      VisitorState.memoize(s -> s.getTypeFromString("com.google.protobuf.MessageLite.Builder"));

  // TODO(kak): catch places where an input parameter is always returned

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    MethodSymbol methodSymbol = getSymbol(methodTree);

    // We have a number of preconditions we can check early to ensure that this method could
    // possibly be @CIRV-suggestible, before attempting a deeper scan of the method.
    if (methodSymbol.isStatic()
        // the return type must be the same as the enclosing type (this skips void methods too)
        || !isSameType(methodSymbol.owner.type, methodSymbol.getReturnType(), state)
        // nb: these methods should probably be @CheckReturnValue!
        // They'd likely be excluded naturally by the isSimpleReturnThisMethod check below, but we
        // check for them explicitly here.
        || isDefinitionOfZeroArgSelf(methodSymbol)
        // Constructors can't "return", and generally shouldn't be @CIRV
        || methodTree.getReturnType() == null
        // b/236423646 - These methods that do nothing *but* `return this;` are likely to be
        // overridden in other contexts, and we've decided that these methods shouldn't be annotated
        // automatically.
        || isSimpleReturnThisMethod(methodTree)
        // TODO(kak): This appears to be a performance optimization for refactoring passes?
        || isSubtype(methodSymbol.owner.type, PROTO_BUILDER.get(state), state)) {
      return Description.NO_MATCH;
    }

    // if the method is already directly annotated w/ @CRV or @CIRV, bail out
    if (hasAnnotation(methodTree, CRV, state) || hasAnnotation(methodSymbol, CIRV, state)) {
      return Description.NO_MATCH;
    }

    // skip @AutoValue and @AutoBuilder methods
    if (isAbstractAutoValueOrAutoBuilderMethod(methodSymbol, state)) {
      return Description.NO_MATCH;
    }

    // if the method looks like a builder, or if it always returns `this`, then make it @CIRV
    if (methodLooksLikeBuilder(methodSymbol) || methodReturnsIgnorableValues(methodTree, state)) {
      SuggestedFix.Builder fix = SuggestedFix.builder();

      // if the method is annotated with @RIU, we need to remove it before adding @CIRV
      AnnotationTree riuAnnotation =
          getAnnotationWithSimpleName(
              methodTree.getModifiers().getAnnotations(), "ResultIgnorabilityUnspecified");
      if (riuAnnotation != null) {
        fix.delete(riuAnnotation);
      }

      // now annotate it with @CanIgnoreReturnValue
      fix.prefixWith(methodTree, "@" + qualifyType(state, fix, CIRV) + "\n");

      return describeMatch(methodTree, fix.build());
    }

    return Description.NO_MATCH;
  }

  private static boolean isAbstractAutoValueOrAutoBuilderMethod(
      MethodSymbol methodSymbol, VisitorState state) {
    Symbol owner = methodSymbol.owner;
    // TODO(kak): use ResultEvaluator instead of duplicating _some_ of the logic (right now we only
    // exclude @AutoValue.Builder's and @AutoBuilder's)
    return isAbstract(methodSymbol)
        && (hasAnnotation(owner, "com.google.auto.value.AutoValue.Builder", state)
            || hasAnnotation(owner, "com.google.auto.value.AutoBuilder", state));
  }

  private static final ImmutableSet<String> BUILDER_METHOD_PREFIXES =
      ImmutableSet.of("add", "set", "with", "clear");

  private static boolean methodLooksLikeBuilder(MethodSymbol methodSymbol) {
    String methodName = methodSymbol.getSimpleName().toString();
    return methodSymbol.owner.getSimpleName().toString().contains("Builder")
        && BUILDER_METHOD_PREFIXES.stream().anyMatch(methodName::startsWith);
  }

  private static boolean isSimpleReturnThisMethod(MethodTree methodTree) {
    if (methodTree.getBody() != null && methodTree.getBody().getStatements().size() == 1) {
      StatementTree onlyStatement = methodTree.getBody().getStatements().get(0);
      if (onlyStatement instanceof ReturnTree) {
        return returnsThisOrSelf((ReturnTree) onlyStatement);
      }
    }
    return false;
  }

  private static boolean isIdentifier(ExpressionTree expr, String identifierName) {
    expr = stripParentheses(expr);
    if (expr instanceof IdentifierTree) {
      return ((IdentifierTree) expr).getName().contentEquals(identifierName);
    }
    return false;
  }

  /** Returns whether or not the given {@link ReturnTree} returns exactly {@code this}. */
  private static boolean returnsThisOrSelf(ReturnTree returnTree) {
    return maybeCastThis(returnTree.getExpression());
  }

  private static boolean maybeCastThis(Tree tree) {
    return firstNonNull(
        new SimpleTreeVisitor<Boolean, Void>() {
          @Override
          public Boolean visitParenthesized(ParenthesizedTree tree, Void unused) {
            return visit(tree.getExpression(), null);
          }

          @Override
          public Boolean visitTypeCast(TypeCastTree tree, Void unused) {
            return visit(tree.getExpression(), null);
          }

          @Override
          public Boolean visitIdentifier(IdentifierTree tree, Void unused) {
            return tree.getName().contentEquals("this");
            // TODO(cpovirk): Or a field that is always set to `this`, as in SelfAlwaysReturnsThis.
          }

          @Override
          public Boolean visitMethodInvocation(MethodInvocationTree tree, Void unused) {
            return getSymbol(tree).getSimpleName().contentEquals("self")
                || getSymbol(tree).getSimpleName().contentEquals("getThis");
          }
        }.visit(tree, null),
        false);
  }

  private static boolean isDefinitionOfZeroArgSelf(MethodSymbol methodSymbol) {
    return (methodSymbol.getSimpleName().contentEquals("self")
            || methodSymbol.getSimpleName().contentEquals("getThis"))
        && methodSymbol.getParameters().isEmpty();
  }

  private static boolean methodReturnsIgnorableValues(MethodTree tree, VisitorState state) {
    class ReturnValuesFromMethodAreIgnorable extends TreeScanner<Void, Void> {
      private final VisitorState state;
      private final Type enclosingClassType;
      private final Type methodReturnType;
      private boolean atLeastOneReturn = false;
      private boolean allReturnsIgnorable = true;

      private ReturnValuesFromMethodAreIgnorable(VisitorState state, MethodSymbol methSymbol) {
        this.state = state;
        this.methodReturnType = methSymbol.getReturnType();
        this.enclosingClassType = methSymbol.enclClass().type;
      }

      @Override
      public Void visitReturn(ReturnTree returnTree, Void unused) {
        atLeastOneReturn = true;
        if (!returnsThisOrSelf(returnTree)
            && !isIgnorableMethodCallOnSameInstance(returnTree, state)) {
          allReturnsIgnorable = false;
        }
        // Don't descend deeper into returns, since we already checked the body of this return.
        return null;
      }

      private boolean isIgnorableMethodCallOnSameInstance(
          ReturnTree returnTree, VisitorState state) {
        if (returnTree.getExpression() instanceof MethodInvocationTree) {
          MethodInvocationTree mit = (MethodInvocationTree) returnTree.getExpression();
          ExpressionTree receiver = getReceiver(mit);
          MethodSymbol calledMethod = getSymbol(mit);
          if ((receiver == null && !calledMethod.isStatic())
              || isIdentifier(receiver, "this")
              || isIdentifier(receiver, "super")) {
            // If the method we're calling is @CIRV and the enclosing class could be represented by
            // the object being returned by the other method, then it's probable that the other
            // method is likely to
            // be an ignorable result.
            return hasAnnotation(calledMethod, CIRV, state)
                && isSubtype(enclosingClassType, methodReturnType, state)
                && isSubtype(enclosingClassType, getReturnType(mit), state);
          }
        }
        return false;
      }

      @Override
      public Void visitLambdaExpression(LambdaExpressionTree node, Void unused) {
        // don't descend into lambdas
        return null;
      }

      @Override
      public Void visitNewClass(NewClassTree node, Void unused) {
        // don't descend into declarations of anonymous classes
        return null;
      }
    }

    var scanner = new ReturnValuesFromMethodAreIgnorable(state, getSymbol(tree));
    scanner.scan(tree, null);
    return scanner.atLeastOneReturn && scanner.allReturnsIgnorable;
  }
}
