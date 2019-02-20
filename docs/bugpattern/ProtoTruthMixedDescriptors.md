ProtoTruth's `#ignoringFields` method accepts integer field numbers, so
supplying field numbers from the wrong protocol buffers is possible. For
example:

```proto
message Bar {
  optional string name = 1;
}
message Foo {
  optional string name = 1;
  optional Bar bar = 2;
}
```

```java {.bad}
void assertOnFoo(Foo foo) {
  assertThat(foo).ignoringFields(Bar.NAME_FIELD_NUMBER).isEqualTo(...);
}
```

This will ignore the `Foo#name` field rather than `Bar#name`. The field number
can be turned into a `Descriptor` object to resolve the correct nested field to
ignore:

```java {.good}
void assertOnFoo(Foo foo) {
  assertThat(foo)
      .ignoringFieldDescriptors(
          Bar.getDescriptor().findFieldByNumber(Bar.NAME_FIELD_NUMBER))
      .isEqualTo(...);
}
```
