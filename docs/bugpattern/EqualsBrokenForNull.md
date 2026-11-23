Object.equals() contract states that "For any non-null reference value x,
x.equals(null) should return false". Thus all classes implementing equals()
function should gracefully handle null as an argument and return false.
Unfortunately, many classes throw NullPointerException when null is passed to
equals(). For example:

```java
@Override
public boolean equals(Object obj) {
  if (!getClass().equals(obj.getClass())) { // NPE dereferencing obj
    return false;
  }
  // … more member equality checking …
  return true;
}
```

EqualsBrokenForNull check modifies these equals() methods to add a null-check to
bring them into compliance for the equals() contract and avoid
NullPointerExceptions.

```java
@Override
public boolean equals(Object obj) {
  if (obj == null) {
    return false;
  }
  if (!getClass().equals(obj.getClass())) {
    return false;
  }
  // … more member equality checking …
  return true;
}
```
