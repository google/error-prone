Fields and methods should always be imported by their canonical name. The
canonical name of a top-level class is the fully-qualified name of the package,
followed by a `.`, followed by the name of the class. The canonical name of a
member class is the canonical name of its declaring class, followed by a `.`,
followed by the name of the member class. The canonical name of a field or
method is the canonical name of the type it is declared in, followed by a `.`,
followed by the name of the field or method.

Fully-qualified names are not necessarily canonical.  Consider some field `f`
declared in a class `C`. There may be another class `D` that extends `C` and
inherits `f`.  Therefore `f` can be accessed using the fully-qualified name `D.f`.
Since `f` is not declared in `D`, this name is not canonical.

The JLS §7.5.3 requires all single static imports to *start* with a canonical
type name, but the fully-qualified name of the imported member is not required
to be canonical.

Importing members using non-canonical names is unnecessary and unclear, and
should be avoided.

Example:

```java
package a;

class One {
  public static final int CONST = 42;
}
```

```java
package a;

class Two extends One {}
```

An import of `CONST` should always refer to it using the canonical name
`a.One.CONST`, not `a.Two.CONST`.
