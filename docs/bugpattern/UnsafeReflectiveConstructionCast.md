Prefer `asSubclass` instead of casting the result of `newInstance` to detect
classes of incorrect type before invoking their constructors. This way, if the
class is of the incorrect type, it will throw an exception before invoking its
constructor.

```java
(Foo) Class.forName(someString).getDeclaredConstructor(...).newInstance(args);
```

Should be written as

```java
Class.forName(someString).asSubclass(Foo.class).getDeclaredConstructor(...).newInstance();
```

This has caused issues in the past:

CVE-2014-7911 - http://seclists.org/fulldisclosure/2014/Nov/51
