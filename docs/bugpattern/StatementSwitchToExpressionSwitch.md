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

``` {.bad}
enum Suit {HEARTS, CLUBS, SPADES, DIAMONDS}

private void foo(Suit suit) {
  switch(suit) {
    case HEARTS:
System.out.println("Red hearts");
    case DIAMONDS:
System.out.println("Red diamonds");
    case SPADES:
      // Fall through
    case DIAMONDS:
      bar();
      System.out.println("Black suit");
    }
}
```

Which can be simplified into the following expression `switch`:

``` {.good}
enum Suit {HEARTS, CLUBS, SPADES, DIAMONDS}

private void foo(Suit suit) {
  switch(suit) {
    case HEARTS -> System.out.println("Red hearts");
    case DIAMONDS -> System.out.println("Red diamonds");
    case CLUBS, SPADES -> {
      bar();
      System.out.println("Black suit");
    }
  }
}
```

Here's an example of a complex statement `switch` with conditional fall-through
and complex control flows. How many potential execution paths can you spot?

``` {.bad}
enum Suit {HEARTS, CLUBS, SPADES, DIAMONDS}

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
