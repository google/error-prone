---
title: Customizing Error Prone with command-line flags
layout: master
---
Customizing Error Prone with command-line flags
===============================================

Error Prone lets the user enable and disable specific checks as well as
override their built-in severity levels (warning vs. error) by passing options
to the Error Prone compiler invocation.  See your build system's
documentation for how to pass options to the Java compiler (usually javac).

A valid Error Prone command-line option looks like:

{% highlight bash %}
-Xep:<checkName>[:severity]
{% endhighlight %}

`checkName` is required and is the canonical name of the check, e.g.
"StringEquality".  `severity` is one of {"OFF", "WARN", "ERROR"}.  Multiple
flags must be passed to enable or disable multiple checks.  The last flag for a
specific check wins.

Examples of usage follow:
{% highlight bash %}
-Xep:StringEquality  [turns on StringEquality check with the severity level from its BugPattern annotation]
-Xep:StringEquality:OFF  [turns off StringEquality check]
-Xep:StringEquality:WARN  [turns on StringEquality check as a warning]
-Xep:StringEquality:ERROR  [turns on StringEquality check as an error]
-Xep:StringEquality:OFF -Xep:StringEquality  [turns on StringEquality check]
{% endhighlight %}

If you pass a flag that refers to an unknown check name, by default Error
Prone will throw an error. You can allow the use of unknown check names by
passing the `-XepIgnoreUnknownCheckNames` flag.

We no longer support the old-style Error Prone disabling flags that used the
`-Xepdisable:<checkName>` syntax.
