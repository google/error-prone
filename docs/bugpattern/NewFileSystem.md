Starting in JDK 13, calls to `FileSystem.newFileSystem(path, null)` are
ambiguous.

The calls match both:

*   [`FileSystem.newFileSystem(Path, ClassLoader)`](https://docs.oracle.com/en/java/javase/13/docs/api/java.base/java/nio/file/FileSystems.html#newFileSystem\(java.nio.file.Path,java.lang.ClassLoader\))
*   [`FileSystem.newFileSystem(Path, Map<?, ?>)`](https://docs.oracle.com/en/java/javase/13/docs/api/java.base/java/nio/file/FileSystems.html#newFileSystem\(java.nio.file.Path,java.util.Map\))

To disambiguate, add a cast to the desired type, to preserve the pre-JDK 13
behaviour.

That is, prefer this:

```java
FileSystem.newFileSystem(path, (ClassLoader) null);
```

Instead of this:

```java
FileSystem.newFileSystem(path, null);
```
