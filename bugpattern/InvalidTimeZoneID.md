---
title: InvalidTimeZoneID
summary: Invalid time zone identifier. TimeZone.getTimeZone(String) will silently return GMT instead of the time zone you intended.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
TimeZone.getTimeZone(String) silently returns GMT when an invalid time zone identifier is passed in.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("InvalidTimeZoneID")` annotation to the enclosing element.
