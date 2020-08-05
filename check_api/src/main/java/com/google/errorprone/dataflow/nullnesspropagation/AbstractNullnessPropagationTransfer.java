/*
 * Copyright 2014 The Error Prone Authors.
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

import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.dataflow.AccessPath;
import com.google.errorprone.dataflow.AccessPathStore;
import com.google.errorprone.dataflow.AccessPathValues;
import com.google.errorprone.dataflow.LocalVariableValues;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.checkerframework.shaded.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.shaded.dataflow.analysis.ForwardTransferFunction;
import org.checkerframework.shaded.dataflow.analysis.RegularTransferResult;
import org.checkerframework.shaded.dataflow.analysis.TransferInput;
import org.checkerframework.shaded.dataflow.analysis.TransferResult;
import org.checkerframework.shaded.dataflow.cfg.UnderlyingAST;
import org.checkerframework.shaded.dataflow.cfg.node.ArrayAccessNode;
import org.checkerframework.shaded.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.shaded.dataflow.cfg.node.ArrayTypeNode;
import org.checkerframework.shaded.dataflow.cfg.node.AssertionErrorNode;
import org.checkerframework.shaded.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.shaded.dataflow.cfg.node.BitwiseAndNode;
import org.checkerframework.shaded.dataflow.cfg.node.BitwiseComplementNode;
import org.checkerframework.shaded.dataflow.cfg.node.BitwiseOrNode;
import org.checkerframework.shaded.dataflow.cfg.node.BitwiseXorNode;
import org.checkerframework.shaded.dataflow.cfg.node.BooleanLiteralNode;
import org.checkerframework.shaded.dataflow.cfg.node.CaseNode;
import org.checkerframework.shaded.dataflow.cfg.node.CharacterLiteralNode;
import org.checkerframework.shaded.dataflow.cfg.node.ClassDeclarationNode;
import org.checkerframework.shaded.dataflow.cfg.node.ClassNameNode;
import org.checkerframework.shaded.dataflow.cfg.node.ConditionalAndNode;
import org.checkerframework.shaded.dataflow.cfg.node.ConditionalNotNode;
import org.checkerframework.shaded.dataflow.cfg.node.ConditionalOrNode;
import org.checkerframework.shaded.dataflow.cfg.node.DoubleLiteralNode;
import org.checkerframework.shaded.dataflow.cfg.node.EqualToNode;
import org.checkerframework.shaded.dataflow.cfg.node.ExplicitThisLiteralNode;
import org.checkerframework.shaded.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.shaded.dataflow.cfg.node.FloatLiteralNode;
import org.checkerframework.shaded.dataflow.cfg.node.FloatingDivisionNode;
import org.checkerframework.shaded.dataflow.cfg.node.FloatingRemainderNode;
import org.checkerframework.shaded.dataflow.cfg.node.FunctionalInterfaceNode;
import org.checkerframework.shaded.dataflow.cfg.node.GreaterThanNode;
import org.checkerframework.shaded.dataflow.cfg.node.GreaterThanOrEqualNode;
import org.checkerframework.shaded.dataflow.cfg.node.ImplicitThisLiteralNode;
import org.checkerframework.shaded.dataflow.cfg.node.InstanceOfNode;
import org.checkerframework.shaded.dataflow.cfg.node.IntegerDivisionNode;
import org.checkerframework.shaded.dataflow.cfg.node.IntegerLiteralNode;
import org.checkerframework.shaded.dataflow.cfg.node.IntegerRemainderNode;
import org.checkerframework.shaded.dataflow.cfg.node.LambdaResultExpressionNode;
import org.checkerframework.shaded.dataflow.cfg.node.LeftShiftNode;
import org.checkerframework.shaded.dataflow.cfg.node.LessThanNode;
import org.checkerframework.shaded.dataflow.cfg.node.LessThanOrEqualNode;
import org.checkerframework.shaded.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.shaded.dataflow.cfg.node.LongLiteralNode;
import org.checkerframework.shaded.dataflow.cfg.node.MarkerNode;
import org.checkerframework.shaded.dataflow.cfg.node.MethodAccessNode;
import org.checkerframework.shaded.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.shaded.dataflow.cfg.node.NarrowingConversionNode;
import org.checkerframework.shaded.dataflow.cfg.node.Node;
import org.checkerframework.shaded.dataflow.cfg.node.NotEqualNode;
import org.checkerframework.shaded.dataflow.cfg.node.NullChkNode;
import org.checkerframework.shaded.dataflow.cfg.node.NullLiteralNode;
import org.checkerframework.shaded.dataflow.cfg.node.NumericalAdditionNode;
import org.checkerframework.shaded.dataflow.cfg.node.NumericalMinusNode;
import org.checkerframework.shaded.dataflow.cfg.node.NumericalMultiplicationNode;
import org.checkerframework.shaded.dataflow.cfg.node.NumericalPlusNode;
import org.checkerframework.shaded.dataflow.cfg.node.NumericalSubtractionNode;
import org.checkerframework.shaded.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.shaded.dataflow.cfg.node.PackageNameNode;
import org.checkerframework.shaded.dataflow.cfg.node.ParameterizedTypeNode;
import org.checkerframework.shaded.dataflow.cfg.node.PrimitiveTypeNode;
import org.checkerframework.shaded.dataflow.cfg.node.ReturnNode;
import org.checkerframework.shaded.dataflow.cfg.node.ShortLiteralNode;
import org.checkerframework.shaded.dataflow.cfg.node.SignedRightShiftNode;
import org.checkerframework.shaded.dataflow.cfg.node.StringConcatenateAssignmentNode;
import org.checkerframework.shaded.dataflow.cfg.node.StringConcatenateNode;
import org.checkerframework.shaded.dataflow.cfg.node.StringConversionNode;
import org.checkerframework.shaded.dataflow.cfg.node.StringLiteralNode;
import org.checkerframework.shaded.dataflow.cfg.node.SuperNode;
import org.checkerframework.shaded.dataflow.cfg.node.SynchronizedNode;
import org.checkerframework.shaded.dataflow.cfg.node.TernaryExpressionNode;
import org.checkerframework.shaded.dataflow.cfg.node.ThrowNode;
import org.checkerframework.shaded.dataflow.cfg.node.TypeCastNode;
import org.checkerframework.shaded.dataflow.cfg.node.UnsignedRightShiftNode;
import org.checkerframework.shaded.dataflow.cfg.node.VariableDeclarationNode;
import org.checkerframework.shaded.dataflow.cfg.node.WideningConversionNode;

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
    implements ForwardTransferFunction<Nullness, AccessPathStore<Nullness>> {
  @Override
  public AccessPathStore<Nullness> initialStore(
      UnderlyingAST underlyingAST, List<LocalVariableNode> parameters) {
    return AccessPathStore.empty();
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
  interface Updates {
    // TODO(cpovirk): consider the API setIfLocalVariable(Node, Nullness)
    void set(LocalVariableNode node, Nullness value);

    void set(VariableDeclarationNode node, Nullness value);

    void set(FieldAccessNode node, Nullness value);

    void set(AccessPath path, Nullness value);
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
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitNullLiteral(
      NullLiteralNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    ReadableUpdates updates = new ReadableUpdates();
    Nullness result = visitNullLiteral();
    return updateRegularStore(result, input, updates);
  }

  Nullness visitNullLiteral() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitTypeCast(
      TypeCastNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness result = visitTypeCast(node, values(input));
    return noStoreChanges(result, input);
  }

  Nullness visitTypeCast(TypeCastNode node, SubNodeValues inputs) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitNumericalAddition(
      NumericalAdditionNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness result = visitNumericalAddition();
    return noStoreChanges(result, input);
  }

  Nullness visitNumericalAddition() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitNarrowingConversion(
      NarrowingConversionNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness result = visitNarrowingConversion();
    return noStoreChanges(result, input);
  }

  Nullness visitNarrowingConversion() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitEqualTo(
      EqualToNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    ReadableUpdates thenUpdates = new ReadableUpdates();
    ReadableUpdates elseUpdates = new ReadableUpdates();
    visitEqualTo(node, values(input), thenUpdates, elseUpdates);
    ResultingStore thenStore = updateStore(input.getThenStore(), thenUpdates);
    ResultingStore elseStore = updateStore(input.getElseStore(), elseUpdates);
    return conditionalResult(
        thenStore.store, elseStore.store, thenStore.storeChanged || elseStore.storeChanged);
  }

  void visitEqualTo(
      EqualToNode node, SubNodeValues inputs, Updates thenUpdates, Updates elseUpdates) {}

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitNotEqual(
      NotEqualNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    ReadableUpdates thenUpdates = new ReadableUpdates();
    ReadableUpdates elseUpdates = new ReadableUpdates();
    visitNotEqual(node, values(input), thenUpdates, elseUpdates);
    ResultingStore thenStore = updateStore(input.getThenStore(), thenUpdates);
    ResultingStore elseStore = updateStore(input.getElseStore(), elseUpdates);
    return conditionalResult(
        thenStore.store, elseStore.store, thenStore.storeChanged || elseStore.storeChanged);
  }

  void visitNotEqual(
      NotEqualNode node, SubNodeValues inputs, Updates thenUpdates, Updates elseUpdates) {}

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitAssignment(
      AssignmentNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    ReadableUpdates updates = new ReadableUpdates();
    Nullness result = visitAssignment(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  Nullness visitAssignment(AssignmentNode node, SubNodeValues inputs, Updates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitLocalVariable(
      LocalVariableNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    ReadableUpdates updates = new ReadableUpdates();
    Nullness result = visitLocalVariable(node, input.getRegularStore());
    return updateRegularStore(result, input, updates);
  }

  Nullness visitLocalVariable(LocalVariableNode node, LocalVariableValues<Nullness> store) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitFieldAccess(
      FieldAccessNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    ReadableUpdates updates = new ReadableUpdates();
    Nullness result = visitFieldAccess(node, updates, input.getRegularStore());
    return updateRegularStore(result, input, updates);
  }

  Nullness visitFieldAccess(
      FieldAccessNode node, Updates updates, AccessPathValues<Nullness> store) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitMethodInvocation(
      MethodInvocationNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    ReadableUpdates thenUpdates = new ReadableUpdates();
    ReadableUpdates elseUpdates = new ReadableUpdates();
    ReadableUpdates bothUpdates = new ReadableUpdates();
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
          thenStore.store, elseStore.store, thenStore.storeChanged || elseStore.storeChanged);
    } else {
      return updateRegularStore(result, input, bothUpdates);
    }
  }

  Nullness visitMethodInvocation(
      MethodInvocationNode node, Updates thenUpdates, Updates elseUpdates, Updates bothUpdates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitConditionalAnd(
      ConditionalAndNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    return conditionalResult(input.getThenStore(), input.getElseStore(), NO_STORE_CHANGE);
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitConditionalOr(
      ConditionalOrNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    return conditionalResult(input.getThenStore(), input.getElseStore(), NO_STORE_CHANGE);
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitConditionalNot(
      ConditionalNotNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    /*
     * Weird case: We swap the contents of the THEN and ELSE stores without otherwise modifying
     * them. Presumably that can still count as a change?
     */
    boolean storeChanged = !input.getThenStore().equals(input.getElseStore());
    return conditionalResult(
        /* thenStore= */ input.getElseStore(), /* elseStore= */ input.getThenStore(), storeChanged);
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitObjectCreation(
      ObjectCreationNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness result = visitObjectCreation();
    return noStoreChanges(result, input);
  }

  Nullness visitObjectCreation() {
    return NULLABLE;
  }

  private static TransferResult<Nullness, AccessPathStore<Nullness>> noStoreChanges(
      Nullness value, TransferInput<?, AccessPathStore<Nullness>> input) {
    return new RegularTransferResult<>(value, input.getRegularStore());
  }

  @CheckReturnValue
  private TransferResult<Nullness, AccessPathStore<Nullness>> updateRegularStore(
      Nullness value, TransferInput<?, AccessPathStore<Nullness>> input, ReadableUpdates updates) {
    ResultingStore newStore = updateStore(input.getRegularStore(), updates);
    return new RegularTransferResult<>(value, newStore.store, newStore.storeChanged);
  }

  private static TransferResult<Nullness, AccessPathStore<Nullness>> conditionalResult(
      AccessPathStore<Nullness> thenStore,
      AccessPathStore<Nullness> elseStore,
      boolean storeChanged) {
    return new ConditionalTransferResult<>(NONNULL, thenStore, elseStore, storeChanged);
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitShortLiteral(
      ShortLiteralNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness result = visitShortLiteral();
    return noStoreChanges(result, input);
  }

  Nullness visitShortLiteral() {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitIntegerLiteral(
      IntegerLiteralNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness result = visitIntegerLiteral();
    return noStoreChanges(result, input);
  }

  Nullness visitIntegerLiteral() {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitLongLiteral(
      LongLiteralNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness result = visitLongLiteral();
    return noStoreChanges(result, input);
  }

  Nullness visitLongLiteral() {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitFloatLiteral(
      FloatLiteralNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness result = visitFloatLiteral();
    return noStoreChanges(result, input);
  }

  Nullness visitFloatLiteral() {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitDoubleLiteral(
      DoubleLiteralNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness result = visitDoubleLiteral();
    return noStoreChanges(result, input);
  }

  Nullness visitDoubleLiteral() {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitBooleanLiteral(
      BooleanLiteralNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness result = visitBooleanLiteral();
    return noStoreChanges(result, input);
  }

  Nullness visitBooleanLiteral() {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitCharacterLiteral(
      CharacterLiteralNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    ReadableUpdates updates = new ReadableUpdates();
    Nullness result = visitCharacterLiteral(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  Nullness visitCharacterLiteral(CharacterLiteralNode node, SubNodeValues inputs, Updates updates) {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitStringLiteral(
      StringLiteralNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    ReadableUpdates updates = new ReadableUpdates();
    Nullness result = visitStringLiteral(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  Nullness visitStringLiteral(StringLiteralNode node, SubNodeValues inputs, Updates updates) {
    return visitValueLiteral();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitNumericalMinus(
      NumericalMinusNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitNumericalMinus();
    return noStoreChanges(value, input);
  }

  Nullness visitNumericalMinus() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitNumericalPlus(
      NumericalPlusNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitNumericalPlus();
    return noStoreChanges(value, input);
  }

  Nullness visitNumericalPlus() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitBitwiseComplement(
      BitwiseComplementNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitBitwiseComplement();
    return noStoreChanges(value, input);
  }

  Nullness visitBitwiseComplement() {
    return visitBitwiseOperation();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitNullChk(
      NullChkNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitNullChk();
    return noStoreChanges(value, input);
  }

  Nullness visitNullChk() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitStringConcatenate(
      StringConcatenateNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitStringConcatenate();
    return noStoreChanges(value, input);
  }

  Nullness visitStringConcatenate() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitNumericalSubtraction(
      NumericalSubtractionNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitNumericalSubtraction();
    return noStoreChanges(value, input);
  }

  Nullness visitNumericalSubtraction() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitNumericalMultiplication(
      NumericalMultiplicationNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitNumericalMultiplication();
    return noStoreChanges(value, input);
  }

  Nullness visitNumericalMultiplication() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitIntegerDivision(
      IntegerDivisionNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitIntegerDivision();
    return noStoreChanges(value, input);
  }

  Nullness visitIntegerDivision() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitFloatingDivision(
      FloatingDivisionNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitFloatingDivision();
    return noStoreChanges(value, input);
  }

  Nullness visitFloatingDivision() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitIntegerRemainder(
      IntegerRemainderNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitIntegerRemainder();
    return noStoreChanges(value, input);
  }

  Nullness visitIntegerRemainder() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitFloatingRemainder(
      FloatingRemainderNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitFloatingRemainder();
    return noStoreChanges(value, input);
  }

  Nullness visitFloatingRemainder() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitLeftShift(
      LeftShiftNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitLeftShift();
    return noStoreChanges(value, input);
  }

  Nullness visitLeftShift() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitSignedRightShift(
      SignedRightShiftNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitSignedRightShift();
    return noStoreChanges(value, input);
  }

  Nullness visitSignedRightShift() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitUnsignedRightShift(
      UnsignedRightShiftNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitUnsignedRightShift();
    return noStoreChanges(value, input);
  }

  Nullness visitUnsignedRightShift() {
    return visitNumericalOperation();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitBitwiseAnd(
      BitwiseAndNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitBitwiseAnd();
    return noStoreChanges(value, input);
  }

  Nullness visitBitwiseAnd() {
    return visitBitwiseOperation();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitBitwiseOr(
      BitwiseOrNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitBitwiseOr();
    return noStoreChanges(value, input);
  }

  Nullness visitBitwiseOr() {
    return visitBitwiseOperation();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitBitwiseXor(
      BitwiseXorNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitBitwiseXor();
    return noStoreChanges(value, input);
  }

  Nullness visitBitwiseXor() {
    return visitBitwiseOperation();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitStringConcatenateAssignment(
      StringConcatenateAssignmentNode node,
      TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitStringConcatenateAssignment();
    return noStoreChanges(value, input);
  }

  Nullness visitStringConcatenateAssignment() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitLessThan(
      LessThanNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitLessThan();
    return noStoreChanges(value, input);
  }

  Nullness visitLessThan() {
    return visitNumericalComparison();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitLessThanOrEqual(
      LessThanOrEqualNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitLessThanOrEqual();
    return noStoreChanges(value, input);
  }

  Nullness visitLessThanOrEqual() {
    return visitNumericalComparison();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitGreaterThan(
      GreaterThanNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitGreaterThan();
    return noStoreChanges(value, input);
  }

  Nullness visitGreaterThan() {
    return visitNumericalComparison();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitGreaterThanOrEqual(
      GreaterThanOrEqualNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitGreaterThanOrEqual();
    return noStoreChanges(value, input);
  }

  Nullness visitGreaterThanOrEqual() {
    return visitNumericalComparison();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitTernaryExpression(
      TernaryExpressionNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
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
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitVariableDeclaration(
      VariableDeclarationNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    ReadableUpdates updates = new ReadableUpdates();
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
      VariableDeclarationNode node, SubNodeValues inputs, Updates updates) {}

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitMethodAccess(
      MethodAccessNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitMethodAccess();
    return noStoreChanges(value, input);
  }

  Nullness visitMethodAccess() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitArrayAccess(
      ArrayAccessNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    ReadableUpdates updates = new ReadableUpdates();
    Nullness result = visitArrayAccess(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  Nullness visitArrayAccess(ArrayAccessNode node, SubNodeValues inputs, Updates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitImplicitThisLiteral(
      ImplicitThisLiteralNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitImplicitThisLiteral();
    return noStoreChanges(value, input);
  }

  Nullness visitImplicitThisLiteral() {
    return visitThisLiteral();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitExplicitThisLiteral(
      ExplicitThisLiteralNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitExplicitThisLiteral();
    return noStoreChanges(value, input);
  }

  Nullness visitExplicitThisLiteral() {
    return visitThisLiteral();
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitSuper(
      SuperNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitSuper();
    return noStoreChanges(value, input);
  }

  Nullness visitSuper() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitReturn(
      ReturnNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitReturn();
    return noStoreChanges(value, input);
  }

  Nullness visitReturn() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitLambdaResultExpression(
      LambdaResultExpressionNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitLambdaResultExpression();
    return noStoreChanges(value, input);
  }

  Nullness visitLambdaResultExpression() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitStringConversion(
      StringConversionNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitStringConversion();
    return noStoreChanges(value, input);
  }

  Nullness visitStringConversion() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitWideningConversion(
      WideningConversionNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitWideningConversion();
    return noStoreChanges(value, input);
  }

  Nullness visitWideningConversion() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitInstanceOf(
      InstanceOfNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    ReadableUpdates thenUpdates = new ReadableUpdates();
    ReadableUpdates elseUpdates = new ReadableUpdates();
    Nullness result = visitInstanceOf(node, values(input), thenUpdates, elseUpdates);
    ResultingStore thenStore = updateStore(input.getThenStore(), thenUpdates);
    ResultingStore elseStore = updateStore(input.getElseStore(), elseUpdates);
    return new ConditionalTransferResult<>(
        result, thenStore.store, elseStore.store, thenStore.storeChanged || elseStore.storeChanged);
  }

  Nullness visitInstanceOf(
      InstanceOfNode node, SubNodeValues inputs, Updates thenUpdates, Updates elseUpdates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitSynchronized(
      SynchronizedNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    ReadableUpdates updates = new ReadableUpdates();
    Nullness result = visitSynchronized(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  Nullness visitSynchronized(SynchronizedNode node, SubNodeValues inputs, Updates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitAssertionError(
      AssertionErrorNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitAssertionError();
    return noStoreChanges(value, input);
  }

  Nullness visitAssertionError() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitThrow(
      ThrowNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    ReadableUpdates updates = new ReadableUpdates();
    Nullness result = visitThrow(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  Nullness visitThrow(ThrowNode node, SubNodeValues inputs, Updates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitCase(
      CaseNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitCase();
    return noStoreChanges(value, input);
  }

  Nullness visitCase() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitMemberReference(
      FunctionalInterfaceNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    ReadableUpdates updates = new ReadableUpdates();
    Nullness result = visitMemberReference(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  Nullness visitMemberReference(
      FunctionalInterfaceNode node, SubNodeValues inputs, Updates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitArrayCreation(
      ArrayCreationNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    ReadableUpdates updates = new ReadableUpdates();
    Nullness result = visitArrayCreation(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  Nullness visitArrayCreation(ArrayCreationNode node, SubNodeValues inputs, Updates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitArrayType(
      ArrayTypeNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitArrayType();
    return noStoreChanges(value, input);
  }

  Nullness visitArrayType() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitPrimitiveType(
      PrimitiveTypeNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitPrimitiveType();
    return noStoreChanges(value, input);
  }

  Nullness visitPrimitiveType() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitClassName(
      ClassNameNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitClassName();
    return noStoreChanges(value, input);
  }

  Nullness visitClassName() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitPackageName(
      PackageNameNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitPackageName();
    return noStoreChanges(value, input);
  }

  Nullness visitPackageName() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitParameterizedType(
      ParameterizedTypeNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness value = visitParameterizedType();
    return noStoreChanges(value, input);
  }

  Nullness visitParameterizedType() {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitMarker(
      MarkerNode node, TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    ReadableUpdates updates = new ReadableUpdates();
    Nullness result = visitMarker(node, values(input), updates);
    return updateRegularStore(result, input, updates);
  }

  Nullness visitMarker(MarkerNode node, SubNodeValues inputs, Updates updates) {
    return NULLABLE;
  }

  @Override
  public final TransferResult<Nullness, AccessPathStore<Nullness>> visitClassDeclaration(
      ClassDeclarationNode classDeclarationNode,
      TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    Nullness result = visitClassDeclaration();
    return noStoreChanges(result, input);
  }

  Nullness visitClassDeclaration() {
    return NULLABLE;
  }

  private static final class ReadableUpdates implements Updates {
    final Map<AccessPath, Nullness> values = new HashMap<>();

    @Override
    public void set(LocalVariableNode node, Nullness value) {
      values.put(AccessPath.fromLocalVariable(node), checkNotNull(value));
    }

    @Override
    public void set(VariableDeclarationNode node, Nullness value) {
      values.put(AccessPath.fromVariableDecl(node), checkNotNull(value));
    }

    @Override
    public void set(FieldAccessNode node, Nullness value) {
      AccessPath path = AccessPath.fromFieldAccess(node);
      if (path != null) {
        values.put(path, checkNotNull(value));
      }
    }

    @Override
    public void set(AccessPath path, Nullness value) {
      values.put(checkNotNull(path), checkNotNull(value));
    }
  }

  @CheckReturnValue
  private static ResultingStore updateStore(
      AccessPathStore<Nullness> oldStore, ReadableUpdates... updates) {
    AccessPathStore.Builder<Nullness> builder = oldStore.toBuilder();
    for (ReadableUpdates update : updates) {
      for (Map.Entry<AccessPath, Nullness> entry : update.values.entrySet()) {

        builder.setInformation(entry.getKey(), entry.getValue());
      }
    }
    AccessPathStore<Nullness> newStore = builder.build();
    return new ResultingStore(newStore, !newStore.equals(oldStore));
  }

  private static SubNodeValues values(
      final TransferInput<Nullness, AccessPathStore<Nullness>> input) {
    return input::getValueOfSubNode;
  }

  private static final class ResultingStore {
    final AccessPathStore<Nullness> store;
    final boolean storeChanged;

    ResultingStore(AccessPathStore<Nullness> store, boolean storeChanged) {
      this.store = store;
      this.storeChanged = storeChanged;
    }
  }

  private static final boolean NO_STORE_CHANGE = false;
}
