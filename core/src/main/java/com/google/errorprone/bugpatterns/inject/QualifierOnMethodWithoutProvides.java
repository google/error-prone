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

package com.google.errorprone.bugpatterns.inject;

import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.InjectMatchers.DAGGER_PROVIDES_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.GUICE_PROVIDES_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.INSIDE_GUICE_MODULE;
import static com.google.errorprone.matchers.InjectMatchers.IS_BINDING_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.IS_DAGGER_COMPONENT_OR_MODULE;
import static com.google.errorprone.matchers.InjectMatchers.IS_SCOPING_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.hasProvidesAnnotation;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.hasModifier;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.matchers.Matchers.not;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFix.Builder;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.MultiMatcher;
import com.google.errorprone.matchers.MultiMatcher.MultiMatchResult;
import com.google.errorprone.suppliers.Suppliers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;
import java.util.List;
import javax.lang.model.element.Modifier;

/** @author Nick Glorioso (glorioso@google.com) */
@BugPattern(
  name = "QualifierOnMethodWithoutProvides",
  category = Category.INJECT,
  summary =
      "Qualifier applied to a method that isn't a @Provides method. This method won't be used for "
          + "dependency injection",
  severity = SeverityLevel.ERROR
)
public class QualifierOnMethodWithoutProvides extends BugChecker implements MethodTreeMatcher {

  private static final MultiMatcher<MethodTree, AnnotationTree> QUALIFIER_ANNOTATION_FINDER =
      annotations(AT_LEAST_ONE, anyOf(IS_BINDING_ANNOTATION, IS_SCOPING_ANNOTATION));

  // This is slightly blunt, but fixes the following examples:
  // * GIN Ginjector interfaces (the method declarations' types and annotations describe the keys
  //   that are extracted from the GIN modules)
  // * Dagger module abstract @Binds methods
  // * Guice Factory interfaces (an arbitrary interface can serve as a Guice factory, with the
  //   same annotation/type keys).
  private static final Matcher<MethodTree> NOT_ABSTRACT = not(hasModifier(Modifier.ABSTRACT));

  private static final Matcher<MethodTree> NOT_PROVIDES_METHOD = not(hasProvidesAnnotation());

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    MultiMatchResult<AnnotationTree> qualifierAnnotations =
        QUALIFIER_ANNOTATION_FINDER.multiMatchResult(tree, state);
    if (qualifierAnnotations.matches()
        && NOT_ABSTRACT.matches(tree, state)
        && NOT_PROVIDES_METHOD.matches(tree, state)) {
      // This is the bad case. We can suggest adding @Provides if it:
      // * doesn't return void
      // * Is inside a Guice or Dagger module
      //
      // Otherwise, suggest removing the qualifier annotation
      if (not(
              methodReturns(
                  anyOf(
                      isSameType(Suppliers.VOID_TYPE), isSameType(Suppliers.JAVA_LANG_VOID_TYPE))))
          .matches(tree, state)) {

        if (INSIDE_GUICE_MODULE.matches(tree, state)) {
          // Guice Module
          SuggestedFix fix =
              SuggestedFix.builder()
                  .addStaticImport(GUICE_PROVIDES_ANNOTATION)
                  .prefixWith(tree, "@Provides ")
                  .build();
          return describeMatch(tree, fix);
        }
        if (enclosingClass(IS_DAGGER_COMPONENT_OR_MODULE).matches(tree, state)) {
          // Dagger component
          SuggestedFix fix =
              SuggestedFix.builder()
                  .addStaticImport(DAGGER_PROVIDES_ANNOTATION)
                  .prefixWith(tree, "@Provides ")
                  .build();
          return describeMatch(tree, fix);
        }
      }

      List<AnnotationTree> matchingNodes = qualifierAnnotations.matchingNodes();
      Builder fixBuilder = SuggestedFix.builder();
      matchingNodes.forEach(fixBuilder::delete);
      return describeMatch(matchingNodes.get(0), fixBuilder.build());
    }

    return Description.NO_MATCH;
  }
}
