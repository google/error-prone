---
title: DeadException
layout: bugpattern
category: JDK
severity: ERROR
maturity: MATURE
---

# Bug pattern: DeadException
__Exception created but not thrown__

_Alternate names: ThrowableInstanceNeverThrown_

## The problem
The exception is created with new, but is not thrown, and the reference is lost.


## Suppression
Suppress false positives by adding an `@SuppressWarnings("DeadException")` annotation to the enclosing element.

----------

# Examples
__DeadExceptionPositiveCase.java__
{% highlight java %}
here is an example
{% endhighlight %}
