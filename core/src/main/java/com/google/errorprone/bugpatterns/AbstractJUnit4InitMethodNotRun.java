/*
 * Copyright 2014 The Error Prone Authors.
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

import static com.google.errorprone.matchers.JUnitMatchers.isJUnit4TestClass;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.hasAnnotationOnAnyOverriddenMethod;
import static com.google.errorprone.matchers.Matchers.not;

import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.tools.javac.code.Symbol;
import java.io.Serializable;
import java.util.List;
import javax.lang.model.element.Modifier;

/**
 * Base class for JUnit4SetUp/TearDown not run. This will take care of the nitty-gritty about
 * replacing @After with @Before, adding @Before on unannotated methods, making them public if
 * necessary, fixing the imports of other @Before, etc.
 *
 * @author glorioso@google.com
 */
abstract class AbstractJUnit4InitMethodNotRun extends BugChecker implements MethodTreeMatcher {

  private static final String JUNIT_TEST = "org.junit.Test";

  /**
   * Returns a matcher that selects which methods this matcher applies to (e.g. public void setUp()
   * without @Before/@BeforeClass annotation)
   */
  protected abstract Matcher<MethodTree> methodMatcher();

  /**
   * Returns the fully qualified class name of the annotation this bugpattern should apply to
   * matched methods.
   *
   * <p>If another annotation is on the method that has the same name, the import will be replaced
   * with the appropriate one (e.g.: com.example.Before becomes org.junit.Before)
   */
  protected abstract String correctAnnotation();

  /**
   * Returns a collection of 'before-and-after' pairs of annotations that should be replaced on
   * these methods.
   *
   * <p>If this method matcher finds a method annotated with {@link
   * AnnotationReplacements#badAnnotation}, instead of applying {@link #correctAnnotation()},
   * instead replace it with {@link AnnotationReplacements#goodAnnotation}
   */
  protected abstract List<AnnotationReplacements> annotationReplacements();

  /**
   * Matches if all of the following conditions are true: 1) The method matches {@link
   * #methodMatcher()}, (looks like setUp() or tearDown(), and none of the overrides in the
   * hierarchy of the method have the appropriate @Before or @After annotations) 2) The method is
   * not annotated with @Test 3) The enclosing class has an @RunWith annotation and does not extend
   * TestCase. This marks that the test is intended to run with JUnit 4.
   */
  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    boolean matches =
        allOf(
                methodMatcher(),
                not(hasAnnotationOnAnyOverriddenMethod(JUNIT_TEST)),
                enclosingClass(isJUnit4TestClass))
            .matches(methodTree, state);
    if (!matches) {
      return Description.NO_MATCH;
    }

    // For each annotationReplacement, replace the first annotation that matches. If any of them
    // matches, don't try and do the rest of the work.
    Description description;
    for (AnnotationReplacements replacement : annotationReplacements()) {
      description =
          tryToReplaceAnnotation(
              methodTree, state, replacement.badAnnotation, replacement.goodAnnotation);
      if (description != null) {
        return description;
      }
    }

    // Search for another @Before annotation on the method and replace the import
    // if we find one
    String correctAnnotation = correctAnnotation();
    String unqualifiedClassName = getUnqualifiedClassName(correctAnnotation);
    for (AnnotationTree annotationNode : methodTree.getModifiers().getAnnotations()) {
      Symbol annoSymbol = ASTHelpers.getSymbol(annotationNode);
      if (annoSymbol.getSimpleName().toString().equals(unqualifiedClassName)) {
        SuggestedFix.Builder suggestedFix =
            SuggestedFix.builder()
                .removeImport(annoSymbol.getQualifiedName().toString())
                .addImport(correctAnnotation);
        makeProtectedPublic(methodTree, state, suggestedFix);
        return describeMatch(annotationNode, suggestedFix.build());
      }
    }

    // Add correctAnnotation() to the unannotated method
    // (and convert protected to public if it is)
    SuggestedFix.Builder suggestedFix = SuggestedFix.builder().addImport(correctAnnotation);
    makeProtectedPublic(methodTree, state, suggestedFix);
    suggestedFix.prefixWith(methodTree, "@" + unqualifiedClassName + "\n");
    return describeMatch(methodTree, suggestedFix.build());
  }

  private void makeProtectedPublic(
      MethodTree methodTree, VisitorState state, SuggestedFix.Builder suggestedFix) {
    if (Matchers.<MethodTree>hasModifier(Modifier.PROTECTED).matches(methodTree, state)) {
      ModifiersTree modifiers = methodTree.getModifiers();
      CharSequence modifiersSource = state.getSourceForNode(modifiers);
      suggestedFix.replace(
          modifiers, modifiersSource.toString().replaceFirst("protected", "public"));
    }
  }

  private Description tryToReplaceAnnotation(
      MethodTree methodTree, VisitorState state, String badAnnotation, String goodAnnotation) {
    String finalName = getUnqualifiedClassName(goodAnnotation);
    if (hasAnnotation(badAnnotation).matches(methodTree, state)) {
      AnnotationTree annotationTree = findAnnotation(methodTree, state, badAnnotation);
      return describeMatch(
          annotationTree,
          SuggestedFix.builder()
              .addImport(goodAnnotation)
              .replace(annotationTree, "@" + finalName)
              .build());
    } else {
      return null;
    }
  }

  private String getUnqualifiedClassName(String goodAnnotation) {
    return goodAnnotation.substring(goodAnnotation.lastIndexOf(".") + 1);
  }

  private AnnotationTree findAnnotation(
      MethodTree methodTree, VisitorState state, String annotationName) {
    AnnotationTree annotationNode = null;
    for (AnnotationTree annotation : methodTree.getModifiers().getAnnotations()) {
      if (ASTHelpers.getSymbol(annotation).equals(state.getSymbolFromString(annotationName))) {
        annotationNode = annotation;
      }
    }
    return annotationNode;
  }

  protected static class AnnotationReplacements implements Serializable {
    private final String goodAnnotation;
    private final String badAnnotation;

    protected AnnotationReplacements(String badAnnotation, String goodAnnotation) {
      this.goodAnnotation = goodAnnotation;
      this.badAnnotation = badAnnotation;
    }
  }
}
