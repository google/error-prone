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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static com.google.errorprone.util.ASTHelpers.isVoidType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Checker that recommends annotating a method with {@code @CanIgnoreReturnValue} if the method
 * always returns {@code this}.
 */
@BugPattern(
    summary = "Methods that always 'return this' should be annotated with @CanIgnoreReturnValue",
    severity = WARNING)
public final class CanIgnoreReturnValueSuggester extends BugChecker implements MethodTreeMatcher {
  private static final String CRV = "com.google.errorprone.annotations.CheckReturnValue";
  private static final String CIRV = "com.google.errorprone.annotations.CanIgnoreReturnValue";

  private static final Supplier<Type> PROTO_BUILDER =
      VisitorState.memoize(s -> s.getTypeFromString("com.google.protobuf.MessageLite.Builder"));

  // TODO(kak): catch places where an input parameter is always returned

  // TODO(b/202772719): catch cases where a method delegates to a CIRV method. E.g.:
  //
  //   public Builder addFoos(Foo... foos) {
  //     return addFoos(asList(foos));
  //   }
  //   @CanIgnoreReturnValue
  //   public Builder addFoos(Iterable<Foo> foos) {
  //     this.foos = checkNotNull(foos);
  //     return this;
  //   }
  //
  // This would also catch the `return self();` pattern and `return thisFoo();` patterns that we
  // sometimes see (b/202772578#comment6 and b/202772578#comment7).
  // All of this would work better if we had multi-pass, otherwise newly annotated @CIRV methods
  // won't "propagate" until the next analysis pass.

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    MethodSymbol methodSymbol = getSymbol(methodTree);
    // if the method is static or doesn't have a body, it can't possibly "return this", bail out
    if (methodSymbol.isStatic() || methodTree.getBody() == null) {
      return Description.NO_MATCH;
    }

    // if the method is a proto builder method, bail out
    if (isProtoBuilderSubtype(methodSymbol.owner.type, state)) {
      // TODO(kak): could we just use this instead?
      // ProtoRules.protoBuilders().evaluate(methodSymbol, state).isPresent()
      return Description.NO_MATCH;
    }

    if (methodSymbol.getSimpleName().contentEquals("self")
        && methodSymbol.getParameters().isEmpty()) {
      // would we want to actually suggest @CheckReturnValue on a self() method???
      return Description.NO_MATCH;
    }

    // if the method doesn't do anything but "return this", bail out; see b/236423646
    if (methodTree.getBody().getStatements().size() == 1) {
      StatementTree onlyStatement = methodTree.getBody().getStatements().get(0);
      if (onlyStatement instanceof ReturnTree) {
        if (returnsThis((ReturnTree) onlyStatement, state)) {
          return Description.NO_MATCH;
        }
      }
    }

    // if the method is already directly annotated w/ @CIRV, bail out
    if (hasAnnotation(methodTree, CIRV, state)) {
      return Description.NO_MATCH;
    }

    // if the enclosing type is already annotated with CIRV, we could theoretically _not_ directly
    // annotate the method but we're likely to discourage annotating types with CIRV: b/229776283

    // if the method is already directly annotated w/ @CRV, bail out
    if (hasAnnotation(methodTree, CRV, state)) {
      // TODO(kak): we might want to actually _remove_ @CRV and add @CIRV in this case!
      return Description.NO_MATCH;
    }

    // if the method is a constructor or has a void/Void return type, bail out
    Tree returnType = methodTree.getReturnType();
    if (returnType == null /* constructors have a null returnType */
        || isVoidType(getType(returnType), state)) {
      // TODO(b/234176673): isVoidType() also flags the `Void` (capital) type; is that desired?
      return Description.NO_MATCH;
    }

    AtomicBoolean allReturnThis = new AtomicBoolean(true);
    AtomicBoolean atLeastOneReturn = new AtomicBoolean(false);

    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitReturn(ReturnTree returnTree, Void unused) {
        atLeastOneReturn.set(true);
        if (!returnsThis(returnTree, state)) {
          allReturnThis.set(false);
          // once we've set allReturnThis to false, no need to descend further
          return null;
        }
        return super.visitReturn(returnTree, null);
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
    }.scan(state.getPath(), null);

    if (atLeastOneReturn.get() && allReturnThis.get()) {
      SuggestedFix.Builder fix = SuggestedFix.builder();
      String cirvName = qualifyType(state, fix, CIRV);

      // we could add a trailing comment (e.g., @CanIgnoreReturnValue // returns `this`), but all
      // developers will become familiar with these annotations sooner or later
      fix.prefixWith(methodTree, "@" + cirvName + "\n");

      return describeMatch(methodTree, fix.build());
    }
    return Description.NO_MATCH;
  }

  /** Returns whether or not the given {@link ReturnTree} returns exactly {@code this}. */
  private static boolean returnsThis(ReturnTree returnTree, VisitorState state) {
    ExpressionTree returnExpression = returnTree.getExpression();
    if (returnExpression instanceof IdentifierTree) {
      if (((IdentifierTree) returnExpression).getName().contentEquals("this")) {
        return true;
      }
    }
    if (returnExpression instanceof MethodInvocationTree) {
      MethodInvocationTree mit = (MethodInvocationTree) returnExpression;
      if (state.getSourceForNode(mit.getMethodSelect()).contentEquals("self")) {
        return true;
      }
    }
    return false;
  }

  private static boolean isProtoBuilderSubtype(Type ownerType, VisitorState state) {
    return isSubtype(ownerType, PROTO_BUILDER.get(state), state);
  }
}
