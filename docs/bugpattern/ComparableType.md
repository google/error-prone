When a class implements 'Comparable', it is possible to declare a type 'T' for
Comparable that is incompatible with the implementing class. This violates the
javadoc specs of 'compareTo'.

## But I'm doing something funky with my compareTo method!

The javadoc of [`Object.compareTo(Object)`][objcomp] defines object
comparability very precisely:

> 1) The implementor must ensure sgn(x.compareTo(y)) == -sgn(y.compareTo(x)) for
> all x and y. (This implies that x.compareTo(y) must throw an exception iff
> y.compareTo(x) throws an exception.)
>
> 2) The implementor must also ensure that the relation is transitive:
> (x.compareTo(y)>0 && y.compareTo(z)>0) implies x.compareTo(z)>0.
>
> 3) Finally, the implementor must ensure that x.compareTo(y)==0 implies that
> sgn(x.compareTo(z)) == sgn(y.compareTo(z)), for all z.

Consider the following example:

```java
public static class Foo implements Comparable<Integer> { // FAIL: Integer not compatible with Foo
  @Override
  public int compareTo(Integer o) {
    ...
  }
}
```

Here the type of 'Comparable' is Integer, which is not compatible with the type
of the class `Foo`. This implies that there is no way to satisfy the first
condition: `sgn(Foo.compareTo(Integer)) == -sgn(Integer.compareTo(Foo))` since
Integer's `compareTo` accepts `Integer`, not `Foo`.

[objcomp]: https://docs.oracle.com/javase/8/docs/api/java/lang/Comparable.html#compareTo-T-
