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

## Configuration

By default, this check does not report findings in `package-info.java` files:
there, the import added by the fix appears *after* the package annotation that
uses it, and a single fully qualified annotation is arguably clearer (see
[#1652](https://github.com/google/error-prone/issues/1652)). To check
`package-info.java` files as well, for example to be consistent with
`module-info.java` files (which are always checked), pass:

```
-XepOpt:UnnecessarilyFullyQualified:ExemptPackageInfo=false
```

To report only one finding per fully qualified type in a compilation unit (the
fix still updates all usages), pass:

```
-XepOpt:UnnecessarilyFullyQualified:BatchFindings=true
```
