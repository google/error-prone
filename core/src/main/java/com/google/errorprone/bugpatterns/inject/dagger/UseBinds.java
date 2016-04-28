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

import static com.google.errorprone.BugPattern.Category.DAGGER;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.hasArgumentWithValue;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.sun.source.tree.Tree.Kind.RETURN;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Flags.Flag;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.util.Name;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Modifier;

@BugPattern(
  name = "UseBinds",
  summary = "@Binds is a more efficient and declaritive mechanism for delegating a binding.",
  explanation =
      "A @Provides or @Produces method that returns its single parameter has long been Dagger's "
          + "only mechanism for delegating a binding. Since the delgation is implemented via a "
          + "user-defined method there is a disproportionate amount of overhead for such a "
          + "conceptually simple operation. @Binds was introduced to provide a declarative way of "
          + "delegating from one binding to another in a way that allows for minimal overhead in "
          + "the implementation. @Binds should always be preferred over @Provides or @Produces for "
          + "delegation.",
  category = DAGGER,
  severity = SUGGESTION,
  maturity = EXPERIMENTAL
)
public class UseBinds extends BugChecker implements MethodTreeMatcher {
  private static final Matcher<AnnotationTree> HAS_DAGGER_ONE_MODULE_ARGUMENT =
      anyOf(
          hasArgumentWithValue("injects", Matchers.<ExpressionTree>anything()),
          hasArgumentWithValue("staticInjections", Matchers.<ExpressionTree>anything()),
          hasArgumentWithValue("overrides", Matchers.<ExpressionTree>anything()),
          hasArgumentWithValue("addsTo", Matchers.<ExpressionTree>anything()),
          hasArgumentWithValue("complete", Matchers.<ExpressionTree>anything()),
          hasArgumentWithValue("library", Matchers.<ExpressionTree>anything()));

  private static final Matcher<MethodTree> SIMPLE_METHOD =
      new Matcher<MethodTree>() {
        @Override
        public boolean matches(MethodTree t, VisitorState state) {
          for (AnnotationTree annotation : t.getModifiers().getAnnotations()) {
            Name annotationQualifiedName =
                ASTHelpers.getType(annotation.getAnnotationType()).asElement().getQualifiedName();
            // TODO(gak): remove this restriction when we support @Binds for multibindings
            if ((annotationQualifiedName.contentEquals("dagger.Provides")
                    || annotationQualifiedName.contentEquals("dagger.producers.Produces"))
                && !annotation.getArguments().isEmpty()) {
              return false;
            }
          }

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

  private static final Matcher<Tree> ANNOTATED_WITH_PRODUCES_OR_PROVIDES =
      anyOf(hasAnnotation("dagger.Provides"), hasAnnotation("dagger.producers.Produces"));

  private static final Matcher<Tree> ANNOTATED_WITH_MULTIBINDING_ANNOTATION =
      anyOf(
          hasAnnotation("dagger.multibindings.IntoSet"),
          hasAnnotation("dagger.multibindings.ElementsIntoSet"),
          hasAnnotation("dagger.multibindings.IntoMap"));

  private static final Matcher<MethodTree> CAN_BE_A_BINDS_METHOD =
      allOf(
          ANNOTATED_WITH_PRODUCES_OR_PROVIDES,
          // TODO(gak): remove this restriction when we support @Binds for multibindings
          not(ANNOTATED_WITH_MULTIBINDING_ANNOTATION),
          SIMPLE_METHOD);

  @Override
  public Description matchMethod(MethodTree method, VisitorState state) {
    if (!CAN_BE_A_BINDS_METHOD.matches(method, state)) {
      return NO_MATCH;
    }

    JCClassDecl enclosingClass = ASTHelpers.findEnclosingNode(state.getPath(), JCClassDecl.class);

    // Check to see if this is in a Dagger 1 module b/c it doesn't support @Binds
    for (JCAnnotation annotation : enclosingClass.getModifiers().getAnnotations()) {
      if (ASTHelpers.getSymbol(annotation.getAnnotationType())
              .getQualifiedName()
              .contentEquals("dagger.Module")
          && HAS_DAGGER_ONE_MODULE_ARGUMENT.matches(annotation, state)) {
        return NO_MATCH;
      }
    }

    if (enclosingClass.getExtendsClause() != null) {
      return fixByDelegating();
    }

    for (Tree member : enclosingClass.getMembers()) {
      if (member.getKind().equals(Tree.Kind.METHOD)
          && !ASTHelpers.getSymbol(member).isConstructor()) {
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
    JCModifiers methodModifiers = ((JCMethodDecl) method).getModifiers();
    String replacementModifiersString = createReplacementMethodModifiers(state, methodModifiers);

    JCModifiers enclosingClassModifiers = enclosingClass.getModifiers();
    String enclosingClassReplacementModifiersString =
        createReplacementClassModifiers(state, enclosingClassModifiers);

    SuggestedFix.Builder fixBuilder =
        SuggestedFix.builder()
            .addImport("dagger.Binds")
            .replace(methodModifiers, replacementModifiersString)
            .replace(method.getBody(), ";");
    fixBuilder =
        (enclosingClassModifiers.pos == -1)
            ? fixBuilder.prefixWith(enclosingClass, enclosingClassReplacementModifiersString)
            : fixBuilder.replace(enclosingClassModifiers, enclosingClassReplacementModifiersString);
    return describeMatch(method, fixBuilder.build());
  }

  private Description fixByDelegating() {
    // TODO(gak): add a suggested fix by which we make a nested abstract module that we can include
    return NO_MATCH;
  }

  private String createReplacementMethodModifiers(VisitorState state, JCModifiers modifiers) {
    ImmutableList.Builder<String> modifierStringsBuilder =
        new ImmutableList.Builder<String>().add("@Binds");

    for (JCAnnotation annotation : modifiers.annotations) {
      Name annotationQualifiedName = ASTHelpers.getSymbol(annotation).getQualifiedName();
      if (!(annotationQualifiedName.contentEquals("dagger.Provides")
          || annotationQualifiedName.contentEquals("dagger.producers.Produces"))) {
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

    return Joiner.on(' ').join(modifierStringsBuilder.build());
  }

  private String createReplacementClassModifiers(
      VisitorState state, JCModifiers enclosingClassModifiers) {
    ImmutableList.Builder<String> classModifierStringsBuilder = new ImmutableList.Builder<String>();

    for (JCAnnotation annotation : enclosingClassModifiers.annotations) {
      classModifierStringsBuilder.add(state.getSourceForNode(annotation));
    }

    EnumSet<Flag> classFlags = Flags.asFlagSet(enclosingClassModifiers.flags);
    classFlags.remove(Flags.Flag.FINAL);
    classFlags.add(Flags.Flag.ABSTRACT);
    for (Flag flag : classFlags) {
      classModifierStringsBuilder.add(flag.toString());
    }

    return Joiner.on(' ').join(classModifierStringsBuilder.build());
  }
}
