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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.fixes.SuggestedFixes.replaceVariableType;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.common.base.Ascii;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.TargetType;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.lang.model.element.ElementKind;

/**
 * Finds and fixes unnecessarily boxed variables.
 *
 * @author awturner@google.com (Andy Turner)
 */
@BugPattern(
    summary = "It is unnecessary for this variable to be boxed. Use the primitive instead.",
    explanation =
        "This variable is of boxed type, but equivalent semantics can be achieved using the"
            + " corresponding primitive type, which avoids the cost of constructing an unnecessary"
            + " object.",
    severity = SeverityLevel.SUGGESTION)
public class UnnecessaryBoxedVariable extends BugChecker implements CompilationUnitTreeMatcher {
  private static final Matcher<ExpressionTree> VALUE_OF_MATCHER =
      staticMethod().onClass(UnnecessaryBoxedVariable::isBoxableType).named("valueOf");

  private final WellKnownKeep wellKnownKeep;

  @Inject
  UnnecessaryBoxedVariable(WellKnownKeep wellKnownKeep) {
    this.wellKnownKeep = wellKnownKeep;
  }

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    FindBoxedUsagesScanner usages = new FindBoxedUsagesScanner(state);
    usages.scan(tree, null);

    new SuppressibleTreePathScanner<Void, Void>(state) {
      @Override
      public Void visitVariable(VariableTree tree, Void unused) {
        VisitorState innerState = state.withPath(getCurrentPath());
        unboxed(tree, innerState)
            .flatMap(u -> handleVariable(u, usages, tree, innerState))
            .ifPresent(state::reportMatch);
        return super.visitVariable(tree, null);
      }
    }.scan(tree, null);
    return NO_MATCH;
  }

  private Optional<Description> handleVariable(
      Type unboxed, FindBoxedUsagesScanner usages, VariableTree tree, VisitorState state) {
    VarSymbol varSymbol = getSymbol(tree);
    switch (varSymbol.getKind()) {
      case PARAMETER:
        if (!canChangeMethodSignature(state, (MethodSymbol) varSymbol.getEnclosingElement())
            || state.getPath().getParentPath().getLeaf() instanceof LambdaExpressionTree) {
          return Optional.empty();
        }
      // Fall through.
      case LOCAL_VARIABLE:
        if (!variableMatches(tree, state)) {
          return Optional.empty();
        }
        break;
      case FIELD:
        if (wellKnownKeep.shouldKeep(tree)
            || !ASTHelpers.canBeRemoved(varSymbol)
            || !ASTHelpers.isStatic(varSymbol)
            || !ASTHelpers.isConsideredFinal(varSymbol)
            || usages.boxedUsageFound.contains(varSymbol)) {
          return Optional.empty();
        }
        var initializer = tree.getInitializer();
        if (initializer == null || !ASTHelpers.getType(initializer).isPrimitive()) {
          return Optional.empty();
        }
        break;
      case BINDING_VARIABLE: // Revisit if https://openjdk.org/jeps/488 happens.
      default:
        return Optional.empty();
    }

    return fixVariable(unboxed, usages, tree, state);
  }

  private Optional<Description> fixVariable(
      Type unboxed, FindBoxedUsagesScanner usages, VariableTree tree, VisitorState state) {
    VarSymbol varSymbol = getSymbol(tree);
    if (usages.boxedUsageFound.contains(varSymbol)) {
      return Optional.empty();
    }
    if (!usages.dereferenced.contains(varSymbol) && varSymbol.getKind() == ElementKind.PARAMETER) {
      // If it isn't used and it is a parameter, don't fix it, because this could introduce a new
      // NPE.
      return Optional.empty();
    }

    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
    replaceVariableType(tree, unboxed.tsym.getSimpleName().toString(), state)
        .ifPresent(fixBuilder::merge);

    fixMethodInvocations(usages.fixableSimpleMethodInvocations.get(varSymbol), fixBuilder, state);
    fixNullCheckInvocations(usages.fixableNullCheckInvocations.get(varSymbol), fixBuilder, state);
    fixCastingInvocations(usages.fixableCastMethodInvocations.get(varSymbol), fixBuilder, state);
    fixAssignments(usages.fixableAssignments.get(varSymbol), fixBuilder, state);
    if (tree.getInitializer() != null) {
      fixAssignment(tree.getInitializer(), fixBuilder, state);
    }

    // Remove @Nullable annotation, if present.
    AnnotationTree nullableAnnotation =
        ASTHelpers.getAnnotationWithSimpleName(tree.getModifiers().getAnnotations(), "Nullable");
    if (nullableAnnotation == null) {
      return Optional.of(describeMatch(tree, fixBuilder.build()));
    }
    fixBuilder.replace(nullableAnnotation, "");
    var message =
        switch (varSymbol.getKind()) {
          case FIELD ->
              "This field is assigned a primitive value, and not used in a boxed context. Prefer"
                  + " using the primitive type directly.";
          default ->
              "All usages of this @Nullable variable would result in a NullPointerException when"
                  + " it actually is null. Use the primitive type if this variable should never"
                  + " be null, or else fix the code to avoid unboxing or invoking its instance"
                  + " methods.";
        };
    return Optional.of(
        buildDescription(tree).setMessage(message).addFix(fixBuilder.build()).build());
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

  private static void fixNullCheckInvocations(
      List<TreePath> nullCheckInvocations, SuggestedFix.Builder fixBuilder, VisitorState state) {
    for (TreePath pathForTree : nullCheckInvocations) {
      checkArgument(pathForTree.getLeaf() instanceof MethodInvocationTree);
      MethodInvocationTree methodInvocation = (MethodInvocationTree) pathForTree.getLeaf();

      TargetType targetType = TargetType.targetType(state.withPath(pathForTree));
      if (targetType == null) {
        // If the check is the only thing in a statement, remove the statement.
        StatementTree statementTree =
            ASTHelpers.findEnclosingNode(pathForTree, StatementTree.class);
        if (statementTree != null) {
          fixBuilder.delete(statementTree);
        }
      } else {
        // If it's an expression, we can replace simply with the first argument.
        fixBuilder.replace(
            methodInvocation, state.getSourceForNode(methodInvocation.getArguments().getFirst()));
      }
    }
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
      List<TreePath> castMethodInvocations, SuggestedFix.Builder fixBuilder, VisitorState state) {
    for (TreePath castPath : castMethodInvocations) {
      MethodInvocationTree castInvocation = (MethodInvocationTree) castPath.getLeaf();
      ExpressionTree receiver = ASTHelpers.getReceiver(castInvocation);
      Type expressionType = ASTHelpers.getType(castInvocation);

      if (castPath.getParentPath() != null
          && castPath.getParentPath().getLeaf() instanceof ExpressionStatementTree) {
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

  private static void fixAssignments(
      List<TreePath> paths, SuggestedFix.Builder fixBuilder, VisitorState state) {
    for (TreePath path : paths) {
      ExpressionTree expressionTree = (ExpressionTree) path.getLeaf();
      fixAssignment(expressionTree, fixBuilder, state);
    }
  }

  private static void fixAssignment(
      ExpressionTree expressionTree, SuggestedFix.Builder fixBuilder, VisitorState state) {
    Type boxedType = getType(expressionTree);
    Type unboxedType = state.getTypes().unboxedType(boxedType);
    if (unboxedType.hasTag(TypeTag.NONE)) {
      return;
    }
    String name = unboxedType.tsym.getSimpleName().toString();
    String parseName = "parse" + Ascii.toUpperCase(name.charAt(0)) + name.substring(1);
    switch (expressionTree) {
      case MethodInvocationTree methodInvocation -> {
        if (VALUE_OF_MATCHER.matches(expressionTree, state)) {
          Tree argument = getOnlyElement(methodInvocation.getArguments());
          Type argumentType = ASTHelpers.getType(argument);
          if (isSameType(argumentType, state.getSymtab().stringType, state)) {
            fixBuilder.merge(
                SuggestedFixes.renameMethodInvocation(
                    (MethodInvocationTree) expressionTree, parseName, state));
          } else if (isSameType(argumentType, unboxedType, state)) {
            fixBuilder.replace(expressionTree, state.getSourceForNode(argument));
          }
        }
      }
      case NewClassTree newClassTree -> {
        Tree argument = getOnlyElement(newClassTree.getArguments());
        Type argumentType = ASTHelpers.getType(argument);
        if (isSameType(argumentType, state.getSymtab().stringType, state)) {
          fixBuilder.replace(
              expressionTree,
              String.format(
                  "%s.%s(%s)",
                  SuggestedFixes.qualifyType(state, fixBuilder, boxedType),
                  parseName,
                  state.getSourceForNode(argument)));
        } else if (isSameType(argumentType, unboxedType, state)) {
          fixBuilder.replace(expressionTree, state.getSourceForNode(argument));
        }
      }
      default -> {}
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
      if (!(leaf instanceof EnhancedForLoopTree node)) {
        return true;
      }
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

  private static boolean isBoxableType(Type type, VisitorState state) {
    Type unboxedType = state.getTypes().unboxedType(type);
    return unboxedType != null && unboxedType.getTag() != TypeTag.NONE;
  }

  private static boolean canChangeMethodSignature(VisitorState state, MethodSymbol methodSymbol) {
    return !ASTHelpers.methodCanBeOverridden(methodSymbol)
        && ASTHelpers.findSuperMethods(methodSymbol, state.getTypes()).isEmpty()
        && !ASTHelpers.isRecord(methodSymbol);
  }

  private static class FindBoxedUsagesScanner extends TreePathScanner<Void, Void> {
    // Method invocations like V.hashCode() can be replaced with TypeOfV.hashCode(v).
    private static final Matcher<ExpressionTree> SIMPLE_METHOD_MATCH =
        instanceMethod().anyClass().namedAnyOf("hashCode", "toString");

    // Method invocations like V.intValue() can be replaced with (int) v.
    private static final Matcher<ExpressionTree> CAST_METHOD_MATCH =
        instanceMethod()
            .onClass(UnnecessaryBoxedVariable::isBoxableType)
            .namedAnyOf(
                "byteValue",
                "shortValue",
                "intValue",
                "longValue",
                "floatValue",
                "doubleValue",
                "booleanValue");

    // Method invocations that check (and throw) if the value is potentially null.
    private static final Matcher<ExpressionTree> NULL_CHECK_MATCH =
        anyOf(
            staticMethod().onClass("com.google.common.base.Preconditions").named("checkNotNull"),
            staticMethod().onClass("com.google.common.base.Verify").named("verifyNonNull"),
            staticMethod().onClass("java.util.Objects").named("requireNonNull"));

    private final VisitorState state;
    private final ListMultimap<VarSymbol, MethodInvocationTree> fixableSimpleMethodInvocations =
        ArrayListMultimap.create();
    private final ListMultimap<VarSymbol, TreePath> fixableNullCheckInvocations =
        ArrayListMultimap.create();
    private final ListMultimap<VarSymbol, TreePath> fixableCastMethodInvocations =
        ArrayListMultimap.create();
    private final ListMultimap<VarSymbol, TreePath> fixableAssignments = ArrayListMultimap.create();

    private final Set<VarSymbol> boxedUsageFound = new HashSet<>();
    private final Set<VarSymbol> dereferenced = new HashSet<>();

    FindBoxedUsagesScanner(VisitorState state) {
      this.state = state;
    }

    @Override
    public Void scan(Tree tree, Void unused) {
      var symbol = getSymbol(tree);
      if (boxedUsageFound.contains(symbol)) {
        return null;
      }
      return super.scan(tree, null);
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void unused) {
      Symbol nodeSymbol = getSymbol(node.getVariable());
      if (!isBoxed(nodeSymbol, state)) {
        return super.visitAssignment(node, null);
      }
      VarSymbol varSymbol = (VarSymbol) nodeSymbol;
      fixableAssignments.put(varSymbol, new TreePath(getCurrentPath(), node.getExpression()));
      // The variable of interest is being assigned. Check if the expression is non-primitive,
      // and go on to scan the expression.
      if (!checkAssignmentExpression(node.getExpression())) {
        return scan(node.getExpression(), null);
      }

      boxedUsageFound.add(varSymbol);
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
      return !VALUE_OF_MATCHER.matches(expression, state.withPath(getCurrentPath()))
          && !(expression instanceof NewClassTree);
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, Void unused) {
      handleIdentifier(node);
      return super.visitIdentifier(node, null);
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void unused) {
      handleIdentifier(node);
      return super.visitMemberSelect(node, null);
    }

    private void handleIdentifier(Tree node) {
      Symbol nodeSymbol = getSymbol(node);
      if (isBoxed(nodeSymbol, state)) {
        dereferenced.add((VarSymbol) nodeSymbol);
        VisitorState identifierState = state.withPath(getCurrentPath());
        TargetType targetType = TargetType.targetType(identifierState);
        if (targetType != null && !targetType.type().isPrimitive()) {
          boxedUsageFound.add((VarSymbol) nodeSymbol);
        }
      }
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void unused) {
      // Don't count the LHS of compound assignments as boxed usages, because they have to be
      // unboxed. Just visit the expression.
      return scan(node.getExpression(), null);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
      if (NULL_CHECK_MATCH.matches(node, state)) {
        Symbol firstArgSymbol =
            getSymbol(ASTHelpers.stripParentheses(node.getArguments().getFirst()));
        if (isBoxed(firstArgSymbol, state)) {
          dereferenced.add((VarSymbol) firstArgSymbol);
          fixableNullCheckInvocations.put((VarSymbol) firstArgSymbol, getCurrentPath());
          return null;
        }
      }

      Tree receiver = ASTHelpers.getReceiver(node);
      Symbol receiverSymbol = getSymbol(receiver);
      if (receiver != null && isBoxed(receiverSymbol, state)) {
        if (SIMPLE_METHOD_MATCH.matches(node, state)) {
          fixableSimpleMethodInvocations.put((VarSymbol) receiverSymbol, node);
          return null;
        }
        if (CAST_METHOD_MATCH.matches(node, state)) {
          fixableCastMethodInvocations.put((VarSymbol) receiverSymbol, getCurrentPath());
          return null;
        }

        boxedUsageFound.add((VarSymbol) receiverSymbol);
        return null;
      }

      return super.visitMethodInvocation(node, null);
    }

    @Override
    public Void visitMemberReference(MemberReferenceTree node, Void unused) {
      ExpressionTree qualifierExpression = node.getQualifierExpression();
      if (qualifierExpression instanceof IdentifierTree) {
        Symbol symbol = getSymbol(qualifierExpression);
        if (isBoxed(symbol, state)) {
          boxedUsageFound.add((VarSymbol) symbol);
          dereferenced.add((VarSymbol) symbol);
          return null;
        }
      }
      return super.visitMemberReference(node, null);
    }
  }

  private static boolean isBoxed(Symbol symbol, VisitorState state) {
    return symbol instanceof VarSymbol
        && !state.getTypes().isSameType(state.getTypes().unboxedType(symbol.type), Type.noType);
  }
}
