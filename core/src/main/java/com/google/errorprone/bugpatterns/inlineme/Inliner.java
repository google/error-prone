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

package com.google.errorprone.bugpatterns.inlineme;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.SideEffectAnalysis.hasSideEffect;

import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.InlineMe;
import com.google.errorprone.annotations.InlineMeValidationDisabled;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checker that performs the inlining at call-sites (where the invoked APIs are annotated as
 * {@code @InlineMe}).
 */
@BugPattern(
    name = "InlineMeInliner",
    summary = "This API is deprecated and the caller should be 'inlined'.",
    severity = WARNING,
    tags = Inliner.FINDING_TAG)
public final class Inliner extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  public static final String FINDING_TAG = "JavaInlineMe";

  private static final Splitter PACKAGE_SPLITTER = Splitter.on('.');

  static final String PREFIX_FLAG = "InlineMe:Prefix";

  static final String ALLOW_BREAKING_CHANGES_FLAG = "InlineMe:AllowBreakingChanges";
  static final String ALLOW_UNVALIDATED_INLININGS_FLAG = "InlineMe:AllowUnvalidatedInlinings";

  private final ImmutableSet<String> apiPrefixes;
  private final boolean allowBreakingChanges;
  private final boolean refactorUnvalidatedMethods;

  public Inliner(ErrorProneFlags flags) {
    this.apiPrefixes =
        ImmutableSet.copyOf(flags.getSet(PREFIX_FLAG).orElse(ImmutableSet.<String>of()));
    this.allowBreakingChanges = flags.getBoolean(ALLOW_BREAKING_CHANGES_FLAG).orElse(false);
    this.refactorUnvalidatedMethods =
        flags.getBoolean(ALLOW_UNVALIDATED_INLININGS_FLAG).orElse(false);
  }

  // TODO(b/163596864): Add support for inlining fields

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    ImmutableList<String> callingVars =
        tree.getArguments().stream().map(state::getSourceForNode).collect(toImmutableList());

    String receiverString = "new " + state.getSourceForNode(tree.getIdentifier());

    return match(tree, getSymbol(tree), callingVars, receiverString, null, state);
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    ImmutableList<String> callingVars =
        tree.getArguments().stream().map(state::getSourceForNode).collect(toImmutableList());

    String receiverString = "";

    ExpressionTree receiver = getReceiver(tree);
    if (receiver != null) {
      receiverString = state.getSourceForNode(receiver);
    }

    ExpressionTree methodSelectTree = tree.getMethodSelect();
    if (methodSelectTree != null) {
      String methodSelect = state.getSourceForNode(methodSelectTree);
      if ("super".equals(methodSelect)) {
        receiverString = methodSelect;
      }
      // TODO(kak): Can we omit the `this` case? The getReceiver() call above handles `this`
      if ("this".equals(methodSelect)) {
        receiverString = methodSelect;
      }
    }

    return match(tree, getSymbol(tree), callingVars, receiverString, receiver, state);
  }

  private Description match(
      ExpressionTree tree,
      MethodSymbol symbol,
      ImmutableList<String> callingVars,
      String receiverString,
      ExpressionTree receiver,
      VisitorState state) {
    if (!hasAnnotation(symbol, InlineMe.class, state)) {
      return Description.NO_MATCH;
    }

    Api api = Api.create(symbol);
    if (!matchesApiPrefixes(api)) {
      return Description.NO_MATCH;
    }
    if (shouldSkipUnvalidatedMethod(symbol, state)) {
      return Description.NO_MATCH;
    }

    InlineMe inlineMe = symbol.getAnnotation(InlineMe.class);

    SuggestedFix.Builder builder = SuggestedFix.builder();

    Map<String, String> typeNames = new HashMap<>();
    for (String newImport : inlineMe.imports()) {
      String typeName = Iterables.getLast(PACKAGE_SPLITTER.split(newImport));
      String qualifiedTypeName = SuggestedFixes.qualifyType(state, builder, newImport);
      typeNames.put(typeName, qualifiedTypeName);
    }
    for (String newStaticImport : inlineMe.staticImports()) {
      builder.addStaticImport(newStaticImport);
    }

    ImmutableList<String> varNames =
        symbol.getParameters().stream()
            .map(varSymbol -> varSymbol.getSimpleName().toString())
            .collect(toImmutableList());

    boolean varargsWithEmptyArguments = false;
    if (symbol.isVarArgs()) {
      // If we're calling a varargs method, its inlining *should* have the varargs parameter in a
      // reasonable position. If there are are 0 arguments, we'll need to do more surgery
      if (callingVars.size() - 1 == varNames.size()) {
        varargsWithEmptyArguments = true;
      } else {
        ImmutableList<String> nonvarargs = callingVars.subList(0, varNames.size() - 1);
        String varargsJoined =
            Joiner.on(", ").join(callingVars.subList(varNames.size() - 1, callingVars.size()));
        callingVars =
            ImmutableList.<String>builderWithExpectedSize(varNames.size())
                .addAll(nonvarargs)
                .add(varargsJoined)
                .build();
      }
    }

    String replacement = inlineMe.replacement();
    int replacementStart = ((DiagnosticPosition) tree).getStartPosition();
    int replacementEnd = state.getEndPosition(tree);

    // Special case replacements starting with "this." so the receiver portion is not included in
    // the replacement. This avoids overlapping replacement regions for fluent chains.
    if (replacement.startsWith("this.") && receiver != null) {
      replacementStart = state.getEndPosition(receiver);
      replacement = replacement.substring("this".length());
    }

    if (Strings.isNullOrEmpty(receiverString)) {
      replacement = replacement.replaceAll("\\bthis\\.\\b", "");
    } else {
      if (replacement.equals("this")) { // e.g.: foo.b() -> foo
        Tree parent = state.getPath().getParentPath().getLeaf();
        // If the receiver is a side-effect-free expression and the whole expression is standalone,
        // the receiver likely can't stand on its own (e.g.: "foo;" is not a valid statement while
        // "foo.noOpMethod();" is).
        if (parent instanceof ExpressionStatementTree && !hasSideEffect(receiver)) {
          return describe(parent, SuggestedFix.delete(parent), api);
        }
      }
      replacement = replacement.replaceAll("\\bthis\\b", receiverString);
    }

    // Qualify imports first, then replace parameter values to avoid clobbering source from the
    // inlined method.
    for (Map.Entry<String, String> typeName : typeNames.entrySet()) {
      replacement =
          replacement.replaceAll(
              "\\b" + Pattern.quote(typeName.getKey()) + "\\b",
              Matcher.quoteReplacement(typeName.getValue()));
    }
    for (int i = 0; i < varNames.size(); i++) {
      // Ex: foo(int a, int... others) -> this.bar(a, others)
      // If caller passes 0 args in the varargs position, we want to remove the preceding comma to
      // make this.bar(a) (as opposed to "this.bar(a, )"
      String capturePrefixForVarargs =
          (varargsWithEmptyArguments && i == varNames.size() - 1) ? "(,\\s*)?" : "";
      // We want to avoid replacing a method invocation with the same name as the method.
      String findArgName =
          "\\b" + capturePrefixForVarargs + Pattern.quote(varNames.get(i)) + "\\b([^(])";
      replacement =
          replacement.replaceAll(findArgName, Matcher.quoteReplacement(callingVars.get(i)) + "$1");
    }

    builder.replace(replacementStart, replacementEnd, replacement);

    SuggestedFix fix = builder.build();

    // If there are no imports to add, then there's no new dependencies, so we can verify that it
    // compilesWithFix(); if there are new imports to add, then we can't validate that it compiles.
    if (fix.getImportsToAdd().isEmpty() && !allowBreakingChanges) {
      return SuggestedFixes.compilesWithFix(fix, state)
          ? describe(tree, fix, api)
          : Description.NO_MATCH;
    }

    return describe(tree, fix, api);
  }

  private Description describe(Tree tree, SuggestedFix fix, Api api) {
    return buildDescription(tree)
        .setMessage(
            api.shortName()
                + " is deprecated and should be inlined"
            )
        .addFix(fix)
        .build();
  }

  @AutoValue
  abstract static class Api {
    private static final Splitter CLASS_NAME_SPLITTER = Splitter.on('.');

    static Api create(MethodSymbol method) {
      return new AutoValue_Inliner_Api(
          method.owner.getQualifiedName().toString(),
          method.getSimpleName().toString(),
          method.isConstructor());
    }

    abstract String className();

    abstract String methodName();

    abstract boolean isConstructor();

    /** Returns {@code FullyQualifiedClassName#methodName}. */
    String methodId() {
      return String.format("%s#%s", className(), methodName());
    }

    /**
     * Returns a short, human readable description of this API (e.g., {@code
     * ClassName.methodName()}).
     */
    String shortName() {
      return String.format("%s.%s()", simpleClassName(), methodName());
    }

    /** Returns the simple class name (e.g., {@code ClassName}). */
    String simpleClassName() {
      return Iterables.getLast(CLASS_NAME_SPLITTER.split(className()));
    }
  }

  private boolean shouldSkipUnvalidatedMethod(MethodSymbol symbol, VisitorState state) {
    return !refactorUnvalidatedMethods
        && ASTHelpers.hasAnnotation(symbol, InlineMeValidationDisabled.class, state);
  }

  private boolean matchesApiPrefixes(Api api) {
    if (apiPrefixes.isEmpty()) {
      return true;
    }
    for (String apiPrefix : apiPrefixes) {
      if (api.methodId().startsWith(apiPrefix)) {
        return true;
      }
    }
    return false;
  }
}
