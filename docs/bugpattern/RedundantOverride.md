An override of a method that delegates its implementation to the super method is
redudant, and can be removed.

For example, the `equals` method in the following class implementation can be
deleted.

```java
class Test {
  @Override
  public boolean equals(Object o) {
    return super.equals(o);
  }
}
```
