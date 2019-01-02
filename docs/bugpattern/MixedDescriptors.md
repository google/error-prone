A proto's `Descriptor` was created by mixing the `Descriptors` class from one
proto with the field number from another. E.g.:

```java {.bad}
Foo.getDescriptors().findFieldByNumber(Bar.ID_FIELD_NUMBER)
```

This accesses the `Descriptor` of a field in `Foo` with a field number from
`Bar`. One of these was probably intended:

```java {.good}
Foo.getDescriptors().findFieldByNumber(Foo.ID_FIELD_NUMBER)
Bar.getDescriptors().findFieldByNumber(Bar.ID_FIELD_NUMBER)
```
