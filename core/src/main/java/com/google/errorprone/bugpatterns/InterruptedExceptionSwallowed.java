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

package com.google.errorprone.bugpatterns;

import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.TryTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.UnionClassType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * Checks for cases where an {@link InterruptedException} is caught as part of a catch block
 * catching a supertype, and not specially handled.
 */
@BugPattern(
    name = "InterruptedExceptionSwallowed",
    summary =
        "This catch block appears to be catching an explicitly declared InterruptedException as an"
            + " Exception/Throwable and not handling the interruption separately.",
    severity = WARNING,
    providesFix = REQUIRES_HUMAN_ATTENTION,
    documentSuppression = false)
public final class InterruptedExceptionSwallowed extends BugChecker
    implements MethodTreeMatcher, TryTreeMatcher {
  private static final String AUTOCLOSEABLE = "java.lang.AutoCloseable";

  private static final String METHOD_DESCRIPTION =
      "This method can throw InterruptedException but declares that it throws Exception/Throwable."
          + " This makes it difficult for callers to recognize the need to handle interruption"
          + " properly.";

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (state.errorProneOptions().isTestOnlyTarget()) {
      return NO_MATCH;
    }
    Type interrupted = state.getSymtab().interruptedExceptionType;
    if (tree.getThrows().stream().anyMatch(t -> isSubtype(getType(t), interrupted, state))) {
      return NO_MATCH;
    }
    ImmutableSet<Type> thrownExceptions = getThrownExceptions(tree.getBody(), state);
    // Bail out if none of the exceptions thrown are subtypes of InterruptedException.
    if (thrownExceptions.stream().noneMatch(t -> isSubtype(t, interrupted, state))) {
      return NO_MATCH;
    }
    // Bail if any of the thrown exceptions are masking InterruptedException: that is, we don't want
    // to suggest updating with `throws Exception, InterruptedException`.
    if (thrownExceptions.stream()
        .anyMatch(t -> !isSameType(t, interrupted, state) && isSubtype(interrupted, t, state))) {
      return NO_MATCH;
    }
    Set<Type> exceptions =
        Stream.concat(
                thrownExceptions.stream()
                    .filter(t -> !isSubtype(t, state.getSymtab().runtimeExceptionType, state)),
                tree.getThrows().stream()
                    .filter(t -> !isSubtype(interrupted, getType(t), state))
                    .map(ASTHelpers::getType))
            .collect(toCollection(HashSet::new));
    for (Type type : ImmutableSet.copyOf(exceptions)) {
      exceptions.removeIf(t -> !isSameType(t, type, state) && isSubtype(t, type, state));
    }

    // Don't suggest adding more than five exceptions to the method signature.
    if (exceptions.size() > 5) {
      return NO_MATCH;
    }

    SuggestedFix fix = narrowExceptionTypes(tree, exceptions, state);
    return buildDescription(tree).setMessage(METHOD_DESCRIPTION).addFix(fix).build();
  }

  private static SuggestedFix narrowExceptionTypes(
      MethodTree tree, Set<Type> exceptions, VisitorState state) {
    SuggestedFix.Builder fix = SuggestedFix.builder();
    fix.replace(
        ((JCTree) tree.getThrows().get(0)).getStartPosition(),
        state.getEndPosition(getLast(tree.getThrows())),
        exceptions.stream()
            .map(t -> SuggestedFixes.qualifyType(state, fix, t))
            .sorted()
            .collect(joining(", ")));
    return fix.build();
  }

  @Override
  public Description matchTry(TryTree tree, VisitorState state) {
    for (CatchTree catchTree : tree.getCatches()) {
      Type type = getType(catchTree.getParameter());
      Type interrupted = state.getSymtab().interruptedExceptionType;
      ImmutableList<Type> caughtTypes = extractTypes(type);
      if (caughtTypes.stream().anyMatch(t -> isSubtype(t, interrupted, state))) {
        return NO_MATCH;
      }
      if (caughtTypes.stream().anyMatch(t -> isSubtype(interrupted, t, state))) {
        ImmutableSet<Type> thrownExceptions = getThrownExceptions(tree.getBlock(), state);
        if (thrownExceptions.stream().anyMatch(t -> isSubtype(t, interrupted, state))
            && !blockChecksForInterruptedException(catchTree.getBlock(), state)
            && !isSuppressed(catchTree.getParameter())) {
          return describeMatch(catchTree, createFix(catchTree));
        }
      }
    }
    return NO_MATCH;
  }

  private static SuggestedFix createFix(CatchTree catchTree) {
    List<? extends StatementTree> block = catchTree.getBlock().getStatements();
    String fix =
        String.format(
            "if (%s instanceof InterruptedException) {\nThread.currentThread().interrupt();\n}\n",
            catchTree.getParameter().getName());
    if (block.isEmpty()) {
      return SuggestedFix.replace(catchTree.getBlock(), String.format("{%s}", fix));
    }
    return SuggestedFix.prefixWith(block.get(0), fix);
  }

  private static boolean blockChecksForInterruptedException(BlockTree block, VisitorState state) {
    return TRUE.equals(
        new TreeScanner<Boolean, Void>() {
          @Override
          public Boolean reduce(Boolean a, Boolean b) {
            return TRUE.equals(a) || TRUE.equals(b);
          }

          @Override
          public Boolean visitInstanceOf(InstanceOfTree instanceOfTree, Void aVoid) {
            return isSubtype(
                getType(instanceOfTree.getType()),
                state.getSymtab().interruptedExceptionType,
                state);
          }
        }.scan(block, null));
  }

  private static ImmutableSet<Type> getThrownExceptions(Tree tree, VisitorState state) {
    ScanThrownTypes scanner = new ScanThrownTypes(state);
    scanner.scan(tree, null);
    return ImmutableSet.copyOf(scanner.getThrownTypes());
  }

  static final class ScanThrownTypes extends TreeScanner<Void, Void> {
    boolean inResources = false;
    ArrayDeque<Set<Type>> thrownTypes = new ArrayDeque<>();

    private final VisitorState state;
    private final Types types;

    ScanThrownTypes(VisitorState state) {
      this.state = state;
      this.types = state.getTypes();
      thrownTypes.push(new HashSet<>());
    }

    private Set<Type> getThrownTypes() {
      return thrownTypes.peek();
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree invocation, Void unused) {
      MethodSymbol symbol = getSymbol(invocation);
      if (symbol != null) {
        getThrownTypes().addAll(symbol.getThrownTypes());
      }
      return super.visitMethodInvocation(invocation, null);
    }

    @Override
    public Void visitTry(TryTree tree, Void unused) {
      thrownTypes.push(new HashSet<>());
      inResources = true;
      scan(tree.getResources(), null);
      inResources = false;
      scan(tree.getBlock(), null);
      scan(tree.getCatches(), null);
      scan(tree.getFinallyBlock(), null);
      Set<Type> fromBlock = thrownTypes.pop();
      getThrownTypes().addAll(fromBlock);
      // The above getCatches scan isn't quite accurate when exceptions are thrown from earlier catch blocks.
      // It's necessary to prune exceptions thrown by resources and the main block, but doesn't adequately
      // capture those thrown from catch blocks.
      for (CatchTree catchTree : tree.getCatches()) {
        ScanThrownTypes nestedScanner = new ScanThrownTypes(state);
        catchTree.getBlock().accept(nestedScanner, null);
        getThrownTypes().addAll(nestedScanner.getThrownTypes());
      }
      return null;
    }

    @Override
    public Void visitCatch(CatchTree tree, Void unused) {
      Type type = getType(tree.getParameter());
      // Remove the thrown types; if they're re-thrown they'll get re-added in visitTry
      for (Type unionMember : extractTypes(type)) {
        getThrownTypes().removeIf(thrownType -> types.isAssignable(thrownType, unionMember));
      }
      return null;
    }

    @Override
    public Void visitThrow(ThrowTree tree, Void unused) {
      getThrownTypes().addAll(extractTypes(getType(tree.getExpression())));
      return super.visitThrow(tree, null);
    }

    @Override
    public Void visitNewClass(NewClassTree tree, Void unused) {
      MethodSymbol symbol = getSymbol(tree);
      if (symbol != null) {
        getThrownTypes().addAll(symbol.getThrownTypes());
      }
      return super.visitNewClass(tree, null);
    }

    @Override
    public Void visitVariable(VariableTree tree, Void unused) {
      if (inResources) {
        Symbol symbol = getSymbol(tree.getType());
        if (symbol instanceof ClassSymbol) {
          ClassSymbol classSymbol = (ClassSymbol) symbol;
          Optional<MethodSymbol> methodSymbol = getCloseMethod(classSymbol, state);
          methodSymbol.map(MethodSymbol::getThrownTypes).ifPresent(getThrownTypes()::addAll);
        }
      }
      return super.visitVariable(tree, null);
    }

    // We don't need to account for anything thrown by declarations,
    @Override
    public Void visitLambdaExpression(LambdaExpressionTree tree, Void unused) {
      return null;
    }

    @Override
    public Void visitClass(ClassTree tree, Void unused) {
      return null;
    }

    @Override
    public Void visitMethod(MethodTree tree, Void unused) {
      return null;
    }
  }

  private static Optional<MethodSymbol> getCloseMethod(ClassSymbol symbol, VisitorState state) {
    Types types = state.getTypes();
    return symbol.getEnclosedElements().stream()
        .filter(
            sym ->
                types.isAssignable(symbol.type, state.getTypeFromString(AUTOCLOSEABLE))
                    && sym.getSimpleName().contentEquals("close")
                    && sym.getTypeParameters().isEmpty())
        .map(e -> (MethodSymbol) e)
        .findFirst();
  }

  private static ImmutableList<Type> extractTypes(@Nullable Type type) {
    if (type == null) {
      return ImmutableList.of();
    }
    if (type.isUnion()) {
      UnionClassType unionType = (UnionClassType) type;
      return ImmutableList.copyOf(unionType.getAlternativeTypes());
    }
    return ImmutableList.of(type);
  }
}
