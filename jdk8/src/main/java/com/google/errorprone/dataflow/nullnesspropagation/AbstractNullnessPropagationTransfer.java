/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTransfer.tryGetMethodSymbol;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessValue.NONNULL;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessValue.NULLABLE;

import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferFunction;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.node.ArrayAccessNode;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.dataflow.cfg.node.ArrayTypeNode;
import org.checkerframework.dataflow.cfg.node.AssertionErrorNode;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.BitwiseAndNode;
import org.checkerframework.dataflow.cfg.node.BitwiseComplementNode;
import org.checkerframework.dataflow.cfg.node.BitwiseOrNode;
import org.checkerframework.dataflow.cfg.node.BitwiseXorNode;
import org.checkerframework.dataflow.cfg.node.BooleanLiteralNode;
import org.checkerframework.dataflow.cfg.node.CaseNode;
import org.checkerframework.dataflow.cfg.node.CharacterLiteralNode;
import org.checkerframework.dataflow.cfg.node.ClassNameNode;
import org.checkerframework.dataflow.cfg.node.ConditionalAndNode;
import org.checkerframework.dataflow.cfg.node.ConditionalNotNode;
import org.checkerframework.dataflow.cfg.node.ConditionalOrNode;
import org.checkerframework.dataflow.cfg.node.DoubleLiteralNode;
import org.checkerframework.dataflow.cfg.node.EqualToNode;
import org.checkerframework.dataflow.cfg.node.ExplicitThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.FloatLiteralNode;
import org.checkerframework.dataflow.cfg.node.FloatingDivisionNode;
import org.checkerframework.dataflow.cfg.node.FloatingRemainderNode;
import org.checkerframework.dataflow.cfg.node.FunctionalInterfaceNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.ImplicitThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.InstanceOfNode;
import org.checkerframework.dataflow.cfg.node.IntegerDivisionNode;
import org.checkerframework.dataflow.cfg.node.IntegerLiteralNode;
import org.checkerframework.dataflow.cfg.node.IntegerRemainderNode;
import org.checkerframework.dataflow.cfg.node.LeftShiftNode;
import org.checkerframework.dataflow.cfg.node.LessThanNode;
import org.checkerframework.dataflow.cfg.node.LessThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.LongLiteralNode;
import org.checkerframework.dataflow.cfg.node.MarkerNode;
import org.checkerframework.dataflow.cfg.node.MethodAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.NarrowingConversionNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NotEqualNode;
import org.checkerframework.dataflow.cfg.node.NullChkNode;
import org.checkerframework.dataflow.cfg.node.NullLiteralNode;
import org.checkerframework.dataflow.cfg.node.NumericalAdditionNode;
import org.checkerframework.dataflow.cfg.node.NumericalMinusNode;
import org.checkerframework.dataflow.cfg.node.NumericalMultiplicationNode;
import org.checkerframework.dataflow.cfg.node.NumericalPlusNode;
import org.checkerframework.dataflow.cfg.node.NumericalSubtractionNode;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.PackageNameNode;
import org.checkerframework.dataflow.cfg.node.ParameterizedTypeNode;
import org.checkerframework.dataflow.cfg.node.PrimitiveTypeNode;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.dataflow.cfg.node.ShortLiteralNode;
import org.checkerframework.dataflow.cfg.node.SignedRightShiftNode;
import org.checkerframework.dataflow.cfg.node.StringConcatenateAssignmentNode;
import org.checkerframework.dataflow.cfg.node.StringConcatenateNode;
import org.checkerframework.dataflow.cfg.node.StringConversionNode;
import org.checkerframework.dataflow.cfg.node.StringLiteralNode;
import org.checkerframework.dataflow.cfg.node.SuperNode;
import org.checkerframework.dataflow.cfg.node.SynchronizedNode;
import org.checkerframework.dataflow.cfg.node.TernaryExpressionNode;
import org.checkerframework.dataflow.cfg.node.ThrowNode;
import org.checkerframework.dataflow.cfg.node.TypeCastNode;
import org.checkerframework.dataflow.cfg.node.UnsignedRightShiftNode;
import org.checkerframework.dataflow.cfg.node.VariableDeclarationNode;
import org.checkerframework.dataflow.cfg.node.WideningConversionNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A default implementation of a transfer function for nullability analysis with more convenient
 * visitor methods. The default implementation consists of labeling almost every kind of node as
 * {@code NULLABLE}. The convenient visitor methods consist first of "summary" methods, like the
 * {@code visitValueLiteral} method called for every {@code ValueLiteralNode} (like
 * {@code AbstractNodeVisitor}), and second of more targeted parameters to the individual
 * {@code visit*} methods. For example, a {@code visitTypeCast} does not need access to the full
 * {@link TransferInput}{@code <NullnessValue, NullnessPropagationStore>}, only to
 * {@linkplain TransferInput#getValueOfSubNode the subnode values it provides}. To accomplish this,
 * this class provides a {@code final} implementation of the inherited {@code visitTypeCast} method
 * that delegates to an overrideable {@code visitTypeCast} method with simpler parameters.
 *
 * <p>Despite being "abstract," this class is fairly tightly coupled to its sole current
 * implementation, {@link NullnessPropagationTransfer}. I expect that changes to that class will
 * sometimes require corresponding changes to this one. The value of separating the two classes
 * isn't in decoupling the two so much as in hiding the boilerplate in this class.
 *
 * @author cpovirk@google.com (Chris Povirk)
 */
abstract class AbstractNullnessPropagationTransfer
    implements TransferFunction<NullnessValue, NullnessPropagationStore> {
  @Override
  public final NullnessPropagationStore initialStore(UnderlyingAST underlyingAST,
      List<LocalVariableNode> parameters) {
    return new NullnessPropagationStore();
  }

  /**
   * Provides the previously computed nullness values of descendant nodes. All descendant nodes have
   * already been assigned a value, if only the default of {@code NULLABLE}.
   */
  interface SubNodeValues {
    NullnessValue valueOfSubNode(Node node);
  }

  /**
   * Provides the nullness values of local variables based only on their past assignments (as far as
   * they can be determined). If the nullness value cannot be definitively determined (for example,
   * because the variable is a parameter with no assignments within the method), it will be returned
   * as {@code NULLABLE}.
   */
  interface LocalVariableValues {
    NullnessValue valueOfLocalVariable(LocalVariableNode node);
  }

  /**
   * Receives updates to the nullness values of local parameters. The transfer function
   * implementation calls {@link #set} when it can conclude that a variable must have a given
   * nullness value upon successful (non-exceptional) execution of the current node's expression.
   */
  interface LocalVariableUpdates {
    // TODO(cpovirk): consider the API setIfLocalVariable(Node, NullnessValue)
    void set(LocalVariableNode node, NullnessValue value);
  }

  /** "Summary" method called by default for every {@code ValueLiteralNode}. */
  NullnessValue visitValueLiteral() {
    return NULLABLE;
  }

  /** "Summary" method called by default for bitwise operations. */
  NullnessValue visitBitwiseOperation() {
    return NULLABLE;
  }

  /** "Summary" method called by default for numerical comparisons. */
  NullnessValue visitNumericalComparison() {
    return NULLABLE;
  }

  /** "Summary" method called by default for numerical operations. */
  NullnessValue visitNumericalOperation() {
    return NULLABLE;
  }

  /** "Summary" method called by default for every {@code ThisLiteralNode}. */
  NullnessValue visitThisLiteral() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitNullLiteral(
      NullLiteralNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitNullLiteral();
    updates.apply(input);
    return result(input, result);
  }

  NullnessValue visitNullLiteral() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitTypeCast(
      TypeCastNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue result = visitTypeCast(node, values(input));
    return result(input, result);
  }

  NullnessValue visitTypeCast(TypeCastNode node, SubNodeValues inputs) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitNumericalAddition(
      NumericalAdditionNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue result = visitNumericalAddition();
    return result(input, result);
  }

  NullnessValue visitNumericalAddition() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitNarrowingConversion(
      NarrowingConversionNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue result = visitNarrowingConversion();
    return result(input, result);
  }

  NullnessValue visitNarrowingConversion() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitEqualTo(
      EqualToNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    ReadableLocalVariableUpdates thenUpdates = new ReadableLocalVariableUpdates();
    ReadableLocalVariableUpdates elseUpdates = new ReadableLocalVariableUpdates();
    visitEqualTo(node, values(input), thenUpdates, elseUpdates);
    NullnessPropagationStore thenStore = input.getRegularStore();
    NullnessPropagationStore elseStore = thenStore.copy();
    thenUpdates.apply(thenStore);
    elseUpdates.apply(elseStore);
    return conditionalResult(thenStore, elseStore);
  }

  void visitEqualTo(EqualToNode node, SubNodeValues inputs, LocalVariableUpdates thenUpdates,
      LocalVariableUpdates elseUpdates) {}

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitNotEqual(
      NotEqualNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    ReadableLocalVariableUpdates thenUpdates = new ReadableLocalVariableUpdates();
    ReadableLocalVariableUpdates elseUpdates = new ReadableLocalVariableUpdates();
    visitNotEqual(node, values(input), thenUpdates, elseUpdates);
    NullnessPropagationStore thenStore = input.getRegularStore();
    NullnessPropagationStore elseStore = thenStore.copy();
    thenUpdates.apply(thenStore);
    elseUpdates.apply(elseStore);
    return conditionalResult(thenStore, elseStore);
  }

  void visitNotEqual(NotEqualNode node, SubNodeValues inputs, LocalVariableUpdates thenUpdates,
      LocalVariableUpdates elseUpdates) {}

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitAssignment(
      AssignmentNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitAssignment(node, values(input), updates);
    updates.apply(input);
    return result(input, result);
  }

  NullnessValue visitAssignment(AssignmentNode node, SubNodeValues inputs,
      LocalVariableUpdates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitLocalVariable(
      LocalVariableNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitLocalVariable(node, values(input.getRegularStore()));
    updates.apply(input);
    return result(input, result);
  }

  NullnessValue visitLocalVariable(LocalVariableNode node, LocalVariableValues store) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitFieldAccess(
      FieldAccessNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitFieldAccess(node, updates);
    updates.apply(input);
    return result(input, result);
  }

  NullnessValue visitFieldAccess(FieldAccessNode node, LocalVariableUpdates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitMethodInvocation(
      MethodInvocationNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    ReadableLocalVariableUpdates thenUpdates = new ReadableLocalVariableUpdates();
    ReadableLocalVariableUpdates elseUpdates = new ReadableLocalVariableUpdates();
    ReadableLocalVariableUpdates bothUpdates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitMethodInvocation(node, thenUpdates, elseUpdates, bothUpdates);

    /*
     * Returning a ConditionalTransferResult for a non-boolean node causes weird test failures, even
     * if I'm careful to give it its correct NullnessValue instead of hardcoding it to NONNULL as
     * the current code does. To avoid problems, we return a RegularTransferResult when possible.
     */
    if (tryGetMethodSymbol(node.getTree()).isBoolean) {
      NullnessPropagationStore thenStore = input.getThenStore();
      NullnessPropagationStore elseStore = input.getElseStore();
      bothUpdates.apply(thenStore);
      bothUpdates.apply(elseStore);
      thenUpdates.apply(thenStore);
      elseUpdates.apply(elseStore);
      return conditionalResult(thenStore, elseStore);
    } else {
      bothUpdates.apply(input);
      return result(input, result);
    }
  }

  NullnessValue visitMethodInvocation(MethodInvocationNode node, LocalVariableUpdates thenUpdates,
      LocalVariableUpdates elseUpdates, LocalVariableUpdates bothUpdates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitConditionalAnd(
      ConditionalAndNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    return conditionalResult(input.getThenStore(), input.getElseStore());
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitConditionalOr(
      ConditionalOrNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    return conditionalResult(input.getThenStore(), input.getElseStore());
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitConditionalNot(
      ConditionalNotNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    return conditionalResult(input.getElseStore(), input.getThenStore());
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitObjectCreation(
      ObjectCreationNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue result = visitObjectCreation();
    return result(input, result);
  }

  NullnessValue visitObjectCreation() {
    return NULLABLE;
  }

  // TODO(cpovirk): reverse order of arguments on TransferResult methods to match constructors?

  private static TransferResult<NullnessValue, NullnessPropagationStore> result(
      TransferInput<?, NullnessPropagationStore> input, NullnessValue value) {
    return new RegularTransferResult<>(value, input.getRegularStore());
  }

  private static TransferResult<NullnessValue, NullnessPropagationStore> conditionalResult(
      NullnessPropagationStore thenStore, NullnessPropagationStore elseStore) {
    return new ConditionalTransferResult<>(NONNULL, thenStore, elseStore);
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitShortLiteral(
      ShortLiteralNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue result = visitShortLiteral();
    return result(input, result);
  }

  NullnessValue visitShortLiteral() {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitIntegerLiteral(
      IntegerLiteralNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue result = visitIntegerLiteral();
    return result(input, result);
  }

  NullnessValue visitIntegerLiteral() {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitLongLiteral(
      LongLiteralNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue result = visitLongLiteral();
    return result(input, result);
  }

  NullnessValue visitLongLiteral() {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitFloatLiteral(
      FloatLiteralNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue result = visitFloatLiteral();
    return result(input, result);
  }

  NullnessValue visitFloatLiteral() {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitDoubleLiteral(
      DoubleLiteralNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue result = visitDoubleLiteral();
    return result(input, result);
  }

  NullnessValue visitDoubleLiteral() {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitBooleanLiteral(
      BooleanLiteralNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue result = visitBooleanLiteral();
    return result(input, result);
  }

  NullnessValue visitBooleanLiteral() {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitCharacterLiteral(
      CharacterLiteralNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitCharacterLiteral(node, values(input), updates);
    updates.apply(input);
    return result(input, result);
  }

  NullnessValue visitCharacterLiteral(CharacterLiteralNode node, SubNodeValues inputs,
      LocalVariableUpdates updates) {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitStringLiteral(
      StringLiteralNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitStringLiteral(node, values(input), updates);
    updates.apply(input);
    return result(input, result);
  }

  NullnessValue visitStringLiteral(StringLiteralNode node, SubNodeValues inputs,
      LocalVariableUpdates updates) {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitNumericalMinus(
      NumericalMinusNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitNumericalMinus();
    return result(input, value);
  }

  NullnessValue visitNumericalMinus() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitNumericalPlus(
      NumericalPlusNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitNumericalPlus();
    return result(input, value);
  }

  NullnessValue visitNumericalPlus() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitBitwiseComplement(
      BitwiseComplementNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitBitwiseComplement();
    return result(input, value);
  }

  NullnessValue visitBitwiseComplement() {
    return visitBitwiseOperation();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitNullChk(
      NullChkNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitNullChk();
    return result(input, value);
  }

  NullnessValue visitNullChk() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitStringConcatenate(
      StringConcatenateNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitStringConcatenate();
    return result(input, value);
  }

  NullnessValue visitStringConcatenate() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitNumericalSubtraction(
      NumericalSubtractionNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitNumericalSubtraction();
    return result(input, value);
  }

  NullnessValue visitNumericalSubtraction() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitNumericalMultiplication(
      NumericalMultiplicationNode node,
      TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitNumericalMultiplication();
    return result(input, value);
  }

  NullnessValue visitNumericalMultiplication() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitIntegerDivision(
      IntegerDivisionNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitIntegerDivision();
    return result(input, value);
  }

  NullnessValue visitIntegerDivision() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitFloatingDivision(
      FloatingDivisionNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitFloatingDivision();
    return result(input, value);
  }

  NullnessValue visitFloatingDivision() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitIntegerRemainder(
      IntegerRemainderNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitIntegerRemainder();
    return result(input, value);
  }

  NullnessValue visitIntegerRemainder() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitFloatingRemainder(
      FloatingRemainderNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitFloatingRemainder();
    return result(input, value);
  }

  NullnessValue visitFloatingRemainder() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitLeftShift(
      LeftShiftNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitLeftShift();
    return result(input, value);
  }

  NullnessValue visitLeftShift() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitSignedRightShift(
      SignedRightShiftNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitSignedRightShift();
    return result(input, value);
  }

  NullnessValue visitSignedRightShift() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitUnsignedRightShift(
      UnsignedRightShiftNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitUnsignedRightShift();
    return result(input, value);
  }

  NullnessValue visitUnsignedRightShift() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitBitwiseAnd(
      BitwiseAndNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitBitwiseAnd();
    return result(input, value);
  }

  NullnessValue visitBitwiseAnd() {
    return visitBitwiseOperation();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitBitwiseOr(
      BitwiseOrNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitBitwiseOr();
    return result(input, value);
  }

  NullnessValue visitBitwiseOr() {
    return visitBitwiseOperation();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitBitwiseXor(
      BitwiseXorNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitBitwiseXor();
    return result(input, value);
  }

  NullnessValue visitBitwiseXor() {
    return visitBitwiseOperation();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore>
      visitStringConcatenateAssignment(StringConcatenateAssignmentNode node,
          TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitStringConcatenateAssignment();
    return result(input, value);
  }

  NullnessValue visitStringConcatenateAssignment() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitLessThan(
      LessThanNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitLessThan();
    return result(input, value);
  }

  NullnessValue visitLessThan() {
    return visitNumericalComparison();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitLessThanOrEqual(
      LessThanOrEqualNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitLessThanOrEqual();
    return result(input, value);
  }

  NullnessValue visitLessThanOrEqual() {
    return visitNumericalComparison();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitGreaterThan(
      GreaterThanNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitGreaterThan();
    return result(input, value);
  }

  NullnessValue visitGreaterThan() {
    return visitNumericalComparison();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitGreaterThanOrEqual(
      GreaterThanOrEqualNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitGreaterThanOrEqual();
    return result(input, value);
  }

  NullnessValue visitGreaterThanOrEqual() {
    return visitNumericalComparison();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitTernaryExpression(
      TernaryExpressionNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitTernaryExpression(node, values(input), updates);
    updates.apply(input);
    return result(input, result);
  }

  NullnessValue visitTernaryExpression(TernaryExpressionNode node, SubNodeValues inputs,
      LocalVariableUpdates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitVariableDeclaration(
      VariableDeclarationNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitVariableDeclaration();
    return result(input, value);
  }

  NullnessValue visitVariableDeclaration() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitMethodAccess(
      MethodAccessNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitMethodAccess();
    return result(input, value);
  }

  NullnessValue visitMethodAccess() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitArrayAccess(
      ArrayAccessNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitArrayAccess(node, values(input), updates);
    updates.apply(input);
    return result(input, result);
  }

  NullnessValue visitArrayAccess(ArrayAccessNode node, SubNodeValues inputs,
      LocalVariableUpdates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitImplicitThisLiteral(
      ImplicitThisLiteralNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitImplicitThisLiteral();
    return result(input, value);
  }

  NullnessValue visitImplicitThisLiteral() {
    return visitThisLiteral();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitExplicitThisLiteral(
      ExplicitThisLiteralNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitExplicitThisLiteral();
    return result(input, value);
  }

  NullnessValue visitExplicitThisLiteral() {
    return visitThisLiteral();
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitSuper(SuperNode node,
      TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitSuper();
    return result(input, value);
  }

  NullnessValue visitSuper() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitReturn(ReturnNode node,
      TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitReturn();
    return result(input, value);
  }

  NullnessValue visitReturn() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitStringConversion(
      StringConversionNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitStringConversion();
    return result(input, value);
  }

  NullnessValue visitStringConversion() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitWideningConversion(
      WideningConversionNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitWideningConversion();
    return result(input, value);
  }

  NullnessValue visitWideningConversion() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitInstanceOf(
      InstanceOfNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitInstanceOf(node, values(input), updates);
    updates.apply(input);
    return result(input, result);
  }

  NullnessValue visitInstanceOf(InstanceOfNode node, SubNodeValues inputs,
      LocalVariableUpdates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitSynchronized(
      SynchronizedNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitSynchronized(node, values(input), updates);
    updates.apply(input);
    return result(input, result);
  }

  NullnessValue visitSynchronized(SynchronizedNode node, SubNodeValues inputs,
      LocalVariableUpdates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitAssertionError(
      AssertionErrorNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitAssertionError();
    return result(input, value);
  }

  NullnessValue visitAssertionError() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitThrow(ThrowNode node,
      TransferInput<NullnessValue, NullnessPropagationStore> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitThrow(node, values(input), updates);
    updates.apply(input);
    return result(input, result);
  }

  NullnessValue visitThrow(ThrowNode node, SubNodeValues inputs, LocalVariableUpdates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitCase(CaseNode node,
      TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitCase();
    return result(input, value);
  }

  NullnessValue visitCase() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitMemberReference(
      FunctionalInterfaceNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitMemberReference(node, values(input), updates);
    updates.apply(input);
    return result(input, result);
  }

  NullnessValue visitMemberReference(FunctionalInterfaceNode node, SubNodeValues inputs,
      LocalVariableUpdates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitArrayCreation(
      ArrayCreationNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitArrayCreation(node, values(input), updates);
    updates.apply(input);
    return result(input, result);
  }

  NullnessValue visitArrayCreation(ArrayCreationNode node, SubNodeValues inputs,
      LocalVariableUpdates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitArrayType(
      ArrayTypeNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitArrayType();
    return result(input, value);
  }

  NullnessValue visitArrayType() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitPrimitiveType(
      PrimitiveTypeNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitPrimitiveType();
    return result(input, value);
  }

  NullnessValue visitPrimitiveType() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitClassName(
      ClassNameNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitClassName();
    return result(input, value);
  }

  NullnessValue visitClassName() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitPackageName(
      PackageNameNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitPackageName();
    return result(input, value);
  }

  NullnessValue visitPackageName() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitParameterizedType(
      ParameterizedTypeNode node, TransferInput<NullnessValue, NullnessPropagationStore> input) {
    NullnessValue value = visitParameterizedType();
    return result(input, value);
  }

  NullnessValue visitParameterizedType() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, NullnessPropagationStore> visitMarker(MarkerNode node,
      TransferInput<NullnessValue, NullnessPropagationStore> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitMarker(node, values(input), updates);
    updates.apply(input);
    return result(input, result);
  }

  NullnessValue visitMarker(MarkerNode node, SubNodeValues inputs, LocalVariableUpdates updates) {
    return NULLABLE;
  }

  private static final class ReadableLocalVariableUpdates implements LocalVariableUpdates {
    private final Map<LocalVariableNode, NullnessValue> values = new HashMap<>();

    @Override
    public void set(LocalVariableNode node, NullnessValue value) {
      values.put(checkNotNull(node), checkNotNull(value));
    }

    void apply(TransferInput<NullnessValue, NullnessPropagationStore> input) {
      apply(input.getRegularStore());
    }

    void apply(NullnessPropagationStore store) {
      for (Entry<LocalVariableNode, NullnessValue> entry : values.entrySet()) {
        store.setInformation(entry.getKey(), entry.getValue());
      }
    }
  }

  private static SubNodeValues values(
      final TransferInput<NullnessValue, NullnessPropagationStore> input) {
    return new SubNodeValues() {
      @Override
      public NullnessValue valueOfSubNode(Node node) {
        return input.getValueOfSubNode(node);
      }
    };
  }

  private static LocalVariableValues values(final NullnessPropagationStore store) {
    return new LocalVariableValues() {
      @Override
      public NullnessValue valueOfLocalVariable(LocalVariableNode node) {
        return orNullable(store.getInformation(node));
      }
    };
  }

  private static NullnessValue orNullable(NullnessValue nullnessValue) {
    return (nullnessValue != null) ? nullnessValue : NULLABLE;
  }
}
