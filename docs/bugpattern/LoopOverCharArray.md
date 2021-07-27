[`String#toCharArray`](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html#toCharArray\(\))
allocates a new array. Calling `charAt` is more efficient, because it avoids
creating a new array with a copy of the character data.

That is, prefer this:

```java
boolean isDigits(String string) {
  for (int i = 0; i < s.length(); i++) {
    char c = s.charAt(i);
    if (!Character.isDigit(c)) {
      return false;
    }
  }
  return true;
}
```

to this:

```java
boolean isDigits(String string) {
  // this allocates a new char[]
  for (char c : string.toCharArray()) {
    if (!Character.isDigit(c)) {
      return false;
    }
  }
  return true;
}
```

Note that many loops over characters can be expressed using streams with
[`String#chars`][chars] or [`String#codePoints`][codePoints], for example:

```java
boolean isDigits(String string) {
  string.codePoints().allMatch(Character::isDigit);
}
```

[chars]: https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html#chars()
[codePoints]: https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html#codePoints()
