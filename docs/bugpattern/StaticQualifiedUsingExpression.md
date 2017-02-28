To refer to a static member of another class, we typically *qualify* that member
name by prepending the name of the class it's in, and a dot:
`TheClass.theMethod()`.

But the Java language also permits you to qualify this call using any
*expression* (typically, a variable) whose static type is the class that
contains the method: `instanceOfTheClass.theMethod()`.

Doing this creates the appearance of an ordinary polymorphic method call, but it
behaves very differently. For example:

```
public class Main {
  static class TheClass {
    public static int theMethod() {
      return 1;
    }
  }

  static class TheSubclass extends TheClass {
    public static int theMethod() {
      return 2;
    }
  }

  public static void main(String[] args) {
    TheClass instanceOfTheClass = new TheSubclass();
    System.out.println(instanceOfTheClass.theMethod());
  }
}
```

`TheSubclass` appears to "override" `theMethod`, so we might expect this code to
print the number `2`.

The code, however, prints the number `1`. The runtime type of
`instanceOfTheClass`, `TheSubclass`, is ignored; only the static type of the
reference, as seen by `javac`, matters.

In fact, the instance that `instanceOfTheClass` points to is *entirely*
irrelevant. To prove this, set the variable to `null` and run again. The program
will *still* print `1`, not throw a `NullPointerException`!

Qualifying a static reference in this way creates an unnecessarily confusing
situation. To prevent it, always qualify static method calls using a class name,
never an expression.
