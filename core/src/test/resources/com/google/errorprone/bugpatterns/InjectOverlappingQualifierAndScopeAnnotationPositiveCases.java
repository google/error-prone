package com.google.errorprone.bugpatterns;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
public class InjectOverlappingQualifierAndScopeAnnotationPositiveCases {
 
  //BUG: Suggestion includes "remove"
  @javax.inject.Scope
  //BUG: Suggestion includes "remove"
  @javax.inject.Qualifier
  @interface JavaxScopeAndJavaxQualifier {}
    
  //BUG: Suggestion includes "remove"
  @com.google.inject.ScopeAnnotation
  //BUG: Suggestion includes "remove"
  @javax.inject.Qualifier
  @interface GuiceScopeAndJavaxQualifier {}

  //BUG: Suggestion includes "remove"
  @com.google.inject.ScopeAnnotation
  //BUG: Suggestion includes "remove"
  @com.google.inject.BindingAnnotation
  @interface GuiceScopeAndGuiceBindingAnnotation {}
    
  //BUG: Suggestion includes "remove"
  @javax.inject.Scope
  //BUG: Suggestion includes "remove"
  @com.google.inject.BindingAnnotation
  @interface JavaxScopeAndGuiceBindingAnnotation {}

}
