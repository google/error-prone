---
title: DeadException
summary: Exception created but not thrown
layout: bugpattern
tags: LikelyError
severity: ERROR
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: ThrowableInstanceNeverThrown_

## The problem
The exception is created with new, but is not thrown, and the reference is lost.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("DeadException")` annotation to the enclosing element.

----------

### Positive examples
__DeadExceptionPositiveCase.java__

{% highlight java %}
here is an example
{% endhighlight %}

