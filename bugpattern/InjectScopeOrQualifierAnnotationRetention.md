---
title: InjectScopeOrQualifierAnnotationRetention
summary: Scoping and qualifier annotations must have runtime retention.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Qualifier and Scope annotations are used by dependency injection frameworks to
adjust their behavior. Not having runtime retention on scoping or qualifier
annotations will cause unexpected behavior in frameworks that use reflection:

```java
class CreditCardProcessor { @Inject CreditCardProcessor(...) }

@Qualifier
@interface ForTests

@Provides
@ForTests
CreditCardProcessor providesTestProcessor() { return new TestCreditCardProcessor(...) }
...

@Inject
MyApp(CreditCardProcessor processor) {
  processor.issueCharge(...); // Issues a charge against a fake!
}
```

Since the Qualifier doesn't have runtime retention, the Guice provider method
doesn't see the annotation, and will use the TestCreditCardProcessor for the
normal CreditCardProcessor injection point.

NOTE: Even for dependency injection frameworks traditionally considered to be
compile-time dependent, the JSR-330 specification still requires runtime
retention for both [`Qualifier`] and [`Scope`].

[`Qualifier`]: http://docs.oracle.com/javaee/6/api/javax/inject/Qualifier.html
[`Scope`]: http://docs.oracle.com/javaee/6/api/javax/inject/Scope.html

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("InjectScopeOrQualifierAnnotationRetention")` to the enclosing element.
