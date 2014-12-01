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

import com.google.errorprone.dataflow.LocalStore;

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

import javax.annotation.CheckReturnValue;

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
    implements TransferFunction<NullnessValue, LocalStore<NullnessValue>> {
  @Override
  public final LocalStore<NullnessValue> initialStore(
      UnderlyingAST underlyingAST, List<LocalVariableNode> parameters) {
    return LocalStore.empty();
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
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitNullLiteral(
      NullLiteralNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitNullLiteral();
    return updateRegularStore(result, input, updates);
  }

  NullnessValue visitNullLiteral() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitTypeCast(
      TypeCastNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue result = visitTypeCast(node, values(input));
    return noStoreChanges(result, input);
  }

  NullnessValue visitTypeCast(TypeCastNode node, SubNodeValues inputs) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitNumericalAddition(
      NumericalAdditionNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue result = visitNumericalAddition();
    return noStoreChanges(result, input);
  }

  NullnessValue visitNumericalAddition() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitNarrowingConversion(
      NarrowingConversionNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue result = visitNarrowingConversion();
    return noStoreChanges(result, input);
  }

  NullnessValue visitNarrowingConversion() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitEqualTo(
      EqualToNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    ReadableLocalVariableUpdates thenUpdates = new ReadableLocalVariableUpdates();
    ReadableLocalVariableUpdates elseUpdates = new ReadableLocalVariableUpdates();
    visitEqualTo(node, values(input), thenUpdates, elseUpdates);
    ResultingStore thenStore = updateStore(input.getThenStore(), thenUpdates);
    ResultingStore elseStore = updateStore(input.getElseStore(), elseUpdates);
    return conditionalResult(
        thenStore.store, elseStore.store, thenStore.storeChanged | elseStore.storeChanged);
  }

  void visitEqualTo(EqualToNode node, SubNodeValues inputs, LocalVariableUpdates thenUpdates,
      LocalVariableUpdates elseUpdates) {}

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitNotEqual(
      NotEqualNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    ReadableLocalVariableUpdates thenUpdates = new ReadableLocalVariableUpdates();
    ReadableLocalVariableUpdates elseUpdates = new ReadableLocalVariableUpdates();
    visitNotEqual(node, values(input), thenUpdates, elseUpdates);
    ResultingStore thenStore = updateStore(input.getThenStore(), thenUpdates);
    ResultingStore elseStore = updateStore(input.getElseStore(), elseUpdates);
    return conditionalResult(
        thenStore.store, elseStore.store, thenStore.storeChanged | elseStore.storeChanged);
  }

  void visitNotEqual(NotEqualNode node, SubNodeValues inputs, LocalVariableUpdates thenUpdates,
      LocalVariableUpdates elseUpdates) {}

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitAssignment(
      AssignmentNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitAssignment(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  NullnessValue visitAssignment(AssignmentNode node, SubNodeValues inputs,
      LocalVariableUpdates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitLocalVariable(
      LocalVariableNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitLocalVariable(node, values(input.getRegularStore()));
    return updateRegularStore(result, input, updates);
  }

  NullnessValue visitLocalVariable(LocalVariableNode node, LocalVariableValues store) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitFieldAccess(
      FieldAccessNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitFieldAccess(node, updates);
    return updateRegularStore(result, input, updates);
  }

  NullnessValue visitFieldAccess(FieldAccessNode node, LocalVariableUpdates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitMethodInvocation(
      MethodInvocationNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
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
      ResultingStore thenStore = updateStore(input.getThenStore(), thenUpdates, bothUpdates);
      ResultingStore elseStore = updateStore(input.getElseStore(), elseUpdates, bothUpdates);
      return conditionalResult(
          thenStore.store, elseStore.store, thenStore.storeChanged | elseStore.storeChanged);
    } else {
      return updateRegularStore(result, input, bothUpdates);
    }
  }

  NullnessValue visitMethodInvocation(MethodInvocationNode node, LocalVariableUpdates thenUpdates,
      LocalVariableUpdates elseUpdates, LocalVariableUpdates bothUpdates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitConditionalAnd(
      ConditionalAndNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    return conditionalResult(input.getThenStore(), input.getElseStore(), NO_STORE_CHANGE);
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitConditionalOr(
      ConditionalOrNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    return conditionalResult(input.getThenStore(), input.getElseStore(), NO_STORE_CHANGE);
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitConditionalNot(
      ConditionalNotNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    /*
     * Weird case: We swap the contents of the THEN and ELSE stores without otherwise modifying
     * them. Presumably that can still count as a change?
     */
    boolean storeChanged = !input.getThenStore().equals(input.getElseStore());
    return conditionalResult(input.getElseStore(), input.getThenStore(), storeChanged);
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitObjectCreation(
      ObjectCreationNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue result = visitObjectCreation();
    return noStoreChanges(result, input);
  }

  NullnessValue visitObjectCreation() {
    return NULLABLE;
  }

  private static TransferResult<NullnessValue, LocalStore<NullnessValue>> noStoreChanges(
      NullnessValue value, TransferInput<?, LocalStore<NullnessValue>> input) {
    return new RegularTransferResult<>(value, input.getRegularStore());
  }

  @CheckReturnValue
  private TransferResult<NullnessValue, LocalStore<NullnessValue>> updateRegularStore(
      NullnessValue value, TransferInput<?, LocalStore<NullnessValue>> input,
      ReadableLocalVariableUpdates updates) {
    ResultingStore newStore = updateStore(input.getRegularStore(), updates);
    return new RegularTransferResult<>(value, newStore.store, newStore.storeChanged);
  }

  private static TransferResult<NullnessValue, LocalStore<NullnessValue>> conditionalResult(
      LocalStore<NullnessValue> thenStore, LocalStore<NullnessValue> elseStore,
      boolean storeChanged) {
    return new ConditionalTransferResult<>(NONNULL, thenStore, elseStore, storeChanged);
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitShortLiteral(
      ShortLiteralNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue result = visitShortLiteral();
    return noStoreChanges(result, input);
  }

  NullnessValue visitShortLiteral() {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitIntegerLiteral(
      IntegerLiteralNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue result = visitIntegerLiteral();
    return noStoreChanges(result, input);
  }

  NullnessValue visitIntegerLiteral() {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitLongLiteral(
      LongLiteralNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue result = visitLongLiteral();
    return noStoreChanges(result, input);
  }

  NullnessValue visitLongLiteral() {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitFloatLiteral(
      FloatLiteralNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue result = visitFloatLiteral();
    return noStoreChanges(result, input);
  }

  NullnessValue visitFloatLiteral() {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitDoubleLiteral(
      DoubleLiteralNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue result = visitDoubleLiteral();
    return noStoreChanges(result, input);
  }

  NullnessValue visitDoubleLiteral() {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitBooleanLiteral(
      BooleanLiteralNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue result = visitBooleanLiteral();
    return noStoreChanges(result, input);
  }

  NullnessValue visitBooleanLiteral() {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitCharacterLiteral(
      CharacterLiteralNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitCharacterLiteral(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  NullnessValue visitCharacterLiteral(CharacterLiteralNode node, SubNodeValues inputs,
      LocalVariableUpdates updates) {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitStringLiteral(
      StringLiteralNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitStringLiteral(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  NullnessValue visitStringLiteral(StringLiteralNode node, SubNodeValues inputs,
      LocalVariableUpdates updates) {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitNumericalMinus(
      NumericalMinusNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitNumericalMinus();
    return noStoreChanges(value, input);
  }

  NullnessValue visitNumericalMinus() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitNumericalPlus(
      NumericalPlusNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitNumericalPlus();
    return noStoreChanges(value, input);
  }

  NullnessValue visitNumericalPlus() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitBitwiseComplement(
      BitwiseComplementNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitBitwiseComplement();
    return noStoreChanges(value, input);
  }

  NullnessValue visitBitwiseComplement() {
    return visitBitwiseOperation();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitNullChk(
      NullChkNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitNullChk();
    return noStoreChanges(value, input);
  }

  NullnessValue visitNullChk() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitStringConcatenate(
      StringConcatenateNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitStringConcatenate();
    return noStoreChanges(value, input);
  }

  NullnessValue visitStringConcatenate() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitNumericalSubtraction(
      NumericalSubtractionNode node,
      TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitNumericalSubtraction();
    return noStoreChanges(value, input);
  }

  NullnessValue visitNumericalSubtraction() {
    return visitNumericalOperation();
  }

  @Override
  public final
      TransferResult<NullnessValue, LocalStore<NullnessValue>> visitNumericalMultiplication(
          NumericalMultiplicationNode node,
          TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitNumericalMultiplication();
    return noStoreChanges(value, input);
  }

  NullnessValue visitNumericalMultiplication() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitIntegerDivision(
      IntegerDivisionNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitIntegerDivision();
    return noStoreChanges(value, input);
  }

  NullnessValue visitIntegerDivision() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitFloatingDivision(
      FloatingDivisionNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitFloatingDivision();
    return noStoreChanges(value, input);
  }

  NullnessValue visitFloatingDivision() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitIntegerRemainder(
      IntegerRemainderNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitIntegerRemainder();
    return noStoreChanges(value, input);
  }

  NullnessValue visitIntegerRemainder() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitFloatingRemainder(
      FloatingRemainderNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitFloatingRemainder();
    return noStoreChanges(value, input);
  }

  NullnessValue visitFloatingRemainder() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitLeftShift(
      LeftShiftNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitLeftShift();
    return noStoreChanges(value, input);
  }

  NullnessValue visitLeftShift() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitSignedRightShift(
      SignedRightShiftNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitSignedRightShift();
    return noStoreChanges(value, input);
  }

  NullnessValue visitSignedRightShift() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitUnsignedRightShift(
      UnsignedRightShiftNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitUnsignedRightShift();
    return noStoreChanges(value, input);
  }

  NullnessValue visitUnsignedRightShift() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitBitwiseAnd(
      BitwiseAndNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitBitwiseAnd();
    return noStoreChanges(value, input);
  }

  NullnessValue visitBitwiseAnd() {
    return visitBitwiseOperation();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitBitwiseOr(
      BitwiseOrNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitBitwiseOr();
    return noStoreChanges(value, input);
  }

  NullnessValue visitBitwiseOr() {
    return visitBitwiseOperation();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitBitwiseXor(
      BitwiseXorNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitBitwiseXor();
    return noStoreChanges(value, input);
  }

  NullnessValue visitBitwiseXor() {
    return visitBitwiseOperation();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>>
      visitStringConcatenateAssignment(StringConcatenateAssignmentNode node,
          TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitStringConcatenateAssignment();
    return noStoreChanges(value, input);
  }

  NullnessValue visitStringConcatenateAssignment() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitLessThan(
      LessThanNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitLessThan();
    return noStoreChanges(value, input);
  }

  NullnessValue visitLessThan() {
    return visitNumericalComparison();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitLessThanOrEqual(
      LessThanOrEqualNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitLessThanOrEqual();
    return noStoreChanges(value, input);
  }

  NullnessValue visitLessThanOrEqual() {
    return visitNumericalComparison();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitGreaterThan(
      GreaterThanNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitGreaterThan();
    return noStoreChanges(value, input);
  }

  NullnessValue visitGreaterThan() {
    return visitNumericalComparison();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitGreaterThanOrEqual(
      GreaterThanOrEqualNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitGreaterThanOrEqual();
    return noStoreChanges(value, input);
  }

  NullnessValue visitGreaterThanOrEqual() {
    return visitNumericalComparison();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitTernaryExpression(
      TernaryExpressionNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitTernaryExpression(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  NullnessValue visitTernaryExpression(TernaryExpressionNode node, SubNodeValues inputs,
      LocalVariableUpdates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitVariableDeclaration(
      VariableDeclarationNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitVariableDeclaration();
    return noStoreChanges(value, input);
  }

  NullnessValue visitVariableDeclaration() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitMethodAccess(
      MethodAccessNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitMethodAccess();
    return noStoreChanges(value, input);
  }

  NullnessValue visitMethodAccess() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitArrayAccess(
      ArrayAccessNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitArrayAccess(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  NullnessValue visitArrayAccess(ArrayAccessNode node, SubNodeValues inputs,
      LocalVariableUpdates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitImplicitThisLiteral(
      ImplicitThisLiteralNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitImplicitThisLiteral();
    return noStoreChanges(value, input);
  }

  NullnessValue visitImplicitThisLiteral() {
    return visitThisLiteral();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitExplicitThisLiteral(
      ExplicitThisLiteralNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitExplicitThisLiteral();
    return noStoreChanges(value, input);
  }

  NullnessValue visitExplicitThisLiteral() {
    return visitThisLiteral();
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitSuper(SuperNode node,
      TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitSuper();
    return noStoreChanges(value, input);
  }

  NullnessValue visitSuper() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitReturn(ReturnNode node,
      TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitReturn();
    return noStoreChanges(value, input);
  }

  NullnessValue visitReturn() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitStringConversion(
      StringConversionNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitStringConversion();
    return noStoreChanges(value, input);
  }

  NullnessValue visitStringConversion() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitWideningConversion(
      WideningConversionNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitWideningConversion();
    return noStoreChanges(value, input);
  }

  NullnessValue visitWideningConversion() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitInstanceOf(
      InstanceOfNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitInstanceOf(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  NullnessValue visitInstanceOf(InstanceOfNode node, SubNodeValues inputs,
      LocalVariableUpdates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitSynchronized(
      SynchronizedNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitSynchronized(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  NullnessValue visitSynchronized(SynchronizedNode node, SubNodeValues inputs,
      LocalVariableUpdates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitAssertionError(
      AssertionErrorNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitAssertionError();
    return noStoreChanges(value, input);
  }

  NullnessValue visitAssertionError() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitThrow(ThrowNode node,
      TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitThrow(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  NullnessValue visitThrow(ThrowNode node, SubNodeValues inputs, LocalVariableUpdates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitCase(CaseNode node,
      TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitCase();
    return noStoreChanges(value, input);
  }

  NullnessValue visitCase() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitMemberReference(
      FunctionalInterfaceNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitMemberReference(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  NullnessValue visitMemberReference(FunctionalInterfaceNode node, SubNodeValues inputs,
      LocalVariableUpdates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitArrayCreation(
      ArrayCreationNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitArrayCreation(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  NullnessValue visitArrayCreation(ArrayCreationNode node, SubNodeValues inputs,
      LocalVariableUpdates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitArrayType(
      ArrayTypeNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitArrayType();
    return noStoreChanges(value, input);
  }

  NullnessValue visitArrayType() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitPrimitiveType(
      PrimitiveTypeNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitPrimitiveType();
    return noStoreChanges(value, input);
  }

  NullnessValue visitPrimitiveType() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitClassName(
      ClassNameNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitClassName();
    return noStoreChanges(value, input);
  }

  NullnessValue visitClassName() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitPackageName(
      PackageNameNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitPackageName();
    return noStoreChanges(value, input);
  }

  NullnessValue visitPackageName() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitParameterizedType(
      ParameterizedTypeNode node, TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    NullnessValue value = visitParameterizedType();
    return noStoreChanges(value, input);
  }

  NullnessValue visitParameterizedType() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<NullnessValue, LocalStore<NullnessValue>> visitMarker(MarkerNode node,
      TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    NullnessValue result = visitMarker(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  NullnessValue visitMarker(MarkerNode node, SubNodeValues inputs, LocalVariableUpdates updates) {
    return NULLABLE;
  }

  private static final class ReadableLocalVariableUpdates implements LocalVariableUpdates {
    final Map<LocalVariableNode, NullnessValue> values = new HashMap<>();

    @Override
    public void set(LocalVariableNode node, NullnessValue value) {
      values.put(checkNotNull(node), checkNotNull(value));
    }
  }

  @CheckReturnValue
  private static ResultingStore updateStore(
      LocalStore<NullnessValue> oldStore, ReadableLocalVariableUpdates... updates) {
    LocalStore.Builder<NullnessValue> builder = oldStore.toBuilder();
    for (ReadableLocalVariableUpdates update : updates) {
      for (Entry<LocalVariableNode, NullnessValue> entry : update.values.entrySet()) {
        builder.setInformation(entry.getKey(), entry.getValue());
      }
    }
    LocalStore<NullnessValue> newStore = builder.build();
    return new ResultingStore(newStore, !newStore.equals(oldStore));
  }

  private static SubNodeValues values(
      final TransferInput<NullnessValue, LocalStore<NullnessValue>> input) {
    return new SubNodeValues() {
      @Override
      public NullnessValue valueOfSubNode(Node node) {
        return input.getValueOfSubNode(node);
      }
    };
  }

  private static LocalVariableValues values(final LocalStore<NullnessValue> store) {
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

  private static final class ResultingStore {
    final LocalStore<NullnessValue> store;
    final boolean storeChanged;

    ResultingStore(LocalStore<NullnessValue> store, boolean storeChanged) {
      this.store = store;
      this.storeChanged = storeChanged;
    }
  }

  private static final boolean NO_STORE_CHANGE = false;
}
