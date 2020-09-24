---
title: DepAnn
summary: Item documented with a @deprecated javadoc note is not annotated with @Deprecated
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: dep-ann_

## The problem
A declaration has the `@deprecated` Javadoc tag but no `@Deprecated` annotation.
Please add an `@Deprecated` annotation to this declaration in addition to the
`@deprecated` tag in the Javadoc.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("DepAnn")` to the enclosing element.
