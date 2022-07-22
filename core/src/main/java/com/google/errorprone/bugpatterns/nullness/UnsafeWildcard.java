/*
 * Copyright 2019 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static java.lang.Math.min;

import com.google.common.collect.ImmutableListMultimap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.AssignmentTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ConditionalExpressionTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.LambdaExpressionTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ParenthesizedTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.TypeCastTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.IntersectionClassType;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.code.Type.UnionClassType;
import com.sun.tools.javac.code.Type.WildcardType;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCParens;
import com.sun.tools.javac.tree.JCTree.JCTypeCast;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import java.util.HashSet;
import java.util.Map;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;

/** Check to detect unsafe upcasts of {@code null} values to wildcard types. */
@BugPattern(summary = "Certain wildcard types can confuse the compiler.", severity = ERROR)
public class UnsafeWildcard extends BugChecker
    implements AssignmentTreeMatcher,
        ClassTreeMatcher,
        ConditionalExpressionTreeMatcher,
        LambdaExpressionTreeMatcher,
        MethodInvocationTreeMatcher,
        NewClassTreeMatcher,
        ParenthesizedTreeMatcher,
        ReturnTreeMatcher,
        TypeCastTreeMatcher,
        VariableTreeMatcher {

  @Override
  public Description matchAssignment(AssignmentTree tree, VisitorState state) {
    return checkForUnsafeNullAssignment(
        ((JCExpression) tree.getVariable()).type, tree.getExpression(), state);
  }

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    JCClassDecl classDecl = (JCClassDecl) tree;
    // Check "extends" and "implements" for unsafe wildcards
    for (JCExpression implemented : classDecl.getImplementsClause()) {
      state.reportMatch(
          checkForUnsafeWildcards(implemented, "Unsafe wildcard type: ", implemented.type, state));
    }
    if (classDecl.getExtendsClause() != null) {
      return checkForUnsafeWildcards(
          classDecl.getExtendsClause(),
          "Unsafe wildcard type: ",
          classDecl.getExtendsClause().type,
          state);
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchConditionalExpression(
      ConditionalExpressionTree tree, VisitorState state) {
    // Ternary branches are implicitly upcast, so check in case they're null
    Type ternaryType = ((JCExpression) tree).type;
    state.reportMatch(checkForUnsafeNullAssignment(ternaryType, tree.getTrueExpression(), state));
    return checkForUnsafeNullAssignment(ternaryType, tree.getFalseExpression(), state);
  }

  @Override
  public Description matchLambdaExpression(LambdaExpressionTree tree, VisitorState state) {
    if (tree.getBody() instanceof ExpressionTree) {
      Type targetType = ((JCLambda) tree).getDescriptorType(state.getTypes()).getReturnType();
      return checkForUnsafeNullAssignment(targetType, (ExpressionTree) tree.getBody(), state);
    } // else covered by matchReturn
    return Description.NO_MATCH;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    // MethodType of this invocation gives us the args' target types with any type parameters
    // substituted (unlike the method's symbol, which doesn't give us effective target types).
    MethodType mtype = (MethodType) ((JCMethodInvocation) tree).meth.type;
    MethodSymbol callee = ASTHelpers.getSymbol(tree);
    if (!((JCMethodInvocation) tree).getTypeArguments().isEmpty()) {
      // Check type arguments for problematic wildcards given in source.
      for (JCExpression typearg : ((JCMethodInvocation) tree).getTypeArguments()) {
        state.reportMatch(
            checkForUnsafeWildcards(typearg, "Unsafe wildcard type: ", typearg.type, state));
      }
    } else if (!callee.type.getTypeArguments().isEmpty()) {
      // Otherwise, check any inferred type arguments
      ImmutableListMultimap<TypeVariableSymbol, Type> mapping =
          ASTHelpers.getTypeSubstitution(mtype, callee);
      HashSet<Type> seen = new HashSet<>();
      for (Map.Entry<TypeVariableSymbol, Type> inferredTypearg : mapping.entries()) {
        if (!seen.add(inferredTypearg.getValue())) {
          continue; // avoid duplicate reports for the same type
        }
        state.reportMatch(
            checkForUnsafeWildcards(
                tree,
                "Unsafe wildcard in inferred type argument for callee's type parameter "
                    + inferredTypearg.getKey()
                    + ": ",
                inferredTypearg.getValue(),
                state));
      }
    }

    int paramIndex = 0;
    for (ExpressionTree arg : tree.getArguments()) {
      // Check null arguments against parameter type
      // NB: this will be an array type for vararg parameters, but checkForUnsafeNullAssignment
      // sees through array types, so there's no need to unwrap the type here
      Type paramType = mtype.argtypes.get(paramIndex);
      state.reportMatch(checkForUnsafeNullAssignment(paramType, arg, state));
      paramIndex = min(paramIndex + 1, mtype.argtypes.size() - 1);
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    // MethodType of this invocation gives us the args' target types with any type parameters
    // substituted (unlike the method's symbol, which doesn't give us effective target types).
    MethodType mtype = (MethodType) ((JCNewClass) tree).constructorType;
    // mtype contains type of outer object in some cases, which we can skip since null enclosing
    // expression would cause exception.
    int paramIndex = tree.getClassBody() != null && tree.getEnclosingExpression() != null ? 1 : 0;
    for (ExpressionTree arg : tree.getArguments()) {
      // Check null arguments against parameter type
      // NB: this will be an array type for vararg parameters, but checkForUnsafeNullAssignment
      // sees through array types, so there's no need to unwrap the type here
      Type paramType = mtype.argtypes.get(paramIndex);
      state.reportMatch(checkForUnsafeNullAssignment(paramType, arg, state));
      paramIndex = min(paramIndex + 1, mtype.argtypes.size() - 1);
    }
    // Check type arguments for problematic wildcards (visiting class type will recursively visit
    // its arguments
    return checkForUnsafeWildcards(
        tree, "Unsafe wildcard type argument: ", ((JCNewClass) tree).type, state);
  }

  @Override
  public Description matchParenthesized(ParenthesizedTree tree, VisitorState state) {
    // Treat (null) like null
    return checkForUnsafeNullAssignment(((JCParens) tree).type, tree.getExpression(), state);
  }

  @Override
  public Description matchReturn(ReturnTree tree, VisitorState state) {
    // Check "return null" against return type
    if (tree.getExpression() == null
        || ((JCExpression) tree.getExpression()).type.getKind() != TypeKind.NULL) {
      return Description.NO_MATCH;
    }

    // Figure out return type of surrounding method or lambda and check it
    Tree method = state.findEnclosing(MethodTree.class, LambdaExpressionTree.class);
    if (method instanceof MethodTree) {
      return checkForUnsafeNullAssignment(
          ((JCMethodDecl) method).getReturnType().type, tree.getExpression(), state);
    } else if (method instanceof LambdaExpressionTree) {
      Type targetType = ((JCLambda) method).getDescriptorType(state.getTypes()).getReturnType();
      return checkForUnsafeNullAssignment(targetType, tree.getExpression(), state);
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchTypeCast(TypeCastTree tree, VisitorState state) {
    // Check explicit casts of null
    return checkForUnsafeNullAssignment(
        ((JCTypeCast) tree).getType().type, tree.getExpression(), state);
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    // Check initializer like assignment
    if (tree.getInitializer() != null) {
      return checkForUnsafeNullAssignment(
          ((JCVariableDecl) tree).type, tree.getInitializer(), state);
    }

    VarSymbol symbol = ASTHelpers.getSymbol(tree);
    if (symbol.getKind() == ElementKind.FIELD && (symbol.flags() & Flags.FINAL) == 0) {
      // Fields start out as null, so check their type as if this was a null assignment. While all
      // fields start out null, we ignore them here if they're final or we checked their
      // initializer above. In either case we know we'll see at least one assignment to the field,
      // and if those check out they guarantees us that the field's type is ok: non-null
      // assignments guarantee that there is a concrete type compatible with any
      // wildcards in this field's type, and null assignments will be checked separately (possibly
      // right above).
      return checkForUnsafeWildcards(
          tree,
          "Uninitialized field with unsafe wildcard type: ",
          ((JCVariableDecl) tree).type,
          state);
    }
    return Description.NO_MATCH;
  }

  /**
   * Checks for unsafe wildcards in {@code targetType} if the given expression is `null`.
   *
   * @return diagnostic for {@code tree} if unsafe wildcard is found, {@link Description#NO_MATCH}
   *     otherwise.
   * @see #checkForUnsafeWildcards
   */
  private Description checkForUnsafeNullAssignment(
      Type targetType, ExpressionTree tree, VisitorState state) {
    if (!targetType.isReference() || ((JCExpression) tree).type.getKind() != TypeKind.NULL) {
      return Description.NO_MATCH;
    }
    return checkForUnsafeWildcards(tree, "Cast to wildcard type unsafe: ", targetType, state);
  }

  /**
   * Recursively looks through {@code targetType} for any wildcard whose lower bounds isn't known to
   * be a subtype of the corresponding type parameter's upper bound.
   *
   * @return diagnostic for {@code tree} with given {@code messageHeader} if unsafe wildcard found,
   *     {@link Description#NO_MATCH} otherwise.
   */
  private Description checkForUnsafeWildcards(
      Tree tree, String messageHeader, Type targetType, VisitorState state) {
    while (targetType instanceof ArrayType) {
      // Check array component type
      targetType = ((ArrayType) targetType).getComponentType();
    }
    int i = 0;
    for (Type arg : targetType.getTypeArguments()) {
      // Check components of generic types (getTypeArguments() is empty for other kinds of types)
      if (arg instanceof WildcardType && ((WildcardType) arg).getSuperBound() != null) {
        Type lowerBound = ((WildcardType) arg).getSuperBound();
        // We only check lower bounds that are themselves type variables with trivial upper bounds.
        // Javac already checks other lower bounds, namely lower bounds that are concrete types or
        // type variables with non-trivial upper bounds, to be in bounds of the corresponding type
        // parameter (boundVar below).
        // We skip these cases because the subtype check below can spuriously fail for them because
        // it doesn't correctly substitute type variables when comparing lowerBound and boundVar's
        // upper bound.
        // Note javax.lang.model.type.TypeVariable#getUpperBound() guarantees the result to be non-
        // null for type variables, so we use null check as a proxy for whether lowerBound is a type
        // variable.
        // TODO(kmb): avoid counting on compiler's handling of non-trivial upper bounds here
        if (lowerBound.getUpperBound() != null
            && lowerBound.getUpperBound().toString().endsWith("java.lang.Object")) {
          Type boundVar = targetType.tsym.type.getTypeArguments().get(i);

          if (!state.getTypes().isSubtypeNoCapture(lowerBound, boundVar.getUpperBound())) {
            return buildDescription(tree)
                .setMessage(
                    messageHeader
                        + targetType
                        + " because of type argument "
                        + i
                        + " with implicit upper bound "
                        + boundVar.getUpperBound())
                .build();
          }
        }
        // Also check the super bound itself
        Description contained =
            checkForUnsafeWildcards(tree, messageHeader + i + " nested: ", lowerBound, state);
        if (contained != Description.NO_MATCH) {
          return contained;
        }
      } else if (arg instanceof WildcardType && ((WildcardType) arg).getExtendsBound() != null) {
        // Check the wildcard's bound
        Description contained =
            checkForUnsafeWildcards(
                tree,
                messageHeader + i + " nested: ",
                ((WildcardType) arg).getExtendsBound(),
                state);
        if (contained != Description.NO_MATCH) {
          return contained;
        }
      } else {
        // Check for wildcards in the type argument
        Description contained =
            checkForUnsafeWildcards(tree, messageHeader + i + " nested: ", arg, state);
        if (contained != Description.NO_MATCH) {
          return contained;
        }
      }
      ++i;
    }
    // For union and intersection types, check their components.
    if (targetType instanceof IntersectionClassType) {
      for (Type bound : ((IntersectionClassType) targetType).getExplicitComponents()) {
        Description contained =
            checkForUnsafeWildcards(tree, messageHeader + "bound ", bound, state);
        if (contained != Description.NO_MATCH) {
          return contained;
        }
      }
    }
    if (targetType instanceof UnionClassType) {
      for (Type alternative : ((UnionClassType) targetType).getAlternativeTypes()) {
        Description contained =
            checkForUnsafeWildcards(tree, messageHeader + "alternative ", alternative, state);
        if (contained != Description.NO_MATCH) {
          return contained;
        }
      }
    }
    return Description.NO_MATCH;
  }
}
