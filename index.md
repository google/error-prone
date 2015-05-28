---
title: Error Prone
layout: master
---

# Error Prone

It's common for even the best programmers to make simple mistakes. And sometimes a refactoring which seems safe can leave behind code which will never do what's intended.

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

__src/Main.java__

```java
public class Main {
  public static void main(String[] args) {
    if (args.length < 1) {
      new IllegalArgumentException("Missing required argument");
    }
  }
}
```

```bash
../examples/maven/error_prone_should_flag$ mvn compile
[INFO] Compiling 1 source file to .../examples/maven/error_prone_should_flag/target/classes
.../examples/maven/error_prone_should_flag/src/main/java/Main.java:20: error: [DeadException] Exception created but not thrown
    new Exception();
    ^
    (see http://errorprone.info/bugpattern/DeadException)
  Did you mean 'throw new Exception();'?
1 error
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
```
