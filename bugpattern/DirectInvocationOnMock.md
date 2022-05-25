---
title: DirectInvocationOnMock
summary: Methods should not be directly invoked on mocks. Should this be part of a
  verify(..) call?
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Direct invocations on [mocks](mockito) should be avoided in tests.

When you call a method on a mock, the call normally does only what you have
configured it to do (through calls to `when(...).thenReturn/thenAnswer`, etc.)
and makes a record of the call that can be read by later `verify(...)` calls.
Both of these are rarely what you want:

-   The reason to configure a mock's behavior is so that the code under test
    will react to that behavior. If you want for the test itself to do
    something, then do it directly instead of by calling a method on the mock.
-   If a future reader of your test sees a call to `verify(foo).bar()`, then the
    reader will expect the test to succeed only because the code under test
    called `bar()`, not because the test itself did.

Sometimes, test authors, especially those familiar with other mocking frameworks
(like EasyMock), will call a method on a mock for one of two reasons:

1.  By default, EasyMock requires the test setup to call every method that the
    code under test will call. Mockito's defaults do *not* require this.
2.  Many EasyMock tests choose to have EasyMock require that the code under test
    call *all* the methods that the test setup calls. Mockito tests that want to
    verify calls to methods must verify each call individually. To check that
    any particular method has been called, a Mockito test must call
    `verify(foo).bar()`, and it must do so *after* the code under test has run.

```java
@Test
public void balanceIsChecked() {
  Account account = mock(Account.class);
  LoanChecker loanChecker = new LoanChecker(account);

  assertThat(loanChecker.checkEligibility()).isFalse();

  // Should be verify(account).checkBalance();, or be removed if the call to
  // `checkEligibility` is sufficient proof the code is behaving as intended.
  account.checkBalance();
}
```

There is at least one edge case in which a call to a mock has different effects:
A call to a `final` method will normally *not* be intercepted by Mockito, so it
will run the implementation of that method in the code under test. Sometimes,
that method will call other methods on the mock object, producing effects
similar to if the test had called those methods directly. Sometimes, the method
will have other effects. Both kinds of effects can be confusing, so prefer to
avoid such calls when possible.

[mockito]: https://site.mockito.org/

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("DirectInvocationOnMock")` to the enclosing element.
