Even though varargs methods are different than methods with an array parameter
as the last parameter, varargs methods are compiled into bytecode as methods
with an array as the last parameter. When a varargs method is _called_, the Java
compiler will insert instructions to automatically box the varargs arguments
into an array.

This detail means that, for example, you can't declare two methods in the same
class where the final parameter is an array in one method, and a varargs of the
same type in the other:

```java
class Foo {
  void bah(double a, double... others) {}
  void bah(double baz, double[] myArray) {} // ERROR: bah(double, double[]) already defined
}
```

This also means that one method with varargs can override another method with an
array as the final parameter:

```java
class A {
  void something(int... ints) {}
}

class B extends A {
  @Override
  void something(int[] ints) {}
}
```

This overriding may be unintentional (since the signatures 'look' different, the
programmer may be unaware that an overriding has occurred).

Even if this overriding is intentional, it causes inconsistencies at call-sites,
as the code required to invoke the overridden method depends on the static type
of the variable being operated on.

Given the example classes above, observe the result on the client side:

```java
class Client {
  public static void main(String[] args) {
    B b = new B();
    A a = b;

    a.something(new int[]{1}); // OK, array invocation of varargs method
    b.something(new int[]{1}); // OK, direct array invocation

    a.something(2); // OK, varargs invocation with 1 element

    // Very strange compile-time error:
    // error: A.something(int...) is defined in an inaccessible class or interface
    b.something(1);
  }
}
```

To avoid these ambiguities, use the same parameter style (varargs or explicit
arrays) when overriding methods.
