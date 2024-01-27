---
title: LenientFormatStringValidation
summary: The number of arguments provided to lenient format methods should match the
  positional specifiers.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
This check ensures that the number of arguments passed to 'lenient' formatting
methods like `Preconditions.checkArgument` match the number of format
specifiers.

WARNING: Only the exact two-character placeholder sequence `%s` is recognized by
these methods. Any others will be ignored, and not used for argument
substitution.

The APIs checked by this bugpattern include:

*   [`com.google.common.base.Strings#lenientFormat`](https://guava.dev/releases/snapshot-jre/api/docs/com/google/common/base/Strings.html#lenientFormat\(java.lang.String,java.lang.Object...\))
*   [`com.google.common.base.Preconditions#check*`](https://guava.dev/releases/snapshot-jre/api/docs/com/google/common/base/Preconditions.html#checkArgument\(boolean,java.lang.String,java.lang.Object...\))
*   [`com.google.common.base.Verify#verify*`](https://guava.dev/releases/snapshot-jre/api/docs/com/google/common/base/Verify.html#verify\(boolean,java.lang.String,java.lang.Object...\))
*   [`com.google.common.truth.Truth#assertWithMessage`](https://truth.dev/api/latest/com/google/common/truth/Truth.html#assertWithMessage\(java.lang.String,java.lang.Object...\))
*   [`com.google.common.truth.Subject#check`](https://truth.dev/api/latest/com/google/common/truth/Subject.html#check\(java.lang.String,java.lang.Object...\))
*   [`com.google.common.truth.StandardSubjectBuilder#withMessage`](https://truth.dev/api/latest/com/google/common/truth/StandardSubjectBuilder.html#withMessage\(java.lang.String,java.lang.Object...\))

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("LenientFormatStringValidation")` to the enclosing element.
