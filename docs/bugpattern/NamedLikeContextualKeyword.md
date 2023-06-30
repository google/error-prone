The problem we're trying to prevent is clashes between the names of
classes/methods and contextual keywords. Clashes can occur when (1.) naming a
class/method, or (2.) when invoking.

# When naming

Change problematic names for classes and methods.

```java
class Foo {
  ...
  // This can clash with the contextual keyword "yield"
  void yield() {
    ...
  }
}
```

Another example:

```java
// This can clash with Java modules (JPMS)
static class module {
  ...
}
```

# When invoking

In recent versions of Java, `yield` is a restricted identifier:

```java
class T {
  void yield() {}
  {
    yield();
  }
}
```

```
$ javac --release 20 T.java
T.java:3: error: invalid use of a restricted identifier 'yield'
    yield();
    ^
  (to invoke a method called yield, qualify the yield with a receiver or type name)
1 error
```

To invoke existing methods called `yield`, use qualified names:

```java
class T {
  void yield() {}
  {
    this.yield();
  }
}
```

```java
class T {
  static void yield() {}
  {
    T.yield();
  }
}
```

```java
class T {
  void yield() {}
  class I {
    {
      T.this.yield();
    }
  }
}
```
