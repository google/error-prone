Some methods (most obviously `@Inject` constructors) exist only to be invoked by
a framework
([Guice](https://github.com/google/guice/wiki/KeepConstructorsHidden), Dagger,
etc), and so should only have package-private visibility. This helps discourage
manual invocation of these methods.

```java
public final class MyInjectableClass {
  @Inject
  public MyInjectableClass(MyDependency dependency) {}
}
```
