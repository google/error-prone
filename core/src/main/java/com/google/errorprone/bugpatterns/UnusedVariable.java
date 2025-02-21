/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.nullToEmpty;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.SERIALIZATION_METHODS;
import static com.google.errorprone.util.ASTHelpers.canBeRemoved;
import static com.google.errorprone.util.ASTHelpers.findSuperMethods;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.hasExplicitSource;
import static com.google.errorprone.util.ASTHelpers.isAbstract;
import static com.google.errorprone.util.ASTHelpers.isStatic;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static com.google.errorprone.util.ASTHelpers.shouldKeep;
import static com.google.errorprone.util.MoreAnnotations.asStrings;
import static com.google.errorprone.util.MoreAnnotations.getAnnotationValue;
import static com.google.errorprone.util.SideEffectAnalysis.hasSideEffect;
import static com.sun.source.tree.Tree.Kind.POSTFIX_DECREMENT;
import static com.sun.source.tree.Tree.Kind.POSTFIX_INCREMENT;
import static com.sun.source.tree.Tree.Kind.PREFIX_DECREMENT;
import static com.sun.source.tree.Tree.Kind.PREFIX_INCREMENT;

import com.google.auto.value.AutoValue;
import com.google.common.base.Ascii;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.Streams;
import com.google.common.collect.TreeRangeSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.suppliers.Suppliers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ErroneousTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCAssignOp;
import com.sun.tools.javac.util.Position;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.type.NullType;
import javax.tools.JavaFileObject;
import org.jspecify.annotations.Nullable;

/** Bugpattern to detect unused declarations. */
@BugPattern(
    altNames = {"unused", "UnusedParameters"},
    summary = "Unused.",
    severity = WARNING,
    documentSuppression = false)
public final class UnusedVariable extends BugChecker implements CompilationUnitTreeMatcher {
  private final ImmutableSet<String> exemptPrefixes;

  private final ImmutableSet<String> exemptNames;

  /**
   * The set of annotation full names which exempt annotated element from being reported as unused.
   *
   * <p>Try to avoid adding more annotations here. Annotating these annotations with {@code @Keep}
   * has the same effect; this list is chiefly for third-party annotations which cannot be
   * annotated.
   */
  private static final ImmutableSet<String> EXEMPTING_VARIABLE_ANNOTATIONS =
      ImmutableSet.of(
          "javax.persistence.Basic",
          "javax.persistence.Column",
          "javax.persistence.Id",
          "javax.persistence.Version",
          "javax.xml.bind.annotation.XmlElement",
          "org.junit.Rule",
          "org.junit.jupiter.api.extension.RegisterExtension",
          "org.openqa.selenium.support.FindAll",
          "org.openqa.selenium.support.FindBy",
          "org.openqa.selenium.support.FindBys",
          "org.apache.beam.sdk.transforms.DoFn.TimerId",
          "org.apache.beam.sdk.transforms.DoFn.StateId",
          "org.springframework.boot.test.mock.mockito.MockBean");

  // TODO(ghm): Find a sensible place to dedupe this with UnnecessarilyVisible.
  private static final ImmutableSet<String> ANNOTATIONS_INDICATING_PARAMETERS_SHOULD_BE_CHECKED =
      ImmutableSet.of(
          "com.google.errorprone.refaster.annotation.AfterTemplate",
          "com.google.errorprone.refaster.annotation.BeforeTemplate",
          "com.google.inject.Inject",
          "com.google.inject.Provides",
          "com.google.inject.multibindings.ProvidesIntoMap",
          "com.google.inject.multibindings.ProvidesIntoSet",
          "dagger.Provides",
          "javax.inject.Inject",
          // Parameters on test methods imply the test is parameterised, and those parameters should
          // be used or removed.
          "org.junit.Test");

  private final ImmutableSet<String> methodAnnotationsExemptingParameters;

  /** The set of types exempting a type that is extending or implementing them. */
  private static final ImmutableSet<String> EXEMPTING_SUPER_TYPES = ImmutableSet.of();

  /** The set of types exempting a field of type extending them. */
  private static final ImmutableSet<String> EXEMPTING_FIELD_SUPER_TYPES =
      ImmutableSet.of("org.junit.rules.TestRule");

  private static final ImmutableSet<String> SPECIAL_FIELDS =
      ImmutableSet.of(
          "serialVersionUID",
          // TAG fields are used by convention in Android apps.
          "TAG");

  private static final ImmutableSet<String> FUNCTIONAL_INTERFACE_TYPES_TO_CHECK =
      ImmutableSet.of("java.util.Comparator");

  private final boolean reportInjectedFields;

  @Inject
  UnusedVariable(ErrorProneFlags flags) {
    this.methodAnnotationsExemptingParameters =
        ImmutableSet.<String>builder()
            .add("org.robolectric.annotation.Implementation")
            .addAll(flags.getListOrEmpty("Unused:methodAnnotationsExemptingParameters"))
            .build();
    this.reportInjectedFields = flags.getBoolean("Unused:ReportInjectedFields").orElse(false);

    this.exemptNames =
        ImmutableSet.<String>builder()
            .add("ignored")
            .add("") // `var _ = ...` is handled as an empty variable name
            .addAll(flags.getListOrEmpty("Unused:exemptNames"))
            .build();

    this.exemptPrefixes =
        ImmutableSet.<String>builder()
            .add("unused")
            .addAll(flags.getSetOrEmpty("Unused:exemptPrefixes"))
            .build();
  }

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    // We will skip reporting on the whole compilation if there are any native methods found.
    // Use a TreeScanner to find all local variables and fields.
    if (hasNativeMethods(tree)) {
      return Description.NO_MATCH;
    }

    ImmutableMultimap<MethodSymbol, MethodSymbol> superMethodsToOverrides =
        getSuperMethodsToOverrides(tree, state);

    VariableFinder variableFinder = new VariableFinder(state, superMethodsToOverrides);
    variableFinder.scan(state.getPath(), null);

    // Map of symbols to variable declarations. Initially this is a map of all of the local variable
    // and fields. As we go we remove those variables which are used.
    Map<Symbol, TreePath> unusedElements = variableFinder.unusedElements;

    // Whether a symbol should only be checked for reassignments (e.g. public methods' parameters).
    Set<Symbol> onlyCheckForReassignments = variableFinder.onlyCheckForReassignments;

    // Map of symbols to their usage sites. In this map we also include the definition site in
    // addition to all the trees where symbol is used. This map is designed to keep the usage sites
    // of variables (parameters, fields, locals).
    //
    // We populate this map when analyzing the unused variables and then use it to generate
    // appropriate fixes for them.
    ListMultimap<Symbol, TreePath> usageSites = variableFinder.usageSites;

    FilterUsedVariables filterUsedVariables =
        new FilterUsedVariables(state, unusedElements, usageSites);
    filterUsedVariables.scan(state.getPath(), null);

    // Keeps track of whether a symbol was _ever_ used (between reassignments).
    Set<Symbol> isEverUsed = filterUsedVariables.isEverUsed;
    List<UnusedSpec> unusedSpecs = filterUsedVariables.unusedSpecs;

    // Add the left-over unused variables...
    for (Map.Entry<Symbol, TreePath> entry : unusedElements.entrySet()) {
      unusedSpecs.add(
          UnusedSpec.of(entry.getKey(), entry.getValue(), usageSites.get(entry.getKey()), null));
    }

    ImmutableListMultimap<Symbol, UnusedSpec> unusedSpecsBySymbol =
        Multimaps.index(unusedSpecs, UnusedSpec::symbol);

    for (Map.Entry<Symbol, Collection<UnusedSpec>> entry : unusedSpecsBySymbol.asMap().entrySet()) {
      Symbol unusedSymbol = entry.getKey();
      Collection<UnusedSpec> specs = entry.getValue();

      ImmutableList<TreePath> allUsageSites =
          specs.stream().flatMap(u -> u.usageSites().stream()).collect(toImmutableList());
      if (!unusedElements.containsKey(unusedSymbol)) {
        isEverUsed.add(unusedSymbol);
      }
      SuggestedFix makeFirstAssignmentDeclaration =
          makeAssignmentDeclaration(unusedSymbol, specs, allUsageSites, state);
      // Don't complain if this is a public method and we only overwrote it once.
      if (onlyCheckForReassignments.contains(unusedSymbol) && specs.size() <= 1) {
        continue;
      }
      Tree unused = specs.iterator().next().assignmentPath().getLeaf();
      VarSymbol symbol = (VarSymbol) unusedSymbol;
      ImmutableList<SuggestedFix> fixes;
      if (symbol.getKind() == ElementKind.PARAMETER
          && !onlyCheckForReassignments.contains(unusedSymbol)
          && !isEverUsed.contains(unusedSymbol)) {
        fixes = buildUnusedParameterFixes(symbol, allUsageSites, state);
      } else {
        fixes = buildUnusedVarFixes(symbol, allUsageSites, state);
      }
      state.reportMatch(
          buildDescription(unused)
              .setMessage(
                  String.format(
                      "%s %s '%s' is never read.",
                      isEverUsed.contains(symbol) ? "This assignment to the" : "The",
                      describeVariable(symbol),
                      symbol.name))
              .addAllFixes(
                  fixes.stream()
                      .map(f -> SuggestedFix.merge(makeFirstAssignmentDeclaration, f))
                      .collect(toImmutableList()))
              .build());
    }
    return Description.NO_MATCH;
  }

  private static SuggestedFix makeAssignmentDeclaration(
      Symbol unusedSymbol,
      Collection<UnusedSpec> specs,
      ImmutableList<TreePath> allUsageSites,
      VisitorState state) {
    if (unusedSymbol.getKind() != ElementKind.LOCAL_VARIABLE) {
      return SuggestedFix.emptyFix();
    }
    Optional<VariableTree> removedVariableTree =
        allUsageSites.stream()
            .filter(tp -> tp.getLeaf() instanceof VariableTree)
            .findFirst()
            .map(tp -> (VariableTree) tp.getLeaf());

    // Find the first reassignment which wasn't only used by an ultimately unused assignment. If
    // there is one, it should become a variable declaration.
    Optional<AssignmentTree> reassignment =
        specs.stream()
            .map(UnusedSpec::terminatingAssignment)
            .flatMap(Streams::stream)
            .filter(
                a ->
                    allUsageSites.stream()
                        .noneMatch(
                            tp ->
                                tp.getLeaf() instanceof ExpressionStatementTree
                                    && ((ExpressionStatementTree) tp.getLeaf())
                                        .getExpression()
                                        .equals(a)))
            .findFirst();
    if (removedVariableTree.isPresent() && reassignment.isPresent()) {
      return SuggestedFix.prefixWith( // not needed if top-level statement
          reassignment.get(), state.getSourceForNode(removedVariableTree.get().getType()) + " ");
    }
    return SuggestedFix.emptyFix();
  }

  private static String describeVariable(VarSymbol symbol) {
    return switch (symbol.getKind()) {
      case FIELD -> "field";
      case LOCAL_VARIABLE -> "local variable";
      case PARAMETER -> "parameter";
      default -> "variable";
    };
  }

  private static boolean hasNativeMethods(CompilationUnitTree tree) {
    AtomicBoolean hasAnyNativeMethods = new AtomicBoolean(false);
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitMethod(MethodTree tree, Void unused) {
        if (tree.getModifiers().getFlags().contains(Modifier.NATIVE)) {
          hasAnyNativeMethods.set(true);
        }
        return null;
      }
    }.scan(tree, null);
    return hasAnyNativeMethods.get();
  }

  private static ImmutableMultimap<MethodSymbol, MethodSymbol> getSuperMethodsToOverrides(
      CompilationUnitTree tree, VisitorState state) {
    ImmutableMultimap.Builder<MethodSymbol, MethodSymbol> overrides = ImmutableMultimap.builder();
    JavaFileObject sourceFile = tree.getSourceFile();
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitMethod(MethodTree tree, Void unused) {
        MethodSymbol sym = getSymbol(tree);
        findSuperMethods(getSymbol(tree), state.getTypes()).stream()
            .filter(m -> sourceFile.equals(m.enclClass().sourcefile))
            .forEach(m -> overrides.put(m, sym));
        return null;
      }
    }.scan(tree, null);
    return overrides.build();
  }

  // https://docs.oracle.com/javase/specs/jls/se11/html/jls-14.html#jls-ExpressionStatement
  private static final ImmutableSet<Tree.Kind> TOP_LEVEL_EXPRESSIONS =
      ImmutableSet.of(
          Tree.Kind.ASSIGNMENT,
          Tree.Kind.PREFIX_INCREMENT,
          Tree.Kind.PREFIX_DECREMENT,
          Tree.Kind.POSTFIX_INCREMENT,
          Tree.Kind.POSTFIX_DECREMENT,
          Tree.Kind.METHOD_INVOCATION,
          Tree.Kind.NEW_CLASS);

  private static boolean needsBlock(TreePath path) {
    Tree leaf = path.getLeaf();
    class Visitor extends SimpleTreeVisitor<Boolean, Void> {

      @Override
      public Boolean visitIf(IfTree tree, Void unused) {
        return tree.getThenStatement() == leaf || tree.getElseStatement() == leaf;
      }

      @Override
      public Boolean visitDoWhileLoop(DoWhileLoopTree tree, Void unused) {
        return tree.getStatement() == leaf;
      }

      @Override
      public Boolean visitWhileLoop(WhileLoopTree tree, Void unused) {
        return tree.getStatement() == leaf;
      }

      @Override
      public Boolean visitForLoop(ForLoopTree tree, Void unused) {
        return tree.getStatement() == leaf;
      }

      @Override
      public Boolean visitEnhancedForLoop(EnhancedForLoopTree tree, Void unused) {
        return tree.getStatement() == leaf;
      }
    }
    return firstNonNull(path.getParentPath().getLeaf().accept(new Visitor(), null), false);
  }

  private static ImmutableList<SuggestedFix> buildUnusedVarFixes(
      Symbol varSymbol, List<TreePath> usagePaths, VisitorState state) {
    // Don't suggest a fix for fields annotated @Inject: we can warn on them, but they *could* be
    // used outside the class.
    if (ASTHelpers.hasDirectAnnotationWithSimpleName(varSymbol, "Inject")) {
      return ImmutableList.of();
    }
    ElementKind varKind = varSymbol.getKind();
    boolean encounteredSideEffects = false;
    SuggestedFix.Builder keepSideEffectsFix =
        SuggestedFix.builder().setShortDescription("remove unused variable");
    SuggestedFix.Builder removeSideEffectsFix =
        SuggestedFix.builder().setShortDescription("remove unused variable and any side effects");
    for (TreePath usagePath : usagePaths) {
      StatementTree statement = (StatementTree) usagePath.getLeaf();
      if (statement.getKind() == Kind.VARIABLE) {
        if (getSymbol(statement).getKind() == ElementKind.PARAMETER) {
          continue;
        }
        VariableTree variableTree = (VariableTree) statement;
        ExpressionTree initializer = variableTree.getInitializer();
        if (hasSideEffect(initializer) && TOP_LEVEL_EXPRESSIONS.contains(initializer.getKind())) {
          encounteredSideEffects = true;
          if (varKind == ElementKind.FIELD) {
            String newContent =
                String.format(
                    "%s{ %s; }",
                    isStatic(varSymbol) ? "static " : "", state.getSourceForNode(initializer));
            keepSideEffectsFix.merge(
                SuggestedFixes.replaceIncludingComments(usagePath, newContent, state));
            removeSideEffectsFix.replace(statement, "");
          } else {
            keepSideEffectsFix.replace(
                statement, String.format("%s;", state.getSourceForNode(initializer)));
            removeSideEffectsFix.replace(statement, "");
          }
        } else if (isEnhancedForLoopVar(usagePath)) {
          String modifiers =
              nullToEmpty(
                  variableTree.getModifiers() == null
                      ? null
                      : state.getSourceForNode(variableTree.getModifiers()));
          String newContent =
              String.format(
                  "%s%s unused",
                  modifiers.isEmpty() ? "" : (modifiers + " "),
                  state.getSourceForNode(variableTree.getType()));
          // The new content for the second fix should be identical to the content for the first
          // fix in this case because we can't just remove the enhanced for loop variable.
          keepSideEffectsFix.replace(variableTree, newContent);
          removeSideEffectsFix.replace(variableTree, newContent);
        } else {
          String replacement = needsBlock(usagePath) ? "{}" : "";
          keepSideEffectsFix.merge(
              SuggestedFixes.replaceIncludingComments(usagePath, replacement, state));
          removeSideEffectsFix.merge(
              SuggestedFixes.replaceIncludingComments(usagePath, replacement, state));
        }
        continue;
      } else if (statement.getKind() == Kind.EXPRESSION_STATEMENT) {
        JCTree tree = (JCTree) ((ExpressionStatementTree) statement).getExpression();

        if (tree instanceof CompoundAssignmentTree compoundAssignmentTree) {
          if (hasSideEffect(compoundAssignmentTree.getExpression())) {
            // If it's a compound assignment, there's no reason we'd want to remove the expression,
            // so don't set `encounteredSideEffects` based on this usage.
            SuggestedFix replacement =
                SuggestedFix.replace(
                    tree.getStartPosition(),
                    ((JCAssignOp) tree).getExpression().getStartPosition(),
                    "");
            keepSideEffectsFix.merge(replacement);
            removeSideEffectsFix.merge(replacement);
            continue;
          }
        } else if (tree instanceof AssignmentTree assignmentTree) {
          if (hasSideEffect(assignmentTree.getExpression())) {
            encounteredSideEffects = true;
            keepSideEffectsFix.replace(
                tree.getStartPosition(), ((JCAssign) tree).getExpression().getStartPosition(), "");
            removeSideEffectsFix.replace(statement, "");
            continue;
          }
        }
      }
      String replacement = needsBlock(usagePath) ? "{}" : "";
      keepSideEffectsFix.replace(statement, replacement);
      removeSideEffectsFix.replace(statement, replacement);
    }
    return encounteredSideEffects
        ? ImmutableList.of(removeSideEffectsFix.build(), keepSideEffectsFix.build())
        : ImmutableList.of(keepSideEffectsFix.build());
  }

  private static ImmutableList<SuggestedFix> buildUnusedParameterFixes(
      Symbol varSymbol, List<TreePath> usagePaths, VisitorState state) {
    if (!(varSymbol.owner instanceof MethodSymbol methodSymbol)
        || !methodSymbol.params().contains(varSymbol)
        || !canBeRemoved(varSymbol.owner, state)) {
      // We're presumably in a lambda. Don't try to generate a fix.
      return ImmutableList.of();
    }
    int index = methodSymbol.params.indexOf(varSymbol);
    RangeSet<Integer> deletions = TreeRangeSet.create();
    for (TreePath path : usagePaths) {
      deletions.add(
          Range.closed(getStartPosition(path.getLeaf()), state.getEndPosition(path.getLeaf())));
    }
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
        if (getSymbol(tree).equals(methodSymbol)) {
          removeByIndex(tree.getArguments());
        }
        return super.visitMethodInvocation(tree, null);
      }

      @Override
      public Void visitMethod(MethodTree tree, Void unused) {
        if (getSymbol(tree).equals(methodSymbol)) {
          removeByIndex(tree.getParameters());
        }
        return super.visitMethod(tree, null);
      }

      private void removeByIndex(List<? extends Tree> trees) {
        if (index >= trees.size()) {
          // possible when removing a varargs parameter with no corresponding formal parameters
          return;
        }
        if (trees.size() == 1) {
          Tree tree = getOnlyElement(trees);
          if (!hasExplicitSource(tree, state)) {
            // TODO(b/118437729): handle bogus source positions in enum declarations
            return;
          }
          deletions.add(Range.closed(getStartPosition(tree), state.getEndPosition(tree)));
          return;
        }
        int startPos;
        int endPos;
        if (index >= 1) {
          startPos = state.getEndPosition(trees.get(index - 1));
          endPos = state.getEndPosition(trees.get(index));
        } else {
          startPos = getStartPosition(trees.get(index));
          endPos = getStartPosition(trees.get(index + 1));
        }
        if (index == methodSymbol.params().size() - 1 && methodSymbol.isVarArgs()) {
          endPos = state.getEndPosition(getLast(trees));
        }
        if (startPos == Position.NOPOS || endPos == Position.NOPOS) {
          // TODO(b/118437729): handle bogus source positions in enum declarations
          return;
        }
        deletions.add(Range.closed(startPos, endPos));
      }
    }.scan(state.getPath().getCompilationUnit(), null);
    SuggestedFix.Builder fix = SuggestedFix.builder();
    deletions.asRanges().forEach(x -> fix.replace(x.lowerEndpoint(), x.upperEndpoint(), ""));
    return ImmutableList.of(fix.build());
  }

  private static boolean isEnhancedForLoopVar(TreePath variablePath) {
    Tree tree = variablePath.getLeaf();
    Tree parent = variablePath.getParentPath().getLeaf();
    return parent instanceof EnhancedForLoopTree enhancedForLoopTree
        && enhancedForLoopTree.getVariable() == tree;
  }

  /**
   * Looks at the list of {@code annotations} and see if there is any annotation which exists {@code
   * exemptingAnnotations}.
   */
  private static boolean exemptedByAnnotation(List<? extends AnnotationTree> annotations) {
    for (AnnotationTree annotation : annotations) {
      Type annotationType = ASTHelpers.getType(annotation);
      if (annotationType == null) {
        continue;
      }
      TypeSymbol tsym = annotationType.tsym;
      if (EXEMPTING_VARIABLE_ANNOTATIONS.contains(tsym.getQualifiedName().toString())) {
        return true;
      }
    }
    return false;
  }

  private boolean exemptedByName(Name name) {
    String nameString = name.toString();
    String nameStringLower = Ascii.toLowerCase(nameString);
    return exemptPrefixes.stream().anyMatch(nameStringLower::startsWith)
        || exemptNames.contains(nameString);
  }

  private class VariableFinder extends TreePathScanner<Void, Void> {
    private final Map<Symbol, TreePath> unusedElements = new HashMap<>();

    private final Set<Symbol> onlyCheckForReassignments = new HashSet<>();

    private final ListMultimap<Symbol, TreePath> usageSites = ArrayListMultimap.create();

    private final VisitorState state;
    private final ImmutableMultimap<MethodSymbol, MethodSymbol> superMethodsToOverrides;

    private VariableFinder(
        VisitorState state, ImmutableMultimap<MethodSymbol, MethodSymbol> superMethodsToOverrides) {
      this.state = state;
      this.superMethodsToOverrides = superMethodsToOverrides;
    }

    @Override
    public Void visitVariable(VariableTree variableTree, Void unused) {
      handleVariable(variableTree);
      return super.visitVariable(variableTree, null);
    }

    private void handleVariable(VariableTree variableTree) {
      if (exemptedByName(variableTree.getName())) {
        return;
      }
      if (isSuppressed(variableTree, state)) {
        return;
      }
      VarSymbol symbol = getSymbol(variableTree);
      var parent = getCurrentPath().getParentPath().getLeaf();
      if (parent instanceof LambdaExpressionTree) {
        if (FUNCTIONAL_INTERFACE_TYPES_TO_CHECK.stream()
            .anyMatch(t -> isSubtype(getType(parent), state.getTypeFromString(t), state))) {
          unusedElements.put(symbol, getCurrentPath());
          usageSites.put(symbol, getCurrentPath());
        }
        return;
      }
      if (symbol.getKind() == ElementKind.FIELD
          && symbol.getSimpleName().contentEquals("CREATOR")
          && isSubtype(symbol.type, PARCELABLE_CREATOR.get(state), state)) {
        return;
      }
      if (symbol.getKind() == ElementKind.FIELD
          && exemptedFieldBySuperType(getType(variableTree), state)) {
        return;
      }
      // Return if the element is exempted by an annotation.
      if (exemptedByAnnotation(variableTree.getModifiers().getAnnotations())
          || shouldKeep(variableTree)) {
        return;
      }
      switch (symbol.getKind()) {
        case FIELD -> {
          // We are only interested in private fields and those which are not special.
          if (isFieldEligibleForChecking(variableTree, symbol)) {
            unusedElements.put(symbol, getCurrentPath());
            usageSites.put(symbol, getCurrentPath());
          }
        }
        case LOCAL_VARIABLE -> {
          unusedElements.put(symbol, getCurrentPath());
          usageSites.put(symbol, getCurrentPath());
        }
        case PARAMETER -> {
          // ignore the receiver parameter
          if (variableTree.getName().contentEquals("this")) {
            return;
          }
          // Ignore if parameter is part of canonical record constructor; tree does not seem
          // to contain usage in that case, but parameter is always used implicitly
          // For compact canonical constructor parameters don't have record flag so need to
          // check constructor flags (`symbol.owner`) instead
          if (hasRecordFlag(symbol.owner)) {
            return;
          }
          unusedElements.put(symbol, getCurrentPath());
          if (!isParameterSubjectToAnalysis(symbol)) {
            onlyCheckForReassignments.add(symbol);
          }
        }
        default -> {}
      }
    }

    private boolean exemptedFieldBySuperType(Type type, VisitorState state) {
      return EXEMPTING_FIELD_SUPER_TYPES.stream()
          .anyMatch(t -> isSubtype(type, state.getTypeFromString(t), state));
    }

    private boolean isFieldEligibleForChecking(VariableTree variableTree, VarSymbol symbol) {
      if (reportInjectedFields
          && variableTree.getModifiers().getFlags().isEmpty()
          && ASTHelpers.hasDirectAnnotationWithSimpleName(variableTree, "Inject")) {
        return true;
      }
      if (hasRecordFlag(symbol)) {
        return false;
      }
      return canBeRemoved(symbol) && !SPECIAL_FIELDS.contains(symbol.getSimpleName().toString());
    }

    private static final long RECORD_FLAG = 1L << 61;

    private boolean hasRecordFlag(Symbol symbol) {
      return (symbol.flags() & RECORD_FLAG) == RECORD_FLAG;
    }

    /** Returns whether {@code sym} can be removed without updating call sites in other files. */
    private boolean isParameterSubjectToAnalysis(Symbol sym) {
      checkArgument(sym.getKind() == ElementKind.PARAMETER);
      Symbol enclosingMethod = sym.owner;

      if (!(enclosingMethod instanceof MethodSymbol methodSymbol)
          || isAbstract(methodSymbol)
          || superMethodsToOverrides.containsKey(enclosingMethod)) {
        return false;
      }

      if (methodAnnotationsExemptingParameters.stream()
          .anyMatch(anno -> hasAnnotation(enclosingMethod, anno, state))) {
        return false;
      }

      if (ANNOTATIONS_INDICATING_PARAMETERS_SHOULD_BE_CHECKED.stream()
          .anyMatch(a -> hasAnnotation(enclosingMethod, a, state))) {
        return true;
      }

      if (enclosingMethod.owner instanceof ClassSymbol
          && !isAbstract(methodSymbol)
          && FUNCTIONAL_INTERFACE_TYPES_TO_CHECK.stream()
              .map(state::getTypeFromString)
              .anyMatch(
                  t ->
                      isSubtype(enclosingMethod.owner.type, t, state)
                          && isFunctionalInterfaceMethod(t, enclosingMethod, state))) {
        return true;
      }

      return canBeRemoved(enclosingMethod, state);
    }

    private boolean isFunctionalInterfaceMethod(
        Type functionalInterfaceType, Symbol method, VisitorState state) {
      var functionalInterfaceMethod =
          state.getTypes().findDescriptorSymbol(functionalInterfaceType.asElement());
      return method.getSimpleName().contentEquals(functionalInterfaceMethod.getSimpleName())
          && method.overrides(
              functionalInterfaceMethod, method.owner.type.tsym, state.getTypes(), true);
    }

    @Override
    public Void visitTry(TryTree node, Void unused) {
      // Skip resources, as while these may not be referenced, they are used.
      scan(node.getBlock(), null);
      scan(node.getCatches(), null);
      scan(node.getFinallyBlock(), null);
      return null;
    }

    @Override
    public Void visitClass(ClassTree tree, Void unused) {
      if (isSuppressed(tree, state)) {
        return null;
      }
      if (EXEMPTING_SUPER_TYPES.stream()
          .anyMatch(t -> isSubtype(getType(tree), Suppliers.typeFromString(t).get(state), state))) {
        return null;
      }
      return super.visitClass(tree, null);
    }

    @Override
    public Void visitMethod(MethodTree tree, Void unused) {
      if (SERIALIZATION_METHODS.matches(tree, state)) {
        return scan(tree.getBody(), null);
      }
      return isSuppressed(tree, state) ? null : super.visitMethod(tree, null);
    }
  }

  private static final class FilterUsedVariables extends TreePathScanner<Void, Void> {
    private boolean leftHandSideAssignment = false;
    // When this greater than zero, the usage of identifiers are real.
    private int inArrayAccess = 0;
    // This is true when we are processing a `return` statement. Elements used in return statement
    // must not be considered unused.
    private boolean inReturnStatement = false;
    // When this greater than zero, the usage of identifiers are real because they are in a method
    // call.
    private int inMethodCall = 0;

    private final Map<Symbol, TreePath> assignmentSite = new HashMap<>();

    private TreePath currentExpressionStatement = null;

    private final VisitorState state;

    private final Map<Symbol, TreePath> unusedElements;

    private final ListMultimap<Symbol, TreePath> usageSites;

    // Keeps track of whether a symbol was _ever_ used (between reassignments).
    private final Set<Symbol> isEverUsed = new HashSet<>();

    private final List<UnusedSpec> unusedSpecs = new ArrayList<>();

    private final ImmutableMap<Symbol, TreePath> declarationSites;

    private FilterUsedVariables(
        VisitorState state,
        Map<Symbol, TreePath> unusedElements,
        ListMultimap<Symbol, TreePath> usageSites) {
      this.state = state;
      this.unusedElements = unusedElements;
      this.usageSites = usageSites;
      this.declarationSites = ImmutableMap.copyOf(unusedElements);
    }

    private boolean isInExpressionStatementTree() {
      Tree parent = getCurrentPath().getParentPath().getLeaf();
      return parent != null && parent.getKind() == Kind.EXPRESSION_STATEMENT;
    }

    private boolean isUsed(@Nullable Symbol symbol) {
      return symbol != null
          && (!leftHandSideAssignment || inReturnStatement || inArrayAccess > 0 || inMethodCall > 0)
          && unusedElements.containsKey(symbol);
    }

    @Override
    public Void visitVariable(VariableTree tree, Void unused) {
      VarSymbol symbol = getSymbol(tree);
      if (hasBeenAssigned(tree, symbol)) {
        assignmentSite.put(symbol, getCurrentPath());
      }
      return super.visitVariable(tree, null);
    }

    private boolean hasBeenAssigned(VariableTree tree, VarSymbol symbol) {
      if (symbol == null) {
        return false;
      }
      // Parameters and enhanced for loop variables are always considered assigned.
      if (symbol.getKind() == ElementKind.PARAMETER) {
        return true;
      }
      if (getCurrentPath().getParentPath().getLeaf() instanceof EnhancedForLoopTree) {
        return true;
      }
      // Otherwise it's assigned if the VariableTree has an initializer.
      if (unusedElements.containsKey(symbol) && tree.getInitializer() != null) {
        return true;
      }
      return false;
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatementTree tree, Void unused) {
      currentExpressionStatement = getCurrentPath();
      super.visitExpressionStatement(tree, null);
      currentExpressionStatement = null;
      return null;
    }

    @Override
    public Void visitIdentifier(IdentifierTree tree, Void unused) {
      Symbol symbol = getSymbol(tree);
      // Filtering out identifier symbol from vars map. These are real usages of identifiers.
      if (isUsed(symbol)) {
        unusedElements.remove(symbol);
      }
      if (currentExpressionStatement != null && unusedElements.containsKey(symbol)) {
        usageSites.put(symbol, currentExpressionStatement);
      }
      return null;
    }

    @Override
    public Void visitAssignment(AssignmentTree tree, Void unused) {
      scan(tree.getExpression(), null);
      // If a variable is used in the left hand side of an assignment that does not count as a
      // usage.
      if (isInExpressionStatementTree()) {
        handleReassignment(tree);
        leftHandSideAssignment = true;
        scan(tree.getVariable(), null);
        leftHandSideAssignment = false;
      } else {
        super.visitAssignment(tree, null);
      }
      return null;
    }

    /**
     * Deals with assignment trees; works out if the assignment definitely overwrites the variable
     * in all ways that could be observed as we scan forwards.
     */
    private void handleReassignment(AssignmentTree tree) {
      Tree parent = getCurrentPath().getParentPath().getLeaf();
      if (!(parent instanceof StatementTree)) {
        return;
      }
      if (tree.getVariable().getKind() != Kind.IDENTIFIER) {
        return;
      }
      if (ASTHelpers.findEnclosingNode(getCurrentPath(), ForLoopTree.class) != null) {
        return;
      }
      Symbol symbol = getSymbol(tree.getVariable());
      // Check if it was actually assigned to at this depth (or is a parameter).
      if (!((assignmentSite.containsKey(symbol) && symbol.getKind() == ElementKind.LOCAL_VARIABLE)
          || symbol.getKind() == ElementKind.PARAMETER)) {
        return;
      }
      // Don't regard assigning `null` as a potentially unused assignment, as people do this for GC
      // reasons.
      if (getType(tree.getExpression()) instanceof NullType) {
        return;
      }
      TreePath lastAssignmentSite = assignmentSite.get(symbol);
      if (lastAssignmentSite == null) {
        return;
      }
      TreePath declarationSite = declarationSites.get(symbol);
      if (declarationSite == null) {
        return;
      }
      if (scopeDepth(declarationSite) != Iterables.size(getCurrentPath().getParentPath())) {
        return;
      }
      if (unusedElements.containsKey(symbol)) {
        unusedSpecs.add(UnusedSpec.of(symbol, lastAssignmentSite, usageSites.get(symbol), tree));
      } else {
        isEverUsed.add(symbol);
      }
      unusedElements.put(symbol, getCurrentPath());
      usageSites.removeAll(symbol);
      usageSites.put(symbol, getCurrentPath().getParentPath());
      assignmentSite.put(symbol, getCurrentPath().getParentPath());
    }

    // This is a crude proxy for when a variable is unconditionally overwritten. It doesn't match
    // all cases, but it catches a reassignment at the same depth.
    private static int scopeDepth(TreePath assignmentSite) {
      if (assignmentSite.getParentPath().getLeaf() instanceof EnhancedForLoopTree) {
        return Iterables.size(assignmentSite) + 1;
      }
      if (assignmentSite.getLeaf() instanceof VariableTree) {
        VarSymbol symbol = getSymbol((VariableTree) assignmentSite.getLeaf());
        if (symbol.getKind() == ElementKind.PARAMETER) {
          return Iterables.size(assignmentSite) + 1;
        }
      }
      return Iterables.size(assignmentSite);
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree memberSelectTree, Void unused) {
      Symbol symbol = getSymbol(memberSelectTree);
      if (isUsed(symbol)) {
        unusedElements.remove(symbol);
      } else if (currentExpressionStatement != null && unusedElements.containsKey(symbol)) {
        usageSites.put(symbol, currentExpressionStatement);
      }
      // Clear leftHandSideAssignment and descend down the tree to catch any variables in the
      // receiver of this member select, which _are_ considered used.
      boolean wasLeftHandAssignment = leftHandSideAssignment;
      leftHandSideAssignment = false;
      super.visitMemberSelect(memberSelectTree, null);
      leftHandSideAssignment = wasLeftHandAssignment;
      return null;
    }

    @Override
    public Void visitMemberReference(MemberReferenceTree tree, Void unused) {
      super.visitMemberReference(tree, null);
      MethodSymbol symbol = getSymbol(tree);
      symbol.getParameters().forEach(unusedElements::remove);
      return null;
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree tree, Void unused) {
      if (isInExpressionStatementTree()) {
        leftHandSideAssignment = true;
        scan(tree.getVariable(), null);
        leftHandSideAssignment = false;
        scan(tree.getExpression(), null);
      } else {
        super.visitCompoundAssignment(tree, null);
      }
      return null;
    }

    @Override
    public Void visitArrayAccess(ArrayAccessTree node, Void unused) {
      inArrayAccess++;
      super.visitArrayAccess(node, null);
      inArrayAccess--;
      return null;
    }

    @Override
    public Void visitReturn(ReturnTree node, Void unused) {
      inReturnStatement = true;
      scan(node.getExpression(), null);
      inReturnStatement = false;
      return null;
    }

    @Override
    public Void visitUnary(UnaryTree tree, Void unused) {
      // If unary expression is inside another expression, then this is a real usage of unary
      // operand.
      // Example:
      //   array[i++] = 0; // 'i' has a real usage here. 'array' might not have.
      //   list.get(i++);
      // But if it is like this:
      //   i++;
      // Then it is possible that this is not a real usage of 'i'.
      if (isInExpressionStatementTree()
          && (tree.getKind() == POSTFIX_DECREMENT
              || tree.getKind() == POSTFIX_INCREMENT
              || tree.getKind() == PREFIX_DECREMENT
              || tree.getKind() == PREFIX_INCREMENT)) {
        leftHandSideAssignment = true;
        scan(tree.getExpression(), null);
        leftHandSideAssignment = false;
      } else {
        super.visitUnary(tree, null);
      }
      return null;
    }

    @Override
    public Void visitErroneous(ErroneousTree tree, Void unused) {
      return scan(tree.getErrorTrees(), null);
    }

    /**
     * Looks at method invocations and removes the invoked private methods from {@code
     * #unusedElements}.
     */
    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
      inMethodCall++;
      super.visitMethodInvocation(tree, null);
      inMethodCall--;
      return null;
    }

    @Override
    public Void visitMethod(final MethodTree node, final Void unused) {
      handleFieldSource(node);
      return super.visitMethod(node, unused);
    }

    /**
     * If a method is annotated with @FieldSource, the annotation value refers to a field that is
     * used reflectively to supply test parameters, so that field should not be considered unused.
     */
    private void handleFieldSource(MethodTree tree) {
      MethodSymbol sym = getSymbol(tree);
      Name name = ORG_JUNIT_JUPITER_PARAMS_PROVIDER_FIELDSOURCE.get(state);
      sym.getRawAttributes().stream()
          .filter(a -> a.type.tsym.getQualifiedName().equals(name))
          .findAny()
          // get the annotation value array as a set of Names
          .flatMap(a -> getAnnotationValue(a, "value"))
          .map(y -> asStrings(y).map(state::getName).map(Name::toString).collect(toImmutableSet()))
          // remove all potentially unused methods referenced by the @FieldSource
          .ifPresent(
              referencedNames ->
                  unusedElements
                      .entrySet()
                      .removeIf(
                          e -> {
                            Symbol unusedSym = e.getKey();
                            String simpleName = unusedSym.getSimpleName().toString();
                            return referencedNames.contains(simpleName)
                                || referencedNames.contains(
                                    unusedSym.owner.getQualifiedName() + "#" + simpleName);
                          }));
    }
  }

  @AutoValue
  abstract static class UnusedSpec {
    /** {@link Symbol} of the unused element. */
    abstract Symbol symbol();

    /** {@link VariableTree} or {@link AssignmentTree} for the original assignment site. */
    abstract TreePath assignmentPath();

    /**
     * All the usage sites of this variable that we claim are unused (including the initial
     * declaration/assignment).
     */
    abstract ImmutableList<TreePath> usageSites();

    /**
     * If this usage chain was terminated by an unconditional reassignment, the corresponding {@link
     * AssignmentTree}.
     */
    abstract Optional<AssignmentTree> terminatingAssignment();

    private static UnusedSpec of(
        Symbol symbol,
        TreePath assignmentPath,
        Iterable<TreePath> treePaths,
        @Nullable AssignmentTree assignmentTree) {
      return new AutoValue_UnusedVariable_UnusedSpec(
          symbol,
          assignmentPath,
          ImmutableList.copyOf(treePaths),
          Optional.ofNullable(assignmentTree));
    }
  }

  private static final Supplier<Type> PARCELABLE_CREATOR =
      VisitorState.memoize(state -> state.getTypeFromString("android.os.Parcelable.Creator"));

  private static final Supplier<com.sun.tools.javac.util.Name>
          ORG_JUNIT_JUPITER_PARAMS_PROVIDER_FIELDSOURCE =
          VisitorState.memoize(
              state -> state.getName("org.junit.jupiter.params.provider.FieldSource"));
}
