---
title: InjectInvalidTargetingOnScopingAnnotation
summary: A scoping annotation's Target should include TYPE and METHOD.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
`@Scope` annotations should be applicable to TYPE (annotating classes that
should be scoped) and to METHOD (annotating `@Provides` methods to apply scoping
to the returned object.

If an annotation's use is restricted by `@Target` and it doesn't include those
two element types, the annotation can't be used where it should be able to be
used.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("InjectInvalidTargetingOnScopingAnnotation")` to the enclosing element.
