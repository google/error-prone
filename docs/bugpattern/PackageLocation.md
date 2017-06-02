Java files should be located in a directory that
ends with the fully qualified name of the package.

For example, classes in the package `edu.oswego.cs.dl.util.concurrent` should be
located in: `.../edu/oswego/cs/dl/util/concurrent`.


## Suppression

If necessary, the check may be suppressed by annotating the enclosing package
declaration with `@com.google.errorprone.annotations.SuppressPackageLocation`,
for example:

```java
// package-info.java
@com.google.errorprone.annotations.SuppressPackageLocation
package com.google.my.pkg;
```

Note that package annotations must be located in a `package-info.java` file
that must be built together with the package.
