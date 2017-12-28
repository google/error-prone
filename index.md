---
title: Error Prone
layout: master
---

# Error Prone

It's common for even the best programmers to make simple mistakes. And
sometimes a refactoring which seems safe can leave behind code which will never
do what's intended.

We're used to getting help from the compiler, but it doesn't do much beyond
static type checking. Using Error Prone to augment the compiler's type
analysis, you can catch more mistakes before they cost you time, or end up as
bugs in production. We use Error Prone in Google's Java build system to
eliminate classes of serious bugs from entering our code, and we've
open-sourced it, so you can too!

__Error Prone ...__

* __hooks into your standard build, so all developers run it without thinking__
* __tells you about mistakes immediately after they're made__
* __produces suggested fixes, allowing you to build tooling on it__

## How it works

```java
import java.util.Set;
import java.util.HashSet;

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
$ bazel build :hello
ERROR: example/myproject/BUILD:29:1: Java compilation in rule '//example/myproject:hello'
ShortSet.java:6: error: [CollectionIncompatibleType] Argument 'i - 1' should not be passed to this method;
its type int is not compatible with its collection's type argument Short
      s.remove(i - 1);
              ^
    (see http://errorprone.info/bugpattern/CollectionIncompatibleType)
1 error
```

## Testimonials

Doug Lea, on learning of [a bug we discovered in
ConcurrentHashMap](https://bugs.openjdk.java.net/browse/JDK-8176402):

> Definitely embarrassing. I guess I'm back to liking Error Prone even though
> it sometimes annoys me :-)
