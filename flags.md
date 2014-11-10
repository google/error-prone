---
title: Customizing error-prone with command-line flags
layout: master
---

error-prone lets the user enable and disable specific checks as well as
override their built-in severity levels (warning vs. error) by passing options
at the command line.

A valid error-prone command-line option looks like:
```
-Xep:<checkName>[:severity]
```

`checkName` is required and is the canonical name of the check, e.g.
"StringEquality".  `severity` is one of {"OFF", "WARN", "ERROR"}.  Multiple
flags must be passed to enable or disable multiple checks.  The last flag for a
specific check wins.

Examples of usage follow:
```
-Xep:StringEquality  [turns on StringEquality check with the severity level from its BugPattern annotation]
-Xep:StringEquality:OFF  [turns off StringEquality check]
-Xep:StringEquality:WARN  [turns on StringEquality check as a warning]
-Xep:StringEquality:ERROR  [turns on StringEquality check as an error]
-Xep:StringEquality:OFF -Xep:StringEquality  [turns on StringEquality check]
```

We will continue to support the old-style error-prone disabling flags for a
short transition period.  Those flags have the following syntax:
```
-Xepdisable:<checkName>[,<checkName>...]
```

