A field `Descriptor` was created by mixing the message `Descriptor` from one
proto message with the field number from another. For example:

```java
Foo.getDescriptor().findFieldByNumber(Bar.ID_FIELD_NUMBER)
```

This accesses the `Descriptor` of a field in `Foo` with a field number from
`Bar`. One of these was probably intended:

```java
Foo.getDescriptor().findFieldByNumber(Foo.ID_FIELD_NUMBER)
Bar.getDescriptor().findFieldByNumber(Bar.ID_FIELD_NUMBER)
```
