Proto and AutoValue builders provide a fluent interface for constructing
instances. Unlike argument lists, however, they do not prevent the user from
providing multiple values for the same field.

Setting the same field multiple times in the same chained expression is
pointless (as the intermediate value will be overwritten), and can easily mask a
bug, especially if the setter is called with *different* arguments.

```java
return MyProto.newBuilder()
    .setFoo(copy.getFoo())
    .setFoo(copy.getBar())
    .build();
```
