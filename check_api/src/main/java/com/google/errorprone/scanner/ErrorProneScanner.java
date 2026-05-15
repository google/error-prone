/*
 * Copyright 2011 The Error Prone Authors.
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

package com.google.errorprone.scanner;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.ErrorProneError;
import com.google.errorprone.ErrorProneOptions;
import com.google.errorprone.SourcePositionException;
import com.google.errorprone.SuppressionInfo.SuppressedState;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.AnnotatedTypeTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ArrayAccessTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ArrayTypeTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.AssertTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.AssignmentTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.BindingPatternTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.BlockTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.BreakTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.CaseTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.CatchTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.CompoundAssignmentTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ConditionalExpressionTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ConstantCaseLabelTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ContinueTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.DeconstructionPatternTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.DefaultCaseLabelTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.DoWhileLoopTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.EmptyStatementTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.EnhancedForLoopTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ExportsTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ExpressionStatementTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ForLoopTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.IdentifierTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.IfTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ImportTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.InstanceOfTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.IntersectionTypeTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.LabeledStatementTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.LambdaExpressionTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.LiteralTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MemberSelectTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ModifiersTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ModuleTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewArrayTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.OpensTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.PackageTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ParameterizedTypeTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ParenthesizedTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.PatternCaseLabelTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.PrimitiveTypeTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ProvidesTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.RequiresTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.SwitchExpressionTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.SwitchTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.SynchronizedTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ThrowTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.TryTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.TypeCastTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.TypeParameterTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.UnaryTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.UnionTypeTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.UsesTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.WhileLoopTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.WildcardTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.YieldTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Suppressible;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BindingPatternTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ConstantCaseLabelTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.DeconstructionPatternTree;
import com.sun.source.tree.DefaultCaseLabelTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExportsTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.IntersectionTypeTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.OpensTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PatternCaseLabelTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.ProvidesTree;
import com.sun.source.tree.RequiresTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.UsesTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.tree.WildcardTree;
import com.sun.source.tree.YieldTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Name;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.tools.JavaFileObject;

/**
 * Scans the parsed AST, looking for violations of any of the enabled checks.
 *
 * @author Alex Eagle (alexeagle@google.com)
 */
public class ErrorProneScanner extends Scanner {

  private final com.google.errorprone.suppliers.Supplier<? extends Set<? extends Name>>
      customSuppressionAnnotations;

  private final Map<String, SeverityLevel> severities;
  private final ImmutableSet<BugChecker> bugCheckers;

  /**
   * Create an error-prone scanner for the given checkers.
   *
   * @param checkers The checkers that this scanner should use.
   */
  public ErrorProneScanner(BugChecker... checkers) {
    this(Arrays.asList(checkers));
  }

  /**
   * Create an error-prone scanner for a non-hardcoded set of checkers.
   *
   * @param checkers The checkers that this scanner should use.
   */
  public ErrorProneScanner(Iterable<BugChecker> checkers) {
    this(checkers, defaultSeverities(checkers));
  }

  /**
   * Create an error-prone scanner for a non-hardcoded set of checkers.
   *
   * @param checkers The checkers that this scanner should use.
   * @param severities The default check severities.
   */
  public ErrorProneScanner(Iterable<BugChecker> checkers, Map<String, SeverityLevel> severities) {
    this.bugCheckers = ImmutableSet.copyOf(checkers);
    this.severities = severities;
    ImmutableSet.Builder<Class<? extends Annotation>> annotationClassesBuilder =
        ImmutableSet.builder();
    for (BugChecker checker : this.bugCheckers) {
      registerNodeTypes(checker, annotationClassesBuilder);
    }
    ImmutableSet<Class<? extends Annotation>> annotationClasses = annotationClassesBuilder.build();
    this.customSuppressionAnnotations =
        VisitorState.memoize(
            state -> {
              ImmutableSet.Builder<Name> builder = ImmutableSet.builder();
              for (Class<? extends Annotation> annotation : annotationClasses) {
                builder.add(state.getName(annotation.getName()));
              }
              return builder.build();
            });
  }

  private static ImmutableMap<String, BugPattern.SeverityLevel> defaultSeverities(
      Iterable<BugChecker> checkers) {
    ImmutableMap.Builder<String, BugPattern.SeverityLevel> builder = ImmutableMap.builder();
    for (BugChecker check : checkers) {
      builder.put(check.canonicalName(), check.defaultSeverity());
    }
    return builder.buildOrThrow();
  }

  @Override
  protected Set<? extends Name> getCustomSuppressionAnnotations(VisitorState state) {
    return customSuppressionAnnotations.get(state);
  }

  // keep-sorted start
  private final List<AnnotatedTypeTreeMatcher> annotatedTypeMatchers = new ArrayList<>();
  private final List<AnnotationTreeMatcher> annotationMatchers = new ArrayList<>();
  private final List<ArrayAccessTreeMatcher> arrayAccessMatchers = new ArrayList<>();
  private final List<ArrayTypeTreeMatcher> arrayTypeMatchers = new ArrayList<>();
  private final List<AssertTreeMatcher> assertMatchers = new ArrayList<>();
  private final List<AssignmentTreeMatcher> assignmentMatchers = new ArrayList<>();
  private final List<BinaryTreeMatcher> binaryMatchers = new ArrayList<>();
  private final List<BindingPatternTreeMatcher> bindingPatternMatchers = new ArrayList<>();
  private final List<BlockTreeMatcher> blockMatchers = new ArrayList<>();
  private final List<BreakTreeMatcher> breakMatchers = new ArrayList<>();
  private final List<CaseTreeMatcher> caseMatchers = new ArrayList<>();
  private final List<CatchTreeMatcher> catchMatchers = new ArrayList<>();
  private final List<ClassTreeMatcher> classMatchers = new ArrayList<>();
  private final List<CompilationUnitTreeMatcher> compilationUnitMatchers = new ArrayList<>();
  private final List<CompoundAssignmentTreeMatcher> compoundAssignmentMatchers = new ArrayList<>();
  private final List<ConditionalExpressionTreeMatcher> conditionalExpressionMatchers =
      new ArrayList<>();
  private final List<ConstantCaseLabelTreeMatcher> constantCaseLabelMatchers = new ArrayList<>();
  private final List<ContinueTreeMatcher> continueMatchers = new ArrayList<>();
  private final List<DeconstructionPatternTreeMatcher> deconstructionPatternMatchers =
      new ArrayList<>();
  private final List<DefaultCaseLabelTreeMatcher> defaultCaseLabelMatchers = new ArrayList<>();
  private final List<DoWhileLoopTreeMatcher> doWhileLoopMatchers = new ArrayList<>();
  private final List<EmptyStatementTreeMatcher> emptyStatementMatchers = new ArrayList<>();
  private final List<EnhancedForLoopTreeMatcher> enhancedForLoopMatchers = new ArrayList<>();
  private final List<ExportsTreeMatcher> exportsMatchers = new ArrayList<>();
  private final List<ExpressionStatementTreeMatcher> expressionStatementMatchers =
      new ArrayList<>();
  private final List<ForLoopTreeMatcher> forLoopMatchers = new ArrayList<>();
  private final List<IdentifierTreeMatcher> identifierMatchers = new ArrayList<>();
  private final List<IfTreeMatcher> ifMatchers = new ArrayList<>();
  private final List<ImportTreeMatcher> importMatchers = new ArrayList<>();
  private final List<InstanceOfTreeMatcher> instanceOfMatchers = new ArrayList<>();
  private final List<IntersectionTypeTreeMatcher> intersectionTypeMatchers = new ArrayList<>();
  private final List<LabeledStatementTreeMatcher> labeledStatementMatchers = new ArrayList<>();
  private final List<LambdaExpressionTreeMatcher> lambdaExpressionMatchers = new ArrayList<>();
  private final List<LiteralTreeMatcher> literalMatchers = new ArrayList<>();
  private final List<MemberReferenceTreeMatcher> memberReferenceMatchers = new ArrayList<>();
  private final List<MemberSelectTreeMatcher> memberSelectMatchers = new ArrayList<>();
  private final List<MethodInvocationTreeMatcher> methodInvocationMatchers = new ArrayList<>();
  private final List<MethodTreeMatcher> methodMatchers = new ArrayList<>();
  private final List<ModifiersTreeMatcher> modifiersMatchers = new ArrayList<>();
  private final List<ModuleTreeMatcher> moduleMatchers = new ArrayList<>();
  private final List<NewArrayTreeMatcher> newArrayMatchers = new ArrayList<>();
  private final List<NewClassTreeMatcher> newClassMatchers = new ArrayList<>();
  private final List<OpensTreeMatcher> opensMatchers = new ArrayList<>();
  private final List<PackageTreeMatcher> packageMatchers = new ArrayList<>();
  private final List<ParameterizedTypeTreeMatcher> parameterizedTypeMatchers = new ArrayList<>();
  private final List<ParenthesizedTreeMatcher> parenthesizedMatchers = new ArrayList<>();
  private final List<PatternCaseLabelTreeMatcher> patternCaseLabelMatchers = new ArrayList<>();
  private final List<PrimitiveTypeTreeMatcher> primitiveTypeMatchers = new ArrayList<>();
  private final List<ProvidesTreeMatcher> providesMatchers = new ArrayList<>();
  private final List<RequiresTreeMatcher> requiresMatchers = new ArrayList<>();
  private final List<ReturnTreeMatcher> returnMatchers = new ArrayList<>();
  private final List<SwitchExpressionTreeMatcher> switchExpressionMatchers = new ArrayList<>();
  private final List<SwitchTreeMatcher> switchMatchers = new ArrayList<>();
  private final List<SynchronizedTreeMatcher> synchronizedMatchers = new ArrayList<>();
  private final List<ThrowTreeMatcher> throwMatchers = new ArrayList<>();
  private final List<TryTreeMatcher> tryMatchers = new ArrayList<>();
  private final List<TypeCastTreeMatcher> typeCastMatchers = new ArrayList<>();
  private final List<TypeParameterTreeMatcher> typeParameterMatchers = new ArrayList<>();
  private final List<UnaryTreeMatcher> unaryMatchers = new ArrayList<>();
  private final List<UnionTypeTreeMatcher> unionTypeMatchers = new ArrayList<>();
  private final List<UsesTreeMatcher> usesMatchers = new ArrayList<>();
  private final List<VariableTreeMatcher> variableMatchers = new ArrayList<>();
  private final List<WhileLoopTreeMatcher> whileLoopMatchers = new ArrayList<>();
  private final List<WildcardTreeMatcher> wildcardMatchers = new ArrayList<>();
  private final List<YieldTreeMatcher> yieldMatchers = new ArrayList<>();

  // keep-sorted end

  private void registerNodeTypes(
      BugChecker checker,
      ImmutableSet.Builder<Class<? extends Annotation>> customSuppressionAnnotationClasses) {
    customSuppressionAnnotationClasses.addAll(checker.customSuppressionAnnotations());

    // keep-sorted start
    if (checker instanceof AnnotatedTypeTreeMatcher annotatedTypeTreeMatcher) {
      annotatedTypeMatchers.add(annotatedTypeTreeMatcher);
    }
    if (checker instanceof AnnotationTreeMatcher annotationTreeMatcher) {
      annotationMatchers.add(annotationTreeMatcher);
    }
    if (checker instanceof ArrayAccessTreeMatcher arrayAccessTreeMatcher) {
      arrayAccessMatchers.add(arrayAccessTreeMatcher);
    }
    if (checker instanceof ArrayTypeTreeMatcher arrayTypeTreeMatcher) {
      arrayTypeMatchers.add(arrayTypeTreeMatcher);
    }
    if (checker instanceof AssertTreeMatcher assertTreeMatcher) {
      assertMatchers.add(assertTreeMatcher);
    }
    if (checker instanceof AssignmentTreeMatcher assignmentTreeMatcher) {
      assignmentMatchers.add(assignmentTreeMatcher);
    }
    if (checker instanceof BinaryTreeMatcher binaryTreeMatcher) {
      binaryMatchers.add(binaryTreeMatcher);
    }
    if (checker instanceof BindingPatternTreeMatcher bindingPatternTreeMatcher) {
      bindingPatternMatchers.add(bindingPatternTreeMatcher);
    }
    if (checker instanceof BlockTreeMatcher blockTreeMatcher) {
      blockMatchers.add(blockTreeMatcher);
    }
    if (checker instanceof BreakTreeMatcher breakTreeMatcher) {
      breakMatchers.add(breakTreeMatcher);
    }
    if (checker instanceof CaseTreeMatcher caseTreeMatcher) {
      caseMatchers.add(caseTreeMatcher);
    }
    if (checker instanceof CatchTreeMatcher catchTreeMatcher) {
      catchMatchers.add(catchTreeMatcher);
    }
    if (checker instanceof ClassTreeMatcher classTreeMatcher) {
      classMatchers.add(classTreeMatcher);
    }
    if (checker instanceof CompilationUnitTreeMatcher compilationUnitTreeMatcher) {
      compilationUnitMatchers.add(compilationUnitTreeMatcher);
    }
    if (checker instanceof CompoundAssignmentTreeMatcher compoundAssignmentTreeMatcher) {
      compoundAssignmentMatchers.add(compoundAssignmentTreeMatcher);
    }
    if (checker instanceof ConditionalExpressionTreeMatcher conditionalExpressionTreeMatcher) {
      conditionalExpressionMatchers.add(conditionalExpressionTreeMatcher);
    }
    if (checker instanceof ConstantCaseLabelTreeMatcher constantCaseLabelTreeMatcher) {
      constantCaseLabelMatchers.add(constantCaseLabelTreeMatcher);
    }
    if (checker instanceof ContinueTreeMatcher continueTreeMatcher) {
      continueMatchers.add(continueTreeMatcher);
    }
    if (checker instanceof DeconstructionPatternTreeMatcher deconstructionPatternTreeMatcher) {
      deconstructionPatternMatchers.add(deconstructionPatternTreeMatcher);
    }
    if (checker instanceof DefaultCaseLabelTreeMatcher defaultCaseLabelTreeMatcher) {
      defaultCaseLabelMatchers.add(defaultCaseLabelTreeMatcher);
    }
    if (checker instanceof DoWhileLoopTreeMatcher doWhileLoopTreeMatcher) {
      doWhileLoopMatchers.add(doWhileLoopTreeMatcher);
    }
    if (checker instanceof EmptyStatementTreeMatcher emptyStatementTreeMatcher) {
      emptyStatementMatchers.add(emptyStatementTreeMatcher);
    }
    if (checker instanceof EnhancedForLoopTreeMatcher enhancedForLoopTreeMatcher) {
      enhancedForLoopMatchers.add(enhancedForLoopTreeMatcher);
    }
    if (checker instanceof ExportsTreeMatcher exportsTreeMatcher) {
      exportsMatchers.add(exportsTreeMatcher);
    }
    if (checker instanceof ExpressionStatementTreeMatcher expressionStatementTreeMatcher) {
      expressionStatementMatchers.add(expressionStatementTreeMatcher);
    }
    if (checker instanceof ForLoopTreeMatcher forLoopTreeMatcher) {
      forLoopMatchers.add(forLoopTreeMatcher);
    }
    if (checker instanceof IdentifierTreeMatcher identifierTreeMatcher) {
      identifierMatchers.add(identifierTreeMatcher);
    }
    if (checker instanceof IfTreeMatcher ifTreeMatcher) {
      ifMatchers.add(ifTreeMatcher);
    }
    if (checker instanceof ImportTreeMatcher importTreeMatcher) {
      importMatchers.add(importTreeMatcher);
    }
    if (checker instanceof InstanceOfTreeMatcher instanceOfTreeMatcher) {
      instanceOfMatchers.add(instanceOfTreeMatcher);
    }
    if (checker instanceof IntersectionTypeTreeMatcher intersectionTypeTreeMatcher) {
      intersectionTypeMatchers.add(intersectionTypeTreeMatcher);
    }
    if (checker instanceof LabeledStatementTreeMatcher labeledStatementTreeMatcher) {
      labeledStatementMatchers.add(labeledStatementTreeMatcher);
    }
    if (checker instanceof LambdaExpressionTreeMatcher lambdaExpressionTreeMatcher) {
      lambdaExpressionMatchers.add(lambdaExpressionTreeMatcher);
    }
    if (checker instanceof LiteralTreeMatcher literalTreeMatcher) {
      literalMatchers.add(literalTreeMatcher);
    }
    if (checker instanceof MemberReferenceTreeMatcher memberReferenceTreeMatcher) {
      memberReferenceMatchers.add(memberReferenceTreeMatcher);
    }
    if (checker instanceof MemberSelectTreeMatcher memberSelectTreeMatcher) {
      memberSelectMatchers.add(memberSelectTreeMatcher);
    }
    if (checker instanceof MethodInvocationTreeMatcher methodInvocationTreeMatcher) {
      methodInvocationMatchers.add(methodInvocationTreeMatcher);
    }
    if (checker instanceof MethodTreeMatcher methodTreeMatcher) {
      methodMatchers.add(methodTreeMatcher);
    }
    if (checker instanceof ModifiersTreeMatcher modifiersTreeMatcher) {
      modifiersMatchers.add(modifiersTreeMatcher);
    }
    if (checker instanceof ModuleTreeMatcher moduleTreeMatcher) {
      moduleMatchers.add(moduleTreeMatcher);
    }
    if (checker instanceof NewArrayTreeMatcher newArrayTreeMatcher) {
      newArrayMatchers.add(newArrayTreeMatcher);
    }
    if (checker instanceof NewClassTreeMatcher newClassTreeMatcher) {
      newClassMatchers.add(newClassTreeMatcher);
    }
    if (checker instanceof OpensTreeMatcher opensTreeMatcher) {
      opensMatchers.add(opensTreeMatcher);
    }
    if (checker instanceof PackageTreeMatcher packageTreeMatcher) {
      packageMatchers.add(packageTreeMatcher);
    }
    if (checker instanceof ParameterizedTypeTreeMatcher parameterizedTypeTreeMatcher) {
      parameterizedTypeMatchers.add(parameterizedTypeTreeMatcher);
    }
    if (checker instanceof ParenthesizedTreeMatcher parenthesizedTreeMatcher) {
      parenthesizedMatchers.add(parenthesizedTreeMatcher);
    }
    if (checker instanceof PatternCaseLabelTreeMatcher patternCaseLabelTreeMatcher) {
      patternCaseLabelMatchers.add(patternCaseLabelTreeMatcher);
    }
    if (checker instanceof PrimitiveTypeTreeMatcher primitiveTypeTreeMatcher) {
      primitiveTypeMatchers.add(primitiveTypeTreeMatcher);
    }
    if (checker instanceof ProvidesTreeMatcher providesTreeMatcher) {
      providesMatchers.add(providesTreeMatcher);
    }
    if (checker instanceof RequiresTreeMatcher requiresTreeMatcher) {
      requiresMatchers.add(requiresTreeMatcher);
    }
    if (checker instanceof ReturnTreeMatcher returnTreeMatcher) {
      returnMatchers.add(returnTreeMatcher);
    }
    if (checker instanceof SwitchExpressionTreeMatcher switchExpressionTreeMatcher) {
      switchExpressionMatchers.add(switchExpressionTreeMatcher);
    }
    if (checker instanceof SwitchTreeMatcher switchTreeMatcher) {
      switchMatchers.add(switchTreeMatcher);
    }
    if (checker instanceof SynchronizedTreeMatcher synchronizedTreeMatcher) {
      synchronizedMatchers.add(synchronizedTreeMatcher);
    }
    if (checker instanceof ThrowTreeMatcher throwTreeMatcher) {
      throwMatchers.add(throwTreeMatcher);
    }
    if (checker instanceof TryTreeMatcher tryTreeMatcher) {
      tryMatchers.add(tryTreeMatcher);
    }
    if (checker instanceof TypeCastTreeMatcher typeCastTreeMatcher) {
      typeCastMatchers.add(typeCastTreeMatcher);
    }
    if (checker instanceof TypeParameterTreeMatcher typeParameterTreeMatcher) {
      typeParameterMatchers.add(typeParameterTreeMatcher);
    }
    if (checker instanceof UnaryTreeMatcher unaryTreeMatcher) {
      unaryMatchers.add(unaryTreeMatcher);
    }
    if (checker instanceof UnionTypeTreeMatcher unionTypeTreeMatcher) {
      unionTypeMatchers.add(unionTypeTreeMatcher);
    }
    if (checker instanceof UsesTreeMatcher usesTreeMatcher) {
      usesMatchers.add(usesTreeMatcher);
    }
    if (checker instanceof VariableTreeMatcher variableTreeMatcher) {
      variableMatchers.add(variableTreeMatcher);
    }
    if (checker instanceof WhileLoopTreeMatcher whileLoopTreeMatcher) {
      whileLoopMatchers.add(whileLoopTreeMatcher);
    }
    if (checker instanceof WildcardTreeMatcher wildcardTreeMatcher) {
      wildcardMatchers.add(wildcardTreeMatcher);
    }
    if (checker instanceof YieldTreeMatcher yieldTreeMatcher) {
      yieldMatchers.add(yieldTreeMatcher);
    }
    // keep-sorted end
  }

  @FunctionalInterface
  private interface TreeProcessor<M extends Suppressible, T extends Tree> {
    Description process(M matcher, T tree, VisitorState state);
  }

  private <M extends Suppressible, T extends Tree> VisitorState processMatchers(
      Iterable<M> matchers, T tree, TreeProcessor<M, T> processingFunction, VisitorState oldState) {
    ErrorProneOptions errorProneOptions = oldState.errorProneOptions();
    // A VisitorState with our new path, but without mentioning the suppression of any matcher.
    VisitorState newState = oldState.withPath(getCurrentPath());
    for (M matcher : matchers) {
      SuppressedState suppressed = isSuppressed(matcher, errorProneOptions, newState);
      // If the ErrorProneOptions say to visit suppressed code, we still visit it
      if (suppressed == SuppressedState.UNSUPPRESSED
          || errorProneOptions.isIgnoreSuppressionAnnotations()) {
        try (AutoCloseable unused = oldState.timingSpan(matcher)) {
          // We create a new VisitorState with the suppression info specific to this matcher.
          VisitorState stateWithSuppressionInformation = newState.withSuppression(suppressed);
          reportMatch(
              processingFunction.process(matcher, tree, stateWithSuppressionInformation),
              stateWithSuppressionInformation);
        } catch (Exception | AssertionError t) {
          handleError(matcher, t);
        }
      }
    }
    return newState;
  }

  @Override
  public Void visitAnnotation(AnnotationTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            annotationMatchers, tree, AnnotationTreeMatcher::matchAnnotation, visitorState);
    return super.visitAnnotation(tree, state);
  }

  @Override
  public Void visitAnnotatedType(AnnotatedTypeTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            annotatedTypeMatchers,
            tree,
            AnnotatedTypeTreeMatcher::matchAnnotatedType,
            visitorState);
    return super.visitAnnotatedType(tree, state);
  }

  @Override
  public Void visitArrayAccess(ArrayAccessTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            arrayAccessMatchers, tree, ArrayAccessTreeMatcher::matchArrayAccess, visitorState);
    return super.visitArrayAccess(tree, state);
  }

  @Override
  public Void visitArrayType(ArrayTypeTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            arrayTypeMatchers, tree, ArrayTypeTreeMatcher::matchArrayType, visitorState);
    return super.visitArrayType(tree, state);
  }

  @Override
  public Void visitAssert(AssertTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(assertMatchers, tree, AssertTreeMatcher::matchAssert, visitorState);
    return super.visitAssert(tree, state);
  }

  @Override
  public Void visitAssignment(AssignmentTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            assignmentMatchers, tree, AssignmentTreeMatcher::matchAssignment, visitorState);
    return super.visitAssignment(tree, state);
  }

  @Override
  public Void visitBinary(BinaryTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(binaryMatchers, tree, BinaryTreeMatcher::matchBinary, visitorState);
    return super.visitBinary(tree, state);
  }

  @Override
  public Void visitBindingPattern(BindingPatternTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            bindingPatternMatchers,
            tree,
            BindingPatternTreeMatcher::matchBindingPattern,
            visitorState);
    return super.visitBindingPattern(tree, state);
  }

  @Override
  public Void visitBlock(BlockTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(blockMatchers, tree, BlockTreeMatcher::matchBlock, visitorState);
    return super.visitBlock(tree, state);
  }

  @Override
  public Void visitBreak(BreakTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(breakMatchers, tree, BreakTreeMatcher::matchBreak, visitorState);
    return super.visitBreak(tree, state);
  }

  @Override
  public Void visitCase(CaseTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(caseMatchers, tree, CaseTreeMatcher::matchCase, visitorState);
    return super.visitCase(tree, state);
  }

  @Override
  public Void visitCatch(CatchTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(catchMatchers, tree, CatchTreeMatcher::matchCatch, visitorState);
    return super.visitCatch(tree, state);
  }

  @Override
  public Void visitClass(ClassTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(classMatchers, tree, ClassTreeMatcher::matchClass, visitorState);
    return super.visitClass(tree, state);
  }

  @Override
  public Void visitCompilationUnit(CompilationUnitTree tree, VisitorState visitorState) {

    VisitorState state =
        processMatchers(
            compilationUnitMatchers,
            tree,
            CompilationUnitTreeMatcher::matchCompilationUnit,
            visitorState);
    return super.visitCompilationUnit(tree, state);
  }

  @Override
  public Void visitCompoundAssignment(CompoundAssignmentTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            compoundAssignmentMatchers,
            tree,
            CompoundAssignmentTreeMatcher::matchCompoundAssignment,
            visitorState);
    return super.visitCompoundAssignment(tree, state);
  }

  @Override
  public Void visitConditionalExpression(
      ConditionalExpressionTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            conditionalExpressionMatchers,
            tree,
            ConditionalExpressionTreeMatcher::matchConditionalExpression,
            visitorState);
    return super.visitConditionalExpression(tree, state);
  }

  @Override
  public Void visitConstantCaseLabel(ConstantCaseLabelTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            constantCaseLabelMatchers,
            tree,
            ConstantCaseLabelTreeMatcher::matchConstantCaseLabel,
            visitorState);
    return super.visitConstantCaseLabel(tree, state);
  }

  @Override
  public Void visitContinue(ContinueTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(continueMatchers, tree, ContinueTreeMatcher::matchContinue, visitorState);
    return super.visitContinue(tree, state);
  }

  @Override
  public Void visitDeconstructionPattern(
      DeconstructionPatternTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            deconstructionPatternMatchers,
            tree,
            DeconstructionPatternTreeMatcher::matchDeconstructionPattern,
            visitorState);
    return super.visitDeconstructionPattern(tree, state);
  }

  @Override
  public Void visitDefaultCaseLabel(DefaultCaseLabelTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            defaultCaseLabelMatchers,
            tree,
            DefaultCaseLabelTreeMatcher::matchDefaultCaseLabel,
            visitorState);
    return super.visitDefaultCaseLabel(tree, state);
  }

  @Override
  public Void visitDoWhileLoop(DoWhileLoopTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            doWhileLoopMatchers, tree, DoWhileLoopTreeMatcher::matchDoWhileLoop, visitorState);
    return super.visitDoWhileLoop(tree, state);
  }

  @Override
  public Void visitEmptyStatement(EmptyStatementTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            emptyStatementMatchers,
            tree,
            EmptyStatementTreeMatcher::matchEmptyStatement,
            visitorState);
    return super.visitEmptyStatement(tree, state);
  }

  @Override
  public Void visitEnhancedForLoop(EnhancedForLoopTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            enhancedForLoopMatchers,
            tree,
            EnhancedForLoopTreeMatcher::matchEnhancedForLoop,
            visitorState);
    return super.visitEnhancedForLoop(tree, state);
  }

  @Override
  public Void visitExports(ExportsTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(exportsMatchers, tree, ExportsTreeMatcher::matchExports, visitorState);
    return super.visitExports(tree, state);
  }

  // Intentionally skip visitErroneous -- we don't analyze malformed expressions.

  @Override
  public Void visitExpressionStatement(ExpressionStatementTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            expressionStatementMatchers,
            tree,
            ExpressionStatementTreeMatcher::matchExpressionStatement,
            visitorState);
    return super.visitExpressionStatement(tree, state);
  }

  @Override
  public Void visitForLoop(ForLoopTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(forLoopMatchers, tree, ForLoopTreeMatcher::matchForLoop, visitorState);
    return super.visitForLoop(tree, state);
  }

  @Override
  public Void visitIdentifier(IdentifierTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            identifierMatchers, tree, IdentifierTreeMatcher::matchIdentifier, visitorState);
    return super.visitIdentifier(tree, state);
  }

  @Override
  public Void visitIf(IfTree tree, VisitorState visitorState) {
    VisitorState state = processMatchers(ifMatchers, tree, IfTreeMatcher::matchIf, visitorState);
    return super.visitIf(tree, state);
  }

  @Override
  public Void visitImport(ImportTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(importMatchers, tree, ImportTreeMatcher::matchImport, visitorState);
    return super.visitImport(tree, state);
  }

  @Override
  public Void visitInstanceOf(InstanceOfTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            instanceOfMatchers, tree, InstanceOfTreeMatcher::matchInstanceOf, visitorState);
    return super.visitInstanceOf(tree, state);
  }

  @Override
  public Void visitIntersectionType(IntersectionTypeTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            intersectionTypeMatchers,
            tree,
            IntersectionTypeTreeMatcher::matchIntersectionType,
            visitorState);
    return super.visitIntersectionType(tree, state);
  }

  @Override
  public Void visitLabeledStatement(LabeledStatementTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            labeledStatementMatchers,
            tree,
            LabeledStatementTreeMatcher::matchLabeledStatement,
            visitorState);
    return super.visitLabeledStatement(tree, state);
  }

  @Override
  public Void visitLambdaExpression(LambdaExpressionTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            lambdaExpressionMatchers,
            tree,
            LambdaExpressionTreeMatcher::matchLambdaExpression,
            visitorState);
    return super.visitLambdaExpression(tree, state);
  }

  @Override
  public Void visitLiteral(LiteralTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(literalMatchers, tree, LiteralTreeMatcher::matchLiteral, visitorState);
    return super.visitLiteral(tree, state);
  }

  @Override
  public Void visitMemberReference(MemberReferenceTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            memberReferenceMatchers,
            tree,
            MemberReferenceTreeMatcher::matchMemberReference,
            visitorState);
    return super.visitMemberReference(tree, state);
  }

  @Override
  public Void visitMemberSelect(MemberSelectTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            memberSelectMatchers, tree, MemberSelectTreeMatcher::matchMemberSelect, visitorState);
    return super.visitMemberSelect(tree, state);
  }

  @Override
  public Void visitMethod(MethodTree tree, VisitorState visitorState) {
    // Ignore synthetic constructors:
    if (ASTHelpers.isGeneratedConstructor(tree)) {
      return null;
    }

    VisitorState state =
        processMatchers(methodMatchers, tree, MethodTreeMatcher::matchMethod, visitorState);
    return super.visitMethod(tree, state);
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            methodInvocationMatchers,
            tree,
            MethodInvocationTreeMatcher::matchMethodInvocation,
            visitorState);
    return super.visitMethodInvocation(tree, state);
  }

  @Override
  public Void visitModifiers(ModifiersTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            modifiersMatchers, tree, ModifiersTreeMatcher::matchModifiers, visitorState);

    return super.visitModifiers(tree, state);
  }

  @Override
  public Void visitModule(ModuleTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(moduleMatchers, tree, ModuleTreeMatcher::matchModule, visitorState);

    return super.visitModule(tree, state);
  }

  @Override
  public Void visitNewArray(NewArrayTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(newArrayMatchers, tree, NewArrayTreeMatcher::matchNewArray, visitorState);
    return super.visitNewArray(tree, state);
  }

  @Override
  public Void visitNewClass(NewClassTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(newClassMatchers, tree, NewClassTreeMatcher::matchNewClass, visitorState);
    return super.visitNewClass(tree, state);
  }

  @Override
  public Void visitOpens(OpensTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(opensMatchers, tree, OpensTreeMatcher::matchOpens, visitorState);
    return super.visitOpens(tree, state);
  }

  @Override
  public Void visitPackage(PackageTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(packageMatchers, tree, PackageTreeMatcher::matchPackage, visitorState);
    return super.visitPackage(tree, state);
  }

  @Override
  public Void visitPatternCaseLabel(PatternCaseLabelTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            patternCaseLabelMatchers,
            tree,
            PatternCaseLabelTreeMatcher::matchPatternCaseLabel,
            visitorState);
    return super.visitPatternCaseLabel(tree, state);
  }

  // Intentionally skip visitOther. It seems to be used only for let expressions, which are
  // generated by javac to implement autoboxing. We are only interested in source-level constructs.

  @Override
  public Void visitParameterizedType(ParameterizedTypeTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            parameterizedTypeMatchers,
            tree,
            ParameterizedTypeTreeMatcher::matchParameterizedType,
            visitorState);
    return super.visitParameterizedType(tree, state);
  }

  @Override
  public Void visitParenthesized(ParenthesizedTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            parenthesizedMatchers,
            tree,
            ParenthesizedTreeMatcher::matchParenthesized,
            visitorState);
    return super.visitParenthesized(tree, state);
  }

  @Override
  public Void visitPrimitiveType(PrimitiveTypeTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            primitiveTypeMatchers,
            tree,
            PrimitiveTypeTreeMatcher::matchPrimitiveType,
            visitorState);
    return super.visitPrimitiveType(tree, state);
  }

  @Override
  public Void visitProvides(ProvidesTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(providesMatchers, tree, ProvidesTreeMatcher::matchProvides, visitorState);
    return super.visitProvides(tree, state);
  }

  @Override
  public Void visitRequires(RequiresTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(requiresMatchers, tree, RequiresTreeMatcher::matchRequires, visitorState);
    return super.visitRequires(tree, state);
  }

  @Override
  public Void visitReturn(ReturnTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(returnMatchers, tree, ReturnTreeMatcher::matchReturn, visitorState);
    return super.visitReturn(tree, state);
  }

  @Override
  public Void visitSwitchExpression(SwitchExpressionTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            switchExpressionMatchers,
            tree,
            SwitchExpressionTreeMatcher::matchSwitchExpression,
            visitorState);
    return super.visitSwitchExpression(tree, state);
  }

  @Override
  public Void visitSwitch(SwitchTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(switchMatchers, tree, SwitchTreeMatcher::matchSwitch, visitorState);
    return super.visitSwitch(tree, state);
  }

  @Override
  public Void visitSynchronized(SynchronizedTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            synchronizedMatchers, tree, SynchronizedTreeMatcher::matchSynchronized, visitorState);
    return super.visitSynchronized(tree, state);
  }

  @Override
  public Void visitThrow(ThrowTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(throwMatchers, tree, ThrowTreeMatcher::matchThrow, visitorState);
    return super.visitThrow(tree, state);
  }

  @Override
  public Void visitTry(TryTree tree, VisitorState visitorState) {
    VisitorState state = processMatchers(tryMatchers, tree, TryTreeMatcher::matchTry, visitorState);
    return super.visitTry(tree, state);
  }

  @Override
  public Void visitTypeCast(TypeCastTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(typeCastMatchers, tree, TypeCastTreeMatcher::matchTypeCast, visitorState);
    return super.visitTypeCast(tree, state);
  }

  @Override
  public Void visitTypeParameter(TypeParameterTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            typeParameterMatchers,
            tree,
            TypeParameterTreeMatcher::matchTypeParameter,
            visitorState);
    return super.visitTypeParameter(tree, state);
  }

  @Override
  public Void visitUnary(UnaryTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(unaryMatchers, tree, UnaryTreeMatcher::matchUnary, visitorState);
    return super.visitUnary(tree, state);
  }

  @Override
  public Void visitUnionType(UnionTypeTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            unionTypeMatchers, tree, UnionTypeTreeMatcher::matchUnionType, visitorState);
    return super.visitUnionType(tree, state);
  }

  @Override
  public Void visitUses(UsesTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(usesMatchers, tree, UsesTreeMatcher::matchUses, visitorState);
    return super.visitUses(tree, state);
  }

  @Override
  public Void visitVariable(VariableTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(variableMatchers, tree, VariableTreeMatcher::matchVariable, visitorState);
    return super.visitVariable(tree, state);
  }

  @Override
  public Void visitWhileLoop(WhileLoopTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(
            whileLoopMatchers, tree, WhileLoopTreeMatcher::matchWhileLoop, visitorState);
    return super.visitWhileLoop(tree, state);
  }

  @Override
  public Void visitWildcard(WildcardTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(wildcardMatchers, tree, WildcardTreeMatcher::matchWildcard, visitorState);
    return super.visitWildcard(tree, state);
  }

  @Override
  public Void visitYield(YieldTree tree, VisitorState visitorState) {
    VisitorState state =
        processMatchers(yieldMatchers, tree, YieldTreeMatcher::matchYield, visitorState);
    return super.visitYield(tree, state);
  }

  /**
   * Handles an exception thrown by an individual BugPattern. By default, wraps the exception in an
   * {@link ErrorProneError} and rethrows. May be overridden by subclasses, for example to log the
   * error and continue.
   */
  @Override
  protected void handleError(Suppressible s, Throwable t) {
    if (t instanceof ErrorProneError errorProneError) {
      throw errorProneError;
    }
    if (t instanceof CompletionFailure completionFailure) {
      throw completionFailure;
    }
    TreePath path = getCurrentPath();
    JavaFileObject sourceFile = path.getCompilationUnit().getSourceFile();
    if (t instanceof SourcePositionException sourcePositionException) {
      throw sourcePositionException.toErrorProneError(s.canonicalName(), sourceFile);
    }
    throw new ErrorProneError(
        s.canonicalName(), t, (DiagnosticPosition) path.getLeaf(), sourceFile);
  }

  @Override
  public Map<String, SeverityLevel> severityMap() {
    return severities;
  }

  public ImmutableSet<BugChecker> getBugCheckers() {
    return this.bugCheckers;
  }
}
