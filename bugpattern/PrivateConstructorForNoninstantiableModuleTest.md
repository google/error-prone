---
title: PrivateConstructorForNoninstantiableModuleTest
summary: Add a private constructor to modules that will not be instantiated by Dagger.
layout: bugpattern
category: DAGGER
severity: SUGGESTION
maturity: EXPERIMENTAL
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Modules that contain abstract binding methods (@Binds, @Multibinds) or only static @Provides methods will not be instantiated by Dagger when they are included in a component.  Adding a private constructor clearly conveys that the module will not be used as an instance.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("PrivateConstructorForNoninstantiableModuleTest")` annotation to the enclosing element.
