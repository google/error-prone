# Error Prone

Error Prone is a static analysis tool for Java that catches common programming
mistakes at compile-time.

```java
public class ShortSet {
  public static void main (String[] args) {
    Set<Short> s = new HashSet<>();
    for (short i = 0; i < 100; i++) {
      s.add(i);
      s.remove(i - 1);
    }
    System.out.println(s.size());
  }
}
```

```
error: [CollectionIncompatibleType] Argument 'i - 1' should not be passed to this method;
its type int is not compatible with its collection's type argument Short
      s.remove(i - 1);
              ^
    (see http://errorprone.info/bugpattern/CollectionIncompatibleType)
1 error
```

## Getting Started

Our documentation is at [errorprone.info](http://errorprone.info).

Error Prone works with [Bazel](http://bazel.io), [Maven]
(http://maven.apache.org), [Ant](http://ant.apache.org), and [Gradle]
(http://gradle.org). See our [installation instructions]
(http://errorprone.info/docs/installation) for details.

## Developing Error Prone

Developing and building Error Prone is documented on the [wiki]
(https://github.com/google/error-prone/wiki/For-Developers).

## Links

-   Mailing lists
    -   [General discussion]
        (https://groups.google.com/forum/#!forum/error-prone-discuss)
    -   [Announcements]
        (https://groups.google.com/forum/#!forum/error-prone-announce)
-   [Javadoc](http://errorprone.info/api/latest/)
-   Pre-release snapshots are available from [Sonatype's snapshot repository]
    (https://oss.sonatype.org/content/repositories/snapshots/com/google/errorprone/).
