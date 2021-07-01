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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.MoreCollectors.onlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.MoreAnnotations.asStringValue;
import static com.google.errorprone.util.MoreAnnotations.getValue;
import static com.google.errorprone.util.SideEffectAnalysis.hasSideEffect;

import com.google.auto.value.AutoValue;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.MoreAnnotations;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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

  private static final String INLINE_ME = "com.google.errorprone.annotations.InlineMe";

  private static final String VALIDATION_DISABLED =
      "com.google.errorprone.annotations.InlineMeValidationDisabled";

  private final ImmutableSet<String> apiPrefixes;
  private final boolean allowBreakingChanges;

  public Inliner(ErrorProneFlags flags) {
    this.apiPrefixes =
        ImmutableSet.copyOf(flags.getSet(PREFIX_FLAG).orElse(ImmutableSet.<String>of()));
    this.allowBreakingChanges = flags.getBoolean(ALLOW_BREAKING_CHANGES_FLAG).orElse(false);
  }

  // TODO(b/163596864): Add support for inlining fields

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    MethodSymbol symbol = getSymbol(tree);
    if (!hasAnnotation(symbol, INLINE_ME, state)) {
      return Description.NO_MATCH;
    }
    ImmutableList<String> callingVars =
        tree.getArguments().stream().map(state::getSourceForNode).collect(toImmutableList());

    String receiverString = "new " + state.getSourceForNode(tree.getIdentifier());

    return match(tree, symbol, callingVars, receiverString, null, state);
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    MethodSymbol symbol = getSymbol(tree);
    if (!hasAnnotation(symbol, INLINE_ME, state)) {
      return Description.NO_MATCH;
    }
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

    return match(tree, symbol, callingVars, receiverString, receiver, state);
  }

  private Description match(
      ExpressionTree tree,
      MethodSymbol symbol,
      ImmutableList<String> callingVars,
      String receiverString,
      ExpressionTree receiver,
      VisitorState state) {
    checkState(hasAnnotation(symbol, INLINE_ME, state));

    Api api = Api.create(symbol, state);
    if (!matchesApiPrefixes(api)) {
      return Description.NO_MATCH;
    }

    Attribute.Compound inlineMe =
        symbol.getRawAttributes().stream()
            .filter(a -> a.type.tsym.getQualifiedName().contentEquals(INLINE_ME))
            .collect(onlyElement());

    SuggestedFix.Builder builder = SuggestedFix.builder();

    Map<String, String> typeNames = new HashMap<>();
    for (String newImport : getStrings(inlineMe, "imports")) {
      String typeName = Iterables.getLast(PACKAGE_SPLITTER.split(newImport));
      String qualifiedTypeName = SuggestedFixes.qualifyType(state, builder, newImport);
      typeNames.put(typeName, qualifiedTypeName);
    }
    for (String newStaticImport : getStrings(inlineMe, "staticImports")) {
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
      if (callingVars.size() == varNames.size() - 1) {
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

    String replacement =
        trimTrailingSemicolons(asStringValue(getValue(inlineMe, "replacement").get()).get());
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
      // TODO(b/189535612): we'll need to be smarter about our replacements (to avoid clobbering
      // inline parameter comments like /* paramName= */
      replacement =
          replacement.replaceAll(
              "\\b" + Pattern.quote(typeName.getKey()) + "\\b",
              Matcher.quoteReplacement(typeName.getValue()));
    }
    for (int i = 0; i < varNames.size(); i++) {
      // Ex: foo(int a, int... others) -> this.bar(a, others)
      // If caller passes 0 args in the varargs position, we want to remove the preceding comma to
      // make this.bar(a) (as opposed to "this.bar(a, )"
      boolean terminalVarargsReplacement = varargsWithEmptyArguments && i == varNames.size() - 1;
      String capturePrefixForVarargs = terminalVarargsReplacement ? "(?:,\\s*)?" : "";
      // We want to avoid replacing a method invocation with the same name as the method.
      Pattern extractArgAndNextToken =
          Pattern.compile(
              "\\b" + capturePrefixForVarargs + Pattern.quote(varNames.get(i)) + "\\b([^(])");
      String replacementResult =
          Matcher.quoteReplacement(terminalVarargsReplacement ? "" : callingVars.get(i)) + "$1";
      Matcher matcher = extractArgAndNextToken.matcher(replacement);
      replacement = matcher.replaceAll(replacementResult);
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

  private static ImmutableList<String> getStrings(Attribute.Compound attribute, String name) {
    return getValue(attribute, name)
        .map(MoreAnnotations::asStrings)
        .orElse(Stream.empty())
        .collect(toImmutableList());
  }

  private Description describe(Tree tree, SuggestedFix fix, Api api) {
    return buildDescription(tree).setMessage(api.deprecationMessage()).addFix(fix).build();
  }

  @AutoValue
  abstract static class Api {
    private static final Splitter CLASS_NAME_SPLITTER = Splitter.on('.');

    static Api create(MethodSymbol method, VisitorState state) {
      String extraMessage = "";
      if (hasAnnotation(method, VALIDATION_DISABLED, state)) {
        Attribute.Compound inlineMeValidationDisabled =
            method.getRawAttributes().stream()
                .filter(a -> a.type.tsym.getQualifiedName().contentEquals(VALIDATION_DISABLED))
                .collect(onlyElement());
        String reason = Iterables.getOnlyElement(getStrings(inlineMeValidationDisabled, "value"));
        extraMessage = " NOTE: this is an unvalidated inlining! Reasoning: " + reason;
      }
      return new AutoValue_Inliner_Api(
          method.owner.getQualifiedName().toString(),
          method.getSimpleName().toString(),
          method.isConstructor(),
          extraMessage);
    }

    abstract String className();

    abstract String methodName();

    abstract boolean isConstructor();

    abstract String extraMessage();

    final String deprecationMessage() {
      return shortName()
          + " is deprecated and should be inlined"
          + extraMessage();
    }

    /** Returns {@code FullyQualifiedClassName#methodName}. */
    final String methodId() {
      return String.format("%s#%s", className(), methodName());
    }

    /**
     * Returns a short, human readable description of this API (e.g., {@code
     * ClassName.methodName()}).
     */
    final String shortName() {
      return String.format("%s.%s()", simpleClassName(), methodName());
    }

    /** Returns the simple class name (e.g., {@code ClassName}). */
    final String simpleClassName() {
      return Iterables.getLast(CLASS_NAME_SPLITTER.split(className()));
    }
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

  private static final CharMatcher SEMICOLON = CharMatcher.is(';');

  private static String trimTrailingSemicolons(String s) {
    return SEMICOLON.trimTrailingFrom(s);
  }
}
