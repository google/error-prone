package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.INJECT;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
@BugPattern(name = "InjectAssistedInjectAndInjectOnSameConstructor",
    summary = "@AssistedInject and @Inject cannot be used on the same constructor.",
    explanation = "",
    category = INJECT, severity = ERROR, maturity = EXPERIMENTAL)
public class InjectAssistedInjectAndInjectOnSameConstructor extends BugChecker
    implements AnnotationTreeMatcher {

  private static final String GUICE_INJECT_ANNOTATION = "com.google.inject.Inject";
  private static final String JAVAX_INJECT_ANNOTATION = "javax.inject.Inject";
  private static final String ASSISTED_INJECT_ANNOTATION =
      "com.google.inject.assistedinject.AssistedInject";

  /**
   * Matches a method/constructor that is annotated with an @Inject annotation.
   */
  @SuppressWarnings("unchecked")
  private Matcher<MethodTree> constructorWithInjectMatcher = Matchers.<MethodTree>anyOf(
          hasAnnotation(GUICE_INJECT_ANNOTATION), hasAnnotation(JAVAX_INJECT_ANNOTATION));

  /**
   * Matches a method/constructor that is annotated with an @AssistedInject annotation.
   */
  @SuppressWarnings("unchecked")
  private Matcher<MethodTree> constructorWithAssistedInjectMatcher =
      Matchers.<MethodTree>hasAnnotation(ASSISTED_INJECT_ANNOTATION);
  
  /**
   * Matches the @Inject and @Assisted inject annotations.
   */
  private Matcher<AnnotationTree> injectOrAssistedInjectMatcher = new Matcher<AnnotationTree>() {
    @Override public boolean matches(AnnotationTree annotationTree, VisitorState state) {
      Symbol annotationSymbol = ASTHelpers.getSymbol(annotationTree);
      return (annotationSymbol.equals(state.getSymbolFromString(JAVAX_INJECT_ANNOTATION))
          || annotationSymbol.equals(state.getSymbolFromString(GUICE_INJECT_ANNOTATION))
          || annotationSymbol.equals(state.getSymbolFromString(ASSISTED_INJECT_ANNOTATION)));
    }
  };
      
  @Override
  @SuppressWarnings("unchecked")
  public Description matchAnnotation(AnnotationTree annotationTree, VisitorState state) {
    if (injectOrAssistedInjectMatcher.matches(annotationTree, state)) {
      Tree treeWithAnnotation = state.getPath().getParentPath().getParentPath().getLeaf();
      if (ASTHelpers.getSymbol(treeWithAnnotation).isConstructor()
          && constructorWithInjectMatcher.matches((MethodTree) treeWithAnnotation, state)
          && constructorWithAssistedInjectMatcher.matches((MethodTree) treeWithAnnotation, state)) {
        return describeMatch(annotationTree, new SuggestedFix().delete(annotationTree));
      }
    }
    return Description.NO_MATCH;
  }
}
