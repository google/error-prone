Querying a collection for an element it cannot possibly contain is almost
certainly a bug.

In a generic collection type, query methods such as `Map.get(Object)` and
`Collection.remove(Object)` accept a parameter that identifies a potential
element to *look for* in that collection. This check reports cases where this
element *cannot* be present because its type and the collection's generic
element type are "incompatible." A typical example:

```java
Set<Long> values = ...
if (values.contains(1)) { ... }
```

This code looks reasonable, but there's a problem: The `Set` contains `Long`
instances, but the argument to `contains` is an `Integer`. Because no instance
can be of type `Integer` and of type `Long` at the same time, the `contains`
check always fails. This is clearly not what the developer intended.

Why does the collection API permit this kind of mistake? Why not declare the
method as `contains(E)`? After all, that is what the collections API does for
methods that *store* an element in the collection: They require that passed type
be strictly *assignable to* the collection's element type. For example:

```java
void addIntegerOne(Set<? extends Number> numbers) {
  numbers.add(1); // won't compile
}
```

The code above rightly won't compile, because `numbers` *might* be a (for
example) `Set<Long>`, and adding an `Integer` value would corrupt it.

But this restriction is necessary only for methods that insert elements. Methods
that only query or remove elements cannot corrupt the collection:

```java
void removeIntegerOne(Set<? extends Number> numbers) {
  numbers.remove(1); // should compile (and does)
}
```

In this case, the `Integer` `1` might be contained in `numbers`, and should be
removed if it is, but if `numbers` is a `Set<Long>`, no harm is done.

We'd like to define `contains` in a way that rejects the bad call but permits
the good one. But Java's type system is not powerful enough. Our solution is
static analysis.

The specific restriction we would like to express for the two types is not
assignability, but "compatibility". Informally, we mean that it must at least be
*possible* for some instance to be of both types. Formally, we require that a
"casting conversion" exist between the types as defined by [JLS 5.5.1]
(https://docs.oracle.com/javase/specs/jls/se7/html/jls-5.html#jls-5.5.1).

The result is that the method can be defined as `contains(Object)`, permitting
the "good" call above, but that Error Prone will give errors for incompatible
arguments, preventing the "bad."

### Footnote: Would requiring `E` have been better?

We might say: Sure, a buggy `remove` call can't corrupt a collection. And sure,
someone might want to pass an `Object` reference that happens to contain an `E`.
But isn't that a low standard for an API? We don't normally write code that way:

```java
void throwIfUnchecked(Object throwable) { // no "need" to require Throwable
  if (throwable instanceof RuntimeException) {
    throw (RuntimeException) throwable;
  }
  if (throwable instanceof Error) {
    throw (Error) throwable;
  }
}
```

Such code would invite bugs. To avoid that, we require a `Throwable`. Users who
have an `Object` reference that might be a `Throwable` can test `instanceof` and
cast. So why not require the same thing in the collections API?

Of course, we can't really change the API of `Collection`. But if we were
designing a similar API, what would we do -- require `E` or accept any `Object`?

The burden of proof falls on accepting `Object`, since doing so permits buggy
code. And we're not going to settle for "it occasionally saves users a cast."

*The main reason to accept `Object` is to permit a fast, type-safe `Set.equals`
implementation.*

(`equals` is actually just one example of the general problem, which arises with
many uses of wildcards. Once you've read the following, consider the problem of
implementing `Collection.removeAll(Collection<?>)` without `contains(Object)`.
Then consider how the problem would exist even if the signature were
`removeAll(Collection<? extends E>)`. The `removeAll` problem is at least
"solvable" by changing the signature to `removeAll(Collection<E>)`, but that
signature may reject useful calls.)

Here's how: `equals` necessarily accepts a plain `Object`. It can test whether
that `Object` is a `Set`, but it can't know the element type it was originally
declared with. In short, `equals` has to operate on a `Set<?>`.

If `contains` were to require an `E`, `equals` would be in trouble because it
doesn't know what `E` is. In particular, it wouldn't be able to call
`otherSet.contains(myElement)` for any of its elements.

It would have only two options: It could copy the entire other `Set` into a
`Set<Object>`, or it could perform an unchecked cast. Copying is wasteful, so in
practice, `equals` would need an unchecked cast. This is probably acceptable,
but we might feel strange for defining an API that can be implemented only by
performing unchecked casts.

Does a cleaner implementation (and occasional convenience to callers) outweigh
the bugs that accepting `Object` enables? That's a tough question. The good news
is that this Error Prone check gives you some of the best of both worlds.

### Footnote: Collection containing an incompatible type

It is technically possible for a `Set<Integer>` to contain a `String` element,
but only if an `unchecked` warning was earlier ignored or improperly suppressed.
Such practice should never be treated as acceptable, so it makes no practical
difference to our arguments above.
