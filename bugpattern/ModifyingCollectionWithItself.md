---
title: ModifyingCollectionWithItself
summary: Using a collection function with itself as the argument.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Invoking a collection method with the same collection as the argument is likely
incorrect.

*   `collection.addAll(collection)` may cause an infinite loop, duplicate the
    elements, or do nothing, depending on the type of Collection and
    implementation class.
*   `collection.retainAll(collection)` is a no-op.
*   `collection.removeAll(collection)` is the same as `collection.clear()`.
*   `collection.containsAll(collection)` is always true.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ModifyingCollectionWithItself")` to the enclosing element.
