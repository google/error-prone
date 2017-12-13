---
title: ParameterName
summary: Detects `/* name= */`-style comments on actual parameters where the name doesn't match the formal parameter
layout: bugpattern
tags: ''
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
In certain contexts literal arguments - such as `0`, `""`, `true` and `false`,
or `null` - can make it difficult for readers to know what a method will do.
Defining methods that take boolean parameters or otherwise expect users to pass
in ambiguous literals is generally discouraged. However, when you must call such
a method you're encouraged to use the parameter name as an inline comment at the
call site, so that readers don't need to look at the method declaration to
understand the parameter's purpose.

Error Prone recognizes such comments that use the following formatting, and
emits an error if the comment doesn't match the name of the corresponding formal
parameter:

```java
booleanMethod(/* enableFoo= */ true);
```

If the comment deliberately does not match the formal parameter name, using a
regular block comment without the `=` is recommended: `/* enableFoo */`.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ParameterName")` to the enclosing element.
