`double` and `float` literals that can't be precisely represented should be
avoided.

Example:

```java
double d = 1.9999999999999999999999999999999;
System.err.println(d); // prints 2.0
```
