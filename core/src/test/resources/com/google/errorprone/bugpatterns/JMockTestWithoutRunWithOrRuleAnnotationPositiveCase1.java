package com.google.errorprone.bugpatterns;

import org.jmock.Mockery;

public class JMockTestWithoutRunWithOrRuleAnnotationPositiveCase1 {

    // BUG: Diagnostic contains: JMock tests must have @RunWith class annotation or the mockery field declared as a JUnit rule
    final Mockery mockery = new Mockery();
}