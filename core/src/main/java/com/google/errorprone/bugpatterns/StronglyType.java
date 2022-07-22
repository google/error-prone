/*
 * Copyright 2021 The Error Prone Authors.
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

import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.canBeRemoved;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.auto.value.AutoValue;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.lang.model.element.ElementKind;

/**
 * Helper for strongly typing fields. Fields that are declared as a weaker type but only used when
 * wrapped in a stronger type will be refactored to the stronger type.
 *
 * @see com.google.errorprone.bugpatterns.time.StronglyTypeTime
 */
@AutoValue
public abstract class StronglyType {
  abstract Function<String, String> renameFunction();

  abstract ImmutableSet<Type> primitiveTypesToReplace();

  abstract Matcher<ExpressionTree> factoryMatcher();

  abstract BugChecker bugChecker();

  public static Builder forCheck(BugChecker bugChecker) {
    return new AutoValue_StronglyType.Builder()
        .setBugChecker(bugChecker)
        .setRenameFunction(name -> name);
  }

  /** Builder for {@link StronglyType} */
  @AutoValue.Builder
  public abstract static class Builder {
    /**
     * Set a mapping function that maps from the original name to a new name more befitting the
     * strong type.
     */
    public abstract Builder setRenameFunction(Function<String, String> renameFn);

    /** Set the matcher used to check if an expression is a factory creating a stronger type. */
    public abstract Builder setFactoryMatcher(Matcher<ExpressionTree> matcher);

    abstract Builder setBugChecker(BugChecker bugChecker);

    abstract ImmutableSet.Builder<Type> primitiveTypesToReplaceBuilder();

    /** Add a type that can be replaced with a stronger type. */
    @CanIgnoreReturnValue
    public final Builder addType(Type type) {
      primitiveTypesToReplaceBuilder().add(type);
      return this;
    }

    public abstract StronglyType build();
  }

  public final Description match(CompilationUnitTree tree, VisitorState state) {
    Map<VarSymbol, TreePath> fields =
        new HashMap<>(findPathToPotentialFields(state, primitiveTypesToReplace()));
    SetMultimap<VarSymbol, ExpressionTree> usages = HashMultimap.create();

    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitMemberSelect(MemberSelectTree memberSelectTree, Void unused) {
        handle(memberSelectTree);
        return super.visitMemberSelect(memberSelectTree, null);
      }

      @Override
      public Void visitIdentifier(IdentifierTree identifierTree, Void unused) {
        handle(identifierTree);
        return null;
      }

      private void handle(Tree tree) {
        Symbol symbol = getSymbol(tree);
        if (!fields.containsKey(symbol)) {
          return;
        }
        Tree parent = getCurrentPath().getParentPath().getLeaf();
        if (!(parent instanceof ExpressionTree)
            || !factoryMatcher().matches((ExpressionTree) parent, state)) {
          fields.remove(symbol);
          return;
        }
        usages.put((VarSymbol) symbol, (ExpressionTree) parent);
      }
    }.scan(tree, null);

    for (Map.Entry<VarSymbol, TreePath> entry : fields.entrySet()) {
      state.reportMatch(match(entry.getValue(), entry.getKey(), usages.get(entry.getKey()), state));
    }
    return NO_MATCH;
  }

  private Description match(
      TreePath variableTreePath,
      VarSymbol replacedSymbol,
      Set<ExpressionTree> invocationTrees,
      VisitorState state) {
    if (invocationTrees.stream().map(ASTHelpers::getSymbol).distinct().count() != 1) {
      return NO_MATCH;
    }
    VariableTree variableTree = (VariableTree) variableTreePath.getLeaf();
    ExpressionTree factory = invocationTrees.iterator().next();
    String newName = renameFunction().apply(variableTree.getName().toString());
    SuggestedFix.Builder fix = SuggestedFix.builder();
    Type targetType = getType(factory);
    String typeName = SuggestedFixes.qualifyType(state.withPath(variableTreePath), fix, targetType);

    fix.replace(
        variableTree,
        String.format(
            "%s %s %s = %s(%s);",
            state.getSourceForNode(variableTree.getModifiers()),
            typeName,
            newName,
            getMethodSelectOrNewClass(factory, state),
            getWeakTypeIntitializerCode(variableTree, state)));

    for (ExpressionTree expressionTree : invocationTrees) {
      fix.replace(expressionTree, newName);
    }
    Type replacedType = state.getTypes().unboxedTypeOrType(replacedSymbol.type);

    return bugChecker()
        .buildDescription(variableTree)
        .setMessage(
            String.format(
                "This %s is only used to construct %s instances. It would be"
                    + " clearer to strongly type the field instead.",
                buildStringForType(replacedType, state), targetType.tsym.getSimpleName()))
        .addFix(fix.build())
        .build();
  }

  private static String buildStringForType(Type type, VisitorState state) {
    return SuggestedFixes.prettyType(type, state);
  }

  /**
   * Get the source code for the initializer. If the initializer is an array literal without a type,
   * prefix with new <type>.
   */
  private static String getWeakTypeIntitializerCode(VariableTree weakType, VisitorState state) {
    // If the new array type is missing, we need to add it.
    String prefix =
        (weakType.getInitializer().getKind() == Kind.NEW_ARRAY
                && ((NewArrayTree) weakType.getInitializer()).getType() == null)
            ? String.format("new %s ", state.getSourceForNode(weakType.getType()))
            : "";

    return prefix + state.getSourceForNode(weakType.getInitializer());
  }

  private static String getMethodSelectOrNewClass(ExpressionTree tree, VisitorState state) {
    switch (tree.getKind()) {
      case METHOD_INVOCATION:
        return state.getSourceForNode(((MethodInvocationTree) tree).getMethodSelect());
      case NEW_CLASS:
        return "new " + state.getSourceForNode(((NewClassTree) tree).getIdentifier());
      default:
        throw new AssertionError();
    }
  }

  /** Finds the path to potential fields that we might want to strongly type. */
  // TODO(b/147006492): Consider extracting a helper to find all fields that match a Matcher.
  private ImmutableMap<VarSymbol, TreePath> findPathToPotentialFields(
      VisitorState state, Set<Type> potentialTypes) {
    ImmutableMap.Builder<VarSymbol, TreePath> fields = ImmutableMap.builder();
    bugChecker().new SuppressibleTreePathScanner<Void, Void>(state) {
      @Override
      public Void visitVariable(VariableTree variableTree, Void unused) {
        VarSymbol symbol = getSymbol(variableTree);
        Type type = state.getTypes().unboxedTypeOrType(symbol.type);
        if (symbol.getKind() == ElementKind.FIELD
            && canBeRemoved(symbol)
            && isConsideredFinal(symbol)
            && variableTree.getInitializer() != null
            && potentialTypes.stream()
                .anyMatch(potentialType -> isSameType(type, potentialType, state))
            && !bugChecker().isSuppressed(variableTree, state)) {
          fields.put(symbol, getCurrentPath());
        }
        return super.visitVariable(variableTree, null);
      }
    }.scan(state.getPath().getCompilationUnit(), null);
    return fields.buildOrThrow();
  }
}
