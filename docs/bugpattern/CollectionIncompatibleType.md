Querying a collection for an element it cannot possibly contain is almost
certainly a bug.

In a generic collection type, query methods such as `Map.get(Object)` and
`Collection.remove(Object)` accept a parameter that identifies a potential
element to *look for* in that collection. This check reports cases where this
element *cannot* be present because its type and the collection's generic
element type are "incompatible" (see below). A typical example:

```java
Set<Long> values = ...
if (values.contains(1)) { ... }
```

Because no instance can be of type `Integer` and of type `Long` at the same
time, the `contains` check always fails, which is clearly not what the developer
intended.

Note that for a parameter specifying an element to *store* in a collection, the
passed type must be strictly *assignable to* the collection's element type to
prevent corrupting the collection. Since we can easily express this restriction
in the method signature (`add(E element)`) there is no need for additional
static analysis. This does exactly what we want:

```java
void addIntegerOne(Set<? extends Number> numbers) {
  numbers.add((Integer) 1); // won't compile
}
```

The code above rightly won't compile, because `numbers` *might* be a
(for example) `Set<Double>`, and adding an `Integer` value would corrupt it.

But this same restriction would be overkill for a harmless query method.

```java
void removeIntegerOne(Set<? extends Number> numbers) {
  numbers.remove((Integer) 1); // should compile (and does)
}
```

In this case, the `(Integer) 1` might be contained in `numbers`, and should be
removed if it is, but if `numbers` is a `Set<Double>` no harm is done.

Here, the restriction we would like to express for the two types is not
assignability, but "compatibility". Informally, we mean that it must at least be
*possible* for some instance to be of both types. Formally, we require that a
"casting conversion" exist between the types as defined by
[JLS 5.5.1](https://docs.oracle.com/javase/specs/jls/se8/jls8.pdf#page=140).
Unfortunately this requirement cannot be expressed in the method signature, so
it must be done with static analysis.

*Footnote*

It is technically possible for a `Set<Integer>` to contain a `String` element,
but only if an `unchecked` warning was earlier ignored or improperly suppressed.
Such practice should never be treated as acceptable, so it makes no practical
difference to our arguments above.
