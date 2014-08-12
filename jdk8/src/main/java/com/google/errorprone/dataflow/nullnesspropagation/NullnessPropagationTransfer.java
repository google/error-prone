/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

package com.google.errorprone.dataflow.nullnesspropagation;

import com.google.errorprone.dataflow.nullnesspropagation.NullnessValue.Type;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type.JCPrimitiveType;
import com.sun.tools.javac.tree.JCTree.JCIdent;

import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferFunction;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.node.AbstractNodeVisitor;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.BooleanLiteralNode;
import org.checkerframework.dataflow.cfg.node.CharacterLiteralNode;
import org.checkerframework.dataflow.cfg.node.DoubleLiteralNode;
import org.checkerframework.dataflow.cfg.node.EqualToNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.FloatLiteralNode;
import org.checkerframework.dataflow.cfg.node.IntegerLiteralNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.LongLiteralNode;
import org.checkerframework.dataflow.cfg.node.MethodAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.NarrowingConversionNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NotEqualNode;
import org.checkerframework.dataflow.cfg.node.NullLiteralNode;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.ShortLiteralNode;
import org.checkerframework.dataflow.cfg.node.StringLiteralNode;

import java.util.List;

/**
 * This nullness propagation analysis takes on a conservative approach in which local variables
 * assume a {@code NullnessValue} of type non-null only when it is provably non-null. If not enough
 * information is present, the value is unknown and the type defaults to
 * {@code NullnessValue.Type.TOP}.
 * 
 * @author deminguyen@google.com (Demi Nguyen)
 */
public class NullnessPropagationTransfer extends AbstractNodeVisitor<
    TransferResult<NullnessValue, NullnessPropagationStore>,
    TransferInput<NullnessValue, NullnessPropagationStore>>
    implements TransferFunction<NullnessValue, NullnessPropagationStore> {
  
  /**
   * @return The initial store to be used by the org.checkerframework.dataflow analysis.
   *         {@code parameters} is only set if the underlying AST is a method.
   */
  @Override
  public NullnessPropagationStore initialStore(
      UnderlyingAST underlyingAST, List<LocalVariableNode> parameters) {
    return new NullnessPropagationStore();
  }
  
  /**
   * Handles all other nodes that are not of interest
   */
  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitNode(
      Node node, TransferInput<NullnessValue, NullnessPropagationStore> store) {
    return new RegularTransferResult<NullnessValue, NullnessPropagationStore>(
        null, store.getRegularStore());
  }
  
  // Literals
  /**
   * Note: A short literal appears as an int to the compiler, and the compiler can perform a
   * narrowing conversion on the literal to cast from int to short. For example, when assigning a
   * literal to a short variable, the literal does not transfer its own non-null type to the
   * variable. Instead, the variable receives the non-null type from the return value of the
   * conversion call.
   */
  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitShortLiteral(
      ShortLiteralNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    return setAfterStore(node, before, Type.NONNULL);
  }
  
  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitIntegerLiteral(
      IntegerLiteralNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    return setAfterStore(node, before, Type.NONNULL);
  }
  
  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitLongLiteral(
      LongLiteralNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    return setAfterStore(node, before, Type.NONNULL);
  }
  
  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitFloatLiteral(
      FloatLiteralNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    return setAfterStore(node, before, Type.NONNULL);
  }
  
  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitDoubleLiteral(
      DoubleLiteralNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    return setAfterStore(node, before, Type.NONNULL);
  }
  
  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitBooleanLiteral(
      BooleanLiteralNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    return setAfterStore(node, before, Type.NONNULL);
  }
  
  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitCharacterLiteral(
      CharacterLiteralNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    return setAfterStore(node, before, Type.NONNULL);
  }
  
  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitStringLiteral(
      StringLiteralNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    return setAfterStore(node, before, Type.NONNULL);
  }
  
  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitNullLiteral(
      NullLiteralNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    return setAfterStore(node, before, Type.NULLABLE);
  }
  
  /**
   * Refines the {@code NullnessValue} of a {@code LocalVariableNode} used in comparison. If the lhs
   * and rhs are equal, the local variable(s) receives the most refined type between the two
   * operands. Else, the variable(s) retains its original value. A comparison against null always
   * transfers either {@code NullnessValue.Type.TOP} or {@code NullnessValue.Type.NONNULL} to the
   * variable type.
   */
  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitEqualTo(
      EqualToNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    NullnessPropagationStore thenStore = before.getRegularStore();
    NullnessPropagationStore elseStore = thenStore.copy();
    
    Node leftNode = node.getLeftOperand();
    Node rightNode = node.getRightOperand();    
   
    NullnessValue leftVal = thenStore.getInformation(leftNode);
    NullnessValue rightVal = thenStore.getInformation(rightNode);
    NullnessValue info = leftVal.greatestLowerBound(rightVal);

    if (leftNode instanceof LocalVariableNode) {
      if (rightNode instanceof NullLiteralNode) {
        thenStore.setInformation(leftNode, new NullnessValue(Type.NULLABLE));
        elseStore.setInformation(leftNode, new NullnessValue(Type.NONNULL));
      } else {
        thenStore.setInformation(leftNode, info);
      }
    }
    if (rightNode instanceof LocalVariableNode) {
      if (leftNode instanceof NullLiteralNode) {
        thenStore.setInformation(rightNode, new NullnessValue(Type.NULLABLE));
        elseStore.setInformation(rightNode, new NullnessValue(Type.NONNULL));
      } else {
        thenStore.setInformation(rightNode, info);
      }
    }
    
    return new ConditionalTransferResult<NullnessValue, NullnessPropagationStore>(
        null, thenStore, elseStore);
  }
  
  /**
   * Refines the {@code NullnessValue} of a {@code LocalVariableNode} used in a comparison. If the
   * lhs and rhs are not equal, the local variable(s) retains its original value. Else, both sides
   * are equal, and the local variable(s) receives the most refined type between the two operands.
   * A comparison against null always transfers either {@code NullnessValue.Type.TOP} or
   * {@code NullnessValue.Type.NONNULL} to the variable type.
   */
  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitNotEqual(
      NotEqualNode node, TransferInput<NullnessValue, NullnessPropagationStore> pi) {
    NullnessPropagationStore thenStore = pi.getRegularStore();
    NullnessPropagationStore elseStore = thenStore.copy();
    
    Node leftNode = node.getLeftOperand();
    Node rightNode = node.getRightOperand();
    
    NullnessValue leftVal = thenStore.getInformation(leftNode);
    NullnessValue rightVal = thenStore.getInformation(rightNode);    
    NullnessValue info = leftVal.greatestLowerBound(rightVal);
    
    if (leftNode instanceof LocalVariableNode) {
      if (rightNode instanceof NullLiteralNode) {
        thenStore.setInformation(leftNode, new NullnessValue(Type.NONNULL));
        elseStore.setInformation(leftNode, new NullnessValue(Type.NULLABLE));
      }
      elseStore.setInformation(leftNode, info);
    }
    if (rightNode instanceof LocalVariableNode) {
      if (leftNode instanceof NullLiteralNode) {
        thenStore.setInformation(rightNode, new NullnessValue(Type.NONNULL));
        elseStore.setInformation(rightNode, new NullnessValue(Type.NULLABLE));
      }
      elseStore.setInformation(rightNode, info);
    }
    
    return new ConditionalTransferResult<NullnessValue, NullnessPropagationStore>(
        null, thenStore, elseStore);
  }
  
  /**
   * Transfers the value of the rhs to the local variable on the lhs of an assignment statement.
   */
  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitAssignment(
      AssignmentNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    NullnessPropagationStore after = before.getRegularStore();
    Node target = node.getTarget();
    NullnessValue info = null;
    if (target instanceof LocalVariableNode) {
      LocalVariableNode t = (LocalVariableNode) target;
      info = after.getInformation(node.getExpression());
      after.setInformation(t, info);
    }
    return new RegularTransferResult<NullnessValue, NullnessPropagationStore>(info, after);
  }
  
  /**
   * Primitive type variables are always refined to non-null.
   */
  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitLocalVariable(
      LocalVariableNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    NullnessPropagationStore after = before.getRegularStore();
    NullnessValue info = after.getInformation(node);
   
    Tree tree = node.getTree();
    if (tree instanceof JCIdent) {
      JCIdent ident = (JCIdent) tree;
      if (ident.type instanceof JCPrimitiveType) {
        info = new NullnessValue(Type.NONNULL);
      }
    }
    after.setInformation(node, info);
    
    return new RegularTransferResult<NullnessValue, NullnessPropagationStore>(info, after);
  }
  
  /**
   * Refines the receiver of a field access to type non-null after a successful field access.
   * 
   * Note: If the field access occurs when the node is an l-value, the analysis won't
   * call any transfer functions for that node. For example, {@code object.field = var;} would not
   * call {@code visitFieldAccess} for {@code object.field}. On the other hand,
   * {@code var = object.field} would call {@code visitFieldAccess} and make a successful transfer.
   */
  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitFieldAccess(
      FieldAccessNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    Node receiver = node.getReceiver();
    return setAfterStore(receiver, before, Type.NONNULL);
  }
  
  /**
   * Refines the receiver of a method access to type non-null after a successful method access.
   */
  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitMethodAccess(
      MethodAccessNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    Node receiver = node.getReceiver();
    return setAfterStore(receiver, before, Type.NONNULL);
  }
  
  /**
   * JLS 5.1.3 specifies that narrowing conversions among primitive types never result in a run-time
   * exception. As a safeguard, the {@code NarrowingConversionNode} only handles primitive types and
   * throws an assertion error when a non-primitive type is used in the narrowing conversion.
   */
  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitNarrowingConversion(
      NarrowingConversionNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    return setAfterStore(node, before, Type.NONNULL);
  }
  
  /**
   * Refines the receiver of a method invocation to type non-null after successful invocation.
   */
  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitMethodInvocation(
      MethodInvocationNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    Node receiver = node.getTarget().getReceiver();
    return setAfterStore(receiver, before, Type.NONNULL);
  }
  
  /**
   * The node for object instantiation has a non-null type because {@code new} can never return null
   */
  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitObjectCreation(
      ObjectCreationNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    return setAfterStore(node, before, Type.NONNULL);
  }
  
  /**
   * Maps the given node to the specified value in the resulting store from the node visit
   * 
   * @param node The node of interest getting mapped to a value.
   * @param before The input info from before the node visitation.
   */
  private static RegularTransferResult<NullnessValue, NullnessPropagationStore> setAfterStore(
      Node node, TransferInput<NullnessValue, NullnessPropagationStore> before, Type value) {
    NullnessPropagationStore after = before.getRegularStore();
    NullnessValue info = new NullnessValue(value);
    after.setInformation(node, info);
    return new RegularTransferResult<NullnessValue, NullnessPropagationStore>(info, after);
  }
}

