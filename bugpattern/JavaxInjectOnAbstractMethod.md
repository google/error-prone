---
title: JavaxInjectOnAbstractMethod
summary: Abstract and default methods are not injectable with javax.inject.Inject
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
The [`Inject`] annotation cannot be applied to abstract methods, per the JSR-330
spec, since injectors will only inject those methods if the concrete implementer
of the abstract method has the [`Inject`] annotation as well. See
[OverridesJavaxInjectableMethod] for more examples of this interaction.

Currently, default methods in interfaces are not injected if they have
[`Inject`] for similar reasons, although future updates to dependency injection
frameworks may allow this, since the default methods are not abstract.

See the [Guice wiki] page on JSR-330 for more.

[`Inject`]: http://javax-inject.github.io/javax-inject/api/javax/inject/Inject.html
[OverridesJavaxInjectableMethod]: OverridesJavaxInjectableMethod
[Guice wiki]: https://github.com/google/guice/wiki/JSR330

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JavaxInjectOnAbstractMethod")` to the enclosing element.
