---
title: ThreadJoinLoop
summary: Thread.join needs to be immediately surrounded by a loop until it succeeds.
  Consider using Uninterruptibles.joinUninterruptibly.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Thread.join() can be interrupted, and so requires users to catch
InterruptedException. Most users should be looping until the join() actually
succeeds.

Instead of writing your own try-catch and loop to handle it properly, you may
use **Uninterruptibles.joinUninterruptibly** which does the same for you.

Example:

```
Thread thread = new Thread(new Runnable() {...});

Uninterruptibles.joinUninterruptibly(thread);
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ThreadJoinLoop")` to the enclosing element.
