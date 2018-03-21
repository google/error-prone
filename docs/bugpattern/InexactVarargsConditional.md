When calling a varargs method, you can either pass an explicit array of
arguments, or individual arguments:

```java
void f(Object... xs) {
  System.err.println(Arrays.deepToString(xs));
}
```

Both of the following print `[1, 2]`:

```
f(new Object[] {1, 2}) // prints "[1, 2]"
f(1, 2) // prints "[1, 2]"
```

If the argument to the varargs method is a conditional expression, and either
branch is not an array, the result of the expression will be implicitly wrapped
in an array.

```java
f(flag ? 1 : 2) // prints [1] or [2]
```

This means that if one branch is an array and the other branch is not, the array
branch will become a multi-dimensional array:

```java
f(flag ? new Object[] {1, 2} : 3); // prints [[1, 2]] or [3]
```

To avoid the implicit array creation, the other argument can be explicitly
wrapped in an array:

```java
f(flag ? new Object[] {1, 2} : new Object[] {3}); // prints [1, 2] or [3]
```

Or, if the multi-dimensional array was intentional, it can be written explicitly
as:

```java
f(flag ? new Object[][] {{1, 2}} : new Object[] {3}); // prints [[1, 2]] or [3]
```
