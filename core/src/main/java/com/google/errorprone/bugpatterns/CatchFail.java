/*
 * Copyright 2017 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Streams.stream;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.expressionStatement;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.TryTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.JUnitMatchers;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.TryTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Attribute.Compound;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.UnionClassType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.util.Name;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Ignoring exceptions and calling fail() is unnecessary, and makes test output less useful",
    severity = WARNING)
public class CatchFail extends BugChecker implements TryTreeMatcher {

  private static final Matcher<StatementTree> FAIL_METHOD =
      expressionStatement(
          anyOf(
              staticMethod().onClass("org.junit.Assert").named("fail"),
              staticMethod().onClass("junit.framework.Assert").named("fail"),
              staticMethod().onClass("junit.framework.TestCase").named("fail")));

  @Override
  public Description matchTry(TryTree tree, VisitorState state) {
    if (tree.getCatches().isEmpty()) {
      return NO_MATCH;
    }
    // Find catch blocks that contain only a call to fail, and that ignore the caught exception.
    ImmutableList<CatchTree> catchBlocks =
        tree.getCatches().stream()
            .filter(
                c ->
                    c.getBlock().getStatements().size() == 1
                        && FAIL_METHOD.matches(getOnlyElement(c.getBlock().getStatements()), state))
            .filter(c -> !catchVariableIsUsed(c))
            .collect(toImmutableList());
    if (catchBlocks.isEmpty()) {
      return NO_MATCH;
    }
    Description.Builder description = buildDescription(tree);
    rethrowFix(catchBlocks, state).ifPresent(description::addFix);
    deleteFix(tree, catchBlocks, state).ifPresent(description::addFix);
    return description.build();
  }

  private static String getMessageOrFormat(MethodInvocationTree tree, VisitorState state) {
    if (tree.getArguments().size() > 1) {
      return "String.format("
          + state
              .getSourceCode()
              .subSequence(
                  getStartPosition(tree.getArguments().get(0)),
                  state.getEndPosition(getLast(tree.getArguments())))
          + ")";
    }
    return state.getSourceForNode(getOnlyElement(tree.getArguments()));
  }

  private static Optional<Fix> rethrowFix(
      ImmutableList<CatchTree> catchBlocks, VisitorState state) {
    SuggestedFix.Builder fix = SuggestedFix.builder();
    catchBlocks.forEach(
        c -> {
          // e.g.
          // fail("message") -> throw new AssertionError("message", cause);
          // assertWithMessage("message format %s", 42) ->
          //     throw new AssertionError(String.format("message format %s", 42), cause);
          StatementTree statementTree = getOnlyElement(c.getBlock().getStatements());
          MethodInvocationTree methodInvocationTree =
              (MethodInvocationTree) ((ExpressionStatementTree) statementTree).getExpression();
          if (!methodInvocationTree.getArguments().isEmpty()) {
            String message = getMessageOrFormat(methodInvocationTree, state);
            // only catch and rethrow to add additional context, not for raw `fail()` calls
            fix.replace(
                statementTree,
                String.format(
                    "throw new AssertionError(%s, %s);", message, c.getParameter().getName()));
          }
        });
    return fix.isEmpty() ? Optional.empty() : Optional.of(fix.build());
  }

  // Extract the argument to a call to assertWithMessage, e.g. in:
  // assertWithMessage("message").fail();

  Optional<Fix> deleteFix(TryTree tree, ImmutableList<CatchTree> catchBlocks, VisitorState state) {
    SuggestedFix.Builder fix = SuggestedFix.builder();
    if (tree.getFinallyBlock() != null || catchBlocks.size() < tree.getCatches().size()) {
      // If the try statement has a finally region, or other catch blocks, delete only the
      // unnecessary blocks.
      catchBlocks.forEach(fix::delete);
    } else {
      // The try statement has no finally region and all catch blocks are unnecessary. Replace it
      // with the try statements, deleting all catches.
      List<? extends StatementTree> tryStatements = tree.getBlock().getStatements();

      // If the try block is empty, all of the catches are dead, so just delete the whole try and
      // don't modify the signature of the method
      if (tryStatements.isEmpty()) {
        return Optional.of(fix.delete(tree).build());
      } else {
        String source = state.getSourceCode().toString();
        // Replace the full region to work around a GJF partial formatting bug that prevents it from
        // re-indenting unchanged lines. This means that fixes may overlap, but that's (hopefully)
        // unlikely.
        // TODO(b/24140798): emit more precise replacements if GJF is fixed
        fix.replace(
            tree,
            source.substring(
                getStartPosition(tryStatements.get(0)),
                state.getEndPosition(getLast(tryStatements))));
      }
    }
    MethodTree enclosing = ASTHelpers.findEnclosingMethod(state);
    if (enclosing == null) {
      // There isn't an enclosing method, possibly because we're in a lambda or initializer block.
      return Optional.empty();
    }
    if (isExpectedExceptionTest(ASTHelpers.getSymbol(enclosing), state)) {
      // Replacing the original exception with fail() may break badly-structured expected-exception
      // tests, so don't use that fix for methods annotated with @Test(expected=...).
      return Optional.empty();
    }

    // Fix up the enclosing method's throws declaration to include the new thrown exception types.
    List<Type> thrownTypes = ASTHelpers.getSymbol(enclosing).getThrownTypes();
    Types types = state.getTypes();
    // Find all types in the deleted catch blocks that are not already in the throws declaration.
    ImmutableList<Type> toThrow =
        catchBlocks.stream()
            .map(c -> ASTHelpers.getType(c.getParameter()))
            // convert multi-catch to a list of component types
            .flatMap(
                t ->
                    t instanceof UnionClassType unionClassType
                        ? stream(unionClassType.getAlternativeTypes())
                        : Stream.of(t))
            .filter(t -> thrownTypes.stream().noneMatch(x -> types.isAssignable(t, x)))
            .collect(toImmutableList());
    if (!toThrow.isEmpty()) {
      if (!JUnitMatchers.TEST_CASE.matches(enclosing, state)) {
        // Don't add throws declarations to methods that don't look like test cases, since it may
        // not be a safe local refactoring.
        return Optional.empty();
      }
      String throwsString =
          toThrow.stream()
              .map(t -> SuggestedFixes.qualifyType(state, fix, t))
              .distinct()
              .collect(joining(", "));
      if (enclosing.getThrows().isEmpty()) {
        // Add a new throws declaration.
        fix.prefixWith(enclosing.getBody(), "throws " + throwsString);
      } else {
        // Append to an existing throws declaration.
        fix.postfixWith(Iterables.getLast(enclosing.getThrows()), ", " + throwsString);
      }
    }
    return Optional.of(fix.build());
  }

  /** Returns true if the given method symbol has a {@code @Test(expected=...)} annotation. */
  private static boolean isExpectedExceptionTest(MethodSymbol sym, VisitorState state) {
    Compound attribute = sym.attribute(ORG_JUNIT_TEST.get(state));
    if (attribute == null) {
      return false;
    }
    return attribute.member(EXPECTED.get(state)) != null;
  }

  private boolean catchVariableIsUsed(CatchTree c) {
    VarSymbol sym = ASTHelpers.getSymbol(c.getParameter());
    boolean[] found = {false};
    c.getBlock()
        .accept(
            new TreeScanner<Void, Void>() {
              @Override
              public Void visitIdentifier(IdentifierTree node, Void unused) {
                if (Objects.equals(sym, ASTHelpers.getSymbol(node))) {
                  found[0] = true;
                }
                return super.visitIdentifier(node, null);
              }
            },
            null);
    return found[0];
  }

  private static final Supplier<Name> EXPECTED =
      VisitorState.memoize(state -> state.getName("expected"));

  private static final Supplier<Symbol> ORG_JUNIT_TEST =
      VisitorState.memoize(
          state -> state.getSymbolFromString(JUnitMatchers.JUNIT4_TEST_ANNOTATION));
}
