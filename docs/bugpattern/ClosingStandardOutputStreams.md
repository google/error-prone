Closing the standard output streams `System.out` or `System.err` will cause all
subsequent standard output to be dropped, including stack traces from exceptions
that propagate to the top level.

Avoid using try-with-resources to manage `PrintWriter`s or `OutputStream`s that
wrap `System.out` or `System.err`, since the try-with-resource statement will
close the underlying streams.

That is, prefer this:

``` {.good}
PrintWriter pw = new PrintWriter(new OutputStreamWriter(System.err));
pw.println("hello");
pw.flush();
```

Instead of this:

``` {.bad}
try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(System.err))) {
  pw.println("hello");
}
```

Consider the following example:

```
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import static java.nio.charset.StandardCharsets.UTF_8;

public class X {
  public static void main(String[] args) {
    System.err.println("one");
    try (PrintWriter err = new PrintWriter(new OutputStreamWriter(System.err, UTF_8))) {
      err.print("two");
    }
    // System.err has been closed, no more output will be printed!
    System.err.println("three");
    throw new AssertionError();
  }
}
```

The program will print the following, and return with exit code 1. Note that the
last `println` doesn't produce any output, and the exception's stack trace is
not printed:

```
one
two
```
