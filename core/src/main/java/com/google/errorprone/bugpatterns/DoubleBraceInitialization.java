/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.expressionStatement;
import static com.google.errorprone.matchers.method.MethodMatchers.constructor;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.predicates.TypePredicates.isDescendantOf;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "DoubleBraceInitialization",
    summary =
        "Prefer collection factory methods or builders to the double-brace initialization"
            + " pattern.",
    severity = WARNING)
public class DoubleBraceInitialization extends BugChecker implements NewClassTreeMatcher {

  @SuppressWarnings("ImmutableEnumChecker") // Matcher is immutable in practice
  enum CollectionTypes {
    MAP("Map", "put", "ImmutableMap"),
    SET("Set", "add", "ImmutableSet"),
    LIST("List", "add", "ImmutableList"),
    COLLECTION("Collection", "add", "ImmutableList");

    final Matcher<ExpressionTree> constructorMatcher;
    final Matcher<StatementTree> mutateMatcher;
    final Matcher<Tree> unmodifiableMatcher;
    final String factoryMethod;
    final String factoryImport;
    final String immutableType;
    final String immutableImport;

    CollectionTypes(String type, String mutator, String factory) {
      this.immutableType = "Immutable" + type;
      this.immutableImport = "com.google.common.collect.Immutable" + type;
      this.factoryMethod = factory + ".of";
      this.factoryImport = "com.google.common.collect." + factory;
      this.constructorMatcher = constructor().forClass(isDescendantOf("java.util." + type));
      this.mutateMatcher =
          expressionStatement(instanceMethod().onDescendantOf("java.util." + type).named(mutator));
      this.unmodifiableMatcher =
          Matchers.toType(
              ExpressionTree.class,
              MethodMatchers.staticMethod()
                  .onClass("java.util.Collections")
                  .named("unmodifiable" + type));
    }

    Optional<Fix> maybeFix(NewClassTree tree, VisitorState state, BlockTree block) {
      // scan the body for mutator methods (add, put) and record their arguments for rewriting as
      // a static factory method
      List<List<? extends ExpressionTree>> arguments = new ArrayList<>();
      for (StatementTree statement : block.getStatements()) {
        if (!mutateMatcher.matches(statement, state)) {
          return Optional.empty();
        }
        arguments.add(
            ((MethodInvocationTree) ((ExpressionStatementTree) statement).getExpression())
                .getArguments());
      }
      if (arguments.stream()
          .flatMap(Collection::stream)
          .anyMatch(a -> a.getKind() == Kind.NULL_LITERAL)) {
        return Optional.empty();
      }
      List<String> args =
          arguments.stream()
              .map(
                  arg ->
                      arg.stream()
                          .map(ASTHelpers::stripParentheses)
                          .map(state::getSourceForNode)
                          .collect(joining(", ", "\n", "")))
              .collect(toImmutableList());

      // check the enclosing context: calls to Collections.unmodifiable* are now redundant, and
      // if there's an enclosing constant variable declaration we can rewrite its type to Immutable*
      Tree unmodifiable = null;
      boolean constant = false;
      Tree typeTree = null;
      Tree toReplace = null;

      for (TreePath path = state.getPath().getParentPath();
          path != null;
          path = path.getParentPath()) {
        Tree enclosing = path.getLeaf();
        if (unmodifiableMatcher.matches(enclosing, state)) {
          unmodifiable = enclosing;
          continue;
        }
        if (enclosing instanceof ParenthesizedTree) {
          continue;
        }
        if (enclosing instanceof VariableTree) {
          VariableTree enclosingVariable = (VariableTree) enclosing;
          toReplace = enclosingVariable.getInitializer();
          typeTree = enclosingVariable.getType();
          VarSymbol symbol = ASTHelpers.getSymbol(enclosingVariable);
          constant =
              symbol.isStatic()
                  && symbol.getModifiers().contains(Modifier.FINAL)
                  && symbol.getKind() == ElementKind.FIELD;
        }
        if (enclosing instanceof ReturnTree) {
          toReplace = ((ReturnTree) enclosing).getExpression();
          MethodTree enclosingMethod = ASTHelpers.findEnclosingNode(path, MethodTree.class);
          typeTree = enclosingMethod == null ? null : enclosingMethod.getReturnType();
        }
        break;
      }
      SuggestedFix.Builder fix = SuggestedFix.builder();

      String replacement;
      if (immutableType.equals("ImmutableMap") && args.size() > 5) {
        String typeArguments =
            tree.getIdentifier() instanceof ParameterizedTypeTree
                ? ((ParameterizedTypeTree) tree.getIdentifier())
                    .getTypeArguments().stream()
                        .map(state::getSourceForNode)
                        .collect(joining(", ", "<", ">"))
                : "";
        replacement =
            "ImmutableMap."
                + typeArguments
                + "builder()"
                + args.stream().map(a -> ".put(" + a + ")").collect(joining(""))
                + ".build()";
      } else {
        replacement = args.stream().collect(joining(", ", factoryMethod + "(", ")"));
      }

      fix.addImport(factoryImport);
      fix.addImport(immutableImport);
      if (unmodifiable != null || constant) {
        // there's an enclosing unmodifiable* call, or we're in the initializer of a constant,
        // so rewrite the variable's type to be immutable and drop the unmodifiable* method
        if (typeTree instanceof ParameterizedTypeTree) {
          typeTree = ((ParameterizedTypeTree) typeTree).getType();
        }
        if (typeTree != null) {
          fix.replace(typeTree, immutableType);
        }
        fix.replace(unmodifiable == null ? toReplace : unmodifiable, replacement);
      } else {
        // the result may need to be mutable, so rewrite e.g.
        // `new ArrayList<>() {{ add(1); }}` -> `new ArrayList<>(ImmutableList.of(1));`
        fix.replace(
            state.getEndPosition(tree.getIdentifier()),
            state.getEndPosition(tree),
            "(" + replacement + ")");
      }
      return Optional.of(fix.build());
    }
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    ClassTree body = tree.getClassBody();
    if (body == null) {
      return NO_MATCH;
    }
    ImmutableList<? extends Tree> members =
        body.getMembers().stream()
            .filter(
                m ->
                    !(m instanceof MethodTree && ASTHelpers.isGeneratedConstructor((MethodTree) m)))
            .collect(toImmutableList());
    if (members.size() != 1) {
      return NO_MATCH;
    }
    Tree member = Iterables.getOnlyElement(members);
    if (!(member instanceof BlockTree)) {
      return NO_MATCH;
    }
    BlockTree block = (BlockTree) member;
    Optional<CollectionTypes> collectionType =
        Stream.of(CollectionTypes.values())
            .filter(type -> type.constructorMatcher.matches(tree, state))
            .findFirst();
    if (!collectionType.isPresent()) {
      return NO_MATCH;
    }
    Description.Builder description = buildDescription(tree);
    collectionType.get().maybeFix(tree, state, block).ifPresent(description::addFix);
    return description.build();
  }
}
