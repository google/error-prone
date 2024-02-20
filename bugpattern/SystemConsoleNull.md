---
title: SystemConsoleNull
summary: System.console() no longer returns null in JDK 22 and newer versions
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Null-checking `System.console()` is not a reliable way to detect if the console
is connected to a terminal.

See
[Release Note: JLine As The Default Console Provider](https://bugs.openjdk.org/browse/JDK-8309155):

> `System.console()` now returns a `Console` object when the standard streams
> are redirected or connected to a virtual terminal. In prior releases,
> `System.console()` returned `null` for these cases. This change may impact
> code that uses the return from `System.console()` to test if the VM is
> connected to a terminal. If needed, running with `-Djdk.console=java.base`
> will restore older behavior where the console is only returned when it is
> connected to a terminal.

> A new method `Console.isTerminal()` has been added to test if console is
> connected to a terminal.

To prepare for this change while remaining compatible with JDK versions prior to
JDK 22, consider using reflection to call `Console#isTerminal` on JDK versions
that support it:

```java
  @SuppressWarnings("SystemConsoleNull") // https://errorprone.info/bugpattern/SystemConsoleNull
  private static boolean systemConsoleIsTerminal() {
    Console systemConsole = System.console();
    if (Runtime.version().feature() < 22) {
      return systemConsole != null;
    }
    try {
      return (Boolean) Console.class.getMethod("isTerminal").invoke(systemConsole);
    } catch (ReflectiveOperationException e) {
      throw new LinkageError(e.getMessage(), e);
    }
  }
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("SystemConsoleNull")` to the enclosing element.
