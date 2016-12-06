The problem is described in the [javadoc] for `Files.lines`:

> The returned stream encapsulates a Reader. If timely disposal of file system
> resources is required, the try-with-resources construct should be used to
> ensure that the stream's close method is invoked after the stream operations
> are completed.

[javadoc]: https://docs.oracle.com/javase/8/docs/api/java/nio/file/Files.html#lines-java.nio.file.Path-java.nio.charset.Charset-

To ensure the stream is closed, always use try-with-resources with
`Files.lines`:

```java
String input;
try (Stream<String> stream = Files.lines(path)) {
  input = stream.collect(Collectors.joining(", "));
}
```
