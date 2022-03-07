If a reference to the object is used before fully initialized bad things can happen, as code may presume it is fully initialized when it is accesible to outside code.

In the example below, this would lead to a null pointer exception, as the public member variable was not instantiated before being accessed.

```java
class PassesThisToOther {
    public Integer x;
    PassesThisToOther() {
        Other.someMethod(this);
        x = Integer.valueOf(0);
    }
}

class Other {
    public static void someMethod(PassesThisToOther p) {
        // NullPointerException on the following operation
        p.x++;
    }
}
```

While you may prevent this by always instantiating all members before passing `this` it is in general bad practice and error prone.
