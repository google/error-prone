/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.refaster;

import static com.google.common.base.Preconditions.checkState;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;

import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.refaster.annotation.AlsoNegation;
import com.google.errorprone.refaster.annotation.UseImportPolicy;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCArrayAccess;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCAssignOp;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCConditional;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCInstanceOf;
import com.sun.tools.javac.tree.JCTree.JCTypeCast;
import com.sun.tools.javac.tree.JCTree.JCUnary;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Warner;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 * Implementation of a template to match and replace an expression anywhere in an AST.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
public abstract class ExpressionTemplate extends Template<ExpressionTemplateMatch>
    implements Unifiable<JCExpression> {
  private static final Logger logger = Logger.getLogger(ExpressionTemplate.class.toString());

  public static ExpressionTemplate create(UExpression expression, UType returnType) {
    return create(ImmutableMap.<String, UType>of(), expression, returnType);
  }

  public static ExpressionTemplate create(
      Map<String, ? extends UType> expressionArgumentTypes,
      UExpression expression,
      UType returnType) {
    return create(
        ImmutableClassToInstanceMap.<Annotation>builder().build(),
        ImmutableList.<UTypeVar>of(),
        expressionArgumentTypes,
        expression,
        returnType);
  }

  public static ExpressionTemplate create(
      ImmutableClassToInstanceMap<Annotation> annotations,
      Iterable<UTypeVar> typeVariables,
      Map<String, ? extends UType> expressionArgumentTypes,
      UExpression expression,
      UType returnType) {
    return new AutoValue_ExpressionTemplate(
        annotations,
        ImmutableList.copyOf(typeVariables),
        ImmutableMap.copyOf(expressionArgumentTypes),
        expression,
        returnType);
  }

  abstract UExpression expression();

  abstract UType returnType();

  public boolean generateNegation() {
    return annotations().containsKey(AlsoNegation.class);
  }

  public ExpressionTemplate negation() {
    checkState(
        returnType().equals(UPrimitiveType.BOOLEAN),
        "Return type must be boolean to generate negation, but was %s",
        returnType());
    return create(
        annotations(),
        templateTypeVariables(),
        expressionArgumentTypes(),
        expression().negate(),
        returnType());
  }

  /** Returns the matches of this template against the specified target AST. */
  @Override
  public Iterable<ExpressionTemplateMatch> match(JCTree target, Context context) {
    if (target instanceof JCExpression) {
      JCExpression targetExpr = (JCExpression) target;
      Optional<Unifier> unifier = unify(targetExpr, new Unifier(context)).first();
      if (unifier.isPresent()) {
        return ImmutableList.of(new ExpressionTemplateMatch(targetExpr, unifier.get()));
      }
    }
    return ImmutableList.of();
  }

  static boolean trueOrNull(@Nullable Boolean b) {
    return b == null || b;
  }

  /**
   * Placeholders' verification step only checks that they use variables that haven't *yet* been
   * matched to another local variable. This scanner reruns the verification step for the whole
   * tree, returning false if a violation was found, and true or null otherwise.
   */
  static final TreeScanner<Boolean, Unifier> PLACEHOLDER_VERIFIER =
      new TreeScanner<Boolean, Unifier>() {
        @Override
        public Boolean reduce(Boolean a, Boolean b) {
          return trueOrNull(a) && trueOrNull(b);
        }

        @Override
        public Boolean visitOther(Tree t, Unifier u) {
          if (t instanceof UPlaceholderExpression) {
            return ((UPlaceholderExpression) t).reverify(u);
          } else if (t instanceof UPlaceholderStatement) {
            return ((UPlaceholderStatement) t).reverify(u);
          } else {
            return super.visitOther(t, u);
          }
        }
      };

  @Override
  public Choice<Unifier> unify(final JCExpression target, Unifier unifier) {
    return expression()
        .unify(target, unifier)
        .condition(u -> trueOrNull(PLACEHOLDER_VERIFIER.scan(expression(), u)))
        .thenOption(
            new Function<Unifier, Optional<Unifier>>() {

              @Override
              public Optional<Unifier> apply(Unifier unifier) {
                Inliner inliner = unifier.createInliner();
                try {
                  List<Type> expectedTypes = expectedTypes(inliner);
                  List<Type> actualTypes = actualTypes(inliner);
                  /*
                   * TODO(cushon): the following is not true in javac8, which can apply target-typing to
                   * nested method invocations.
                   *
                   * The Java compiler's type inference doesn't directly take into account the expected
                   * return type, so we test the return type by treating the expected return type as an
                   * extra method argument, and the actual type of the return expression as its actual
                   * value.
                   */
                  if (target.type.getTag() != TypeTag.VOID) {
                    expectedTypes = expectedTypes.prepend(returnType().inline(inliner));
                    Type ty = target.type;
                    // Java 8 types conditional expressions by taking the *widest* possible type
                    // they could be allowed, instead of the narrowest, where Refaster really wants
                    // the narrowest type possible.  We reconstruct that by taking the lub of the
                    // types from each branch.
                    if (target.getKind() == Kind.CONDITIONAL_EXPRESSION) {
                      JCConditional cond = (JCConditional) target;
                      Type trueTy = cond.truepart.type;
                      Type falseTy = cond.falsepart.type;
                      if (trueTy.getTag() == TypeTag.BOT) {
                        ty = falseTy;
                      } else if (falseTy.getTag() == TypeTag.BOT) {
                        ty = trueTy;
                      } else {
                        ty = Types.instance(unifier.getContext()).lub(trueTy, falseTy);
                      }
                    }
                    actualTypes = actualTypes.prepend(ty);
                  }
                  return typecheck(
                      unifier, inliner, new Warner(target), expectedTypes, actualTypes);
                } catch (CouldNotResolveImportException e) {
                  logger.log(FINE, "Failure to resolve import", e);
                  return Optional.absent();
                }
              }
            });
  }

  /**
   * Generates a {@link SuggestedFix} replacing the specified match (usually of another template)
   * with this template.
   */
  @Override
  public Fix replace(ExpressionTemplateMatch match) {
    Inliner inliner = match.createInliner();
    Context context = inliner.getContext();
    if (annotations().containsKey(UseImportPolicy.class)) {
      ImportPolicy.bind(context, annotations().getInstance(UseImportPolicy.class).value());
    } else {
      ImportPolicy.bind(context, ImportPolicy.IMPORT_TOP_LEVEL);
    }
    int prec = getPrecedence(match.getLocation(), context);
    SuggestedFix.Builder fix = SuggestedFix.builder();
    try {
      StringWriter writer = new StringWriter();
      pretty(inliner.getContext(), writer).printExpr(expression().inline(inliner), prec);
      fix.replace(match.getLocation(), writer.toString());
    } catch (CouldNotResolveImportException e) {
      logger.log(SEVERE, "Failure to resolve in replacement", e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return addImports(inliner, fix);
  }

  /**
   * Returns the precedence level appropriate for unambiguously printing leaf as a subexpression of
   * its parent.
   */
  private static int getPrecedence(JCTree leaf, Context context) {
    JCCompilationUnit comp = context.get(JCCompilationUnit.class);
    JCTree parent = TreeInfo.pathFor(leaf, comp).get(1);

    // In general, this should match the logic in com.sun.tools.javac.tree.Pretty.
    //
    // TODO(mdempsky): There are probably cases where we could omit parentheses
    // by tweaking the returned precedence, but they need careful review.
    // For example, consider a template to replace "add(a, b)" with "a + b",
    // which applied to "x + add(y, z)" would result in "x + (y + z)".
    // In most cases, we'd likely prefer "x + y + z" instead, but those aren't
    // always equivalent: "0L + (Integer.MIN_VALUE + Integer.MIN_VALUE)" yields
    // a different value than "0L + Integer.MIN_VALUE + Integer.MIN_VALUE" due
    // to integer promotion rules.

    if (parent instanceof JCConditional) {
      // This intentionally differs from Pretty, because Pretty appears buggy:
      // http://mail.openjdk.java.net/pipermail/compiler-dev/2013-September/007303.html
      JCConditional conditional = (JCConditional) parent;
      return TreeInfo.condPrec + ((conditional.cond == leaf) ? 1 : 0);
    } else if (parent instanceof JCAssign) {
      JCAssign assign = (JCAssign) parent;
      return TreeInfo.assignPrec + ((assign.lhs == leaf) ? 1 : 0);
    } else if (parent instanceof JCAssignOp) {
      JCAssignOp assignOp = (JCAssignOp) parent;
      return TreeInfo.assignopPrec + ((assignOp.lhs == leaf) ? 1 : 0);
    } else if (parent instanceof JCUnary) {
      return TreeInfo.opPrec(parent.getTag());
    } else if (parent instanceof JCBinary) {
      JCBinary binary = (JCBinary) parent;
      return TreeInfo.opPrec(parent.getTag()) + ((binary.rhs == leaf) ? 1 : 0);
    } else if (parent instanceof JCTypeCast) {
      JCTypeCast typeCast = (JCTypeCast) parent;
      return (typeCast.expr == leaf) ? TreeInfo.prefixPrec : TreeInfo.noPrec;
    } else if (parent instanceof JCInstanceOf) {
      JCInstanceOf instanceOf = (JCInstanceOf) parent;
      return TreeInfo.ordPrec + ((instanceOf.clazz == leaf) ? 1 : 0);
    } else if (parent instanceof JCArrayAccess) {
      JCArrayAccess arrayAccess = (JCArrayAccess) parent;
      return (arrayAccess.indexed == leaf) ? TreeInfo.postfixPrec : TreeInfo.noPrec;
    } else if (parent instanceof JCFieldAccess) {
      JCFieldAccess fieldAccess = (JCFieldAccess) parent;
      return (fieldAccess.selected == leaf) ? TreeInfo.postfixPrec : TreeInfo.noPrec;
    } else {
      return TreeInfo.noPrec;
    }
  }
}
