The Java language automatically converts primitive types to their boxed
representations in some contexts (see
[JLS 5.1.7](https://docs.oracle.com/javase/specs/jls/se11/html/jls-5.html#jls-5.1.7)).

That is, prefer this:

```java
int x;
Integer y = x;
```

to the equivalent but more verbose explicit conversion:

```java
int x;
Integer y = Integer.valueOf(x);
```
