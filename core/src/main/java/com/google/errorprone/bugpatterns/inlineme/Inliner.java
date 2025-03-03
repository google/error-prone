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
import static com.google.common.collect.MoreCollectors.onlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.util.ASTHelpers.enclosingPackage;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.hasDirectAnnotationWithSimpleName;
import static com.google.errorprone.util.ASTHelpers.requiresParentheses;
import static com.google.errorprone.util.ASTHelpers.stringContainsComments;
import static com.google.errorprone.util.MoreAnnotations.getValue;
import static com.google.errorprone.util.SideEffectAnalysis.hasSideEffect;
import static java.util.stream.Collectors.joining;

import com.google.auto.value.AutoValue;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.MoreAnnotations;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.inject.Inject;

/**
 * Checker that performs the inlining at call-sites (where the invoked APIs are annotated as
 * {@code @InlineMe}).
 */
@BugPattern(
    name = "InlineMeInliner",
    summary = "Callers of this API should be inlined.",
    severity = WARNING,
    tags = Inliner.FINDING_TAG)
public final class Inliner extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher, MemberReferenceTreeMatcher {

  public static final String FINDING_TAG = "JavaInlineMe";

  static final String PREFIX_FLAG = "InlineMe:Prefix";
  static final String SKIP_COMMENTS_FLAG = "InlineMe:SkipInliningsWithComments";

  private static final Splitter PACKAGE_SPLITTER = Splitter.on('.');

  private static final String CHECK_FIX_COMPILES = "InlineMe:CheckFixCompiles";

  private static final String INLINE_ME = "InlineMe";
  private static final String VALIDATION_DISABLED = "InlineMeValidationDisabled";

  private final ImmutableSet<String> apiPrefixes;
  private final boolean skipCallsitesWithComments;
  private final boolean checkFixCompiles;

  @Inject
  Inliner(ErrorProneFlags flags) {
    this.apiPrefixes = flags.getSetOrEmpty(PREFIX_FLAG);
    this.skipCallsitesWithComments = flags.getBoolean(SKIP_COMMENTS_FLAG).orElse(true);
    this.checkFixCompiles = flags.getBoolean(CHECK_FIX_COMPILES).orElse(false);
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    MethodSymbol symbol = getSymbol(tree);
    if (!hasDirectAnnotationWithSimpleName(symbol, INLINE_ME)) {
      return Description.NO_MATCH;
    }

    String receiverString = "new " + state.getSourceForNode(tree.getIdentifier());

    return match(tree, symbol, tree.getArguments(), receiverString, null, state);
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    MethodSymbol symbol = getSymbol(tree);
    if (!hasDirectAnnotationWithSimpleName(symbol, INLINE_ME)) {
      return Description.NO_MATCH;
    }

    String receiverString = "";

    ExpressionTree receiver = getReceiver(tree);
    if (receiver != null) {
      receiverString = state.getSourceForNode(receiver);
    }

    ExpressionTree methodSelectTree = tree.getMethodSelect();
    if (methodSelectTree != null) {
      String methodSelect = state.getSourceForNode(methodSelectTree);
      if (methodSelect.equals("super")) {
        receiverString = methodSelect;
      }
      // TODO(kak): Can we omit the `this` case? The getReceiver() call above handles `this`
      if (methodSelect.equals("this")) {
        receiverString = methodSelect;
      }
    }

    return match(tree, symbol, tree.getArguments(), receiverString, receiver, state);
  }

  private static final Pattern MEMBER_REFERENCE_PATTERN =
      Pattern.compile(
          "(return\\b+)?((?<qualifier>[^\b]+)\\.)?(?<identifier>\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)\\(\\)");

  @Override
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    MethodSymbol symbol = getSymbol(tree);
    if (symbol.isStatic()) {
      // TODO: b/165938605 - handle static member references
      return Description.NO_MATCH;
    }
    if (!hasDirectAnnotationWithSimpleName(symbol, INLINE_ME)) {
      return Description.NO_MATCH;
    }
    Optional<InlineMeData> inlineMeMaybe = InlineMeData.createFromSymbol(symbol);
    if (inlineMeMaybe.isEmpty()) {
      return Description.NO_MATCH;
    }
    InlineMeData inlineMe = inlineMeMaybe.get();
    if (!inlineMe.imports().isEmpty() || !inlineMe.staticImports().isEmpty()) {
      // TODO: b/165938605 - handle imports
      return Description.NO_MATCH;
    }
    Api api = Api.create(symbol, state);
    if (!matchesApiPrefixes(api)) {
      return Description.NO_MATCH;
    }
    if (skipCallsitesWithComments
        && stringContainsComments(state.getSourceForNode(tree), state.context)) {
      return Description.NO_MATCH;
    }
    Matcher matcher = MEMBER_REFERENCE_PATTERN.matcher(inlineMe.replacement());
    if (!matcher.matches()) {
      return Description.NO_MATCH;
    }
    String qualifier = matcher.group("qualifier");
    if (!qualifier.equals("this")) {
      return Description.NO_MATCH;
    }
    String identifier = matcher.group("identifier");
    SuggestedFix fix =
        SuggestedFix.replace(
            state.getEndPosition(tree.getQualifierExpression()),
            state.getEndPosition(tree),
            "::" + identifier);
    return maybeCheckFixCompiles(tree, state, fix, api);
  }

  private Description match(
      ExpressionTree tree,
      MethodSymbol symbol,
      List<? extends ExpressionTree> callingVars,
      String receiverString,
      ExpressionTree receiver,
      VisitorState state) {
    Optional<InlineMeData> inlineMe = InlineMeData.createFromSymbol(symbol);
    if (inlineMe.isEmpty()) {
      return Description.NO_MATCH;
    }

    Api api = Api.create(symbol, state);
    if (!matchesApiPrefixes(api)) {
      return Description.NO_MATCH;
    }

    if (skipCallsitesWithComments
        && stringContainsComments(state.getSourceForNode(tree), state.context)) {
      return Description.NO_MATCH;
    }

    SuggestedFix.Builder builder = SuggestedFix.builder();

    Map<String, String> typeNames = new HashMap<>();
    for (String newImport : inlineMe.get().imports()) {
      String typeName = Iterables.getLast(PACKAGE_SPLITTER.split(newImport));
      String qualifiedTypeName = SuggestedFixes.qualifyType(state, builder, newImport);
      typeNames.put(typeName, qualifiedTypeName);
    }
    for (String newStaticImport : inlineMe.get().staticImports()) {
      builder.addStaticImport(newStaticImport);
    }

    ImmutableList<String> varNames =
        symbol.getParameters().stream()
            .map(varSymbol -> varSymbol.getSimpleName().toString())
            .collect(toImmutableList());

    ImmutableList<String> callingVarStrings;

    boolean varargsWithEmptyArguments = false;
    if (symbol.isVarArgs()) {
      // If we're calling a varargs method, its inlining *should* have the varargs parameter in a
      // reasonable position. If there are 0 arguments, we'll need to do more surgery
      if (callingVars.size() == varNames.size() - 1) {
        varargsWithEmptyArguments = true;
        callingVarStrings =
            callingVars.stream().map(state::getSourceForNode).collect(toImmutableList());
      } else {
        List<? extends ExpressionTree> nonvarargs = callingVars.subList(0, varNames.size() - 1);
        String varargsJoined =
            callingVars.subList(varNames.size() - 1, callingVars.size()).stream()
                .map(state::getSourceForNode)
                .collect(joining(", "));
        callingVarStrings =
            ImmutableList.<String>builderWithExpectedSize(varNames.size())
                .addAll(nonvarargs.stream().map(state::getSourceForNode).collect(toImmutableList()))
                .add(varargsJoined)
                .build();
      }
    } else {
      callingVarStrings =
          callingVars.stream().map(state::getSourceForNode).collect(toImmutableList());
    }

    String replacement = inlineMe.get().replacement();
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

    RangeMap<Integer, String> replacementsToApply = TreeRangeMap.create();

    for (int i = 0; i < varNames.size(); i++) {
      String varName = varNames.get(i);

      // If the parameter names are missing (b/365094947), don't perform the inlining.
      if (InlinabilityResult.matchesArgN(varName)) {
        return Description.NO_MATCH;
      }

      // The replacement logic below assumes the existence of another token after the parameter
      // in the replacement string (ex: a trailing parens, comma, dot, etc.). However, in the case
      // where the replacement is _just_ one parameter, there isn't a trailing token. We just make
      // the direct replacement here.
      if (replacement.equals(varName)) {
        replacement = callingVarStrings.get(i);
        replacementsToApply.clear();
        break;
      }

      // Ex: foo(int a, int... others) -> this.bar(a, others)
      // If caller passes 0 args in the varargs position, we want to remove the preceding comma to
      // make this.bar(a) (as opposed to "this.bar(a, )"
      boolean terminalVarargsReplacement = varargsWithEmptyArguments && i == varNames.size() - 1;
      String capturePrefixForVarargs = terminalVarargsReplacement ? "(?:,\\s*)?" : "\\b";
      // We want to avoid replacing a method invocation with the same name as the method.
      var extractArgAndNextToken =
          Pattern.compile(capturePrefixForVarargs + "(" + Pattern.quote(varName) + ")\\b([^(])");
      String replacementResult = terminalVarargsReplacement ? "" : callingVarStrings.get(i);
      boolean mayRequireParens =
          i < callingVars.size() && requiresParentheses(callingVars.get(i), state);
      String finalReplacement = replacement;
      extractArgAndNextToken
          .matcher(replacement)
          .results()
          .forEach(
              mr ->
                  replacementsToApply.put(
                      Range.closedOpen(mr.start(0), mr.end(1)),
                      mightRequireParens(mr.start(1), mr.end(1), finalReplacement)
                              && mayRequireParens
                          ? "(" + replacementResult + ")"
                          : replacementResult));
    }

    replacement = applyReplacements(replacement, replacementsToApply);

    builder.replace(replacementStart, replacementEnd, replacement);

    SuggestedFix fix = builder.build();

    return maybeCheckFixCompiles(tree, state, fix, api);
  }

  /**
   * Tries to establish whether substituting an expression into {@code replacement} between {@code
   * start} and {@code end} might require parenthesising.
   *
   * <p>The current heuristic is that the only things that are guaranteed not to are arguments to
   * methods, which we infer with string munging.
   */
  private static boolean mightRequireParens(int start, int end, String replacement) {
    return !LOOKS_LIKE_METHOD_CALL_BEFORE.matcher(replacement.substring(0, start)).matches()
        || !LOOKS_LIKE_METHOD_CALL_AFTER.matcher(replacement.substring(end)).matches();
  }

  private static final Pattern LOOKS_LIKE_METHOD_CALL_BEFORE = Pattern.compile(".*(\\(|,)\\s*$");

  private static final Pattern LOOKS_LIKE_METHOD_CALL_AFTER = Pattern.compile("^\\s*(\\)|,).*");

  private static String applyReplacements(
      String input, RangeMap<Integer, String> replacementsToApply) {
    // Replace in ascending order to avoid quadratic behaviour.
    int idx = 0;
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<Range<Integer>, String> entry : replacementsToApply.asMapOfRanges().entrySet()) {
      Range<Integer> range = entry.getKey();
      String newText = entry.getValue();
      sb.append(input, idx, range.lowerEndpoint()).append(newText);
      idx = range.upperEndpoint();
    }
    sb.append(input, idx, input.length());
    return sb.toString();
  }

  private Description maybeCheckFixCompiles(
      ExpressionTree tree, VisitorState state, SuggestedFix fix, Api api) {
    if (checkFixCompiles && fix.getImportsToAdd().isEmpty()) {
      // If there are no new imports being added (then there are no new dependencies). Therefore, we
      // can verify that the fix compiles (if CHECK_FIX_COMPILES is enabled).
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
    return buildDescription(tree).setMessage(api.message()).addFix(fix).build();
  }

  @AutoValue
  abstract static class Api {
    private static final Splitter CLASS_NAME_SPLITTER = Splitter.on('.');

    static Api create(MethodSymbol method, VisitorState state) {
      String extraMessage = "";
      if (hasDirectAnnotationWithSimpleName(method, VALIDATION_DISABLED)) {
        Attribute.Compound inlineMeValidationDisabled =
            method.getRawAttributes().stream()
                .filter(a -> a.type.tsym.getSimpleName().contentEquals(VALIDATION_DISABLED))
                .collect(onlyElement());
        String reason = Iterables.getOnlyElement(getStrings(inlineMeValidationDisabled, "value"));
        extraMessage = " NOTE: this is an unvalidated inlining! Reasoning: " + reason;
      }
      return new AutoValue_Inliner_Api(
          method.owner.getQualifiedName().toString(),
          method.getSimpleName().toString(),
          enclosingPackage(method).toString(),
          method.isConstructor(),
          hasAnnotation(method, "java.lang.Deprecated", state),
          extraMessage);
    }

    abstract String className();

    abstract String methodName();

    abstract String packageName();

    abstract boolean isConstructor();

    abstract boolean isDeprecated();

    abstract String extraMessage();

    final String message() {
      return "Migrate (via inlining) away from "
          + (isDeprecated() ? "deprecated " : "")
          + shortName()
          + "."
          + extraMessage();
    }

    /** Returns {@code FullyQualifiedClassName#methodName}. */
    final String methodId() {
      return String.format("%s#%s", className(), methodName());
    }

    /**
     * Returns a short, human readable description of this API in markdown format (e.g., {@code
     * `ClassName.methodName()`}).
     */
    final String shortName() {
      String humanReadableClassName = className().replaceFirst(packageName() + ".", "");
      return String.format("`%s.%s()`", humanReadableClassName, methodName());
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
}
