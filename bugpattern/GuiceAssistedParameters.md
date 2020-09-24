---
title: GuiceAssistedParameters
summary: A constructor cannot have two @Assisted parameters of the same type unless they are disambiguated with named @Assisted annotations.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
From the [javadoc of FactoryModuleBuilder][fmb]:

> The types of the factory method's parameters must be distinct. To use multiple
> parameters of the same type, use a named `@Assisted` annotation to
> disambiguate the parameters. The names must be applied to the factory method's
> parameters:

```java
public interface PaymentFactory {
   Payment create(
        @Assisted("startDate") Date startDate,
        @Assisted("dueDate") Date dueDate,
       Money amount);
 }
```

> ...and to the concrete type's constructor parameters:

```java
public class RealPayment implements Payment {
    @Inject
   public RealPayment(
      CreditService creditService,
      AuthService authService,
       @Assisted("startDate") Date startDate,
       @Assisted("dueDate") Date dueDate,
       @Assisted Money amount) {
     ...
   }
 }
```

[fmb]: https://google.github.io/guice/api-docs/latest/javadoc/com/google/inject/assistedinject/FactoryModuleBuilder.html

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("GuiceAssistedParameters")` to the enclosing element.
