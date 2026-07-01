Reference types should normally be compared for value equality with `equals()`,
not for object identity with `==` or `!=`.

## FAQs

### What if I know that this class's `equals()` method uses object identity anyway?

This check allows you to use `==` and `!=` in cases in which it can reliably
determine that they would behave identically to `equals()`, such as for enums or
for final classes that inherit the default `equals()` implementation from
`Object`. You might prefer `==` and `!=` in such cases, since it is less
verbose, especially compared to using `Objects.equals(a, b)` in cases in which
you need to tolerate null values.

### How about comparing interned objects?

It's dangerous to rely on your instances being interned. We have no tooling to
check or enforce that, and it's easy to get wrong.

### But what about `Boolean` values? We _know_ there's just `TRUE` and `FALSE` (and `null`). Surely _they're_ okay!

Well, no, because some tricky client can always generate a new instance with
`new Boolean(true)`. Comparing with `equals` always works; comparing with `==`
doesn't.

### How about a reference equality comparison before a more expensive content equality comparison?

The check allows implementations of `Object#equals()` to perform reference
equality tests on the type equality is being implemented for. For example:

```
abstract class Foo {

  abstract String bar();

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true; // fast path, reference equality is allowed here
    }
    if (!(other instanceof Foo)) {
      return false;
    }
    Foo that = (Foo) other;
    // value equality should still be used for types other than `Foo`
    return bar().equals(that.bar());
  }
}
```

In other cases, calling `Type#equals()` should be just as fast, because that
method will likely be inlined, and the first thing it will likely do is that
same instance comparison.

Alternatively, if you're okay with accepting `null`, you could call
`java.util.Objects.equals()`, which first does a reference equality comparison
and then falls back to content equality for non-null arguments.

### How about asserting in a test that two different references point to the same object (or not)?

Assertion libraries provide clearer ways to assert this, with the bonus of
providing better failure messages:

Truth:

```
assertThat(a).isSameInstanceAs(b);
assertThat(a).isNotSameInstanceAs(b);
```

AssertJ:

```
assertThat(a).isSameAs(b);
assertThat(a).isNotSameAs(b);
```

### How about comparing against a special marker instance?

Classes override `equals` to express when two instances should be treated as
interchangeable with each other. Predominant Java libraries and practices are
built on that assumption. Defining a "magic instance" for such a type goes
against this whole practice, leaving you vulnerable to unexpected bugs.

Consider choosing a sentinel value within the domain of the type (the moral
equivalent of `-1` for indexOf function calls) that you could compare against
using the normal `equals` method.

### So how can I put a special "nothing" value in my map?

Use `Optional<V>` as the value type of your map instead.
