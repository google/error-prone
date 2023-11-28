Constants should be given names that emphasize the semantic meaning of the
value. If the name of the constant doesn't convey any information that isn't
clear from the value, consider inlining it.

For example, prefer this:

```java
System.err.println(1);
System.err.println("");
```

to this:

```java
private static final int ONE = 1;
private static final String EMPTY_STRING = "";
...
System.err.println(ONE);
System.err.println(EMPTY_STRING);
```
