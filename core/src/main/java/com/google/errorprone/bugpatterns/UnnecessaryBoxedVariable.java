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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.ASTHelpers.TargetType;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.lang.model.element.ElementKind;

/**
 * Finds and fixes unnecessarily boxed variables.
 *
 * @author awturner@google.com (Andy Turner)
 */
@BugPattern(
    name = "UnnecessaryBoxedVariable",
    summary = "It is unnecessary for this variable to be boxed. Use the primitive instead.",
    explanation =
        "This variable is of boxed type, but is always unboxed before use. Make it primitive"
            + " instead",
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION,
    severity = SeverityLevel.SUGGESTION)
public class UnnecessaryBoxedVariable extends BugChecker implements VariableTreeMatcher {
  private static final Matcher<ExpressionTree> VALUE_OF_MATCHER =
      MethodMatchers.staticMethod()
          .onClass(UnnecessaryBoxedVariable::isBoxableType)
          .named("valueOf");

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    Optional<Type> unboxed = unboxed(tree, state);
    if (!unboxed.isPresent()) {
      return Description.NO_MATCH;
    }

    VarSymbol varSymbol = ASTHelpers.getSymbol(tree);
    if (varSymbol == null) {
      return Description.NO_MATCH;
    }
    switch (varSymbol.getKind()) {
      case PARAMETER:
        if (!canChangeMethodSignature(state, (MethodSymbol) varSymbol.getEnclosingElement())) {
          return Description.NO_MATCH;
        }
        // Fall through.
      case LOCAL_VARIABLE:
        if (!variableMatches(tree, state)) {
          return Description.NO_MATCH;
        }
        break;
      default:
        return Description.NO_MATCH;
    }

    Optional<TreePath> enclosingMethod = getEnclosingMethod(state.getPath());
    if (!enclosingMethod.isPresent()) {
      return Description.NO_MATCH;
    }

    TreePath path = enclosingMethod.get();
    FindBoxedUsagesScanner scanner = new FindBoxedUsagesScanner(varSymbol, path, state);
    scanner.scan(path, null);
    if (scanner.boxedUsageFound) {
      return Description.NO_MATCH;
    }
    if (!scanner.used && varSymbol.getKind() == ElementKind.PARAMETER) {
      // If it isn't used and it is a parameter, don't fix it, because this could introduce a new
      // NPE.
      return Description.NO_MATCH;
    }

    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
    fixBuilder.replace(tree.getType(), unboxed.get().tsym.getSimpleName().toString());

    fixMethodInvocations(scanner.fixableSimpleMethodInvocations, fixBuilder, state);
    fixCastingInvocations(
        scanner.fixableCastMethodInvocations, enclosingMethod.get(), fixBuilder, state);

    // Remove @Nullable annotation, if present.
    AnnotationTree nullableAnnotation =
        ASTHelpers.getAnnotationWithSimpleName(tree.getModifiers().getAnnotations(), "Nullable");
    if (nullableAnnotation != null) {
      fixBuilder.replace(nullableAnnotation, "");
      return buildDescription(tree)
          .setMessage(
              "This @Nullable is always unboxed when used, which will result in a NPE if it is"
                  + " actually null. Use a primitive if this variable should never be null, or"
                  + " else fix the code to avoid unboxing it.")
          .addFix(fixBuilder.build())
          .build();
    } else {
      return describeMatch(tree, fixBuilder.build());
    }
  }

  private static Optional<Type> unboxed(Tree tree, VisitorState state) {
    Type type = ASTHelpers.getType(tree);
    if (type == null || !type.isReference()) {
      return Optional.empty();
    }
    Type unboxed = state.getTypes().unboxedType(type);
    if (unboxed == null
        || unboxed.getTag() == TypeTag.NONE
        // Don't match java.lang.Void.
        || unboxed.getTag() == TypeTag.VOID) {
      return Optional.empty();
    }
    return Optional.of(unboxed);
  }

  private static void fixMethodInvocations(
      List<MethodInvocationTree> simpleMethodInvocations,
      SuggestedFix.Builder fixBuilder,
      VisitorState state) {
    for (MethodInvocationTree methodInvocation : simpleMethodInvocations) {
      ExpressionTree receiver = ASTHelpers.getReceiver(methodInvocation);
      Type receiverType = ASTHelpers.getType(receiver);
      MemberSelectTree methodSelect = (MemberSelectTree) methodInvocation.getMethodSelect();
      fixBuilder.replace(
          methodInvocation,
          String.format(
              "%s.%s(%s)",
              receiverType.tsym.getSimpleName(),
              methodSelect.getIdentifier(),
              state.getSourceForNode(receiver)));
    }
  }

  private static void fixCastingInvocations(
      List<MethodInvocationTree> castMethodInvocations,
      TreePath enclosingMethod,
      SuggestedFix.Builder fixBuilder,
      VisitorState state) {
    for (MethodInvocationTree castInvocation : castMethodInvocations) {
      ExpressionTree receiver = ASTHelpers.getReceiver(castInvocation);
      Type expressionType = ASTHelpers.getType(castInvocation);

      TreePath castPath = TreePath.getPath(enclosingMethod, castInvocation);
      if (castPath.getParentPath() != null
          && castPath.getParentPath().getLeaf().getKind() == Kind.EXPRESSION_STATEMENT) {
        // If we were to replace X.intValue(); with (int) x;, the code wouldn't compile because
        // that's not a statement. Instead, just delete.
        fixBuilder.delete(castPath.getParentPath().getLeaf());
      } else {
        Type unboxedReceiverType = state.getTypes().unboxedType(ASTHelpers.getType(receiver));
        if (unboxedReceiverType.getTag() == expressionType.getTag()) {
          // someInteger.intValue() can just become someInt.
          fixBuilder.replace(castInvocation, state.getSourceForNode(receiver));
        } else {
          // someInteger.otherPrimitiveValue() can become (otherPrimitive) someInt.
          fixBuilder.replace(
              castInvocation,
              String.format(
                  "(%s) %s",
                  expressionType.tsym.getSimpleName(), state.getSourceForNode(receiver)));
        }
      }
    }
  }

  /**
   * Check to see if the variable should be considered for replacement, i.e.
   *
   * <ul>
   *   <li>A variable without an initializer
   *   <li>Enhanced for loop variables can be replaced if they are loops over primitive arrays
   *   <li>A variable initialized with a primitive value (which is then auto-boxed)
   *   <li>A variable initialized with an invocation of {@code Boxed.valueOf}, since that can be
   *       replaced with {@code Boxed.parseBoxed}.
   * </ul>
   */
  private static boolean variableMatches(VariableTree tree, VisitorState state) {
    ExpressionTree expression = tree.getInitializer();
    if (expression == null) {
      Tree leaf = state.getPath().getParentPath().getLeaf();
      if (!(leaf instanceof EnhancedForLoopTree)) {
        return true;
      }
      EnhancedForLoopTree node = (EnhancedForLoopTree) leaf;
      Type expressionType = ASTHelpers.getType(node.getExpression());
      if (expressionType == null) {
        return false;
      }
      Type elemtype = state.getTypes().elemtype(expressionType);
      // Be conservative - if elemtype is null, treat it as if it is a loop over a wrapped type.
      return elemtype != null && elemtype.isPrimitive();
    }
    Type initializerType = ASTHelpers.getType(expression);
    if (initializerType == null) {
      return false;
    }
    if (initializerType.isPrimitive()) {
      return true;
    }
    // Don't count X.valueOf(...) as a boxed usage, since it can be replaced with X.parseX.
    return VALUE_OF_MATCHER.matches(expression, state);
  }

  private static Optional<TreePath> getEnclosingMethod(TreePath path) {
    while (path != null
        && path.getLeaf().getKind() != Kind.CLASS
        && path.getLeaf().getKind() != Kind.LAMBDA_EXPRESSION) {
      if (path.getLeaf().getKind() == Kind.METHOD) {
        return Optional.of(path);
      }
      path = path.getParentPath();
    }
    return Optional.empty();
  }

  private static boolean isBoxableType(Type type, VisitorState state) {
    Type unboxedType = state.getTypes().unboxedType(type);
    return unboxedType != null && unboxedType.getTag() != TypeTag.NONE;
  }

  private static boolean canChangeMethodSignature(VisitorState state, MethodSymbol methodSymbol) {
    return !ASTHelpers.methodCanBeOverridden(methodSymbol)
        && ASTHelpers.findSuperMethods(methodSymbol, state.getTypes()).isEmpty();
  }

  private static class FindBoxedUsagesScanner extends TreePathScanner<Void, Void> {
    // Method invocations like V.hashCode() can be replaced with TypeOfV.hashCode(v).
    private static final Matcher<ExpressionTree> SIMPLE_METHOD_MATCH =
        MethodMatchers.instanceMethod().anyClass().namedAnyOf("hashCode", "toString");

    // Method invocations like V.intValue() can be replaced with (int) v.
    private static final Matcher<ExpressionTree> CAST_METHOD_MATCH =
        MethodMatchers.instanceMethod()
            .onClass(UnnecessaryBoxedVariable::isBoxableType)
            .namedAnyOf(
                "byteValue",
                "shortValue",
                "intValue",
                "longValue",
                "floatValue",
                "doubleValue",
                "booleanValue");

    private final VarSymbol varSymbol;
    private final TreePath path;
    private final VisitorState state;
    private final List<MethodInvocationTree> fixableSimpleMethodInvocations = new ArrayList<>();
    private final List<MethodInvocationTree> fixableCastMethodInvocations = new ArrayList<>();

    private boolean boxedUsageFound;
    private boolean used;

    FindBoxedUsagesScanner(VarSymbol varSymbol, TreePath path, VisitorState state) {
      this.varSymbol = varSymbol;
      this.path = path;
      this.state = state;
    }

    @Override
    public Void scan(Tree tree, Void unused) {
      if (boxedUsageFound) {
        return null;
      }
      return super.scan(tree, unused);
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void unused) {
      Symbol nodeSymbol = ASTHelpers.getSymbol(node.getVariable());
      if (!Objects.equals(nodeSymbol, varSymbol)) {
        return super.visitAssignment(node, unused);
      }
      used = true;
      // The variable of interest is being assigned. Check if the expression is non-primitive,
      // and go on to scan the expression.
      if (!checkAssignmentExpression(node.getExpression())) {
        return scan(node.getExpression(), unused);
      }

      boxedUsageFound = true;
      return null;
    }

    private boolean checkAssignmentExpression(ExpressionTree expression) {
      Type expressionType = ASTHelpers.getType(expression);
      if (expressionType.isPrimitive()) {
        return false;
      }
      // If the value is assigned a non-primitive value, we need to keep it non-primitive.
      // Unless it's an invocation of Boxed.valueOf or new Boxed, in which case it doesn't need to
      // be kept boxed since we know the result of valueOf is non-null.
      return !VALUE_OF_MATCHER.matches(
              expression, state.withPath(TreePath.getPath(path, expression)))
          && expression.getKind() != Kind.NEW_CLASS;
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, Void unused) {
      Symbol nodeSymbol = ASTHelpers.getSymbol(node);
      if (Objects.equals(nodeSymbol, varSymbol)) {
        used = true;
        TreePath identifierPath = TreePath.getPath(path, node);
        VisitorState identifierState = state.withPath(identifierPath);
        TargetType targetType = ASTHelpers.targetType(identifierState);
        if (targetType != null && !targetType.type().isPrimitive()) {
          boxedUsageFound = true;
          return null;
        }
      }
      return super.visitIdentifier(node, unused);
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void unused) {
      // Don't count the LHS of compound assignments as boxed usages, because they have to be
      // unboxed. Just visit the expression.
      return scan(node.getExpression(), unused);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
      Tree receiver = ASTHelpers.getReceiver(node);
      if (receiver != null) {
        Symbol nodeSymbol = ASTHelpers.getSymbol(receiver);
        if (Objects.equals(nodeSymbol, varSymbol)) {
          used = true;
          if (SIMPLE_METHOD_MATCH.matches(node, state)) {
            fixableSimpleMethodInvocations.add(node);
            return null;
          }
          if (CAST_METHOD_MATCH.matches(node, state)) {
            fixableCastMethodInvocations.add(node);
            return null;
          }

          boxedUsageFound = true;
          return null;
        }
      }
      return super.visitMethodInvocation(node, unused);
    }

    @Override
    public Void visitReturn(ReturnTree node, Void unused) {
      Symbol nodeSymbol = ASTHelpers.getSymbol(ASTHelpers.stripParentheses(node.getExpression()));
      if (!Objects.equals(nodeSymbol, varSymbol)) {
        return super.visitReturn(node, unused);
      }
      used = true;

      // Don't count a return value as a boxed usage, except if we are returning a parameter, and
      // the method's return type is boxed.
      if (varSymbol.getKind() == ElementKind.PARAMETER) {
        MethodTree enclosingMethod =
            ASTHelpers.findEnclosingNode(getCurrentPath(), MethodTree.class);
        Type returnType = ASTHelpers.getType(enclosingMethod.getReturnType());
        if (!returnType.isPrimitive()) {
          boxedUsageFound = true;
        }
      }
      return null;
    }

    @Override
    public Void visitMemberReference(MemberReferenceTree node, Void aVoid) {
      ExpressionTree qualifierExpression = node.getQualifierExpression();
      if (qualifierExpression.getKind() == Kind.IDENTIFIER) {
        Symbol symbol = ASTHelpers.getSymbol(qualifierExpression);
        if (Objects.equals(symbol, varSymbol)) {
          boxedUsageFound = true;
          used = true;
          return null;
        }
      }
      return super.visitMemberReference(node, aVoid);
    }
  }
}
