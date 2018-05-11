Proto builders provide a pleasant fluent interface for constructing instances.
Unlike argument lists, however, they do not prevent the user from providing
multiple values for the same field.

Setting the same field multiple times in the same chained expression is
pointless (as the intermediate value will be overwritten), and certainly
unintentional. If the field is set to different values, it may be a bug, e.g.,

```java
return MyProto.newBuilder()
    .setFoo(copy.getFoo())
    .setFoo(copy.getBar())
    .build();
```
