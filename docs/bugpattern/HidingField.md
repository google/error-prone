If a class has a field of the same name as any field visible to it on any of its
superclasses or superinterfaces, the subclass' field is said to "[hide]
(https://docs.oracle.com/javase/tutorial/java/IandI/hidevariables.html)" the
superclass' field.

When this circumstance occurs, users of the class declaring the hiding field
can't interact with the fields from the superclass.

Let's take a look at how field hiding might cause problems:

```java
class Super {
  public String foo = "bar";
}

class Sub extends Super {
  private int foo = 0; // the same name, so this hides `Super`'s `foo`
}

class Main {
  void stringFn(String s) { /*...*/ }
  public static void main(String... args) {
    // Looking at the API of `Super`, I should be able to access a string `foo`
    // on any object of type `Super` or its subclasses, right?
    stringFn(new Sub().foo); // Oops! `foo` is not visible, and the wrong type!
  }
}
```
