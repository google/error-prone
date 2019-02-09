Consider the following code:

```java
String x = "42";
Integer y = 42;
if (x.equals(y)) {
  System.out.println("What is this, Javascript?");
} else {
  System.out.println("Types have meaning here.");
}
```

We understand that any `Integer` will *not* be equal to any `String`. However,
the signature of the `equals` method accepts any Object, so the compiler will
happily allow us to pass an Integer to the equals method. However, it will
always return false, which is probably not what we intended.

This check detects circumstances where the equals method is called when the two
objects in question can *never* be equal to each other. We check the following
equality methods:

*   `java.lang.Object.equals(Object)`
*   `java.util.Objects.equals(Object, Object)`
*   `com.google.common.base.Objects.equal(Object, Object)`

## I'm trying to test to make sure my equals method works

Good! Many tests of equals methods neglect to test that equals on an unrelated
object return false.

We recommend using Guava's [EqualsTester][equalstester] to perform tests of your
equals method. Simply give it a collection of objects of your class, broken into
groups that should be equal to each other, and EqualsTester will ensure that:

*   Each object is equal to each other object in the same group as that object
*   Each object is equal to itself
*   Each object is unequal to all of the other objects not in the group
*   Each object is unequal to an unrelated object (Relevant to this check)
*   Each object is unequal to null
*   The `hashCode` of each object in a group is the same as the hash code of
    each other member of the group

Which should exhaustively check all of the properties of `equals` and
`hashCode`.

## But I'm doing something funky with my equals method!

The javadoc of [`Object.equals(Object)`][objeq] defines object equality very
precisely:

> The equals method implements an equivalence relation on non-null object
> references:
>
> It is reflexive: for any non-null reference value x, x.equals(x) should return
> true.
>
> It is symmetric: for any non-null reference values x and y, x.equals(y) should
> return true if and only if y.equals(x) returns true.
>
> It is transitive: for any non-null reference values x, y, and z, if
> x.equals(y) returns true and y.equals(z) returns true, then x.equals(z) should
> return true.
>
> It is consistent: for any non-null reference values x and y, multiple
> invocations of x.equals(y) consistently return true or consistently return
> false, provided no information used in equals comparisons on the objects is
> modified.
>
> For any non-null reference value x, x.equals(null) should return false.

TIP: [EqualsTester][equalstester] validates each of these properties.

For most simple value objects (e.g.: a `Point` containing `x` and `y`
coordinates), this generally means that the equals method will only return true
if the other object has the exact same class, and each of the components is
equal to the corresponding component in the other object. Here, there are
numerous tools in the Java ecosystem to generate the appropriate `equals` and
`hashCode` method implementations, including [AutoValue][av].

Another pattern often seen is to declare a common supertype with a defined
`equals` method (like `List`, which defines equality by having equal elements in
the same order). Then, different subclasses of that supertype (`LinkedList` and
`ArrayList`) can be equal to other classes with that supertype, since the
concrete class of the `List` is irrelevant. This checker will allow these types
of equality, as we detect when two objects share a common supertype with an
`equals` implementation and allow that to succeed.

Outside of these two general groups of equals methods, however, it's very
difficult to produce correctly-behaving equals methods. Most of the time, when
`equals` is implemented in a non-obvious manner, one or more of the properties
above isn't satisfied (generally the symmetric property). This can result in
subtle bugs, explained below.

### A bad example of `equals()`

```java
class Foo {
  private String foo; // Some property

  public boolean equals(Object other) {
    if (other instanceof String) {
      return other.equals(foo); // We want to be able to call equals with a String
    }
    if (other instanceof Foo) {
      return ((Foo) other).foo.equals(foo); // Simplified, avoid null checks
    }
    return false;
  }

  public int hashCode() {
    return foo.hashCode();
  }
}
```

Here, `Foo`'s equals method is defined to accept a `String` value in addition to
other `Foo`'s. This may appear to work at first, but you end up with some
complex situations:

```java
Foo a = new Foo("hello");
Foo b = new Foo("hello");
String hi = "hello";

if (a.equals(b)) {
  System.out.println("yes"); // Is printed, expected
}
if (b.equals(hi)) {
  System.out.println("yes"); // Is printed, abusing equals
}
if (hi.equals(b)) {
  System.out.println("no"); // Isn't printed, since String doesn't equals() Foo
}

Set<Foo> set = new HashSet<Foo>();
set.add(a);
set.add(b);

if (set.contains(hi)) {
  // Maybe? Depends on which way HashSet decides to call .equals()
  System.out.println("contained");
  // Is it removed? It's not guaranteed to be, since the .equals() method could
  // be called the other way in the remove path. Object.equals documentation
  // specifies it's supposed to be symmetric, so this could work.
  boolean removed = set.remove(hi);
}
```

[equalstester]: http://static.javadoc.io/com.google.guava/guava-testlib/19.0/com/google/common/testing/EqualsTester.html
[objeq]: https://docs.oracle.com/javase/7/docs/api/java/lang/Object.html#equals(java.lang.Object)
[av]: https://github.com/google/auto/blob/master/value/userguide/index.md
