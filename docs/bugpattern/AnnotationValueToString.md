In [recent versions of javac](https://bugs.openjdk.java.net/browse/JDK-8268729),
`AnnotationValue#toString` returns a string representation of the annotation
value that uses simple names. If the string is used in generated source code, it
may require additional imports.

For example, instead of the class literal `com.pkg.Bar.class`, javac now returns
just `Bar.class`, which may require adding an import for for `com.pkg.Bar`.

`auto-common`'s `AnnotationValues#toString` method produces a string that uses
fully qualified names for annotations, class literals, and enum constants,
ensuring that source code containing that string will compile without additional
imports.

TIP: `AnnotationValues#toString` may be beneficial even if the string isn't
being used in generated code, e.g. if it's part of a diagnostic message or
assertion failure message, since the fully qualified names makes it clearer
which types are being referred to.
