Null-checking `System.console()` is not a reliable way to detect if the console
is connected to a terminal.

See JDK 22
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

and JDK 25 release note
[Release Note: Default Console Implementation No Longer Based On JLine](https://bugs.openjdk.org/browse/JDK-8351576):

> The default Console obtained via `System.console()` is no longer based on
> JLine. Since JDK 20, the JDK has included a JLine-based Console
> implementation, offering a richer user experience and better support for
> virtual terminal environments, such as IDEs. This implementation was initially
> opt-in via a system property in JDK 20 and JDK 21 and became the default in
> JDK 22. However, maintaining the JLine-based Console proved challenging. As a
> result, in JDK 25, it has reverted to being opt-in, as it was in JDK 20 and
> JDK 21.

To prepare for this change while remaining compatible with JDK versions prior to
JDK 22, consider using reflection to call `Console#isTerminal` on JDK versions
that support it:

```java
  @SuppressWarnings("SystemConsoleNull") // https://errorprone.info/bugpattern/SystemConsoleNull
  private static boolean systemConsoleIsTerminal() {
    Console systemConsole = System.console();
    if (Runtime.version().feature() < 22 || systemConsole == null) {
      return systemConsole != null;
    }
    try {
      return (Boolean) Console.class.getMethod("isTerminal").invoke(systemConsole);
    } catch (ReflectiveOperationException e) {
      throw new LinkageError(e.getMessage(), e);
    }
  }
```
