Prefer using imported names to refer to classes, unless a qualified name is
necessary to disambiguate two classes with the same name.

That is, prefer this:

```java
import java.util.ArrayList;
import java.util.List;

class Test {
  List<String> names = new ArrayList<>();
}
```

instead of this:

```java
class Test {
  java.util.List<String> names = new java.util.ArrayList<>();
}
```
