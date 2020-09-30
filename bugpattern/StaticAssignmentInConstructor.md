---
title: StaticAssignmentInConstructor
summary: This assignment is to a static field. Mutating static state from a constructor
  is highly error-prone.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Assigning to a static variable from a constructor is highly indicative of a bug,
or error-prone design.

Common reasons are:

1.  The field simply should be an instance field, and there's a bug.

2.  An attempt is being made to lazily initialize a static field. In this case,
    first consider whether lazy initialization is necessary: it often isn't. If
    it is, doing it from a constructor is very hairy: the static field could be
    accessed from a static method before the class is even initialized. Consider
    using a memoized `Supplier`.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("StaticAssignmentInConstructor")` to the enclosing element.
