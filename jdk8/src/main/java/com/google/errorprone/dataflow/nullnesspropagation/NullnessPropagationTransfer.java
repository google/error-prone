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

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessValue.NONNULL;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessValue.NULL;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessValue.NULLABLE;

import com.google.common.collect.ImmutableSet;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;

import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferFunction;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.node.AbstractNodeVisitor;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.ConditionalAndNode;
import org.checkerframework.dataflow.cfg.node.ConditionalNotNode;
import org.checkerframework.dataflow.cfg.node.ConditionalOrNode;
import org.checkerframework.dataflow.cfg.node.EqualToNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.NarrowingConversionNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NotEqualNode;
import org.checkerframework.dataflow.cfg.node.NullLiteralNode;
import org.checkerframework.dataflow.cfg.node.NumericalAdditionNode;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.TypeCastNode;
import org.checkerframework.dataflow.cfg.node.ValueLiteralNode;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import javax.lang.model.element.VariableElement;

/**
 * The {@code TransferFunction} for our nullability analysis.  This analysis determines, for all 
 * variables and parameters, whether they are definitely null ({@link NullnessValue#NULL}), 
 * definitely non-null ({@link NullnessValue#NONNULL}), possibly null 
 * ({@link NullnessValue#NULLABLE}), or are on an infeasible path ({@link NullnessValue#BOTTOM}).  
 * This analysis depends only on the code and does not take nullness annotations into account.
 * 
 * @author deminguyen@google.com (Demi Nguyen)
 */
public class NullnessPropagationTransfer extends AbstractNodeVisitor<
    TransferResult<NullnessValue, NullnessPropagationStore>,
    TransferInput<NullnessValue, NullnessPropagationStore>>
    implements TransferFunction<NullnessValue, NullnessPropagationStore> {
  
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
      Node node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    return result(before, NULLABLE);
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
  public TransferResult<NullnessValue, NullnessPropagationStore> visitValueLiteral(
      ValueLiteralNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    return result(before, NONNULL);
  }

  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitNullLiteral(
      NullLiteralNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    return result(before, NULL);
  }

  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitTypeCast(TypeCastNode node,
      TransferInput<NullnessValue, NullnessPropagationStore> before) {
    NullnessValue value = hasPrimitiveType(node)
        ? NONNULL
        : before.getValueOfSubNode(node.getOperand());
    return result(before, value);
  }

  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitNumericalAddition(
      NumericalAdditionNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    // TODO(cpovirk): Handle more numerical operations. Also, mark the inputs as dereferenced.
    return result(before, NONNULL);
  }

  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitNarrowingConversion(
      NarrowingConversionNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    return result(before, NONNULL);
  }

  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitEqualTo(
      EqualToNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    return handleEqualityComparison(true, node.getLeftOperand(), node.getRightOperand(), before);
  }
  
  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitNotEqual(
      NotEqualNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    return handleEqualityComparison(false, node.getLeftOperand(), node.getRightOperand(), before);
  }
  
  /**
   * Transfers the value of the rhs to the local variable on the lhs of an assignment statement.
   */
  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitAssignment(
      AssignmentNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    NullnessValue value = before.getValueOfSubNode(node.getExpression());

    Node target = node.getTarget();
    if (target instanceof LocalVariableNode) {
      before.getRegularStore().setInformation((LocalVariableNode) target, value);
    }

    if (target instanceof FieldAccessNode) {
      FieldAccessNode fieldAccess = (FieldAccessNode) target;
      ClassAndField targetField = tryGetFieldSymbol(target.getTree());
      setReceiverNullness(before, fieldAccess.getReceiver(), targetField);
    }

    /*
     * We propagate the value of the target to the value of the assignment expressions as a whole.
     * We do this regardless of whether the target is a local variable. For example:
     *
     * String s = object.field = "foo"; // Now |s| is non-null.
     *
     * It's not clear to me that this is technically correct, but it works in practice with the
     * bytecode generated by both javac and ecj.
     *
     * http://stackoverflow.com/q/12850676/28465
     */
    return result(before, value);
  }

  /**
   * Variables take their values from their past assignments (as far as they can be determined).
   * Additionally, variables of primitive type are always refined to non-null.
   *
   * <p>(This second case is rarely of interest to us. Either the variable is being used as a
   * primitive, in which case we probably wouldn't have bothered to run the nullness checker on it,
   * or it's being used as an Object, in which case its boxing triggers {@link
   * #visitMethodInvocation}.)
   */
  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitLocalVariable(
      LocalVariableNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    NullnessValue value = hasPrimitiveType(node) || hasNonNullConstantValue(node)
        ? NONNULL
        : before.getRegularStore().getInformation(node);

    return result(before, value);
  }

  /**
   * Refines the receiver of a field access to type non-null after a successful field access, and
   * refines the value of the expression as a whole to non-null if applicable (e.g., if the field
   * has a primitive type).
   *
   * <p>Note: If the field access occurs when the node is an l-value, the analysis won't call this
   * method. Instead, it will call {@link #visitAssignment}.
   */
  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitFieldAccess(
      FieldAccessNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    ClassAndField accessed = tryGetFieldSymbol(node.getTree());
    setReceiverNullness(before, node.getReceiver(), accessed);
    return result(before, fieldNullness(accessed));
  }

  /**
   * Refines the receiver of a method invocation to type non-null after successful invocation, and
   * refines the value of the expression as a whole to non-null if applicable (e.g., if the method
   * returns a primitive type).
   */
  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitMethodInvocation(
      MethodInvocationNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    ClassAndMethod callee = tryGetMethodSymbol(node.getTree());
    setReceiverNullness(before, node.getTarget().getReceiver(), callee);
    return result(before, returnValueNullness(callee));
  }

  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitConditionalAnd(
      ConditionalAndNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    return conditionalResult(before.getThenStore(), before.getElseStore());
  }

  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitConditionalOr(
      ConditionalOrNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    return conditionalResult(before.getThenStore(), before.getElseStore());
  }

  /*
   * I considered changing the vanilla visitNode method to always preserve conditional-ness.
   * However, I was spooked by the fact that this would have done exactly the wrong thing for the !
   * operator.
   */

  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitConditionalNot(
      ConditionalNotNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    return conditionalResult(before.getElseStore(), before.getThenStore());
  }

  /**
   * The node for object instantiation has a non-null type because {@code new} can never return
   * {@code null}.
   */
  @Override
  public TransferResult<NullnessValue, NullnessPropagationStore> visitObjectCreation(
      ObjectCreationNode node, TransferInput<NullnessValue, NullnessPropagationStore> before) {
    return result(before, NONNULL);
  }

  private static RegularTransferResult<NullnessValue, NullnessPropagationStore> result(
      TransferInput<?, NullnessPropagationStore> before, NullnessValue value) {
    return new RegularTransferResult<>(value, before.getRegularStore());
  }

  private static TransferResult<NullnessValue, NullnessPropagationStore> conditionalResult(
      NullnessPropagationStore thenStore, NullnessPropagationStore elseStore) {
    return new ConditionalTransferResult<>(NONNULL, thenStore, elseStore);
  }
  
  /**
   * Refines the {@code NullnessValue} of {@code LocalVariableNode}s used in an equality 
   * comparison using the greatest lower bound.     
   *
   * @param equalTo whether the comparison is == (false for !=)
   * @param leftNode the left-hand side of the comparison
   * @param rightNode the right-hand side of the comparison
   * @param before the input {@code TransferResult}
   * @return the output {@code TransferResult} from this node
   */
  private static TransferResult<NullnessValue, NullnessPropagationStore> handleEqualityComparison(
      boolean equalTo,
      Node leftNode,
      Node rightNode,
      TransferInput<NullnessValue, NullnessPropagationStore> before) {
    NullnessPropagationStore equalBranchStore = before.getRegularStore();
    NullnessPropagationStore notEqualBranchStore = equalBranchStore.copy();
       
    NullnessValue leftVal = before.getValueOfSubNode(leftNode);
    NullnessValue rightVal = before.getValueOfSubNode(rightNode);
    NullnessValue equalBranchValue = leftVal.greatestLowerBound(rightVal);
    
    if (leftNode instanceof LocalVariableNode) {
      LocalVariableNode localVar = (LocalVariableNode) leftNode;
      equalBranchStore.setInformation(localVar, equalBranchValue);
      notEqualBranchStore.setInformation(
          localVar, leftVal.greatestLowerBound(rightVal.deducedValueWhenNotEqual()));
    }
      
    if (rightNode instanceof LocalVariableNode) {
      LocalVariableNode localVar = (LocalVariableNode) rightNode;
      equalBranchStore.setInformation(localVar, equalBranchValue);
      notEqualBranchStore.setInformation(
          localVar, rightVal.greatestLowerBound(leftVal.deducedValueWhenNotEqual()));
    }
    
    return equalTo 
        ? conditionalResult(equalBranchStore, notEqualBranchStore)
        : conditionalResult(notEqualBranchStore, equalBranchStore);
  }

  private static boolean hasPrimitiveType(Node node) {
    return node.getType().getKind().isPrimitive();
  }

  private static boolean hasNonNullConstantValue(LocalVariableNode node) {
    if (node.getElement() instanceof VariableElement) {
      VariableElement element = (VariableElement) node.getElement();
      return (element.getConstantValue() != null);
    }
    return false;
  }

  private static ClassAndField tryGetFieldSymbol(Tree tree) {
    Symbol symbol = tryGetSymbol(tree);
    if (symbol instanceof VarSymbol) {
      return ClassAndField.make((VarSymbol) symbol);
    }
    return null;
  }

  private static ClassAndMethod tryGetMethodSymbol(MethodInvocationTree tree) {
    Symbol symbol = tryGetSymbol(tree.getMethodSelect());
    if (symbol instanceof MethodSymbol) {
      return ClassAndMethod.make((MethodSymbol) symbol);
    }
    return null;
  }

  /*
   * We can't use ASTHelpers here. It's in core, which depends on jdk8, so we can't make jdk8 depend
   * back on core.
   */

  private static Symbol tryGetSymbol(Tree tree) {
    if (tree instanceof JCIdent) {
      return ((JCIdent) tree).sym;
    }
    if (tree instanceof JCFieldAccess) {
      return ((JCFieldAccess) tree).sym;
    }
    return null;
  }

  private static NullnessValue fieldNullness(ClassAndField accessed) {
    if (accessed == null) {
      return NULLABLE;
    }

    if (accessed.field.equals("class")) {
      return NONNULL;
    }
    if (accessed.isEnumConstant) {
      return NONNULL;
    }
    if (accessed.isPrimitive) {
      return NONNULL;
    }
    if (accessed.hasNonNullConstantValue) {
      return NONNULL;
    }

    return NULLABLE;
  }

  private static NullnessValue returnValueNullness(ClassAndMethod callee) {
    if (callee == null) {
      return NULLABLE;
    }

    if (callee.method.equals("valueOf")
        && CLASSES_WITH_NON_NULLABLE_VALUE_OF_METHODS.contains(callee.clazz)) {
      return NONNULL;
    }
    if (callee.isPrimitive) {
      return NONNULL;
    }
    if (CLASSES_WITH_ALL_NON_NULLABLE_RETURNS.contains(callee.clazz)) {
      return NONNULL;
    }

    return NULLABLE;
  }

  private static void setReceiverNullness(
      TransferInput<NullnessValue, NullnessPropagationStore> before, Node receiver, Member member) {
    if (!member.isStatic() && receiver instanceof LocalVariableNode) {
      before.getRegularStore().setInformation((LocalVariableNode) receiver, NONNULL);
    }
  }

  interface Member {
    boolean isStatic();
  }

  private static final class ClassAndMethod implements Member {
    final String clazz;
    final String method;
    final boolean isStatic;
    final boolean isPrimitive;

    private ClassAndMethod(String clazz, String method, boolean isStatic, boolean isPrimitive) {
      this.clazz = clazz;
      this.method = method;
      this.isStatic = isStatic;
      this.isPrimitive = isPrimitive;
    }

    static ClassAndMethod make(MethodSymbol symbol) {
      return new ClassAndMethod(symbol.owner.getQualifiedName().toString(),
          symbol.getSimpleName().toString(), symbol.isStatic(),
          symbol.getReturnType().isPrimitive());
    }

    @Override
    public boolean isStatic() {
      return isStatic;
    }
  }

  private static final class ClassAndField implements Member {
    final String clazz;
    final String field;
    final boolean isStatic;
    final boolean isPrimitive;
    final boolean isEnumConstant;
    final boolean hasNonNullConstantValue;

    private ClassAndField(String clazz, String field, boolean isStatic, boolean isPrimitive,
        boolean isEnumConstant, boolean hasNonNullConstantValue) {
      this.clazz = clazz;
      this.field = field;
      this.isStatic = isStatic;
      this.isPrimitive = isPrimitive;
      this.isEnumConstant = isEnumConstant;
      this.hasNonNullConstantValue = hasNonNullConstantValue;
    }

    static ClassAndField make(VarSymbol symbol) {
      return new ClassAndField(symbol.owner.getQualifiedName().toString(),
          symbol.getSimpleName().toString(), symbol.isStatic(), symbol.type.isPrimitive(),
          symbol.isEnum(), symbol.getConstantValue() != null);
    }

    @Override
    public boolean isStatic() {
      return isStatic;
    }
  }

  private static final ImmutableSet<String> CLASSES_WITH_ALL_NON_NULLABLE_RETURNS =
      ImmutableSet.of(String.class.getName());

  private static final ImmutableSet<String> CLASSES_WITH_NON_NULLABLE_VALUE_OF_METHODS =
      ImmutableSet.of(
          // The primitive types.
          Boolean.class.getName(),
          Byte.class.getName(),
          Character.class.getName(),
          Double.class.getName(),
          Float.class.getName(),
          Integer.class.getName(),
          Long.class.getName(),
          Short.class.getName(),

          // Other types.
          BigDecimal.class.getName(),
          BigInteger.class.getName(),
          // TODO(cpovirk): recognize the compiler-generated valueOf() methods on Enum subclasses
          Enum.class.getName(),
          String.class.getName());
}
