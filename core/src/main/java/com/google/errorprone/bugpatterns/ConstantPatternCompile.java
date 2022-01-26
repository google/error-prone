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
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFixes.renameVariableUsages;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;

/**
 * Flags variables initialized with {@link java.util.regex.Pattern#compile(String)} calls that could
 * be constants.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    summary = "Variables initialized with Pattern#compile calls on constants can be constants",
    severity = WARNING)
public final class ConstantPatternCompile extends BugChecker implements VariableTreeMatcher {

  private static final Matcher<ExpressionTree> PATTERN_COMPILE_CHECK =
      staticMethod().onClassAny("java.util.regex.Pattern").named("compile");

  private static final Matcher<ExpressionTree> MATCHER_MATCHER =
      instanceMethod().onExactClassAny("java.util.regex.Pattern").named("matcher");

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    if (state.errorProneOptions().isTestOnlyTarget()) {
      return NO_MATCH;
    }
    ExpressionTree initializer = tree.getInitializer();
    if (!PATTERN_COMPILE_CHECK.matches(initializer, state)) {
      return NO_MATCH;
    }
    if (!((MethodInvocationTree) initializer)
        .getArguments().stream().allMatch(ConstantPatternCompile::isArgStaticAndConstant)) {
      return NO_MATCH;
    }
    MethodTree outerMethodTree = ASTHelpers.findEnclosingNode(state.getPath(), MethodTree.class);
    if (outerMethodTree == null) {
      return NO_MATCH;
    }
    Symbol sym = getSymbol(tree);
    if (sym == null) {
      return NO_MATCH;
    }
    switch (sym.getKind()) {
      case RESOURCE_VARIABLE:
        return describeMatch(tree);
      case LOCAL_VARIABLE:
        SuggestedFix fix = fixLocal(tree, outerMethodTree, state);
        return describeMatch(tree, fix);
      default:
        return NO_MATCH;
    }
  }

  private static SuggestedFix fixLocal(
      VariableTree tree, MethodTree outerMethodTree, VisitorState state) {
    SuggestedFix fix = replaceRegexConstant(tree, state);
    if (!fix.isEmpty()) {
      return fix;
    }
    String name = inferName(tree, state);
    if (name == null) {
      return SuggestedFix.emptyFix();
    }
    MethodSymbol methodSymbol = getSymbol(outerMethodTree);
    boolean canUseStatic =
        (methodSymbol != null
                && methodSymbol.owner.enclClass().getNestingKind() == NestingKind.TOP_LEVEL)
            || outerMethodTree.getModifiers().getFlags().contains(Modifier.STATIC);
    String replacement =
        String.format(
            "private %s final %s %s = %s;",
            canUseStatic ? "static " : "",
            state.getSourceForNode(tree.getType()),
            name,
            state.getSourceForNode(tree.getInitializer()));
    return SuggestedFix.builder()
        .merge(renameVariableUsages(tree, name, state))
        .postfixWith(outerMethodTree, replacement)
        .delete(tree)
        .build();
  }

  /**
   * If the pattern variable is initialized with a regex from a {@code private static final String}
   * constant, and the constant is only used once, store the {@code Pattern} in the existing
   * constant.
   *
   * <p>Before:
   *
   * <pre>{@code
   * private static final String MY_REGEX = "a+";
   * ...
   * Pattern p = Pattern.compile(MY_REGEX);
   * p.matcher(...);
   * }</pre>
   *
   * <p>After:
   *
   * <pre>{@code
   * private static final Pattern MY_REGEX = Pattern.compile("a+");
   * ...
   * MY_REGEX.matcher(...);
   * }</pre>
   */
  private static SuggestedFix replaceRegexConstant(VariableTree tree, VisitorState state) {
    ExpressionTree regex = ((MethodInvocationTree) tree.getInitializer()).getArguments().get(0);
    Symbol regexSym = getSymbol(regex);
    if (regexSym == null
        || !regexSym.getKind().equals(ElementKind.FIELD)
        || !regexSym.isStatic()
        || !regexSym.getModifiers().contains(Modifier.FINAL)
        || !isSelfOrTransitiveOwnerPrivate(regexSym)) {
      return SuggestedFix.emptyFix();
    }
    VariableTree[] defs = {null};
    int[] uses = {0};
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitVariable(VariableTree tree, Void unused) {
        if (regexSym.equals(getSymbol(tree))) {
          defs[0] = tree;
        }
        return super.visitVariable(tree, null);
      }

      @Override
      public Void visitIdentifier(IdentifierTree tree, Void unused) {
        if (regexSym.equals(getSymbol(tree))) {
          uses[0]++;
        }
        return super.visitIdentifier(tree, null);
      }

      @Override
      public Void visitMemberSelect(MemberSelectTree tree, Void unused) {
        if (regexSym.equals(getSymbol(tree))) {
          uses[0]++;
        }
        return super.visitMemberSelect(tree, null);
      }
    }.scan(state.getPath().getCompilationUnit(), null);
    if (uses[0] != 1) {
      return SuggestedFix.emptyFix();
    }
    VariableTree def = defs[0];
    return SuggestedFix.builder()
        .replace(def.getType(), state.getSourceForNode(tree.getType()))
        .prefixWith(
            def.getInitializer(),
            state
                .getSourceCode()
                .subSequence(getStartPosition(tree.getInitializer()), getStartPosition(regex))
                .toString())
        .postfixWith(def.getInitializer(), ")")
        .merge(renameVariableUsages(tree, def.getName().toString(), state))
        .delete(tree)
        .build();
  }

  /**
   * Returns true if the symbol is private, or contained by another symbol that is private (e.g. a
   * private member class).
   */
  private static boolean isSelfOrTransitiveOwnerPrivate(Symbol sym) {
    for (; sym != null; sym = sym.owner) {
      if (sym.isPrivate()) {
        return true;
      }
    }
    return false;
  }

  /** Infer a name when upgrading the {@code Pattern} local to a constant. */
  private static String inferName(VariableTree tree, VisitorState state) {
    String name;
    if ((name = fromName(tree)) != null) {
      return name;
    }
    if ((name = fromInitializer(tree)) != null) {
      return name;
    }
    if ((name = fromUse(tree, state)) != null) {
      return name;
    }
    return null;
  }

  /** Use the existing local variable's name, unless it's terrible. */
  private static String fromName(VariableTree tree) {
    String name = LOWER_CAMEL.to(UPPER_UNDERSCORE, tree.getName().toString());
    if (name.length() > 1 && !name.equals("PATTERN")) {
      return name;
    }
    return null;
  }

  /**
   * If the pattern is initialized from an existing constant, re-use its name.
   *
   * <p>e.g. use {@code FOO_PATTERN} for {@code Pattern.compile(FOO)} and {@code
   * Pattern.compile(FOO_REGEX)}.
   */
  private static String fromInitializer(VariableTree tree) {
    ExpressionTree regex = ((MethodInvocationTree) tree.getInitializer()).getArguments().get(0);
    if (!(regex instanceof IdentifierTree)) {
      return null;
    }
    String name = ((IdentifierTree) regex).getName().toString();
    if (name.endsWith("_REGEX")) {
      name = name.substring(0, name.length() - "_REGEX".length());
    }
    if (name.endsWith("_PATTERN")) {
      // Give up if we have something like Pattern.compile(FOO_PATTERN),
      // we don't want to name the regex FOO_PATTERN_PATTERN.
      return null;
    }
    return name + "_PATTERN";
  }

  /**
   * If the pattern is only used once in a call to {@code matcher}, and the argument is a local, use
   * that local's name. For example, infer {@code FOO_PATTERN} from {@code pattern.matcher(foo)}.
   */
  private static String fromUse(VariableTree tree, VisitorState state) {
    VarSymbol sym = getSymbol(tree);
    ImmutableList.Builder<TreePath> usesBuilder = ImmutableList.builder();
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitIdentifier(IdentifierTree tree, Void unused) {
        if (sym.equals(getSymbol(tree))) {
          usesBuilder.add(getCurrentPath());
        }
        return null;
      }
    }.scan(state.getPath().getCompilationUnit(), null);
    ImmutableList<TreePath> uses = usesBuilder.build();
    if (uses.size() != 1) {
      return null;
    }
    TreePath use = getOnlyElement(uses);
    Tree grandParent = use.getParentPath().getParentPath().getLeaf();
    if (!(grandParent instanceof ExpressionTree)) {
      return null;
    }
    if (!MATCHER_MATCHER.matches((ExpressionTree) grandParent, state)) {
      return null;
    }
    ExpressionTree matchTree = ((MethodInvocationTree) grandParent).getArguments().get(0);
    if (!(matchTree instanceof IdentifierTree)) {
      return null;
    }
    return LOWER_CAMEL.to(UPPER_UNDERSCORE, ((IdentifierTree) matchTree).getName().toString())
        + "_PATTERN";
  }

  private static boolean isArgStaticAndConstant(ExpressionTree arg) {
    if (ASTHelpers.constValue(arg) == null) {
      return false;
    }
    Symbol argSymbol = getSymbol(arg);
    if (argSymbol == null) {
      return true;
    }
    return (argSymbol.flags() & Flags.STATIC) != 0;
  }
}
