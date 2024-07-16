---
title: URLEqualsHashCode
summary: Avoid hash-based containers of java.net.URL--the containers rely on equals()
  and hashCode(), which cause java.net.URL to make blocking internet connections.
layout: bugpattern
tags: FragileCode
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
The `equals` and `hashCode` methods of `java.net.URL` make blocking network
calls. When you place a `URL` into a hash-based container, the container invokes
those methods.

Prefer `java.net.URI`. Or, if you must use `URL` in a
collection, prefer to use a non-hash-based container like a `List<URL>`, and
avoid calling methods like `contains` (which calls `equals`) on it.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("URLEqualsHashCode")` to the enclosing element.
