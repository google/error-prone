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
import static com.google.errorprone.util.ASTHelpers.canBeRemoved;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isStatic;
import static java.lang.String.format;

import com.google.common.base.Ascii;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
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
import java.util.Optional;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Flags variables initialized with {@link java.util.regex.Pattern#compile(String)} calls that could
 * be constants.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    summary = "Variables initialized with Pattern#compile calls on constants can be constants",
    severity = WARNING)
public final class ConstantPatternCompile extends BugChecker implements ClassTreeMatcher {
  private static final ImmutableList<String> PATTERN_CLASSES =
      ImmutableList.of("java.util.regex.Pattern", "com.google.re2j.Pattern");

  private static final Matcher<ExpressionTree> PATTERN_COMPILE_CHECK =
      staticMethod().onClassAny(PATTERN_CLASSES).named("compile");

  private static final Matcher<ExpressionTree> MATCHER_MATCHER =
      instanceMethod().onExactClassAny(PATTERN_CLASSES).named("matcher");

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    NameUniquifier nameUniquifier = new NameUniquifier();
    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
    Tree[] firstHit = new Tree[1];
    for (Tree member : classTree.getMembers()) {
      new TreePathScanner<Void, Void>() {
        @Override
        public Void visitClass(ClassTree node, Void unused) {
          // Don't descend into nested classes - we'll visit them later
          return null;
        }

        private Optional<SuggestedFix> tryFix(
            MethodInvocationTree tree, VisitorState state, NameUniquifier nameUniquifier) {
          if (!PATTERN_COMPILE_CHECK.matches(tree, state)) {
            return Optional.empty();
          }
          if (!tree.getArguments().stream()
              .allMatch(ConstantPatternCompile::isArgStaticAndConstant)) {
            return Optional.empty();
          }
          if (state.errorProneOptions().isTestOnlyTarget()) {
            return Optional.empty();
          }
          Tree parent = state.getPath().getParentPath().getLeaf();
          if (parent instanceof VariableTree) {
            return handleVariable((VariableTree) parent, state);
          }

          return Optional.of(handleInlineExpression(tree, state, nameUniquifier));
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
          tryFix(tree, state.withPath(getCurrentPath()), nameUniquifier)
              .ifPresent(
                  other -> {
                    fixBuilder.merge(other);
                    if (firstHit[0] == null) {
                      firstHit[0] = tree;
                    }
                  });
          return super.visitMethodInvocation(tree, unused);
        }
      }.scan(new TreePath(state.getPath(), member), null);
    }
    if (firstHit[0] == null) {
      return NO_MATCH;
    }
    return describeMatch(firstHit[0], fixBuilder.build());
  }

  private static SuggestedFix handleInlineExpression(
      MethodInvocationTree tree, VisitorState state, NameUniquifier nameUniquifier) {
    String nameSuggestion =
        nameUniquifier.uniquify(
            Optional.ofNullable(findNameFromMatcherArgument(state, state.getPath()))
                .orElse("PATTERN"));
    SuggestedFix.Builder fix = SuggestedFix.builder();
    return fix.replace(tree, nameSuggestion)
        .merge(
            SuggestedFixes.addMembers(
                state.findEnclosing(ClassTree.class),
                state,
                format(
                    "private static final %s %s = %s;",
                    SuggestedFixes.qualifyType(state, fix, getSymbol(tree).getReturnType().tsym),
                    nameSuggestion,
                    state.getSourceForNode(tree))))
        .build();
  }

  private static Optional<SuggestedFix> handleVariable(VariableTree tree, VisitorState state) {
    MethodTree outerMethodTree = ASTHelpers.findEnclosingNode(state.getPath(), MethodTree.class);
    if (outerMethodTree == null) {
      return Optional.empty();
    }
    VarSymbol sym = getSymbol(tree);
    switch (sym.getKind()) {
      case RESOURCE_VARIABLE:
        return Optional.of(SuggestedFix.emptyFix());
      case LOCAL_VARIABLE:
        return Optional.of(fixLocal(tree, outerMethodTree, state));
      default:
        return Optional.empty();
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
        methodSymbol.owner.enclClass().getNestingKind() == NestingKind.TOP_LEVEL
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
        || !isStatic(regexSym)
        || !regexSym.getModifiers().contains(Modifier.FINAL)
        || !canBeRemoved((VarSymbol) regexSym)) {
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

  /** Infer a name when upgrading the {@code Pattern} local to a constant. */
  private static @Nullable String inferName(VariableTree tree, VisitorState state) {
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
  private static @Nullable String fromName(VariableTree tree) {
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
  private static @Nullable String fromInitializer(VariableTree tree) {
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
   * If the pattern is only used once in a call to {@code matcher}, and the argument is a variable,
   * use that variable's name. For example, infer {@code FOO_PATTERN} from {@code
   * pattern.matcher(foo)}. If the argument to the call is a method call, use the method's name.
   */
  private static @Nullable String fromUse(VariableTree tree, VisitorState state) {
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
    return findNameFromMatcherArgument(state, use);
  }

  /**
   * If the path at {@code use} is a Pattern object whose .matcher method is being called on a
   * variable CharSequence, returns the name of that variable.
   */
  private static @Nullable String findNameFromMatcherArgument(VisitorState state, TreePath use) {
    Tree grandParent = use.getParentPath().getParentPath().getLeaf();
    if (!(grandParent instanceof ExpressionTree)) {
      return null;
    }
    if (!MATCHER_MATCHER.matches((ExpressionTree) grandParent, state)) {
      return null;
    }
    ExpressionTree matchTree = ((MethodInvocationTree) grandParent).getArguments().get(0);
    if (matchTree instanceof IdentifierTree) {
      return convertToConstantName(((IdentifierTree) matchTree).getName().toString());
    }
    if (matchTree instanceof MethodInvocationTree) {
      return convertToConstantName(
          getSymbol((MethodInvocationTree) matchTree).getSimpleName().toString());
    }
    return null;
  }

  private static String convertToConstantName(String variableName) {
    String root =
        variableName.equals(Ascii.toUpperCase(variableName))
            ? variableName
            : LOWER_CAMEL.to(UPPER_UNDERSCORE, variableName);
    return root + "_PATTERN";
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

  private static final class NameUniquifier {
    final Multiset<String> assignmentCounts = HashMultiset.create();

    String uniquify(String name) {
      int numPreviousUses = assignmentCounts.add(name, 1);
      if (numPreviousUses == 0) {
        return name;
      }
      return name + (numPreviousUses + 1);
    }
  }
}
