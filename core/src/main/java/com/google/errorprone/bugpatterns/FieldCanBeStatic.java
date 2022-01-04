/*
 * Copyright 2020 The Error Prone Authors.
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

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.fixes.SuggestedFixes.addModifiers;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.annotationsAmong;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static javax.lang.model.element.ElementKind.FIELD;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.STATIC;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions.ConstantExpression;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions.ConstantExpressionVisitor;
import com.google.errorprone.bugpatterns.threadsafety.ThreadSafety;
import com.google.errorprone.bugpatterns.threadsafety.WellKnownMutability;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Name;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.lang.model.element.NestingKind;

/** Finds fields which can be safely made static. */
@BugPattern(
    name = "FieldCanBeStatic",
    summary =
        "A final field initialized at compile-time with an instance of an immutable type can be"
            + " static.",
    severity = SUGGESTION)
public final class FieldCanBeStatic extends BugChecker implements VariableTreeMatcher {

  private static final Supplier<ImmutableSet<Name>> EXEMPTING_VARIABLE_ANNOTATIONS =
      VisitorState.memoize(
          s ->
              Stream.of("com.google.inject.testing.fieldbinder.Bind")
                  .map(s::getName)
                  .collect(toImmutableSet()));

  private final WellKnownMutability wellKnownMutability;
  private final ConstantExpressions constantExpressions;

  public FieldCanBeStatic(ErrorProneFlags flags) {
    this.wellKnownMutability = WellKnownMutability.fromFlags(flags);
    this.constantExpressions = ConstantExpressions.fromFlags(flags);
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    VarSymbol symbol = getSymbol(tree);
    if (symbol == null
        || !symbol.isPrivate()
        || !tree.getModifiers().getFlags().contains(FINAL)
        || symbol.isStatic()
        || !symbol.getKind().equals(FIELD)
        || (symbol.flags() & RECORD_FLAG) == RECORD_FLAG) {
      return NO_MATCH;
    }
    ClassSymbol enclClass = symbol.owner.enclClass();
    if (enclClass == null) {
      return NO_MATCH;
    }
    if (!enclClass.getNestingKind().equals(NestingKind.TOP_LEVEL)
        && !enclClass.isStatic()
        && symbol.getConstantValue() == null) {
      // JLS 8.1.3: inner classes cannot declare static members, unless the member is a constant
      // variable
      return NO_MATCH;
    }
    if (!isTypeKnownImmutable(getType(tree), state)) {
      return NO_MATCH;
    }
    if (!isPure(tree.getInitializer(), state)) {
      return NO_MATCH;
    }
    if (!annotationsAmong(symbol, EXEMPTING_VARIABLE_ANNOTATIONS.get(state), state).isEmpty()) {
      return NO_MATCH;
    }
    SuggestedFix fix =
        SuggestedFix.builder()
            .merge(renameVariable(tree, state))
            .merge(addModifiers(tree, state, STATIC).orElse(SuggestedFix.emptyFix()))
            .build();
    return describeMatch(tree, fix);
  }

  private static final long RECORD_FLAG = 1L << 61;

  /**
   * Renames the variable, clobbering any qualifying (like {@code this.}). This is a tad unsafe, but
   * we need to somehow remove any qualification with an instance.
   */
  private SuggestedFix renameVariable(VariableTree variableTree, VisitorState state) {
    String name = variableTree.getName().toString();
    if (!LOWER_CAMEL_PATTERN.matcher(name).matches()) {
      return SuggestedFix.emptyFix();
    }
    String replacement = LOWER_CAMEL.to(UPPER_UNDERSCORE, variableTree.getName().toString());
    int typeEndPos = state.getEndPosition(variableTree.getType());
    int searchOffset = typeEndPos - ((JCTree) variableTree).getStartPosition();
    int pos =
        ((JCTree) variableTree).getStartPosition()
            + state.getSourceForNode(variableTree).indexOf(name, searchOffset);
    SuggestedFix.Builder fix =
        SuggestedFix.builder().replace(pos, pos + name.length(), replacement);
    VarSymbol sym = getSymbol(variableTree);
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitIdentifier(IdentifierTree tree, Void unused) {
        handle(tree);
        return super.visitIdentifier(tree, null);
      }

      @Override
      public Void visitMemberSelect(MemberSelectTree tree, Void unused) {
        handle(tree);
        return super.visitMemberSelect(tree, null);
      }

      private void handle(Tree tree) {
        if (sym.equals(getSymbol(tree))) {
          fix.replace(tree, replacement);
        }
      }
    }.scan(state.getPath().getCompilationUnit(), null);
    return fix.build();
  }

  private static final Pattern LOWER_CAMEL_PATTERN = Pattern.compile("[a-z][a-zA-Z0-9]+");

  /**
   * Tries to establish whether an expression is pure. For example, literals and invocations of
   * known-pure functions are pure.
   */
  private boolean isPure(ExpressionTree initializer, VisitorState state) {
    return constantExpressions
        .constantExpression(initializer, state)
        .map(FieldCanBeStatic::isStatic)
        .orElse(false);
  }

  private static boolean isStatic(ConstantExpression expression) {
    AtomicBoolean staticable = new AtomicBoolean(true);
    expression.accept(
        new ConstantExpressionVisitor() {
          @Override
          public void visitIdentifier(Symbol identifier) {
            if (!(identifier instanceof ClassSymbol) && !identifier.isStatic()) {
              staticable.set(false);
            }
          }
        });
    return staticable.get();
  }

  private boolean isTypeKnownImmutable(Type type, VisitorState state) {
    ThreadSafety threadSafety =
        ThreadSafety.builder()
            .setPurpose(ThreadSafety.Purpose.FOR_IMMUTABLE_CHECKER)
            .knownTypes(wellKnownMutability)
            .acceptedAnnotations(
                ImmutableSet.of(Immutable.class.getName(), AutoValue.class.getName()))
            .markerAnnotations(ImmutableSet.of())
            .build(state);
    return !threadSafety
        .isThreadSafeType(
            /* allowContainerTypeParameters= */ true,
            threadSafety.threadSafeTypeParametersInScope(type.tsym),
            type)
        .isPresent();
  }
}
