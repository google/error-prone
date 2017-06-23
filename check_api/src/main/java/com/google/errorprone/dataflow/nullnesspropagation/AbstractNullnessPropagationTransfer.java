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
import static com.google.errorprone.dataflow.nullnesspropagation.Nullness.BOTTOM;
import static com.google.errorprone.dataflow.nullnesspropagation.Nullness.NONNULL;
import static com.google.errorprone.dataflow.nullnesspropagation.Nullness.NULLABLE;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTransfer.tryGetMethodSymbol;
import static org.checkerframework.javacutil.TreeUtils.elementFromDeclaration;

import com.google.errorprone.dataflow.LocalStore;
import com.google.errorprone.dataflow.LocalVariableValues;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.CheckReturnValue;
import javax.lang.model.element.Element;
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

/**
 * A default implementation of a transfer function for nullability analysis with more convenient
 * visitor methods. The default implementation consists of labeling almost every kind of node as
 * {@code NULLABLE}. The convenient visitor methods consist first of "summary" methods, like the
 * {@code visitValueLiteral} method called for every {@code ValueLiteralNode} (like {@code
 * AbstractNodeVisitor}), and second of more targeted parameters to the individual {@code visit*}
 * methods. For example, a {@code visitTypeCast} does not need access to the full {@link
 * TransferInput}{@code <Nullness, NullnessPropagationStore>}, only to {@linkplain
 * TransferInput#getValueOfSubNode the subnode values it provides}. To accomplish this, this class
 * provides a {@code final} implementation of the inherited {@code visitTypeCast} method that
 * delegates to an overrideable {@code visitTypeCast} method with simpler parameters.
 *
 * <p>Despite being "abstract," this class is fairly tightly coupled to its sole current
 * implementation, {@link NullnessPropagationTransfer}. I expect that changes to that class will
 * sometimes require corresponding changes to this one. The value of separating the two classes
 * isn't in decoupling the two so much as in hiding the boilerplate in this class.
 *
 * @author cpovirk@google.com (Chris Povirk)
 */
abstract class AbstractNullnessPropagationTransfer
    implements TransferFunction<Nullness, LocalStore<Nullness>> {
  @Override
  public LocalStore<Nullness> initialStore(
      UnderlyingAST underlyingAST, List<LocalVariableNode> parameters) {
    return LocalStore.empty();
  }

  /**
   * Provides the previously computed nullness values of descendant nodes. All descendant nodes have
   * already been assigned a value, if only the default of {@code NULLABLE}.
   */
  interface SubNodeValues {
    Nullness valueOfSubNode(Node node);
  }

  /**
   * Receives updates to the nullness values of local parameters. The transfer function
   * implementation calls {@link #set} when it can conclude that a variable must have a given
   * nullness value upon successful (non-exceptional) execution of the current node's expression.
   */
  interface LocalVariableUpdates {
    // TODO(cpovirk): consider the API setIfLocalVariable(Node, Nullness)
    void set(LocalVariableNode node, Nullness value);

    void set(VariableDeclarationNode node, Nullness value);
  }

  /** "Summary" method called by default for every {@code ValueLiteralNode}. */
  Nullness visitValueLiteral() {
    return NULLABLE;
  }

  /** "Summary" method called by default for bitwise operations. */
  Nullness visitBitwiseOperation() {
    return NULLABLE;
  }

  /** "Summary" method called by default for numerical comparisons. */
  Nullness visitNumericalComparison() {
    return NULLABLE;
  }

  /** "Summary" method called by default for numerical operations. */
  Nullness visitNumericalOperation() {
    return NULLABLE;
  }

  /** "Summary" method called by default for every {@code ThisLiteralNode}. */
  Nullness visitThisLiteral() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitNullLiteral(
      NullLiteralNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    Nullness result = visitNullLiteral();
    return updateRegularStore(result, input, updates);
  }

  Nullness visitNullLiteral() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitTypeCast(
      TypeCastNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness result = visitTypeCast(node, values(input));
    return noStoreChanges(result, input);
  }

  Nullness visitTypeCast(TypeCastNode node, SubNodeValues inputs) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitNumericalAddition(
      NumericalAdditionNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness result = visitNumericalAddition();
    return noStoreChanges(result, input);
  }

  Nullness visitNumericalAddition() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitNarrowingConversion(
      NarrowingConversionNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness result = visitNarrowingConversion();
    return noStoreChanges(result, input);
  }

  Nullness visitNarrowingConversion() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitEqualTo(
      EqualToNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    ReadableLocalVariableUpdates thenUpdates = new ReadableLocalVariableUpdates();
    ReadableLocalVariableUpdates elseUpdates = new ReadableLocalVariableUpdates();
    visitEqualTo(node, values(input), thenUpdates, elseUpdates);
    ResultingStore thenStore = updateStore(input.getThenStore(), thenUpdates);
    ResultingStore elseStore = updateStore(input.getElseStore(), elseUpdates);
    return conditionalResult(
        thenStore.store, elseStore.store, thenStore.storeChanged | elseStore.storeChanged);
  }

  void visitEqualTo(
      EqualToNode node,
      SubNodeValues inputs,
      LocalVariableUpdates thenUpdates,
      LocalVariableUpdates elseUpdates) {}

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitNotEqual(
      NotEqualNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    ReadableLocalVariableUpdates thenUpdates = new ReadableLocalVariableUpdates();
    ReadableLocalVariableUpdates elseUpdates = new ReadableLocalVariableUpdates();
    visitNotEqual(node, values(input), thenUpdates, elseUpdates);
    ResultingStore thenStore = updateStore(input.getThenStore(), thenUpdates);
    ResultingStore elseStore = updateStore(input.getElseStore(), elseUpdates);
    return conditionalResult(
        thenStore.store, elseStore.store, thenStore.storeChanged | elseStore.storeChanged);
  }

  void visitNotEqual(
      NotEqualNode node,
      SubNodeValues inputs,
      LocalVariableUpdates thenUpdates,
      LocalVariableUpdates elseUpdates) {}

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitAssignment(
      AssignmentNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    Nullness result = visitAssignment(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  Nullness visitAssignment(
      AssignmentNode node, SubNodeValues inputs, LocalVariableUpdates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitLocalVariable(
      LocalVariableNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    Nullness result = visitLocalVariable(node, input.getRegularStore());
    return updateRegularStore(result, input, updates);
  }

  Nullness visitLocalVariable(LocalVariableNode node, LocalVariableValues<Nullness> store) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitFieldAccess(
      FieldAccessNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    Nullness result = visitFieldAccess(node, updates);
    return updateRegularStore(result, input, updates);
  }

  Nullness visitFieldAccess(FieldAccessNode node, LocalVariableUpdates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitMethodInvocation(
      MethodInvocationNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    ReadableLocalVariableUpdates thenUpdates = new ReadableLocalVariableUpdates();
    ReadableLocalVariableUpdates elseUpdates = new ReadableLocalVariableUpdates();
    ReadableLocalVariableUpdates bothUpdates = new ReadableLocalVariableUpdates();
    Nullness result = visitMethodInvocation(node, thenUpdates, elseUpdates, bothUpdates);

    /*
     * Returning a ConditionalTransferResult for a non-boolean node causes weird test failures, even
     * if I'm careful to give it its correct Nullness instead of hardcoding it to NONNULL as the
     * current code does. To avoid problems, we return a RegularTransferResult when possible.
     */
    if (tryGetMethodSymbol(node.getTree(), null).isBoolean) {
      ResultingStore thenStore = updateStore(input.getThenStore(), thenUpdates, bothUpdates);
      ResultingStore elseStore = updateStore(input.getElseStore(), elseUpdates, bothUpdates);
      return conditionalResult(
          thenStore.store, elseStore.store, thenStore.storeChanged | elseStore.storeChanged);
    } else {
      return updateRegularStore(result, input, bothUpdates);
    }
  }

  Nullness visitMethodInvocation(
      MethodInvocationNode node,
      LocalVariableUpdates thenUpdates,
      LocalVariableUpdates elseUpdates,
      LocalVariableUpdates bothUpdates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitConditionalAnd(
      ConditionalAndNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    return conditionalResult(input.getThenStore(), input.getElseStore(), NO_STORE_CHANGE);
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitConditionalOr(
      ConditionalOrNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    return conditionalResult(input.getThenStore(), input.getElseStore(), NO_STORE_CHANGE);
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitConditionalNot(
      ConditionalNotNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    /*
     * Weird case: We swap the contents of the THEN and ELSE stores without otherwise modifying
     * them. Presumably that can still count as a change?
     */
    boolean storeChanged = !input.getThenStore().equals(input.getElseStore());
    return conditionalResult(input.getElseStore(), input.getThenStore(), storeChanged);
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitObjectCreation(
      ObjectCreationNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness result = visitObjectCreation();
    return noStoreChanges(result, input);
  }

  Nullness visitObjectCreation() {
    return NULLABLE;
  }

  private static TransferResult<Nullness, LocalStore<Nullness>> noStoreChanges(
      Nullness value, TransferInput<?, LocalStore<Nullness>> input) {
    return new RegularTransferResult<>(value, input.getRegularStore());
  }

  @CheckReturnValue
  private TransferResult<Nullness, LocalStore<Nullness>> updateRegularStore(
      Nullness value,
      TransferInput<?, LocalStore<Nullness>> input,
      ReadableLocalVariableUpdates updates) {
    ResultingStore newStore = updateStore(input.getRegularStore(), updates);
    return new RegularTransferResult<>(value, newStore.store, newStore.storeChanged);
  }

  private static TransferResult<Nullness, LocalStore<Nullness>> conditionalResult(
      LocalStore<Nullness> thenStore, LocalStore<Nullness> elseStore, boolean storeChanged) {
    return new ConditionalTransferResult<>(NONNULL, thenStore, elseStore, storeChanged);
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitShortLiteral(
      ShortLiteralNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness result = visitShortLiteral();
    return noStoreChanges(result, input);
  }

  Nullness visitShortLiteral() {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitIntegerLiteral(
      IntegerLiteralNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness result = visitIntegerLiteral();
    return noStoreChanges(result, input);
  }

  Nullness visitIntegerLiteral() {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitLongLiteral(
      LongLiteralNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness result = visitLongLiteral();
    return noStoreChanges(result, input);
  }

  Nullness visitLongLiteral() {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitFloatLiteral(
      FloatLiteralNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness result = visitFloatLiteral();
    return noStoreChanges(result, input);
  }

  Nullness visitFloatLiteral() {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitDoubleLiteral(
      DoubleLiteralNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness result = visitDoubleLiteral();
    return noStoreChanges(result, input);
  }

  Nullness visitDoubleLiteral() {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitBooleanLiteral(
      BooleanLiteralNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness result = visitBooleanLiteral();
    return noStoreChanges(result, input);
  }

  Nullness visitBooleanLiteral() {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitCharacterLiteral(
      CharacterLiteralNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    Nullness result = visitCharacterLiteral(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  Nullness visitCharacterLiteral(
      CharacterLiteralNode node, SubNodeValues inputs, LocalVariableUpdates updates) {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitStringLiteral(
      StringLiteralNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    Nullness result = visitStringLiteral(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  Nullness visitStringLiteral(
      StringLiteralNode node, SubNodeValues inputs, LocalVariableUpdates updates) {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitNumericalMinus(
      NumericalMinusNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitNumericalMinus();
    return noStoreChanges(value, input);
  }

  Nullness visitNumericalMinus() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitNumericalPlus(
      NumericalPlusNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitNumericalPlus();
    return noStoreChanges(value, input);
  }

  Nullness visitNumericalPlus() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitBitwiseComplement(
      BitwiseComplementNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitBitwiseComplement();
    return noStoreChanges(value, input);
  }

  Nullness visitBitwiseComplement() {
    return visitBitwiseOperation();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitNullChk(
      NullChkNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitNullChk();
    return noStoreChanges(value, input);
  }

  Nullness visitNullChk() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitStringConcatenate(
      StringConcatenateNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitStringConcatenate();
    return noStoreChanges(value, input);
  }

  Nullness visitStringConcatenate() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitNumericalSubtraction(
      NumericalSubtractionNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitNumericalSubtraction();
    return noStoreChanges(value, input);
  }

  Nullness visitNumericalSubtraction() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitNumericalMultiplication(
      NumericalMultiplicationNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitNumericalMultiplication();
    return noStoreChanges(value, input);
  }

  Nullness visitNumericalMultiplication() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitIntegerDivision(
      IntegerDivisionNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitIntegerDivision();
    return noStoreChanges(value, input);
  }

  Nullness visitIntegerDivision() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitFloatingDivision(
      FloatingDivisionNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitFloatingDivision();
    return noStoreChanges(value, input);
  }

  Nullness visitFloatingDivision() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitIntegerRemainder(
      IntegerRemainderNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitIntegerRemainder();
    return noStoreChanges(value, input);
  }

  Nullness visitIntegerRemainder() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitFloatingRemainder(
      FloatingRemainderNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitFloatingRemainder();
    return noStoreChanges(value, input);
  }

  Nullness visitFloatingRemainder() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitLeftShift(
      LeftShiftNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitLeftShift();
    return noStoreChanges(value, input);
  }

  Nullness visitLeftShift() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitSignedRightShift(
      SignedRightShiftNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitSignedRightShift();
    return noStoreChanges(value, input);
  }

  Nullness visitSignedRightShift() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitUnsignedRightShift(
      UnsignedRightShiftNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitUnsignedRightShift();
    return noStoreChanges(value, input);
  }

  Nullness visitUnsignedRightShift() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitBitwiseAnd(
      BitwiseAndNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitBitwiseAnd();
    return noStoreChanges(value, input);
  }

  Nullness visitBitwiseAnd() {
    return visitBitwiseOperation();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitBitwiseOr(
      BitwiseOrNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitBitwiseOr();
    return noStoreChanges(value, input);
  }

  Nullness visitBitwiseOr() {
    return visitBitwiseOperation();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitBitwiseXor(
      BitwiseXorNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitBitwiseXor();
    return noStoreChanges(value, input);
  }

  Nullness visitBitwiseXor() {
    return visitBitwiseOperation();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitStringConcatenateAssignment(
      StringConcatenateAssignmentNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitStringConcatenateAssignment();
    return noStoreChanges(value, input);
  }

  Nullness visitStringConcatenateAssignment() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitLessThan(
      LessThanNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitLessThan();
    return noStoreChanges(value, input);
  }

  Nullness visitLessThan() {
    return visitNumericalComparison();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitLessThanOrEqual(
      LessThanOrEqualNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitLessThanOrEqual();
    return noStoreChanges(value, input);
  }

  Nullness visitLessThanOrEqual() {
    return visitNumericalComparison();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitGreaterThan(
      GreaterThanNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitGreaterThan();
    return noStoreChanges(value, input);
  }

  Nullness visitGreaterThan() {
    return visitNumericalComparison();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitGreaterThanOrEqual(
      GreaterThanOrEqualNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitGreaterThanOrEqual();
    return noStoreChanges(value, input);
  }

  Nullness visitGreaterThanOrEqual() {
    return visitNumericalComparison();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitTernaryExpression(
      TernaryExpressionNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness result = visitTernaryExpression(node, values(input));
    // TODO(kmb): Return conditional result if node itself is of boolean type, as for method calls
    return noStoreChanges(result, input);
  }

  Nullness visitTernaryExpression(TernaryExpressionNode node, SubNodeValues inputs) {
    return inputs
        .valueOfSubNode(node.getThenOperand())
        .leastUpperBound(inputs.valueOfSubNode(node.getElseOperand()));
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitVariableDeclaration(
      VariableDeclarationNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    visitVariableDeclaration(node, values(input), updates);
    /*
     * We can return whatever we want here because a variable declaration is not an expression and
     * thus no one can use its value directly. Any updates to the nullness of the variable are
     * performed in the store so that they are available to future reads.
     */
    Nullness result = BOTTOM;
    return updateRegularStore(result, input, updates);
  }

  void visitVariableDeclaration(
      VariableDeclarationNode node, SubNodeValues inputs, LocalVariableUpdates updates) {}

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitMethodAccess(
      MethodAccessNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitMethodAccess();
    return noStoreChanges(value, input);
  }

  Nullness visitMethodAccess() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitArrayAccess(
      ArrayAccessNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    Nullness result = visitArrayAccess(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  Nullness visitArrayAccess(
      ArrayAccessNode node, SubNodeValues inputs, LocalVariableUpdates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitImplicitThisLiteral(
      ImplicitThisLiteralNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitImplicitThisLiteral();
    return noStoreChanges(value, input);
  }

  Nullness visitImplicitThisLiteral() {
    return visitThisLiteral();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitExplicitThisLiteral(
      ExplicitThisLiteralNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitExplicitThisLiteral();
    return noStoreChanges(value, input);
  }

  Nullness visitExplicitThisLiteral() {
    return visitThisLiteral();
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitSuper(
      SuperNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitSuper();
    return noStoreChanges(value, input);
  }

  Nullness visitSuper() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitReturn(
      ReturnNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitReturn();
    return noStoreChanges(value, input);
  }

  Nullness visitReturn() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitStringConversion(
      StringConversionNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitStringConversion();
    return noStoreChanges(value, input);
  }

  Nullness visitStringConversion() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitWideningConversion(
      WideningConversionNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitWideningConversion();
    return noStoreChanges(value, input);
  }

  Nullness visitWideningConversion() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitInstanceOf(
      InstanceOfNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    ReadableLocalVariableUpdates thenUpdates = new ReadableLocalVariableUpdates();
    ReadableLocalVariableUpdates elseUpdates = new ReadableLocalVariableUpdates();
    Nullness result = visitInstanceOf(node, values(input), thenUpdates, elseUpdates);
    ResultingStore thenStore = updateStore(input.getThenStore(), thenUpdates);
    ResultingStore elseStore = updateStore(input.getElseStore(), elseUpdates);
    return new ConditionalTransferResult<>(
        result, thenStore.store, elseStore.store, thenStore.storeChanged | elseStore.storeChanged);
  }

  Nullness visitInstanceOf(
      InstanceOfNode node,
      SubNodeValues inputs,
      LocalVariableUpdates thenUpdates,
      LocalVariableUpdates elseUpdates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitSynchronized(
      SynchronizedNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    Nullness result = visitSynchronized(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  Nullness visitSynchronized(
      SynchronizedNode node, SubNodeValues inputs, LocalVariableUpdates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitAssertionError(
      AssertionErrorNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitAssertionError();
    return noStoreChanges(value, input);
  }

  Nullness visitAssertionError() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitThrow(
      ThrowNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    Nullness result = visitThrow(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  Nullness visitThrow(ThrowNode node, SubNodeValues inputs, LocalVariableUpdates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitCase(
      CaseNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitCase();
    return noStoreChanges(value, input);
  }

  Nullness visitCase() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitMemberReference(
      FunctionalInterfaceNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    Nullness result = visitMemberReference(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  Nullness visitMemberReference(
      FunctionalInterfaceNode node, SubNodeValues inputs, LocalVariableUpdates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitArrayCreation(
      ArrayCreationNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    Nullness result = visitArrayCreation(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  Nullness visitArrayCreation(
      ArrayCreationNode node, SubNodeValues inputs, LocalVariableUpdates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitArrayType(
      ArrayTypeNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitArrayType();
    return noStoreChanges(value, input);
  }

  Nullness visitArrayType() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitPrimitiveType(
      PrimitiveTypeNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitPrimitiveType();
    return noStoreChanges(value, input);
  }

  Nullness visitPrimitiveType() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitClassName(
      ClassNameNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitClassName();
    return noStoreChanges(value, input);
  }

  Nullness visitClassName() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitPackageName(
      PackageNameNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitPackageName();
    return noStoreChanges(value, input);
  }

  Nullness visitPackageName() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitParameterizedType(
      ParameterizedTypeNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    Nullness value = visitParameterizedType();
    return noStoreChanges(value, input);
  }

  Nullness visitParameterizedType() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, LocalStore<Nullness>> visitMarker(
      MarkerNode node, TransferInput<Nullness, LocalStore<Nullness>> input) {
    ReadableLocalVariableUpdates updates = new ReadableLocalVariableUpdates();
    Nullness result = visitMarker(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  Nullness visitMarker(MarkerNode node, SubNodeValues inputs, LocalVariableUpdates updates) {
    return NULLABLE;
  }

  private static final class ReadableLocalVariableUpdates implements LocalVariableUpdates {
    final Map<Element, Nullness> values = new HashMap<>();

    @Override
    public void set(LocalVariableNode node, Nullness value) {
      values.put(node.getElement(), checkNotNull(value));
    }

    @Override
    public void set(VariableDeclarationNode node, Nullness value) {
      values.put(elementFromDeclaration(node.getTree()), checkNotNull(value));
    }
  }

  @CheckReturnValue
  private static ResultingStore updateStore(
      LocalStore<Nullness> oldStore, ReadableLocalVariableUpdates... updates) {
    LocalStore.Builder<Nullness> builder = oldStore.toBuilder();
    for (ReadableLocalVariableUpdates update : updates) {
      for (Entry<Element, Nullness> entry : update.values.entrySet()) {
        builder.setInformation(entry.getKey(), entry.getValue());
      }
    }
    LocalStore<Nullness> newStore = builder.build();
    return new ResultingStore(newStore, !newStore.equals(oldStore));
  }

  private static SubNodeValues values(final TransferInput<Nullness, LocalStore<Nullness>> input) {
    return new SubNodeValues() {
      @Override
      public Nullness valueOfSubNode(Node node) {
        return input.getValueOfSubNode(node);
      }
    };
  }

  private static final class ResultingStore {
    final LocalStore<Nullness> store;
    final boolean storeChanged;

    ResultingStore(LocalStore<Nullness> store, boolean storeChanged) {
      this.store = store;
      this.storeChanged = storeChanged;
    }
  }

  private static final boolean NO_STORE_CHANGE = false;
}
