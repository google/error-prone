The collections returned by the protobuf API are unmodifiable: attempts to
modify them will result in an `UnsupportedOperationException`.

The protobuf collections are not implemented using Guava's immutable
collections, so copying them as a Guava collection isn't free.

This check suggests omitting copies of unmodifiable protobuf collections as
Guava immutable collections in places where a Guava immutable collection is not
required, for example to replace this:

```java
List<String> foos(MyProtoMessage message) {
  return ImmutableList.copyOf(message.getFoos());
}
```

with this:

```java
List<String> foos(MyProtoMessage message) {
  return message.getFoos();
}
```

Note that the check will not report diagnostics if the result of the copy is
being used somewhere that requires a Guava Immutable collection, for example it
is returned from the current method, or passed to an API that expects a Guava
immutable collection:

```java
ImmutableList<String> foos(MyProtoMessage message) {
  return ImmutableList.copyOf(message.getFoos());
}
```

The check will suggest refactoring local variables, if the local variable is
never used in a context that requires a Guava immutable collection. Using
Guava's collections has some benefit in this case, since it makes the
immutability of the collection clear and can enable other compile-time static
analysis for accidental attempts to modify the collection. But this value is
limited if it is only used as a local variable that never escapes the current
method, and there is some runtime cost from making the copy.
