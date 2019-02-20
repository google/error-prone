/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MemberSelectTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnnotations;
import com.google.errorprone.dataflow.nullnesspropagation.TrustingNullnessAnalysis;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.lang.model.type.TypeKind;

/**
 * Warns when a dereference has a possibly-null receiver.
 *
 * <p>Nullability information is drawn from the trusting nullness analysis, which assumes that
 * fields and method returns are non-null unless otherwise annotated or inferred.
 *
 * @author bennostein@google.com (Benno Stein)
 */
@BugPattern(
    name = "NullableDereference",
    summary = "Dereference of possibly-null value",
    severity = WARNING,
    providesFix = ProvidesFix.NO_FIX)
public class NullableDereference extends BugChecker
    implements MemberSelectTreeMatcher, MethodInvocationTreeMatcher, NewClassTreeMatcher {

  @Override
  public Description matchMemberSelect(MemberSelectTree tree, VisitorState state) {
    JCExpression receiverTree = (JCExpression) tree.getExpression();

    if (receiverTree == null
        || receiverTree.type == null
        || receiverTree.type.getKind() == TypeKind.PACKAGE) {
      return Description.NO_MATCH;
    }

    // sym = null on static field imports. See https://github.com/google/error-prone/issues/1138.
    Symbol sym = getSymbol(tree);
    if ((tree instanceof JCFieldAccess) && (sym == null || sym.isStatic())) {
      return Description.NO_MATCH;
    }

    Description result =
        checkExpression(
            receiverTree,
            state,
            qual ->
                String.format(
                    "Dereferencing method/field \"%s\" of %s null receiver %s",
                    tree.getIdentifier(), qual, receiverTree));
    return result != null ? result : Description.NO_MATCH;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return checkCallArguments(tree.getArguments(), getSymbol(tree), state);
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    // 1. Check any enclosing expression like a dereference
    JCExpression receiverTree = (JCExpression) tree.getEnclosingExpression();
    if (receiverTree != null) {
      Description result =
          checkExpression(
              receiverTree,
              state,
              qual ->
                  String.format(
                      "Outer object %s for %s is %s null",
                      receiverTree, tree.getIdentifier(), qual));
      if (result != null) {
        return result;
      }
    }

    // 2. Check call arguments like a method call
    return checkCallArguments(tree.getArguments(), getSymbol(tree), state);
  }

  private Description checkCallArguments(
      List<? extends ExpressionTree> arguments, @Nullable MethodSymbol sym, VisitorState state) {
    if (sym == null) {
      return Description.NO_MATCH;
    }

    // TODO(b/121273225): Use iterators instead of indexing into these linked lists
    for (int i = 0; i < sym.getParameters().size(); ++i) {
      VarSymbol param = sym.getParameters().get(i);
      if (param.equals(sym.getParameters().last()) && sym.isVarArgs()) {
        break; // TODO(b/121273225): support varargs
      }
      // TODO(b/121273225): handle and check constrained type variables
      // TODO(b/121203670): Recognize @ParametersAreNonnullByDefault etc.
      // Ignore unannotated and @Nullable parameters
      if (NullnessAnnotations.fromAnnotationsOn(param).orElse(null) != Nullness.NONNULL) {
        continue;
      }
      ExpressionTree arg = arguments.get(i);
      Description result =
          checkExpression(
              arg,
              state,
              qual ->
                  String.format(
                      "argument %s is %s null but %s expects it to be non-null",
                      arg, qual, sym.getSimpleName()));
      if (result != null) {
        return result;
      }
    }
    return Description.NO_MATCH;
  }

  @Nullable
  private Description checkExpression(
      ExpressionTree tree, VisitorState state, Function<String, String> describer) {
    Nullness nullness =
        TrustingNullnessAnalysis.instance(state.context)
            .getNullness(new TreePath(state.getPath(), tree), state.context);
    if (nullness == null) {
      return null;
    }

    switch (nullness) {
      case NONNULL:
      case BOTTOM:
        return null;
      case NULL:
        return buildDescription(tree).setMessage(describer.apply("definitely")).build();
      case NULLABLE:
        return buildDescription(tree).setMessage(describer.apply("possibly")).build();
    }
    throw new AssertionError("Unhandled: " + nullness);
  }
}
