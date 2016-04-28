---
title: RestrictedApiChecker
summary: ' Check for non-whitelisted callers to RestrictedApiChecker.'
layout: bugpattern
category: ONE_OFF
severity: ERROR
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Calls to APIs marked @RestrictedApi are prohibited without a corresponding whitelist annotation.

## Suppression
This check may not be suppressed.
