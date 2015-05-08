Defining a method that looks like `Object#equals` but doesn't actually override
`Object#equals` is dangerous.  The result of the comparison could differ
depending on the declared type of the argument passed into the `equals` call.

For example, consider this code:

```java
public class Example {
  private int value;

  public Example(int value) {
    this.value = value;
  }

  public boolean equals(Example other) {
    return this.value == other.value;
  }

  public static void main(String[] args) {
    Example exampleA = new Example(1);
    Example exampleB = new Example(1);
    System.out.println(exampleA.equals(exampleB));
  }
}
```

This will print `true`.  Suppose you refactor it so that the variable
`exampleB` is declared as an `Object` instead.  Now this code will print 
`false`, because Java's overload resolution will choose the default 
`equals(Object)` implementation instead of the `equals(Example)` method defined 
in this class.

If this equals method is intended to be a type-specific helper for an `equals` 
method that *does* override `Object#equals`, either inline it into the 
overriding `equals` method or rename it to something other than `equals` to 
avoid ambiguity in overload resolution.

If you don't want to write and maintain `equals` and `hashCode` methods 
by hand, consider rewriting this class to use [AutoValue]
(https://github.com/google/auto/tree/master/value).
