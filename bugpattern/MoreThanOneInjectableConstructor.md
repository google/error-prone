---
title: MoreThanOneInjectableConstructor
summary: This class has more than one @Inject-annotated constructor. Please remove the @Inject annotation from all but one of them.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: inject-constructors, InjectMultipleAtInjectConstructors_

## The problem
Injection frameworks may use `@Inject` to determine how to construct an object
in the absence of other instructions. Annotating `@Inject` on a constructor
tells the injection framework to use that constructor. However, if multiple
`@Inject` constructors exist, injection frameworks can't reliably choose between
them.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("MoreThanOneInjectableConstructor")` to the enclosing element.
