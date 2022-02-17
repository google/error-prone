/*
 * Copyright 2014 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.threadsafety;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.VisitorState;
import com.google.errorprone.util.MoreAnnotations;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public final class GuardedByUtils {
  public static ImmutableSet<String> getGuardValues(Symbol sym) {
    return getAnnotationValueAsStrings(sym);
  }

  static ImmutableSet<String> getGuardValues(Tree tree, VisitorState state) {
    Symbol sym = getSymbol(tree);
    if (sym == null) {
      return ImmutableSet.of();
    }
    return getAnnotationValueAsStrings(sym);
  }

  private static ImmutableSet<String> getAnnotationValueAsStrings(Symbol sym) {
    List<Attribute.Compound> rawAttributes = sym.getRawAttributes();
    if (rawAttributes.isEmpty()) {
      return ImmutableSet.of();
    }
    return rawAttributes.stream()
        .filter(a -> a.getAnnotationType().asElement().getSimpleName().contentEquals("GuardedBy"))
        .flatMap(
            a ->
                MoreAnnotations.getValue(a, "value")
                    .map(MoreAnnotations::asStrings)
                    .orElse(Stream.empty()))
        .collect(toImmutableSet());
  }

  static JCTree.JCExpression parseString(String guardedByString, Context context) {
    JavacParser parser =
        ParserFactory.instance(context)
            .newParser(
                guardedByString,
                /* keepDocComments= */ false,
                /* keepEndPos= */ true,
                /* keepLineMap= */ false);
    JCTree.JCExpression exp;
    try {
      exp = parser.parseExpression();
    } catch (Throwable e) {
      throw new IllegalGuardedBy(e.getMessage());
    }
    int len = (parser.getEndPos(exp) - exp.getStartPosition());
    if (len != guardedByString.length()) {
      throw new IllegalGuardedBy("Didn't parse entire string.");
    }
    return exp;
  }

  @AutoValue
  abstract static class GuardedByValidationResult {
    abstract String message();

    abstract Boolean isValid();

    static GuardedByValidationResult invalid(String message) {
      return new AutoValue_GuardedByUtils_GuardedByValidationResult(message, false);
    }

    static GuardedByValidationResult ok() {
      return new AutoValue_GuardedByUtils_GuardedByValidationResult("", true);
    }
  }

  public static GuardedByValidationResult isGuardedByValid(
      Tree tree, VisitorState state, GuardedByFlags flags) {
    ImmutableSet<String> guards = GuardedByUtils.getGuardValues(tree, state);
    if (guards.isEmpty()) {
      return GuardedByValidationResult.ok();
    }

    List<GuardedByExpression> boundGuards = new ArrayList<>();
    for (String guard : guards) {
      Optional<GuardedByExpression> boundGuard =
          GuardedByBinder.bindString(guard, GuardedBySymbolResolver.from(tree, state), flags);
      if (!boundGuard.isPresent()) {
        return GuardedByValidationResult.invalid("could not resolve guard");
      }
      boundGuards.add(boundGuard.get());
    }

    Symbol treeSym = getSymbol(tree);
    if (treeSym == null) {
      // this shouldn't happen unless the compilation had already failed.
      return GuardedByValidationResult.ok();
    }

    for (GuardedByExpression boundGuard : boundGuards) {
      boolean staticGuard =
          boundGuard.kind() == GuardedByExpression.Kind.CLASS_LITERAL
              || (boundGuard.sym() != null && boundGuard.sym().isStatic());
      if (treeSym.isStatic() && !staticGuard) {
        return GuardedByValidationResult.invalid("static member guarded by instance");
      }
    }

    return GuardedByValidationResult.ok();
  }

  public static Symbol bindGuardedByString(
      Tree tree, String guard, VisitorState visitorState, GuardedByFlags flags) {
    Optional<GuardedByExpression> bound =
        GuardedByBinder.bindString(guard, GuardedBySymbolResolver.from(tree, visitorState), flags);
    if (!bound.isPresent()) {
      return null;
    }
    return bound.get().sym();
  }

  private GuardedByUtils() {}
}
