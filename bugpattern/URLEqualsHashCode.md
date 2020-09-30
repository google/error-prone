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
Equals and HashCode method of java.net.URL make blocking network calls. Either
use java.net.URI or if that isn't possible, use Collection<URL> or List<URL>.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("URLEqualsHashCode")` to the enclosing element.
