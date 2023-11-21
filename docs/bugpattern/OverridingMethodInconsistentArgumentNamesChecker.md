Inconsistently ordered parameters in method overrides mostly indicate an
accidental bug in the overriding method. An example for an overriding method
with inconsistent parameter names:

```java
class A {
  public void foo(int foo, int baz) { ... }
}

class B extends A {
  @Override
  public void foo(int baz, int foo) { ... }
}
```
