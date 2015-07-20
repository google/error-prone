Types should always be imported by their canonical name. The canonical name of
a top-level class is the fully-qualified name of the package, followed by a
'.', followed by the name of the class. The canonical name of a member class is
the canonical name of its declaring class, followed by a '.', followed by the
name of the member class.

Fully-qualified member class names are not guaranteed to be canonical.
Consider some member class M declared in a class C. There may be another class
D that extends C and inherits M.  Therefore M can be accessed using the
fully-qualified name of D, followed by a '.', followed by 'M'. Since M is not
declared in D, this name is not canonical.

The JLS §7.5.3 requires all single static imports to *start* with a canonical
type name, but the fully-qualified name of the imported member is not required
to be canonical.

Importing types using non-canonical names is unnecessary and unclear, and
should be avoided.

Example:

```java
package a;

class One {
  static class Inner {}
}
```

```java
package a;

class Two extends One {}
```

An import of `Inner` should always refer to it using the canonical name
`a.One.Inner`, not `a.Two.Inner`.
