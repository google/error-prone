/*
 * Copyright 2021 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.findDeclaration;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.fixByAddingNullableAnnotationToType;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.getNullCheck;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasNoExplicitType;
import static javax.lang.model.element.ElementKind.PARAMETER;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.bugpatterns.nullness.NullnessUtils.NullCheck;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnnotations;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "Parameter has handling for null but is not annotated @Nullable",
    severity = SUGGESTION)
public class ParameterMissingNullable extends BugChecker implements BinaryTreeMatcher {
  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    /*
     * This check's basic principle is: If an implementation checks `param == null` or
     * `param != null`, then it's going to take one of two actions:
     *
     * 1. fail: `checkArgument(param != null)`, `if (param == null) throw new IAE()`
     *
     * 2. take some action to treat null as a valid input: `this.p = p == null ? DEFAULT_P : p;`
     *
     * So: If we see a comparison of a parameter against null, we look for code that appears to be
     * implementing "fail." If we don't find such code, then we assume that the method treats a null
     * parameter as a valid input, and we suggest adding @Nullable.
     *
     * TODO(cpovirk): For implementation convenience, we currently make no distinction between
     * `param != null` and `param == null`. This means that we treat `checkArgument(param == null)`
     * as if it implements "fail if null." Fortunately, this doesn't appear to come up much in
     * practice. And anyway, it's OK for us to fail to add @Nullable when it would make sense.
     * Still, it's sloppy, and we might want to clean it up someday. We just may need to be careful
     * about more complex expressions like `checkArgument(!(a == null || b == null))` and about
     * "inverted" methods like `Assert.not(param == null)`.
     */
    NullCheck nullCheck = getNullCheck(tree);
    if (nullCheck == null) {
      return NO_MATCH;
    }
    /*
     * We really do want to use the Symbol here. NullCheck exposes a symbol only in the case of a
     * local variable or parameter, but that's OK here because we care only about parameters. And
     * ultimately we need the Symbol so that we can look at its annotations and find its
     * declaration.
     */
    Symbol symbol = nullCheck.varSymbolButUsuallyPreferBareIdentifier();
    if (!isParameterWithoutNullable(symbol)) {
      return NO_MATCH;
    }

    /*
     * OK, it's `param == null` or `param != null` for some `param` that is not `@Nullable`.
     *
     * We'll move on to determine whether the code implements "fail" or not.
     */

    /*
     * But first, a special case: A null check does *not* guarantee safety if the parameter is
     * dereferenced and reassigned before the check occurs. If that happens, it's probably because
     * of a loop:
     *
     * do { param = param.next(); } while (param ! null);
     *
     * So we don't add @Nullable based on a null check in a loop condition.
     */
    if (isLoopCondition(state.getPath())) {
      return NO_MATCH;
    }

    // Now the big check:
    if (nullCheckLikelyToProduceException(state)) {
      return NO_MATCH;
    }
    // OK, looks like the code handles null as a valid input. Let's add @Nullable if we can+should.

    VariableTree param = findDeclaration(state, symbol);
    if (param == null) {
      return NO_MATCH; // hopefully impossible: A parameter must come from the same compilation unit
    }
    if (hasNoExplicitType(param, state)) {
      return NO_MATCH;
    }
    SuggestedFix fix = fixByAddingNullableAnnotationToType(state, param);
    if (fix.isEmpty()) {
      return NO_MATCH;
    }
    return describeMatch(tree, fix);
  }

  private static boolean isLoopCondition(TreePath path) {
    /*
     * Looking at the grandparent is mostly sufficient:
     *
     * Even if we have `while (something) checkArgument(param != null)`, the null check has parent
     * MethodInvocationTree and grandparent StatementExpressionTree -- i.e., *not* a grandparent
     * WhileLoopTree -- so we don't falsely conclude that the null check is in the loop condition.
     *
     * TODO(cpovirk): Consider looking further up the tree to detect loop conditions like
     * `param != null && param.somethingElse()`.
     */
    switch (path.getParentPath().getParentPath().getLeaf().getKind()) {
      case WHILE_LOOP:
      case DO_WHILE_LOOP:
        return true;
      default:
    }
    switch (path.getParentPath().getLeaf().getKind()) {
      case FOR_LOOP:
        return true;
      default:
    }
    return false;
  }

  private static boolean isParameterWithoutNullable(Symbol sym) {
    return sym != null
        && sym.getKind() == PARAMETER
        && NullnessAnnotations.fromAnnotationsOn(sym).orElse(null) != Nullness.NULLABLE;
  }

  private static boolean nullCheckLikelyToProduceException(VisitorState state) {
    boolean[] likelyToProduceException = {false};
    Tree childInPath = null;
    for (Tree tree : state.getPath()) {
      if (tree instanceof AssertTree || tree instanceof MethodInvocationTree) {
        /*
         * An assert or method call is likely to be something like checkArgument(param != null), so
         * we assume that it will throw an exception for a null argument.
         *
         * That said, we don't check the form of the assert or method call at all. So we might be
         * looking at checkArgument(foo == null) -- which would mean that we *should* add @Nullable.
         * For more discussion, see the TODO at the top of matchBinary.
         */
        return true;
      } else if (tree instanceof IfTree && childInPath.equals(((IfTree) tree).getCondition())) {
        /*
         * We have something like `if (foo == null)`, etc., so we scan the then+else for code that
         * throws exceptions.
         *
         * As in the AssertTree+MethodInvocationTree case above, it would make sense for us to look
         * *only* at the `then` or *only* at the `else`, depending on the form of the null check.
         */
        new TreeScanner<Void, Void>() {
          // Checking for both `new SomeException` and `throw` is probably redundant, but it's easy.

          @Override
          public Void visitNewClass(NewClassTree tree, Void unused) {
            likelyToProduceException[0] |=
                state.getTypes().isSubtype(getType(tree), state.getSymtab().throwableType);
            return super.visitNewClass(tree, unused);
          }

          @Override
          public Void visitThrow(ThrowTree tree, Void unused) {
            likelyToProduceException[0] = true;
            return null;
          }
        }.scan(tree, null);
      }

      childInPath = tree;
    }
    /*
     * TODO(cpovirk): Consider also looking for calls to methods that "look like failures," such as
     * calls to logging APIs or methods with "fail" in the name. However, my initial attempt at this
     * didn't add any value for the code I tested on.
     */
    return likelyToProduceException[0];
  }

  /*
   * TODO(cpovirk): Check for assignment to a @Nullable field. We'll need special cases, though:
   *
   * - fields that should initially be set to a non-null value, only to be nulled out later
   *
   * - fields that are allowed to be set to null from some constructors/methods but not from others
   * (e.g., Foo() sets a field to null; Foo(Object) sets it to the given non-null object)
   *
   * - others?
   */
}
