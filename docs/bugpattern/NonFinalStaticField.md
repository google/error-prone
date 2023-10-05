`static` fields should almost always be both `final` and deeply immutable.

Instead of:

```java
private static String FOO = "foo";
```

Prefer:

```java
private static final String FOO = "foo";
```
