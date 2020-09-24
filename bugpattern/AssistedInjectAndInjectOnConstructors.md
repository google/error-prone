---
title: AssistedInjectAndInjectOnConstructors
summary: '@AssistedInject and @Inject should not be used on different constructors
  in the same class.'
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Mixing @Inject and @AssistedInject leads to confusing code and the documentation
specifies not to do it. See
https://google.github.io/guice/api-docs/latest/javadoc/com/google/inject/assistedinject/AssistedInject.html

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("AssistedInjectAndInjectOnConstructors")` to the enclosing element.
