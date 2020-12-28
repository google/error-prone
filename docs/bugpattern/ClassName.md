While the Java programming langauge requires that `public` top-level classes are
declared in a source file matching their name (e.g.: `Foo.java` for `public
class Foo {}`, it is possible to declare a non-public top-level class in a file
with a different name (e.g.: `Bar.java` for `class Foo {}`).

The Google Java Style Guide §2.1 states, "The source file name consists of the
case-sensitive name of the top-level class it contains, plus the .java
extension."

## Suppression

Since `@SuppressWarnings` cannot be applied to package declarations, this
warning can be suppressed by annotating any top-level class in the compilation
unit with `@SuppressWarnings("ClassName")`.
