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
import static com.google.common.collect.Iterables.getOnlyElement;
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
import static com.google.errorprone.util.ASTHelpers.shouldKeep;
import static com.google.errorprone.util.ASTHelpers.streamSuperMethods;
import static com.google.errorprone.util.ASTHelpers.stripParentheses;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
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
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;

/**
 * Checker that recommends annotating a method with {@code @CanIgnoreReturnValue} if the method
 * returns {@code this}, returns an effectively final input param, or if it looks like a builder
 * method (that is likely to return {@code this}).
 */
@BugPattern(
    summary =
        "Methods that always return 'this' (or return an input parameter) should be annotated with"
            + " @com.google.errorprone.annotations.CanIgnoreReturnValue",
    severity = WARNING)
public final class CanIgnoreReturnValueSuggester extends BugChecker implements MethodTreeMatcher {

  private static final String AUTO_VALUE = "com.google.auto.value.AutoValue";
  private static final String IMMUTABLE = "com.google.errorprone.annotations.Immutable";
  private static final String CIRV = "com.google.errorprone.annotations.CanIgnoreReturnValue";
  private static final ImmutableSet<String> EXEMPTING_METHOD_ANNOTATIONS =
      ImmutableSet.of(
          CIRV,
          "com.google.errorprone.annotations.CheckReturnValue",
          "com.google.errorprone.refaster.annotation.AfterTemplate");

  private static final ImmutableSet<String> EXEMPTING_CLASS_ANNOTATIONS =
      ImmutableSet.of(
          "com.google.auto.value.AutoValue.Builder",
          "com.google.auto.value.AutoBuilder",
          "dagger.Component.Builder",
          "dagger.Subcomponent.Builder");

  private static final Supplier<Type> PROTO_BUILDER =
      VisitorState.memoize(s -> s.getTypeFromString("com.google.protobuf.MessageLite.Builder"));

  private static final ImmutableSet<String> BANNED_METHOD_PREFIXES =
      ImmutableSet.of("get", "is", "has", "new", "clone", "copy");

  private final ImmutableSet<String> exemptingMethodAnnotations;

  @Inject
  CanIgnoreReturnValueSuggester(ErrorProneFlags errorProneFlags) {
    this.exemptingMethodAnnotations =
        Sets.union(
                EXEMPTING_METHOD_ANNOTATIONS,
                errorProneFlags.getSetOrEmpty("CanIgnoreReturnValue:ExemptingMethodAnnotations"))
            .immutableCopy();
  }

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    MethodSymbol methodSymbol = getSymbol(methodTree);
    // Don't fire on overrides of methods within anonymous classes.
    if (streamSuperMethods(methodSymbol, state.getTypes()).findFirst().isPresent()
        && methodSymbol.owner.isAnonymous()) {
      return Description.NO_MATCH;
    }

    // If the method has an exempting annotation, then bail out.
    if (exemptingMethodAnnotations.stream()
        .anyMatch(annotation -> hasAnnotation(methodSymbol, annotation, state))) {
      return Description.NO_MATCH;
    }

    // if the method is annotated with an annotation that is @Keep, bail out
    if (shouldKeep(methodTree)) {
      return Description.NO_MATCH;
    }

    // if the method looks like an accessor, bail out
    String methodName = methodSymbol.getSimpleName().toString();
    // TODO(kak): we also may want to check if methodSymbol.getParameters().isEmpty()
    if (BANNED_METHOD_PREFIXES.stream().anyMatch(methodName::startsWith)) {
      return Description.NO_MATCH;
    }

    // if the method always return a single input param (of the same type), make it CIRV
    if (methodAlwaysReturnsInputParam(methodTree, state)) {
      return annotateWithCanIgnoreReturnValue(methodTree, state);
    }

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

    // skip builder setter methods defined on "known safe" types
    if (isKnownSafeBuilderInterface(methodSymbol, state)) {
      return Description.NO_MATCH;
    }

    // if the method looks like a builder, or if it always returns `this`, then make it @CIRV
    if (classLooksLikeBuilder(methodSymbol.owner, state)
        || methodReturnsIgnorableValues(methodTree, state)) {
      return annotateWithCanIgnoreReturnValue(methodTree, state);
    }

    return Description.NO_MATCH;
  }

  private Description annotateWithCanIgnoreReturnValue(MethodTree methodTree, VisitorState state) {
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

  private static boolean isKnownSafeBuilderInterface(
      MethodSymbol methodSymbol, VisitorState state) {
    // TODO(kak): use ResultEvaluator instead of duplicating logic
    if (isAbstract(methodSymbol)) {
      for (String annotation : EXEMPTING_CLASS_ANNOTATIONS) {
        if (hasAnnotation(methodSymbol.owner, annotation, state)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean classLooksLikeBuilder(Symbol owner, VisitorState state) {
    boolean classIsImmutable =
        hasAnnotation(owner, IMMUTABLE, state) || hasAnnotation(owner, AUTO_VALUE, state);
    return owner.getSimpleName().toString().endsWith("Builder") && !classIsImmutable;
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

  private static boolean methodAlwaysReturnsInputParam(MethodTree methodTree, VisitorState state) {
    // short-circuit if the method has no parameters
    if (methodTree.getParameters().isEmpty()) {
      return false;
    }
    class AllReturnsAreInputParams extends TreeScanner<Void, Void> {
      private final Set<Symbol> returnedSymbols = new HashSet<>();

      @Override
      public Void visitReturn(ReturnTree returnTree, Void unused) {
        // even for cases where getExpression() or getSymbol() returns null, we still want to add
        // those to the returnedSymbols set (that's important if there's > 1 returns)
        returnedSymbols.add(getSymbol(stripParentheses(returnTree.getExpression())));
        return null;
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

    AllReturnsAreInputParams scanner = new AllReturnsAreInputParams();
    scanner.scan(methodTree, null);
    // if we have more than 1 returned symbol, then the value isn't ignorable
    if (scanner.returnedSymbols.size() != 1) {
      return false;
    }
    Symbol returnedSymbol = getOnlyElement(scanner.returnedSymbols);
    if (returnedSymbol == null) {
      return false;
    }
    MethodSymbol methodSymbol = getSymbol(methodTree);
    return isSameType(returnedSymbol.type, methodSymbol.getReturnType(), state)
        && methodSymbol.getParameters().stream()
            .filter(ASTHelpers::isConsideredFinal)
            .anyMatch(returnedSymbol::equals);
  }
}
