package com.google.errorprone.bugpatterns;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
public class InjectOverlappingQualifierAndScopeAnnotationNegativeCases {
  
  @javax.inject.Scope
  @interface MyJavaxScope {}

  @com.google.inject.ScopeAnnotation
  @interface MyGuiceScope {}

  @javax.inject.Qualifier
  @interface MyJavaxQualifier {}

  @com.google.inject.BindingAnnotation
  @interface MyGuiceBindingAnnotation {}
  
  // supression tests
  @SuppressWarnings("OverlappingQualifierAndScopeAnnotation")
  @javax.inject.Scope
  @javax.inject.Qualifier
  @interface JavaxScopeAndJavaxQualifier {}
    
  @SuppressWarnings("OverlappingQualifierAndScopeAnnotation")
  @com.google.inject.ScopeAnnotation
  @javax.inject.Qualifier
  @interface GuiceScopeAndJavaxQualifier {}
  
  @SuppressWarnings("OverlappingQualifierAndScopeAnnotation")
  @com.google.inject.ScopeAnnotation
  @com.google.inject.BindingAnnotation
  @interface GuiceScopeAndGuiceBindingAnnotation {}
    
  @SuppressWarnings("OverlappingQualifierAndScopeAnnotation")
  @javax.inject.Scope
  @com.google.inject.BindingAnnotation
  @interface JavaxScopeAndGuiceBindingAnnotation {}
  
}
