---
title: RestrictedApiChecker
summary: ' Check for non-whitelisted callers to RestrictedApiChecker.'
layout: bugpattern
tags: ''
severity: ERROR
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Calls to APIs marked @RestrictedApi are prohibited without a corresponding
whitelist annotation.

## Suppression
This check may not be suppressed.
