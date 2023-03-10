---
title: ReturnMissingNullable
summary: Method returns a definitely null value but is not annotated @Nullable
layout: bugpattern
tags: ''
severity: SUGGESTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Annotating a method @Nullable communicates to tools that the method can return null. That means they can check that callers handle a returned null correctly.

Adding @Nullable may require updating callers so that they deal with the possibly-null value. This can happen for example with Kotlin callers, or with Java callers that are checked for null-safety by static-analysis tools. Alternatively, depending on the tool, it may be possible to annotate Java callers temporarily with @SuppressWarnings("nullness").

We recommend using JSpecify @Nullable for all new build units, org.jspecify.nullness.Nullable (//third_party/java/jspecify_annotations), but see https://engdoc.corp.google.com/eng/doc/devguide/java/practices/null.md#annotations for more information.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ReturnMissingNullable")` to the enclosing element.
