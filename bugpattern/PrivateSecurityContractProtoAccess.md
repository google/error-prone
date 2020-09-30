---
title: PrivateSecurityContractProtoAccess
summary: Access to a private protocol buffer field is forbidden. This protocol buffer
  carries a security contract, and can only be created using an approved library.
  Direct access to the fields is forbidden.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("PrivateSecurityContractProtoAccess")` to the enclosing element.
