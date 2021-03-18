/*
 * Copyright 2016 The Error Prone Authors.
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

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.RestrictedApi;
import com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.MoreAnnotations;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.model.AnnotationProxyMaker;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/** Check for non-allowlisted callers to RestrictedApiChecker. */
@BugPattern(
    name = "RestrictedApiChecker",
    summary = "Check for non-allowlisted callers to RestrictedApiChecker.",
    severity = SeverityLevel.ERROR)
public class RestrictedApiChecker extends BugChecker
    implements MethodInvocationTreeMatcher,
        NewClassTreeMatcher,
        AnnotationTreeMatcher,
        MemberReferenceTreeMatcher {

  /**
   * The name to use when reporting findings. It's important that this DOES NOT match {@link
   * #canonicalName()}, because otherwise changing the severity won't work.
   */
  // TODO(b/151087021): rationalize this.
  private static final String CHECK_NAME = "RestrictedApi";

  /**
   * Validates a {@code @RestrictedApi} annotation and that the declared restriction makes sense.
   *
   * <p>The other match functions in this class check the <em>usages</em> of a restricted API.
   */
  @Override
  public Description matchAnnotation(AnnotationTree tree, VisitorState state) {
    // TODO(bangert): Validate all the fields
    if (!getSymbol(tree).getQualifiedName().contentEquals(RestrictedApi.class.getName())) {
      return NO_MATCH;
    }
    // TODO(bangert): make a more elegant API to get the annotation within an annotation tree.
    // Maybe find the declared object and get annotations on that...
    Attribute.Compound restrictedApi = (Attribute.Compound) ASTHelpers.getAnnotationMirror(tree);
    if (restrictedApi == null) {
      return NO_MATCH;
    }
    return NO_MATCH;
  }

  private static final ImmutableSet<String> ALLOWLIST_ANNOTATION_NAMES =
      ImmutableSet.of("allowlistAnnotations", "allowlistWithWarningAnnotations");

  private static Tree getAnnotationArgumentTree(AnnotationTree tree, String name) {
    return tree.getArguments().stream()
        .filter(arg -> arg.getKind().equals(Tree.Kind.ASSIGNMENT))
        .map(arg -> (AssignmentTree) arg)
        .filter(arg -> isVariableTreeWithName(arg, name))
        .map(AssignmentTree::getExpression)
        .findFirst()
        .orElse(tree);
  }

  private static boolean isVariableTreeWithName(AssignmentTree tree, String name) {
    ExpressionTree variable = tree.getVariable();
    return variable instanceof IdentifierTree
        && ((IdentifierTree) variable).getName().contentEquals(name);
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return checkMethodUse(getSymbol(tree), tree, state);
  }

  @Override
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    return checkMethodUse(getSymbol(tree), tree, state);
  }

  /**
   * Returns the constructor type for the supplied constructor symbol of an anonymous class object
   * that can be matched with a corresponding constructor in its direct superclass. If the anonymous
   * object creation expression is qualified, i.e, is of the form {@code enclosingExpression.new
   * identifier (arguments)} the constructor type includes an implicit first parameter with type
   * matching enclosingExpression. In such a case a matching constructor type is created by dropping
   * this implicit parameter.
   */
  private static Type dropImplicitEnclosingInstanceParameter(
      NewClassTree tree, VisitorState state, MethodSymbol anonymousClassConstructor) {
    Type type = anonymousClassConstructor.asType();
    if (!hasEnclosingExpression(tree)) {
      // fast path
      return type;
    }
    com.sun.tools.javac.util.List<Type> params = type.getParameterTypes();
    params = firstNonNull(params.tail, com.sun.tools.javac.util.List.nil());
    return state.getTypes().createMethodTypeWithParameters(type, params);
  }

  private static boolean hasEnclosingExpression(NewClassTree tree) {
    if (tree.getEnclosingExpression() != null) {
      return true;
    }
    List<? extends ExpressionTree> arguments = tree.getArguments();
    return !arguments.isEmpty() && ((JCTree) arguments.get(0)).hasTag(JCTree.Tag.NULLCHK);
  }

  private static MethodSymbol superclassConstructorSymbol(NewClassTree tree, VisitorState state) {
    MethodSymbol constructor = getSymbol(tree);
    Types types = state.getTypes();
    TypeSymbol superclass = types.supertype(constructor.enclClass().asType()).asElement();
    Type anonymousClassType = constructor.enclClass().asType();
    Type matchingConstructorType = dropImplicitEnclosingInstanceParameter(tree, state, constructor);

    return (MethodSymbol)
        getOnlyElement(
            superclass
                .members()
                .getSymbols(
                    member ->
                        member.isConstructor()
                            && types.hasSameArgs(
                                member.asMemberOf(anonymousClassType, types).asType(),
                                matchingConstructorType)));
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (tree.getClassBody() != null) {
      return checkMethodUse(superclassConstructorSymbol(tree, state), tree, state);
    } else {
      return checkRestriction(getRestrictedApiAnnotation(getSymbol(tree), state), tree, state);
    }
  }

  private Description checkMethodUse(
      MethodSymbol method, ExpressionTree where, VisitorState state) {
    Attribute.Compound annotation = getRestrictedApiAnnotation(method, state);
    if (annotation != null) {
      return checkRestriction(annotation, where, state);
    }

    // Try each super method for @RestrictedApi
    Optional<MethodSymbol> superWithRestrictedApi =
        ASTHelpers.findSuperMethods(method, state.getTypes()).stream()
            .filter((t) -> ASTHelpers.hasAnnotation(t, RestrictedApi.class, state))
            .findFirst();
    if (!superWithRestrictedApi.isPresent()) {
      return NO_MATCH;
    }
    return checkRestriction(
        getRestrictedApiAnnotation(superWithRestrictedApi.get(), state), where, state);
  }

  @Nullable
  private static Attribute.Compound getRestrictedApiAnnotation(Symbol sym, VisitorState state) {
    if (sym == null) {
      return null;
    }
    return sym.attribute(state.getSymbolFromString(RestrictedApi.class.getName()));
  }

  private Description checkRestriction(
      @Nullable Attribute.Compound attribute, Tree where, VisitorState state) {
    if (attribute == null) {
      return NO_MATCH;
    }
    RestrictedApi restriction =
        AnnotationProxyMaker.generateAnnotation(attribute, RestrictedApi.class);
    if (restriction == null) {
      return NO_MATCH;
    }
    if (!restriction.allowedOnPath().isEmpty()) {
      JCCompilationUnit compilationUnit = (JCCompilationUnit) state.getPath().getCompilationUnit();
      String path = compilationUnit.getSourceFile().toUri().toString();
      if (Pattern.matches(restriction.allowedOnPath(), path)) {
        return NO_MATCH;
      }
    }
    boolean warn = Matchers.enclosingNode(shouldAllowWithWarning(attribute)).matches(where, state);

    boolean allow = Matchers.enclosingNode(shouldAllow(attribute)).matches(where, state);
    if (warn && allow) {
      // TODO(bangert): Clarify this message if possible.
      return buildDescription(where)
          .setMessage(
              "The Restricted API ("
                  + restriction.explanation()
                  + ") call here is both allowlisted-as-warning and "
                  + "silently allowlisted. "
                  + "Please remove one of the conflicting suppression annotations.")
          .build();
    }
    if (allow) {
      return NO_MATCH;
    }
    SeverityLevel level = warn ? SeverityLevel.WARNING : SeverityLevel.ERROR;

    Description.Builder description =
        Description.builder(
            where, CHECK_NAME, restriction.link(), level, restriction.explanation());
    return description.build();
  }

  // TODO(bangert): Memoize these if necessary.
  private static Matcher<Tree> shouldAllow(Attribute.Compound api) {
    Optional<Attribute> allowlistAnnotations =
        MoreAnnotations.getValue(api, "allowlistAnnotations");
    // TODO(b/178905039): remove handling of legacy names
    if (!allowlistAnnotations.isPresent()) {
      allowlistAnnotations = MoreAnnotations.getValue(api, "whitelistAnnotations");
    }
    return Matchers.hasAnyAnnotation(
        allowlistAnnotations
            .map(MoreAnnotations::asTypes)
            .orElse(Stream.empty())
            .collect(toImmutableList()));
  }

  private static Matcher<Tree> shouldAllowWithWarning(Attribute.Compound api) {
    Optional<Attribute> allowlistWithWarningAnnotations =
        MoreAnnotations.getValue(api, "allowlistWithWarningAnnotations");
    // TODO(b/178905039): remove handling of legacy names
    if (!allowlistWithWarningAnnotations.isPresent()) {
      allowlistWithWarningAnnotations =
          MoreAnnotations.getValue(api, "whitelistWithWarningAnnotations");
    }
    return Matchers.hasAnyAnnotation(
        allowlistWithWarningAnnotations
            .map(MoreAnnotations::asTypes)
            .orElse(Stream.empty())
            .collect(toImmutableList()));
  }
}
