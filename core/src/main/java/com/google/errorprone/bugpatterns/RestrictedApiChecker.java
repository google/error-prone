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
import static com.google.common.collect.Iterables.getOnlyElement;

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
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.MirroredTypesException;

/** Check for non-whitelisted callers to RestrictedApiChecker. */
@BugPattern(
    name = "RestrictedApiChecker",
    summary = "Check for non-whitelisted callers to RestrictedApiChecker.",
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
    if (!ASTHelpers.getSymbol(tree)
        .getQualifiedName()
        .contentEquals(RestrictedApi.class.getName())) {
      return Description.NO_MATCH;
    }
    // TODO(bangert): make a more elegant API to get the annotation within an annotation tree.
    // Maybe find the declared object and get annotations on that...
    AnnotationMirror restrictedApi = ASTHelpers.getAnnotationMirror(tree);
    if (restrictedApi == null) {
      return Description.NO_MATCH;
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return checkMethodUse(ASTHelpers.getSymbol(tree), tree, state);
  }

  @Override
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    return checkMethodUse(ASTHelpers.getSymbol(tree), tree, state);
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
    MethodSymbol constructor = ASTHelpers.getSymbol(tree);
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
      return checkRestriction(ASTHelpers.getAnnotation(tree, RestrictedApi.class), tree, state);
    }
  }

  private Description checkMethodUse(
      MethodSymbol method, ExpressionTree where, VisitorState state) {
    RestrictedApi annotation = ASTHelpers.getAnnotation(method, RestrictedApi.class);
    if (annotation != null) {
      return checkRestriction(annotation, where, state);
    }

    // Try each super method for @RestrictedApi
    Optional<MethodSymbol> superWithRestrictedApi =
        ASTHelpers.findSuperMethods(method, state.getTypes()).stream()
            .filter((t) -> ASTHelpers.hasAnnotation(t, RestrictedApi.class, state))
            .findFirst();
    if (!superWithRestrictedApi.isPresent()) {
      return Description.NO_MATCH;
    }
    return checkRestriction(
        ASTHelpers.getAnnotation(superWithRestrictedApi.get(), RestrictedApi.class), where, state);
  }

  private Description checkRestriction(
      @Nullable RestrictedApi restriction, Tree where, VisitorState state) {
    if (restriction == null) {
      return Description.NO_MATCH;
    }
    if (!restriction.allowedOnPath().isEmpty()) {
      JCCompilationUnit compilationUnit = (JCCompilationUnit) state.getPath().getCompilationUnit();
      String path = compilationUnit.getSourceFile().toUri().toString();
      if (Pattern.matches(restriction.allowedOnPath(), path)) {
        return Description.NO_MATCH;
      }
    }
    boolean warn =
        Matchers.enclosingNode(shouldAllowWithWarning(restriction)).matches(where, state);

    boolean allow = Matchers.enclosingNode(shouldAllow(restriction)).matches(where, state);
    if (warn && allow) {
      // TODO(bangert): Clarify this message if possible.
      return buildDescription(where)
          .setMessage(
              "The Restricted API ("
                  + restriction.explanation()
                  + ") call here is both whitelisted-as-warning and "
                  + "silently whitelisted. "
                  + "Please remove one of the conflicting suppression annotations.")
          .build();
    }
    if (allow) {
      return Description.NO_MATCH;
    }
    SeverityLevel level = warn ? SeverityLevel.WARNING : SeverityLevel.ERROR;

    Description.Builder description =
        Description.builder(
            where, CHECK_NAME, restriction.link(), level, restriction.explanation());
    return description.build();
  }

  // TODO(bangert): Memoize these if necessary.
  private static Matcher<Tree> shouldAllow(RestrictedApi api) {
    try {
      return Matchers.hasAnyAnnotation(api.whitelistAnnotations());
    } catch (MirroredTypesException e) {
      return Matchers.hasAnyAnnotation(e.getTypeMirrors());
    }
  }

  private static Matcher<Tree> shouldAllowWithWarning(RestrictedApi api) {
    try {
      return Matchers.hasAnyAnnotation(api.whitelistWithWarningAnnotations());
    } catch (MirroredTypesException e) {
      return Matchers.hasAnyAnnotation(e.getTypeMirrors());
    }
  }
}
