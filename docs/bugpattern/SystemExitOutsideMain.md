Calling `System.exit` terminates the java process and returns a status code.
Since it is disruptive to shut down the process within library code,
`System.exit` should not be called outside of a main method.

Instead of calling `System.exit` consider throwing an unchecked exception to
signal failure.

For example, prefer this:

```java
public static void main(String[] args) {
  try {
    doSomething(args);
  } catch (MyUncheckedException e) {
    System.err.println(e.getMessage());
    System.exit(1);
  }
}

private static void doSomething(args) {
  try {
    doSomethingElse(...);
  } catch (MyCheckedException e) {
    throw new MyUncheckedException(e);
  }
}
```

to this:

```java
public static void main(String[] args) {
  try {
    doSomething(...);
  } catch (MyCheckedException e) {
    System.err.println(e.getMessage());
    System.exit(1);
  }
}
```
