Using `stream::iterator` to create an `Iterable` results in an `Iterable` which
can only be iterated once, and will throw an `IllegalArgumentException` on
subsequent attempts.

The contract of `Iterable` is poorly defined, but many APIs will assume that
`Iterable`s allow multiple iteration.

To give a concrete example using protocol buffers,

```java
MyProto construct(Stream<Integer> ints) {
  return MyProto.newBuilder()
      .addAllSubMessage(
          ints.map(i -> SubMessage.newBuilder().setVal(i).build())::iterator)
      .build();
}
```

With the current implementation of the protocol buffer generated code, this will
work, but the following minor change will lead to re-iteration and failure,

```java
MyProto construct(Stream<Integer> ints) {
  MyProto.Builder builder = MyProto.newBuilder();
  builder.addSubMessageBuilder().setVal(0);
  return builder
      .addAllSubMessage(
          ints.map(i -> SubMessage.newBuilder().setVal(i).build())::iterator)
      .build(); // build iterates twice, and throws.
}
```

To avoid such pitfalls, the `Stream` can either be collected

```java
MyProto construct(Stream<Integer> ints) {
  return MyProto.newBuilder()
      .addAllSubMessage(
          ints.map(i -> SubMessage.newBuilder().setVal(i).build())
              .collect(toImmutableList()))
      .build();
}
```

or terminated with `forEachOrdered`

```java
MyProto construct(Stream<Integer> ints) {
  MyProto.Builder builder = MyProto.newBuilder();
  ints.map(i -> SubMessage.newBuilder().setVal(i).build())
      .forEachOrdered(builder::addSubMessage);
  return builder.build();
}
```
