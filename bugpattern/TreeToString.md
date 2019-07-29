---
title: TreeToString
summary: Tree#toString shouldn't be used for Trees deriving from the code being compiled, as it discards whitespace and comments. If this code is within an ErrorProne check, consider VisitorState#getSourceForNode.
layout: bugpattern
tags: ''
severity: WARNING
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("TreeToString")` to the enclosing element.
