Wildcard imports are forbidden by §3.3.1 of the Google Java Style Guide.

They make code brittle and difficult to reason about, both for programmers and
for tools. In the following example, the processing of the first import
requires reasoning about the classes `Outer` and `Nested`, including their
supertypes, and ends up depending on information in the second import
statement. This code is (incorrectly) rejected by the latest version of javac.
Also, note that `I` is actually being imported via the name `p.q.C.I` even
though it's not declared in `C`! Its canonical name is `p.q.D.I`. Regular
single-type imports require that all types are imported by their canonical
name, but static imports do not.

```java
package p;

import static p.Outer.Nested.*;
import static p.q.C.*;

public class Outer {
  public static class Nested implements I {
  }
}
```

```java
package p.q;

public class C extends D {
}
```

```java
package p.q;

public class D {
  public interface I {
  }
}
```
