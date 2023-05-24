---
title: LockOnNonEnclosingClassLiteral
summary: Lock on the class other than the enclosing class of the code block can unintentionally
  prevent the locked class being used properly.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Lock on the class other than the enclosing class of the code block can
unintentionally prevent the locked class or other classing using the same lock
being used properly. A issue can be caused when the locked class being edited or
deleted. And it can also be exhausting and time consuming for the others who is
using the same class literal as lock to make sure the synchronized blocks can
work properly as expected. Hence locking on the class other than the enclosing
class of the synchronized code block is disencouraged by the error prone.
Locking on the enclosing class or an instance is a preferred practice.

For example, lock on `Other.class` rather than `Example.class` will trigger the
error prone warning: ``` class Example {

method() { synchronized (Other.class) { } } } ```

Lock on instance or the enclosing class of the synchronized code block will not
trigger the warning: ``` class Example {

method() { synchronized (Example.class) {} synchronized (this) {} } } ```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("LockOnNonEnclosingClassLiteral")` to the enclosing element.
