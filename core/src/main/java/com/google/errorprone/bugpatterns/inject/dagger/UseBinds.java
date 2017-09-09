/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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
package com.google.errorprone.bugpatterns.inject.dagger;

import static com.google.common.base.Preconditions.checkState;
import static com.google.errorprone.BugPattern.Category.DAGGER;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.bugpatterns.inject.dagger.DaggerAnnotations.ELEMENTS_INTO_SET_CLASS_NAME;
import static com.google.errorprone.bugpatterns.inject.dagger.DaggerAnnotations.INTO_MAP_CLASS_NAME;
import static com.google.errorprone.bugpatterns.inject.dagger.DaggerAnnotations.INTO_SET_CLASS_NAME;
import static com.google.errorprone.bugpatterns.inject.dagger.DaggerAnnotations.PRODUCES_CLASS_NAME;
import static com.google.errorprone.bugpatterns.inject.dagger.DaggerAnnotations.PROVIDES_CLASS_NAME;
import static com.google.errorprone.bugpatterns.inject.dagger.DaggerAnnotations.isBindingMethod;
import static com.google.errorprone.bugpatterns.inject.dagger.Util.IS_DAGGER_2_MODULE;
import static com.google.errorprone.bugpatterns.inject.dagger.Util.makeConcreteClassAbstract;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.sun.source.tree.Tree.Kind.ASSIGNMENT;
import static com.sun.source.tree.Tree.Kind.RETURN;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Flags.Flag;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.util.Name;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Modifier;

@BugPattern(
  name = "UseBinds",
  summary = "@Binds is a more efficient and declarative mechanism for delegating a binding.",
  explanation =
      "A @Provides or @Produces method that returns its single parameter has long been Dagger's "
          + "only mechanism for delegating a binding. Since the delegation is implemented via a "
          + "user-defined method there is a disproportionate amount of overhead for such a "
          + "conceptually simple operation. @Binds was introduced to provide a declarative way of "
          + "delegating from one binding to another in a way that allows for minimal overhead in "
          + "the implementation. @Binds should always be preferred over @Provides or @Produces for "
          + "delegation.",
  category = DAGGER,
  severity = SUGGESTION,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class UseBinds extends BugChecker implements MethodTreeMatcher {
  private static final Matcher<MethodTree> SIMPLE_METHOD =
      new Matcher<MethodTree>() {
        @Override
        public boolean matches(MethodTree t, VisitorState state) {
          List<? extends VariableTree> parameters = t.getParameters();
          if (parameters.size() != 1) {
            return false;
          }
          final VariableTree onlyParameter = Iterables.getOnlyElement(parameters);

          BlockTree body = t.getBody();
          if (body == null) {
            return false;
          }
          List<? extends StatementTree> statements = body.getStatements();
          if (statements.size() != 1) {
            return false;
          }
          StatementTree onlyStatement = Iterables.getOnlyElement(statements);

          if (!onlyStatement.getKind().equals(RETURN)) {
            return false;
          }
          Symbol returnedSymbol = getSymbol(((ReturnTree) onlyStatement).getExpression());
          if (returnedSymbol == null) {
            return false;
          }

          return getSymbol(onlyParameter).equals(returnedSymbol);
        }
      };

  private static final Matcher<MethodTree> CAN_BE_A_BINDS_METHOD =
      allOf(isBindingMethod(), SIMPLE_METHOD);

  @Override
  public Description matchMethod(MethodTree method, VisitorState state) {
    if (!CAN_BE_A_BINDS_METHOD.matches(method, state)) {
      return NO_MATCH;
    }

    JCClassDecl enclosingClass = ASTHelpers.findEnclosingNode(state.getPath(), JCClassDecl.class);

    // Dagger 1 modules don't support @Binds.
    if (!IS_DAGGER_2_MODULE.matches(enclosingClass, state)) {
      return NO_MATCH;
    }

    if (enclosingClass.getExtendsClause() != null) {
      return fixByDelegating();
    }

    for (Tree member : enclosingClass.getMembers()) {
      if (member.getKind().equals(Tree.Kind.METHOD) && !getSymbol(member).isConstructor()) {
        MethodTree siblingMethod = (MethodTree) member;
        Set<Modifier> siblingFlags = siblingMethod.getModifiers().getFlags();
        if (!(siblingFlags.contains(Modifier.STATIC) || siblingFlags.contains(Modifier.ABSTRACT))
            && !CAN_BE_A_BINDS_METHOD.matches(siblingMethod, state)) {
          return fixByDelegating();
        }
      }
    }

    return fixByModifyingMethod(state, enclosingClass, method);
  }

  private Description fixByModifyingMethod(
      VisitorState state, JCClassDecl enclosingClass, MethodTree method) {
    return describeMatch(
        method,
        SuggestedFix.builder()
            .addImport("dagger.Binds")
            .merge(convertMethodToBinds(method, state))
            .merge(makeConcreteClassAbstract(enclosingClass, state))
            .build());
  }

  private SuggestedFix.Builder convertMethodToBinds(MethodTree method, VisitorState state) {
    SuggestedFix.Builder fix = SuggestedFix.builder();

    JCModifiers modifiers = ((JCMethodDecl) method).getModifiers();
    ImmutableList.Builder<String> modifierStringsBuilder =
        ImmutableList.<String>builder().add("@Binds");

    for (JCAnnotation annotation : modifiers.annotations) {
      Name annotationQualifiedName = getSymbol(annotation).getQualifiedName();
      if (annotationQualifiedName.contentEquals(PROVIDES_CLASS_NAME)
          || annotationQualifiedName.contentEquals(PRODUCES_CLASS_NAME)) {
        List<JCExpression> arguments = annotation.getArguments();
        if (!arguments.isEmpty()) {
          JCExpression argument = Iterables.getOnlyElement(arguments);
          checkState(argument.getKind().equals(ASSIGNMENT));
          JCAssign assignment = (JCAssign) argument;
          checkState(getSymbol(assignment.getVariable()).getSimpleName().contentEquals("type"));
          String typeName = getSymbol(assignment.getExpression()).getSimpleName().toString();
          switch (typeName) {
            case "SET":
              modifierStringsBuilder.add("@IntoSet");
              fix.addImport(INTO_SET_CLASS_NAME);
              break;
            case "SET_VALUES":
              modifierStringsBuilder.add("@ElementsIntoSet");
              fix.addImport(ELEMENTS_INTO_SET_CLASS_NAME);
              break;
            case "MAP":
              modifierStringsBuilder.add("@IntoMap");
              fix.addImport(INTO_MAP_CLASS_NAME);
              break;
            default:
              throw new AssertionError("Unknown type name: " + typeName);
          }
        }
      } else {
        modifierStringsBuilder.add(state.getSourceForNode(annotation));
      }
    }

    EnumSet<Flag> methodFlags = Flags.asFlagSet(modifiers.flags);
    methodFlags.remove(Flags.Flag.STATIC);
    methodFlags.remove(Flags.Flag.FINAL);
    methodFlags.add(Flags.Flag.ABSTRACT);

    for (Flag flag : methodFlags) {
      modifierStringsBuilder.add(flag.toString());
    }

    fix.replace(modifiers, Joiner.on(' ').join(modifierStringsBuilder.build()));
    fix.replace(method.getBody(), ";");
    return fix;
  }

  private Description fixByDelegating() {
    // TODO(gak): add a suggested fix by which we make a nested abstract module that we can include
    return NO_MATCH;
  }
}
