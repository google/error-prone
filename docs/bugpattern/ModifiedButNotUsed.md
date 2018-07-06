Collections and proto builders which are created and mutated but never used may
be a sign of a bug, for example:

```java {.bad}
  MyProto.Builder builder = MyProto.newBuilder();
  if (field != null) {
    MyProto.NestedField.Builder nestedBuilder = MyProto.NestedField.newBuilder();
    nestedBuilder.setValue(field);
    // Oops--forgot to do anything with nestedBuilder.
  }
  return builder.build();
```
