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
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.hasDirectAnnotationWithSimpleName;
import static com.google.errorprone.util.ASTHelpers.requiresParentheses;
import static com.google.errorprone.util.ASTHelpers.stringContainsComments;
import static com.google.errorprone.util.MoreAnnotations.getValue;
import static com.google.errorprone.util.SideEffectAnalysis.hasSideEffect;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.AppliedFix;
import com.google.errorprone.fixes.ErrorProneEndPosTable;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ErrorProneParser;
import com.google.errorprone.util.MoreAnnotations;
import com.google.errorprone.util.OperatorPrecedence;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.tree.JCTree;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
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
    JavacParser parser = newParser(inlineMe.replacement(), state);
    if (!(parser.parseExpression() instanceof MethodInvocationTree mit
        && mit.getArguments().isEmpty()
        && getReceiver(mit) instanceof IdentifierTree it
        && it.getName().contentEquals("this"))) {
      return Description.NO_MATCH;
    }
    String identifier = ((MemberSelectTree) mit.getMethodSelect()).getIdentifier().toString();
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

    JavacParser parser = newParser(replacement, state);
    ExpressionTree replacementExpression = parser.parseExpression();
    SuggestedFix.Builder replacementFixes = SuggestedFix.builder();

    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();

    for (String newImport : inlineMe.get().imports()) {
      String typeName = Iterables.getLast(PACKAGE_SPLITTER.split(newImport));
      String qualifiedTypeName = SuggestedFixes.qualifyType(state, fixBuilder, newImport);

      visitIdentifiers(
          replacementExpression,
          (node, unused) -> {
            if (node.getName().contentEquals(typeName)) {
              replacementFixes.replace(node, qualifiedTypeName);
            }
          });
    }
    for (String newStaticImport : inlineMe.get().staticImports()) {
      fixBuilder.addStaticImport(newStaticImport);
    }

    int replacementStart = getStartPosition(tree);
    int replacementEnd = state.getEndPosition(tree);

    // Special case replacements starting with "this." so the receiver portion is not included in
    // the replacement. This avoids overlapping replacement regions for fluent chains.
    boolean removedThisPrefix = replacement.startsWith("this.") && receiver != null;
    if (removedThisPrefix) {
      replacementFixes.replace(0, "this".length(), "");
      replacementStart = state.getEndPosition(receiver);
    }

    if (Strings.isNullOrEmpty(receiverString)) {
      visitIdentifiers(
          replacementExpression,
          (node, unused) -> {
            if (node.getName().contentEquals("this")) {
              replacementFixes.replace(
                  getStartPosition(node), parser.getEndPos((JCTree) node) + 1, "");
            }
          });
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
      visitIdentifiers(
          replacementExpression,
          (node, unused) -> {
            if ((!removedThisPrefix || getStartPosition(node) != 0)
                && node.getName().contentEquals("this")) {
              replacementFixes.replace(
                  getStartPosition(node), parser.getEndPos((JCTree) node), receiverString);
            }
          });
    }

    for (int i = 0; i < varNames.size(); i++) {
      String varName = varNames.get(i);

      // If the parameter names are missing (b/365094947), don't perform the inlining.
      if (InlinabilityResult.matchesArgN(varName)) {
        return Description.NO_MATCH;
      }

      // Ex: foo(int a, int... others) -> this.bar(a, others)
      // If caller passes 0 args in the varargs position, we want to remove the preceding comma to
      // make this.bar(a) (as opposed to "this.bar(a, )"
      boolean terminalVarargsReplacement = varargsWithEmptyArguments && i == varNames.size() - 1;
      String replacementResult = terminalVarargsReplacement ? "" : callingVarStrings.get(i);
      boolean mayRequireParens =
          i < callingVars.size() && requiresParentheses(callingVars.get(i), state);

      visitIdentifiers(
          replacementExpression,
          (node, path) -> {
            if (!node.getName().contentEquals(varName)) {
              return;
            }
            // Substituting into a method invocation never requires parens.
            boolean outerNeverRequiresParens =
                path.size() < 2 || getArguments(path.get(path.size() - 2)).contains(node);
            if (terminalVarargsReplacement) {
              var calledMethodArguments = getArguments(path.get(path.size() - 2));
              replacementFixes.replace(
                  calledMethodArguments.indexOf(node) == 0
                      ? getStartPosition(node)
                      : parser.getEndPos(
                          (JCTree)
                              calledMethodArguments.get(calledMethodArguments.indexOf(node) - 1)),
                  parser.getEndPos((JCTree) node),
                  replacementResult);
            } else {
              replacementFixes.replace(
                  node,
                  !outerNeverRequiresParens && mayRequireParens
                      ? "(" + replacementResult + ")"
                      : replacementResult);
            }
          });
    }

    String fixedReplacement =
        AppliedFix.applyReplacements(replacement, asEndPosTable(parser), replacementFixes.build());

    fixBuilder.replace(
        replacementStart,
        replacementEnd,
        inliningRequiresParentheses(state.getPath(), replacementExpression)
            ? format("(%s)", fixedReplacement)
            : fixedReplacement);

    return maybeCheckFixCompiles(tree, state, fixBuilder.build(), api);
  }

  private static JavacParser newParser(String replacement, VisitorState state) {
    return ErrorProneParser.newParser(
        state.context,
        replacement,
        /* keepDocComments= */ true,
        /* keepEndPos= */ true,
        /* keepLineMap= */ true);
  }

  private static List<? extends ExpressionTree> getArguments(Tree tree) {
    return switch (tree) {
      case MethodInvocationTree mit -> mit.getArguments();
      case NewClassTree nct -> nct.getArguments();
      default -> ImmutableList.of();
    };
  }

  /**
   * Checks whether an expression requires parentheses when substituting in.
   *
   * <p>{@code treePath} is the original path including the old tree at the tip; {@code replacement}
   * is the proposed replacement tree.
   *
   * <p>This was originally from {@link com.google.errorprone.util.ASTHelpers#requiresParentheses}
   * but is heavily specialised for this use case.
   */
  private static boolean inliningRequiresParentheses(
      TreePath treePath, ExpressionTree replacement) {
    var originalExpression = treePath.getLeaf();
    var parent = treePath.getParentPath().getLeaf();

    Optional<OperatorPrecedence> replacementPrecedence =
        OperatorPrecedence.optionallyFrom(replacement.getKind());
    Optional<OperatorPrecedence> parentPrecedence =
        OperatorPrecedence.optionallyFrom(parent.getKind());
    if (replacementPrecedence.isPresent() && parentPrecedence.isPresent()) {
      return parentPrecedence.get().isHigher(replacementPrecedence.get());
    }

    // There are some locations, based on the parent path, where we never want to parenthesise.
    // This list is likely not exhaustive.
    switch (parent.getKind()) {
      case RETURN, EXPRESSION_STATEMENT -> {
        return false;
      }
      case VARIABLE -> {
        if (Objects.equals(((VariableTree) parent).getInitializer(), originalExpression)) {
          return false;
        }
      }
      case ASSIGNMENT -> {
        if (((AssignmentTree) parent).getExpression().equals(originalExpression)) {
          return false;
        }
      }
      case METHOD_INVOCATION, NEW_CLASS -> {
        if (getArguments(parent).contains(originalExpression)) {
          return false;
        }
      }
      default -> {
        // continue below
      }
    }
    switch (replacement.getKind()) {
      case IDENTIFIER,
          MEMBER_SELECT,
          METHOD_INVOCATION,
          ARRAY_ACCESS,
          PARENTHESIZED,
          NEW_CLASS,
          MEMBER_REFERENCE -> {
        return false;
      }
      default -> {
        // continue below
      }
    }
    if (replacement instanceof UnaryTree) {
      return parent instanceof MemberSelectTree;
    }
    return true;
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

  private static void visitIdentifiers(
      Tree tree, BiConsumer<IdentifierTree, List<Tree>> identifierConsumer) {
    new TreeScanner<Void, Void>() {
      // It'd be nice to use a TreePathScanner, but we don't have CompilationUnit-rooted AST.
      private final List<Tree> path = new ArrayList<>();

      @Override
      public Void scan(Tree tree, Void unused) {
        if (tree != null) {
          path.add(tree);
          super.scan(tree, null);
          path.remove(path.size() - 1);
        }
        return null;
      }

      @Override
      public Void visitIdentifier(IdentifierTree node, Void unused) {
        identifierConsumer.accept(node, path);
        return super.visitIdentifier(node, null);
      }
    }.scan(tree, null);
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

  private record Api(
      String className,
      String methodName,
      String packageName,
      boolean isConstructor,
      boolean isDeprecated,
      String extraMessage) {
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
      return new Api(
          method.owner.getQualifiedName().toString(),
          method.getSimpleName().toString(),
          enclosingPackage(method).toString(),
          method.isConstructor(),
          hasAnnotation(method, "java.lang.Deprecated", state),
          extraMessage);
    }

    final String message() {
      return "Migrate (via inlining) away from "
          + (isDeprecated() ? "deprecated " : "")
          + shortName()
          + "."
          + extraMessage();
    }

    /** Returns {@code FullyQualifiedClassName#methodName}. */
    final String methodId() {
      return format("%s#%s", className(), methodName());
    }

    /**
     * Returns a short, human readable description of this API in markdown format (e.g., {@code
     * `ClassName.methodName()`}).
     */
    final String shortName() {
      String humanReadableClassName = className().replaceFirst(packageName() + ".", "");
      return format("`%s.%s()`", humanReadableClassName, methodName());
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

  private static ErrorProneEndPosTable asEndPosTable(JavacParser parser) {
    return tree -> parser.getEndPos((JCTree) tree);
  }
}
