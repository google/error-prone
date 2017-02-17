When declaring type parameters, it's possible to declare a type parameter with
the same name as another type parameter in scope causing unintended
incompatibilities when trying to use them together.

```java
public class Foo<T> {
    void instanceMethod(T t) {}
    <T> void genericMethod(T t) {
      instanceMethod(t); // FAIL: T declared in this method doesn't correspond to Foo<T>'s T
    }
}
```

In some cases, the type variable being declared has no relation to the type variable being shadowed.
It may be appropriate to rename the shadowing type variable:

```java
class Logger<T> {
  void log(T t) {}
  <T> logOther(T t) { ... } // Really should be <O> void logOther(O o), since this T is unrelated.
}
```

Depending on the nature of the surrounding code, you might be able to remove the generic declaration
on a method, or convert the generic method into a static method that doesn't inherit the surrounding
type parameter:

```java
class Holder<T> {
  T held;

  public Foo<T> fooIt() {
    return fooify(held);
  }

  private <T> Foo<T> fooify(T t) { ... } // Could be static, or non-generic
}
```

If an inner class declaration shadows a type variable, you may be able to remove the type variable,
make it a static inner class, or rename the type variable:

```java
class BoxingBox<T> {
  // Works if you make the class static.
  // If you remove the type parameter, you'll need to update stuff to List<Container>
  class Container<T> {
    T held;
  }
  List<Container<T>> stuff = new ArrayList<>();

  T get(int index) {
    return stuff.get(index).held;
  }
}
```

