We're trying to make long chains of `if` statements clearer (and potentially
faster) by converting them into `switch`es.

### Long chains of `if` statements

*   When a chain of `if ... else if ... else if ...` statements has `K` total
    branches, at runtime one needs to check `O(K)` conditions on average
    (assuming equal likelihood of each branch)
*   Condition-checking expressions are often repeated multiple times, once in
    each `if (...)`. Besides redundancy, this introduces a potential bug vector:
    an `if` in the chain could unintentionally have a slightly different
    condition than others, an ordering bug (see below), *etc.*

### `switch`es:

*   Support constants (`1`, `2`, ...), `enum` values, `null`, and pattern
    matching, including mixtures of these
*   Reduces redundancy
*   Runtime performance may benefit

### Examples

#### 1. Enum conversion

``` {.bad}
enum Suit {HEARTS, CLUBS, SPADES, DIAMONDS};

private void foo(Suit suit) {
  if (suit == Suit.SPADE) {
    System.out.println("spade");
  } else if (suit == Suit.DIAMOND) {
    System.out.println("diamond");
  } else if (suit == Suit.HEART) {
    System.out.println("heart);
  } else if (suit == Suit.CLUB) {
    System.out.println("club");
  }
}
```

Which can be converted into:

``` {.good}
enum Suit {HEARTS, CLUBS, SPADES, DIAMONDS};

private void foo(Suit suit) {
  switch (suit) {
    case Suit.SPADE -> System.out.println("spade");
    case Suit.DIAMOND -> System.out.println("diamond");
    case Suit.HEART -> System.out.println("heart");
    case Suit.CLUB -> System.out.println("club");
  }
}
```

Note that with the new `switch` style (`->`), one gets exhaustiveness checking
"for free". That is, if a new `Suit` value were to be added to the `enum`, then
the `switch` would raise a compile-time error, whereas the original chain of
`if` statements would need to be manually detected and edited.

If the flag `EnableSafe` is set, the output will include an empty `case null`;
this more closely matches the behavior of the original if-chain when `suit` is
`null`, although is more verbose and may not match the intent.

``` {.good}
enum Suit {HEARTS, CLUBS, SPADES, DIAMONDS};

private void foo(Suit suit) {
  switch (suit) {
    case Suit.SPADE -> System.out.println("spade");
    case Suit.DIAMOND -> System.out.println("diamond");
    case Suit.HEART -> System.out.println("heart");
    case Suit.CLUB -> System.out.println("club");
    case null -> {}
  }
}
```

#### 2. Patterns

This conversion works for `instanceof`s too:

``` {.bad}
enum Suit {HEARTS, CLUBS, SPADES, DIAMONDS};

private void describeObject(Object obj) {

  if (obj instanceof String) {
    System.out.println("It's a string!");
  } else if (obj instanceof Number n) {
    System.out.println("It's a number!");
  } else if (obj instanceof Object) {
    System.out.println("It's an object!");
  }
}
```

This can be converted as follows (if the `instanceof` does not originally have a
pattern variable, then `unused` will be inserted):

``` {.good}
enum Suit {HEARTS, CLUBS, SPADES, DIAMONDS};

private void describeObject(Object obj) {

  switch(obj) {
    case String unused -> System.out.println("It's a string!");
    case Number n -> System.out.println("It's a number!");
    case Object unused -> System.out.println("It's an object!");
  }
}
```

In later Java versions, an unnamed variable (`_`) can be used in place of
`unused`.

#### 3. Ordering Bugs

With `if` chains, it's possible to write code such as:

``` {.bad}
private void describeObject(Object obj) {

  if (obj instanceof Object) {
    System.out.println("It's an object!");
  } else if (obj instanceof Number n) {
    System.out.println("It's a number!");
  } else if (obj instanceof String) {
    System.out.println("It's a string!");
  }
}
```

When calling `describeObject("hello")`, one might expect to have `It's a
string!` printed, but this is not what happens. Because the `Object` check
happens first in code, it matches, resulting in `It's an object!`. This behavior
is most likely a bug, and can sometimes be hard to spot. This checker will
automatically reorder `case`s if needed to correct the issue, like this:

``` {.good}
private void describeObject(Object obj) {

  switch(obj) {
    case Number n -> System.out.println("It's a number!");
    case String unused -> System.out.println("It's a string!");
    case Object unused -> System.out.println("It's an object!");
  }
}
```

In this way, the behavior of the switch (`It's a string!`) and the original
if-chain (`It's an object!`) are different.
