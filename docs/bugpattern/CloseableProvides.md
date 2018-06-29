If you provide
[`Closeable`](https://docs.oracle.com/javase/7/docs/api/java/io/Closeable.html)
resources through dependency injection, it can be difficult to effectively
manage the lifecycle of the Closable:

```java
class MyModule extends AbstractModule() {
  @Provides
  FileOutputStream provideFileStream() {
    return new FileOutputStream("/tmp/outfile");
  }
}

...

class Client {
  private final FileOutputStream fos;
  @Inject Client(FileOutputStream fos, OtherDependency other, ...) {
     this.fos = fos;
  }
  void doSomething() throws IOException {
    fos.write("hello!");
  }
}
```

There are a number of issues with this approach as it relates to resource
management:

*   If multiple Client classes are constructed, multiple output streams are
    opened against the same file, and writes to the file may clash with
    each other.
*   It's not clear which class has the responsibility of closing the
    `FileOutputStream` resource:

    *   If the resource is created for the sole ownership of `Client`, then it
        makes sense for the client to close it.
    *   However, if the resource is scoped (e.g.: you add `@Singleton` to the
        `@Provides` method), then suddenly it's *not* the responsibility of
        `Client` to close the resource. If one `Client` closes the stream, then
        all of the other users of that stream will be dealing with a closed
        resource. There needs to be some other 'resource manager' object that
        also gets that stream, and its closing functions are call in the right
        place. That can be tricky to do correctly.

*   If, for example, the construction of the other dependencies of `Client`
    fails (resulting in a `ProvisionException`), then the `FileOutputStream`
    that may have been constructed leaks and isn't properly closed, even if
    `Client` normally closes its resources correctly.

The preferred solution is to not inject closable resources, but instead, objects
that can expose short-lived closable resources that are used as necessary. The
following example uses Guava's
[CharSource](https://github.com/google/guava/wiki/IOExplained#sources-and-sinks)
as the resource manager object:

```java
class MyModule extends AbstractModule() {
  @Provides
  CharSink provideCharSink() {
    return Files.asCharSink(new File("/tmp/outfile"), StandardCharsets.UTF_8);
  }
}

...

class Client {
  private final CharSink sink;
  @Inject Client(CharSink sink, OtherDependency other, ...) {
     this.sink = sink;
  }
  void doSomething() throws IOException {
    sink.write("hello!"); // Opens the file at this point, and closes once its done.
  }
}
```

If there's not a similar non-closable resource, you can write a simple wrapper:

```java
class ResourceManager {
  @Inject ResourceManager(@Config String configs, ...) {}

  /**
   * Returns a new thing for you to use and dispose of
   */
  OutputStream provideInstance() { return new...(); }
}

...
class Client {
  private final ResourceManager resource;
  @Inject Client(ResourceManager resource, OtherDependency other, ...) {
     this.resource = resource;
  }
  void doSomething() {
    try (OutputStream actualStream = resource.provideInstance()) {
      // write to actualStream, closing with try-with-resources
    }
  }
}

```

This pattern can be extended to other resources: as opposed to injecting
database connection handles directly, inject connection pool objects that
require your object to ask for those connection objects when they're needed and
close them safely.
