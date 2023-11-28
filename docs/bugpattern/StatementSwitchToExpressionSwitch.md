We're trying to make `switch` statements simpler to understand at a glance.
Misunderstanding the control flow of a `switch` block is a common source of
bugs.

### Statement `switch` statements:

*   Have a colon between the `case` and the case's code. For example, `case
    HEARTS:`
*   Because of the potential for fall-through, it takes time and cognitive load
    to understand the control flow for each `case`
*   When a `switch` block is large, just skimming each `case` can be toilsome
*   Fall-though can also be conditional (see example below). In this scenario,
    one would need to reason about all possible flows for each `case`. When
    conditionally falling-through multiple `case`s in a row is possible, the
    number of potential control flows can grow rapidly

### Expression `switch` statements

*   Have an arrow between the `case` and the case's code. For example, `case
    HEARTS ->`
*   With an expression `switch` statement, you know at a glance that no cases
    fall through. No control flow analysis needed
*   Safely and easily reorder `case`s (within a `switch`)
*   It's also possible to group identical cases together (`case A, B, C`) for
    improved readability

### Examples

#### 1. Eliminate fall through

``` {.bad}
enum Suit {HEARTS, CLUBS, SPADES, DIAMONDS};

private void foo(Suit suit) {
  switch(suit) {
    case HEARTS:
      System.out.println("Red hearts");
      break;
    case DIAMONDS:
      System.out.println("Red diamonds");
      break;
    case SPADES:
      // Fall through
    case CLUBS:
      bar();
      System.out.println("Black suit");
    }
}
```

Which can be simplified into the following expression `switch`:

``` {.good}
enum Suit {HEARTS, CLUBS, SPADES, DIAMONDS};

private void foo(Suit suit) {
  switch(suit) {
    case HEARTS -> System.out.println("Red hearts");
    case DIAMONDS -> System.out.println("Red diamonds");
    case SPADES, CLUBS -> {
      bar();
      System.out.println("Black suit");
    }
  }
}
```

#### 2. Return switch

Sometimes `switch` is used with `return`. Below, even though a `case` is
specified for each possible value of the `enum`, note that we nevertheless need
a "should never happen" clause:

``` {.bad}
enum SideOfCoin {OBVERSE, REVERSE};

private String foo(SideOfCoin sideOfCoin) {
  switch(sideOfCoin) {
    case OBVERSE:
      return "Heads";
    case REVERSE:
      return "Tails";
    }
    // This should never happen, but removing this will cause a compile-time error
    throw new RuntimeException("Unknown side of coin");
}
```

Using an expression switch simplifies the code and removes the need for an
explicit "should never happen" clause.

```
enum SideOfCoin {OBVERSE, REVERSE};

private String foo(SideOfCoin sideOfCoin) {
  return switch(sideOfCoin) {
    case OBVERSE -> "Heads";
    case REVERSE -> "Tails";
  };
}
```

If you nevertheless wish to have an explicit "should never happen" clause, this
can be accomplished by placing the logic under a `default` case. For example:

```

enum SideOfCoin {OBVERSE, REVERSE};

private String foo(SideOfCoin sideOfCoin) {
  return switch(sideOfCoin) {
    case OBVERSE -> "Heads";
    case REVERSE -> "Tails";
    default -> {
      // This should never happen
      throw new RuntimeException("Unknown side of coin");
    }
  };
}
```

#### 3. Assignment switch

If every branch of a `switch` is making an assignment to the same variable, it
can be re-written as an assignment switch:

``` {.bad}
enum Suit {HEARTS, CLUBS, SPADES, DIAMONDS};

int score = 0;

private void updateScore(Suit suit) {
  switch(suit) {
    case HEARTS:
      // Fall thru
    case DIAMONDS:
      score += -1;
      break;
    case SPADES:
      score += 2;
      break;
    case CLUBS:
      score += 3;
    }
}
```

This can be simplified as follows:

```
enum Suit {HEARTS, CLUBS, SPADES, DIAMONDS};

int score = 0;

private void updateScore(Suit suit) {
  score += switch(suit) {
    case HEARTS, DIAMONDS -> -1;
    case SPADES -> 2;
    case CLUBS -> 3;
    };
}
```

#### 4. Complex control flows

Here's an example of a complex statement `switch` with conditional fall-through
and complex control flows. How many potential execution paths can you spot?

``` {.bad}
enum Suit {HEARTS, CLUBS, SPADES, DIAMONDS};

private int foo(Suit suit){
  switch(suit) {
    case HEARTS:
      if (bar()) {
        break;
      }
      // Fall through
    case CLUBS:
      if (baz()) {
        return 1;
      } else if (baz2()) {
        throw new AssertionError(...);
      }
      // Fall through
    case SPADES:
      // Fall through
    case DIAMONDS:
      return 0;
  }
  return -1;
}
```
