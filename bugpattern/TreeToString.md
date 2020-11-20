---
title: TreeToString
summary: Tree#toString shouldn't be used for Trees deriving from the code being compiled,
  as it discards whitespace and comments.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
`Tree#toString` shouldn't be used for Trees deriving from the code being
compiled, as it discards whitespace and comments.

*   If this code is within an Error Prone check, using
    `VisitorState#getSourceForNode` will give you the original source text. Note
    that for synthetic trees (e.g.: implicit constructors), that source may be
    `null`.

*   Prefer `Elements#getConstantExpression` to `TreeMaker.Literal` for escaping
    constants in generated code.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("TreeToString")` to the enclosing element.
