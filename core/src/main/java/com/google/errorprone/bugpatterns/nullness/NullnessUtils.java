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

package com.google.errorprone.bugpatterns.nullness;

import static com.google.common.collect.Lists.reverse;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.NullCheck.Polarity.IS_NOT_NULL;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.NullCheck.Polarity.IS_NULL;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.NullableAnnotationToUse.annotationToBeImported;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.NullableAnnotationToUse.annotationWithoutImporting;
import static com.google.errorprone.fixes.SuggestedFix.emptyFix;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.suppliers.Suppliers.JAVA_LANG_VOID_TYPE;
import static com.google.errorprone.util.ASTHelpers.enclosingClass;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.stripParentheses;
import static com.sun.source.tree.Tree.Kind.ARRAY_TYPE;
import static com.sun.source.tree.Tree.Kind.IDENTIFIER;
import static com.sun.source.tree.Tree.Kind.NULL_LITERAL;
import static com.sun.source.tree.Tree.Kind.PARAMETERIZED_TYPE;
import static com.sun.tools.javac.parser.Tokens.TokenKind.DOT;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.nullness.NullnessUtils.NullCheck.Polarity;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnnotations;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.FindIdentifiers;
import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Kinds.KindSelector;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.Name;

/**
 * Static utility methods for common functionality in the nullable checkers.
 *
 * @author awturner@google.com (Andy Turner)
 */
class NullnessUtils {
  private NullnessUtils() {}

  private static final Matcher<ExpressionTree> OPTIONAL_OR_NULL =
      instanceMethod().onDescendantOf("com.google.common.base.Optional").named("orNull");
  private static final Matcher<ExpressionTree> OPTIONAL_OR_ELSE =
      instanceMethod().onDescendantOf("java.util.Optional").named("orElse");

  /**
   * Returns {@code true} if the flags request that we look to add @Nullable annotations only where
   * they are nearly certain to be correct and to be about as uncontroversial as nullness
   * annotations can ever be. In Google terms, that means annotations that we'd be willing to roll
   * out across the depot with global approval.
   *
   * <p>If this method returns {@code false}, that gives checkers permission to be more aggressive.
   * Their suggestions should still be very likely to be correct, but the goal is more to assist a
   * human who is aiming to annotate a codebase. The expectation, then, is that at least one human
   * will check whether each new annotation is justified.
   */
  static boolean nullnessChecksShouldBeConservative(ErrorProneFlags flags) {
    return flags.getBoolean("Nullness:Conservative").orElse(true);
  }

  /*
   * TODO(cpovirk): Walking up the tree of enclosing elements may be more expensive than we'd like.
   * (But I haven't checked.) To improve upon that, would we go so far as to build special tracking
   * of @NullMarked-ness of the current TreePath into Error Prone itself? (Of course, even that
   * would help only with the case in which we're interested in the @NullMarked-ness of the tree
   * we're currently visiting.)
   *
   * Another advantage of that approach is that callers wouldn't need to start from a Symbol. For
   * example, VoidMissingNullable.matchParameterizedType wouldn't have to walk up the path to find
   * such a Symbol.
   */

  static boolean isInNullMarkedScope(Symbol sym, VisitorState state) {
    for (; sym != null; sym = sym.getEnclosingElement()) {
      if (hasAnnotation(sym, "org.jspecify.nullness.NullMarked", state)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns a {@link SuggestedFix} to add a {@code Nullable} annotation to the given method's
   * return type.
   */
  static SuggestedFix fixByAddingNullableAnnotationToReturnType(
      VisitorState state, MethodTree method) {
    return fixByAddingNullableAnnotationToElementOrType(
        state, method, method.getReturnType(), "nullness:return");
  }

  /**
   * Returns a {@link SuggestedFix} to add a {@code Nullable} annotation to the given variable's
   * type.
   */
  static SuggestedFix fixByAddingNullableAnnotationToType(
      VisitorState state, VariableTree variable) {
    return fixByAddingNullableAnnotationToElementOrType(
        state, variable, variable.getType(), /* suppressionToRemove= */ null);
  }

  private static SuggestedFix fixByAddingNullableAnnotationToElementOrType(
      VisitorState state, Tree elementTree, Tree typeTree, @Nullable String suppressionToRemove) {
    NullableAnnotationToUse nullableAnnotationToUse = pickNullableAnnotation(state);
    switch (applyOnlyIfAlreadyInScope(state)) {
      case TRUE:
        if (!nullableAnnotationToUse.isAlreadyInScope()) {
          return emptyFix();
        }
        break;
      case IF_NOT:
        if (nullableAnnotationToUse.isAlreadyInScope()) {
          return emptyFix();
        }
        break;
      default:
        break;
    }

    if (!nullableAnnotationToUse.isTypeUse()) {
      return nullableAnnotationToUse.fixPrefixingOnto(elementTree, state, suppressionToRemove);
    }

    return fixByAddingKnownTypeUseNullableAnnotation(
        state, typeTree, nullableAnnotationToUse, suppressionToRemove);
  }

  /**
   * Returns a {@link SuggestedFix} to add a <b>type-use</b> {@code Nullable} annotation to the
   * given tree. The tree should be a "type-use-only" location, like a type argument or a bounds of
   * a type parameter or wildcard. Prefer to use {@link #fixByAddingNullableAnnotationToReturnType}
   * and {@link #fixByAddingNullableAnnotationToType} instead of this method when applicable.
   */
  static SuggestedFix fixByAnnotatingTypeUseOnlyLocationWithNullableAnnotation(
      VisitorState state, Tree typeTree) {
    NullableAnnotationToUse nullableAnnotationToUse = pickNullableAnnotation(state);
    if (!nullableAnnotationToUse.isTypeUse()) {
      return emptyFix();
    }

    return fixByAddingKnownTypeUseNullableAnnotation(
        state, typeTree, nullableAnnotationToUse, /* suppressionToRemove= */ null);
  }

  private static SuggestedFix fixByAddingKnownTypeUseNullableAnnotation(
      VisitorState state,
      Tree typeTree,
      NullableAnnotationToUse nullableAnnotationToUse,
      @Nullable String suppressionToRemove) {
    if (typeTree.getKind() == PARAMETERIZED_TYPE) {
      typeTree = ((ParameterizedTypeTree) typeTree).getType();
    }
    switch (typeTree.getKind()) {
      case ARRAY_TYPE:
        Tree beforeBrackets;
        for (beforeBrackets = typeTree;
            beforeBrackets.getKind() == ARRAY_TYPE;
            beforeBrackets = ((ArrayTypeTree) beforeBrackets).getType()) {}
        // For an explanation of "int @Foo [][] f," etc., see JLS 4.11.
        return nullableAnnotationToUse.fixPostfixingOnto(
            beforeBrackets, state, suppressionToRemove);

      case MEMBER_SELECT:
        int lastDot =
            reverse(state.getOffsetTokensForNode(typeTree)).stream()
                .filter(t -> t.kind() == DOT)
                .findFirst()
                .get()
                .pos();
        return nullableAnnotationToUse.fixPostfixingOnto(lastDot, state, suppressionToRemove);

      case ANNOTATED_TYPE:
        return nullableAnnotationToUse.fixPrefixingOnto(
            ((AnnotatedTypeTree) typeTree).getAnnotations().get(0), state, suppressionToRemove);

      case IDENTIFIER:
        return nullableAnnotationToUse.fixPrefixingOnto(typeTree, state, suppressionToRemove);

      default:
        throw new AssertionError(
            "unexpected kind for type tree: " + typeTree.getKind() + " for " + typeTree);
    }
    // TODO(cpovirk): Remove any @NonNull, etc. annotation that is present?
  }

  static boolean isAlreadyAnnotatedNullable(Symbol symbol) {
    return NullnessAnnotations.fromAnnotationsOn(symbol).orElse(null) == Nullness.NULLABLE;
  }

  static boolean hasExtraParameterForEnclosingInstance(MethodSymbol symbol) {
    // TODO(b/232103314): Figure out which cases the implicit outer `this` parameter exists in.
    if (!symbol.isConstructor()) {
      return false;
    }
    ClassSymbol constructedClass = enclosingClass(symbol);
    return enclosingClass(constructedClass) != null && !constructedClass.isStatic();
  }

  @com.google.auto.value.AutoValue // fully qualified to work around JDK-7177813(?) in JDK8 build
  abstract static class NullableAnnotationToUse {
    static NullableAnnotationToUse annotationToBeImported(String qualifiedName, boolean isTypeUse) {
      return new AutoValue_NullnessUtils_NullableAnnotationToUse(
          qualifiedName,
          qualifiedName.replaceFirst(".*[.]", ""),
          isTypeUse,
          /*isAlreadyInScope=*/ false);
    }

    static NullableAnnotationToUse annotationWithoutImporting(
        String name, boolean isTypeUse, boolean isAlreadyInScope) {
      return new AutoValue_NullnessUtils_NullableAnnotationToUse(
          null, name, isTypeUse, isAlreadyInScope);
    }

    /**
     * Returns a {@link SuggestedFix} to add a {@code Nullable} annotation after the given position.
     */
    final SuggestedFix fixPostfixingOnto(
        int position, VisitorState state, @Nullable String suppressionToRemove) {
      return prepareBuilder(state, suppressionToRemove)
          .replace(position + 1, position + 1, " @" + use() + " ")
          .build();
    }

    /** Returns a {@link SuggestedFix} to add a {@code Nullable} annotation after the given tree. */
    final SuggestedFix fixPostfixingOnto(
        Tree tree, VisitorState state, @Nullable String suppressionToRemove) {
      return prepareBuilder(state, suppressionToRemove)
          .postfixWith(tree, " @" + use() + " ")
          .build();
    }

    /**
     * Returns a {@link SuggestedFix} to add a {@code Nullable} annotation before the given tree.
     */
    final SuggestedFix fixPrefixingOnto(
        Tree tree, VisitorState state, @Nullable String suppressionToRemove) {
      return prepareBuilder(state, suppressionToRemove).prefixWith(tree, "@" + use() + " ").build();
    }

    @Nullable
    abstract String importToAdd();

    abstract String use();

    abstract boolean isTypeUse();

    abstract boolean isAlreadyInScope();

    private SuggestedFix.Builder prepareBuilder(
        VisitorState state, @Nullable String suppressionToRemove) {
      SuggestedFix.Builder builder = SuggestedFix.builder();
      if (importToAdd() != null) {
        builder.addImport(importToAdd());
      }
      if (applyRemoveSuppressWarnings(state)) {
        SuggestedFixes.removeSuppressWarnings(builder, state, suppressionToRemove);
      }
      return builder;
    }
  }

  private static NullableAnnotationToUse pickNullableAnnotation(VisitorState state) {
    /*
     * TODO(cpovirk): Instead of hardcoding these two annotations, pick the one that seems most
     * appropriate for each user:
     *
     * - Look for usages in other files in the compilation?
     *
     * - Look for imports of other annotations that are part of an artifact that also contains
     *   @Nullable (e.g., javax.annotation.Nonnull).
     *
     * - Call getSymbolFromString. (But that may return transitive dependencies that will cause
     *   compilation to fail strict-deps checking.)
     *
     * - Among available candidates, prefer type-usage annotations.
     *
     * - When we suggest a jsr305 annotation, might we want to suggest @CheckForNull over @Nullable?
     *   It's more verbose, but it's more obviously a declaration annotation, and it's the
     *   annotation that is *technically* defined to produce the behaviors that users want. (But do
     *   tools like Dagger recognize it?)
     */
    Symbol sym = FindIdentifiers.findIdent("Nullable", state, KindSelector.VAL_TYP);
    ErrorProneFlags flags = state.errorProneOptions().getFlags();
    String defaultType =
        flags
            .get("Nullness:DefaultNullnessAnnotation")
            .orElse(
                state.isAndroidCompatible()
                    ? "androidx.annotation.Nullable"
                    : "javax.annotation.Nullable");
    if (sym != null) {
      ClassSymbol classSym = (ClassSymbol) sym;
      if (classSym.isAnnotationType()) {
        // We've got an existing annotation called Nullable. We can use this.
        return annotationWithoutImporting(
            "Nullable", isTypeUse(classSym.className()), /*isAlreadyInScope=*/ true);
      } else {
        // It's not an annotation type. We have to fully-qualify the import.
        return annotationWithoutImporting(
            defaultType, isTypeUse(defaultType), /*isAlreadyInScope=*/ false);
      }
    }
    // There is no symbol already. Import and use.
    return annotationToBeImported(defaultType, isTypeUse(defaultType));
  }

  private static boolean isTypeUse(String className) {
    /*
     * TODO(b/205115472): Make this tri-state ({type-use, declaration, both}) and avoid using "both"
     * annotations in any cases in which they would be ambiguous (e.g., arrays/elements).
     */
    switch (className) {
      case "libcore.util.Nullable":
      case "org.checkerframework.checker.nullness.compatqual.NullableType":
      case "org.checkerframework.checker.nullness.qual.Nullable":
      case "org.jspecify.nullness.Nullable":
        return true;
      default:
        // TODO(cpovirk): Detect type-use-ness from the class symbol if it's available?
        return false;
    }
  }

  @Nullable
  static NullCheck getNullCheck(ExpressionTree tree) {
    tree = stripParentheses(tree);

    Polarity polarity;
    switch (tree.getKind()) {
      case EQUAL_TO:
        polarity = IS_NULL;
        break;
      case NOT_EQUAL_TO:
        polarity = IS_NOT_NULL;
        break;
      default:
        return null;
    }

    BinaryTree equalityTree = (BinaryTree) tree;
    ExpressionTree nullChecked;
    if (equalityTree.getRightOperand().getKind() == NULL_LITERAL) {
      nullChecked = equalityTree.getLeftOperand();
    } else if (equalityTree.getLeftOperand().getKind() == NULL_LITERAL) {
      nullChecked = equalityTree.getRightOperand();
    } else {
      return null;
    }

    Name name =
        nullChecked.getKind() == IDENTIFIER ? ((IdentifierTree) nullChecked).getName() : null;

    Symbol symbol = getSymbol(nullChecked);
    VarSymbol varSymbol = symbol instanceof VarSymbol ? (VarSymbol) symbol : null;

    return new AutoValue_NullnessUtils_NullCheck(name, varSymbol, polarity);
  }

  /**
   * A check of a variable against {@code null}, like {@code foo == null}.
   *
   * <p>This class exposes the variable in two forms: the {@link VarSymbol} (if available) and the
   * {@link Name} (if the null check was performed on a bare identifier, like {@code foo}). Many
   * callers restrict themselves to bare identifiers because it's easy and safe: Using {@code
   * Symbol} might lead code to assume that a null check of {@code foo.bar} guarantees something
   * about {@code otherFoo.bar}, which is represented by the same symbol.
   *
   * <p>Even when restricting themselves to bare identifiers, callers should be wary when examining
   * code that might:
   *
   * <ul>
   *   <li>assign a new value to the identifier after the null check but before some usage
   *   <li>declare a new identifier that hides the old
   * </ul>
   *
   * TODO(cpovirk): What our callers really care about is not "bare identifiers" but "this
   * particular 'instance' of a variable," so we could generalize to cover more cases of that. For
   * example, we could probably assume that a null check of {@code foo.bar} ensures that {@code
   * foo.bar} is non-null in the future. One case that might be particularly useful is {@code
   * this.bar}. We might even go further, assuming that {@code foo.bar()} will continue to have the
   * same value in some cases.
   */
  @com.google.auto.value.AutoValue // fully qualified to work around JDK-7177813(?) in JDK8 build
  abstract static class NullCheck {
    /**
     * Returns the bare identifier that was checked against {@code null}, if the null check took
     * that form. Prefer this over {@link #varSymbolButUsuallyPreferBareIdentifier} in most cases,
     * as discussed in the class documentation.
     */
    @Nullable
    abstract Name bareIdentifier();

    /** Returns the symbol that was checked against {@code null}. */
    @Nullable
    abstract VarSymbol varSymbolButUsuallyPreferBareIdentifier();

    abstract Polarity polarity();

    boolean bareIdentifierMatches(ExpressionTree other) {
      return other.getKind() == IDENTIFIER
          && bareIdentifier() != null
          && bareIdentifier().equals(((IdentifierTree) other).getName());
    }

    ExpressionTree nullCase(ConditionalExpressionTree tree) {
      return polarity() == IS_NULL ? tree.getTrueExpression() : tree.getFalseExpression();
    }

    StatementTree nullCase(IfTree tree) {
      return polarity() == IS_NULL ? tree.getThenStatement() : tree.getElseStatement();
    }

    enum Polarity {
      IS_NULL,
      IS_NOT_NULL,
    }
  }

  static boolean hasDefinitelyNullBranch(
      ExpressionTree tree,
      Set<VarSymbol> definitelyNullVars,
      /*
       * TODO(cpovirk): Compute varsProvenNullByParentIf inside this method, using the TreePath from
       * an instance of VisitorState, which must be an instance with the current path instead of
       * stateForCompilationUnit? (This would also let us eliminate the `tree` parameter, since that
       * would be accessible through getLeaf().) But we'll need to be consistent about whether we
       * pass the path of the expression or its enclosing statement.
       */
      ImmutableSet<Name> varsProvenNullByParentIf,
      VisitorState stateForCompilationUnit) {
    return new SimpleTreeVisitor<Boolean, Void>() {
      @Override
      public Boolean visitAssignment(AssignmentTree tree, Void unused) {
        return visit(tree.getExpression(), unused);
      }

      @Override
      public Boolean visitConditionalExpression(ConditionalExpressionTree tree, Void unused) {
        return visit(tree.getTrueExpression(), unused)
            || visit(tree.getFalseExpression(), unused)
            || isTernaryXIfXIsNull(tree);
      }

      @Override
      public Boolean visitIdentifier(IdentifierTree tree, Void unused) {
        return super.visitIdentifier(tree, unused)
            || varsProvenNullByParentIf.contains(tree.getName());
      }

      @Override
      public Boolean visitMethodInvocation(MethodInvocationTree tree, Void unused) {
        return super.visitMethodInvocation(tree, unused) || isOptionalOrNull(tree);
      }

      @Override
      public Boolean visitParenthesized(ParenthesizedTree tree, Void unused) {
        return visit(tree.getExpression(), unused);
      }

      // TODO(cpovirk): visitSwitchExpression

      @Override
      public Boolean visitTypeCast(TypeCastTree tree, Void unused) {
        return visit(tree.getExpression(), unused);
      }

      @Override
      protected Boolean defaultAction(Tree tree, Void unused) {
        /*
         * This covers not only "Void" and "CAP#1 extends Void" but also the null literal. (It
         * covers the null literal even through parenthesized expressions. Still, we end up
         * needing special handling for parenthesized expressions for cases like `(foo ? bar :
         * null)`.)
         */
        return isVoid(getType(tree), stateForCompilationUnit)
            || definitelyNullVars.contains(getSymbol(tree));
      }

      boolean isOptionalOrNull(MethodInvocationTree tree) {
        return OPTIONAL_OR_NULL.matches(tree, stateForCompilationUnit)
            || (OPTIONAL_OR_ELSE.matches(tree, stateForCompilationUnit)
                && tree.getArguments().get(0).getKind() == NULL_LITERAL);
        /*
         * TODO(cpovirk): Instead of checking only for NULL_LITERAL, call hasDefinitelyNullBranch?
         * But consider whether that would interfere with the TODO at the top of that method.
         */
      }
    }.visit(tree, null);
  }

  /** Returns true if this is {@code x == null ? x : ...} or similar. */
  private static boolean isTernaryXIfXIsNull(ConditionalExpressionTree tree) {
    NullCheck nullCheck = getNullCheck(tree.getCondition());
    if (nullCheck == null) {
      return false;
    }
    ExpressionTree needsToBeKnownNull = nullCheck.nullCase(tree);
    return nullCheck.bareIdentifierMatches(needsToBeKnownNull);
  }

  static boolean isVoid(Type type, VisitorState state) {
    return type != null && state.getTypes().isSubtype(type, JAVA_LANG_VOID_TYPE.get(state));
  }

  /** Returns x if the path's leaf is the only statement inside {@code if (x == null) { ... }}. */
  static ImmutableSet<Name> varsProvenNullByParentIf(TreePath path) {
    Tree parent = path.getParentPath().getLeaf();
    if (!(parent instanceof BlockTree)) {
      return ImmutableSet.of();
    }
    if (((BlockTree) parent).getStatements().size() > 1) {
      return ImmutableSet.of();
    }
    Tree grandparent = path.getParentPath().getParentPath().getLeaf();
    if (!(grandparent instanceof IfTree)) {
      return ImmutableSet.of();
    }
    IfTree ifTree = (IfTree) grandparent;
    NullCheck nullCheck = getNullCheck(ifTree.getCondition());
    if (nullCheck == null) {
      return ImmutableSet.of();
    }
    if (parent != nullCheck.nullCase(ifTree)) {
      return ImmutableSet.of();
    }
    if (nullCheck.bareIdentifier() == null) {
      return ImmutableSet.of();
    }
    return ImmutableSet.of(nullCheck.bareIdentifier());
  }

  @Nullable
  static VariableTree findDeclaration(VisitorState state, Symbol sym) {
    JavacProcessingEnvironment javacEnv = JavacProcessingEnvironment.instance(state.context);
    TreePath declPath = Trees.instance(javacEnv).getPath(sym);
    // Skip fields declared in other compilation units since we can't make a fix for them here.
    if (declPath != null
        && declPath.getCompilationUnit() == state.getPath().getCompilationUnit()
        && (declPath.getLeaf() instanceof VariableTree)) {
      return (VariableTree) declPath.getLeaf();
    }
    return null;
  }

  private enum OnlyIfInScope {
    IF_NOT,
    FALSE,
    TRUE
  }

  private static OnlyIfInScope applyOnlyIfAlreadyInScope(VisitorState state) {
    return state
        .errorProneOptions()
        .getFlags()
        .getEnum("Nullness:OnlyIfAnnotationAlreadyInScope", OnlyIfInScope.class)
        .orElse(OnlyIfInScope.FALSE);
  }

  private static boolean applyRemoveSuppressWarnings(VisitorState state) {
    return state
        .errorProneOptions()
        .getFlags()
        .getBoolean("Nullness:RemoveSuppressWarnings")
        .orElse(false);
  }
}
