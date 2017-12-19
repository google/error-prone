`ThreadLocal`s should be stored in `static` variables to avoid memory leaks. If
a `ThreadLocal` is stored in an instance (non-static) variable, there will be `M
* N` instances of the `ThreadLocal` value where `M` is the number of threads,
and `N` is the number of instances of the containing class. Each instance may
remain live as long the thread that stored it stays live.

Example:

```java {.bad}
class C {
   private final ThreadLocal<D> local = new ThreadLocal<D>();

   public f() {
       D d = local.get();
       if (d == null) {
          d = new D(this);
          local.set(d);
       }
       d.doSomething();
   }
}
```

The fix is often to make the field `static`:

```java {.good}
private static final ThreadLocal<D> local = new ThreadLocal<D>();
```
