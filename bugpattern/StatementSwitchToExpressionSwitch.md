---
title: StatementSwitchToExpressionSwitch
summary: This statement switch can be converted to a new-style arrow switch
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
We're trying to make `switch`es simpler to understand at a glance.
Misunderstanding the control flow of a `switch` is a common source of bugs.

As part of this simplification, new-style arrow (`->`) switches are encouraged
instead of old-style colon (`:`) switches. And where possible, neighboring cases
are grouped together (e.g. `case A, B, C`).

### Old-style colon (`:`) `switch`es:

*   Have a colon between the `case` and the `case`'s code. For example, `case
    HEARTS:`
*   Because of the potential for fall-through, it takes time and cognitive load
    to understand the control flow. When a `switch` block is large, just
    skimming each `case` can be toilsome. Fall-through can also be conditional
    (see example 5. below). In this scenario, one would potentially need to
    reason about all possible flows for each `case`. When conditionally
    falling-through multiple `case`s, the number of potential control flows can
    grow rapidly
*   Lexical scopes overlap, which can lead to surprising behaviors: definitions
    of local variables from earlier `case`s are propagated down to later
    `case`s, however the *values* that initialize those local variables do not
    propagate in the same way

### New-style arrow (`->`) `switch`es:

*   Have an arrow between the `case` and the `case`'s code. For example, `case
    HEARTS ->`
*   No `case`s fall through; no control flow analysis needed
*   Safely and easily reorder `case`s (within a `switch`)
*   Lexical scopes are isolated between different `case`s; if you define a local
    variable within a `case`, it can only be used within that specific `case`.

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

Which can be simplified by grouping and using a new-style switch:

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

#### 2. `return switch ...`

Sometimes `switch` is used with a `return` for each `case`, like this:

``` {.bad}
enum SideOfCoin {OBVERSE, REVERSE};

private String renderName(SideOfCoin sideOfCoin) {
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

Note that even though a `case` is present for each possible value of the `enum`,
a boilerplate "should never happen" clause is still needed. The transformed code
is simpler and doesn't need a "should never happen" clause.

```
enum SideOfCoin {OBVERSE, REVERSE};

private String renderName(SideOfCoin sideOfCoin) {
  return switch(sideOfCoin) {
    case OBVERSE -> "Heads";
    case REVERSE -> "Tails";
  };
}
```

If you nevertheless wish to define an explicit "should never happen" clause,
this can be accomplished by placing the logic inside a `default` case. For
example:

```
enum SideOfCoin {OBVERSE, REVERSE};

private String foo(SideOfCoin sideOfCoin) {
  return switch(sideOfCoin) {
    case OBVERSE -> "Heads";
    case REVERSE -> "Tails";
    default -> throw new RuntimeException("Unknown side of coin"); // should never happen
  };
}
```

When the checker detects an existing `default` that appears to be redundant, it
may suggest a secondary auto-fix which removes the redundant `default` and its
code (if any).

#### 3. Assignment `switch`

If every branch of a `switch` is making an assignment to the same variable, the
code can be simplified into a combined assignment and `switch`:

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

Taking this one step further: if a local variable is defined, and then
immediately followed by a `switch` in which every `case` assigns to that same
variable, then all three (the `switch`, the variable declaration, and the
assignment) can be merged:

``` {.bad}
enum Suit {HEARTS, CLUBS, SPADES, DIAMONDS};

private void updateStatus(Suit suit) {
  int score;

  switch(suit) {
    case HEARTS:
    // Fall thru
    case DIAMONDS:
      score = 1;
      break;
    case SPADES:
      score = 2;
      break;
    case CLUBS:
      score = 3;
    }
  ...

}
```

Becomes:

```
enum Suit {HEARTS, CLUBS, SPADES, DIAMONDS};

private void updateStatus(Suit suit) {
  int score = switch(suit) {
    case HEARTS, DIAMONDS -> 1;
    case SPADES -> 2;
    case CLUBS -> 3;
  };
  ...
}
```

#### 4. Just converting to new arrow `switch`

Even when the simplifications discussed above are not applicable, conversion to
new arrow `switch`es can be automated by this checker:

``` {.bad}
enum Suit {HEARTS, CLUBS, SPADES, DIAMONDS};

private void processEvent(Suit suit) {
    switch (suit) {
      case CLUBS:
        String message = "hello";
        var anotherMessage = "salut";
        processMessages(message, anotherMessage);
        break;
      case DIAMONDS:
        anotherMessage = "bonjour";
        processMessage(anotherMessage);
    }
}
```

Note that the local variables referenced in multiple cases are hoisted up out of
the `switch` statement, and `var` declarations are converted to explicit types,
resulting in:

```
enum Suit {HEARTS, CLUBS, SPADES, DIAMONDS};

private void processEvent(Suit suit) {
    String anotherMessage;
    switch (suit) {
      case CLUBS -> {
        String message = "hello";
        anotherMessage = "salut";
        processMessages(message, anotherMessage);
      }
      case DIAMONDS -> {
        anotherMessage = "bonjour";
        processMessage(anotherMessage);
      }
    }
}
```

#### 5. Complex control flows

Here's an example of a complex statement `switch` with conditional fall-through
and various control flows. Unfortunately, the checker does not currently have
the ability to automatically convert such code to new-style arrow `switch`es.
Manually converting the code could be a good opportunity to improve its
readability.

How many potential execution paths can you spot?

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

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("StatementSwitchToExpressionSwitch")` to the enclosing element.
