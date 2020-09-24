---
title: MisusedWeekYear
summary: Use of "YYYY" (week year) in a date pattern without "ww" (week in year). You probably meant to use "yyyy" (year) instead.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
"YYYY" in a date pattern means "week year". The week year is defined to begin at
the beginning of the week that contains the year's first Thursday. For example,
the week year 2015 began on Monday, December 29, 2014, since January 1, 2015,
was on a Thursday.

"Week year" is intended to be used for week dates, e.g. "2015-W01-1", but is
often mistakenly used for calendar dates, e.g. 2014-12-29, in which case the
year may be incorrect during the last week of the year. If you are formatting
anything other than a week date, you should use the year specifier "yyyy"
instead.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("MisusedWeekYear")` to the enclosing element.
