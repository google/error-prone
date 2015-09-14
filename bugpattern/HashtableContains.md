---
title: HashtableContains
summary: contains() is a legacy method that is equivalent to containsValue()
layout: bugpattern
category: JDK
severity: ERROR
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
`Hashtable.contains(Object)` and `ConcurrentHashMap.contains(Object)` are
legacy methods for testing if the given object is a value in the hash table. 
They are often mistaken for `containsKey`, which checks whether the given object
is a *key* in the  hash table.

If you intended to check whether the given object is a key in the hash table,
use `containsKey` instead.  If you really intended to check whether the 
given object is a value in the hash table, use `containsValue` for clarity.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("HashtableContains")` annotation to the enclosing element.
