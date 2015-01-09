---
title: TypeParameterUnusedInFormals
layout: bugpattern
category: JDK
severity: WARNING
maturity: MATURE
---

# Bug pattern: TypeParameterUnusedInFormals
__Declaring a type parameter that is only used in the return type is a misuse of generics: operations on the type parameter are unchecked, it hides unsafe casts at invocations of the method, and it interacts badly with method overload resolution__

## The problem
A method's type parameters should always be referenced in the declaration of one or more formal parameters. Type parameters that are only used in the return type are a source of type-unsafety.
First, operations on the type will be unchecked after the type parameter is erased. For example:

    static <T> T doCast(Object o) {
      return (T) o; // this will always succeed, since T is erased
    }

The 'doCast' method would be better implemented as:

    static <T> T doCast(Class<T> clazz, Object o) {
      return clazz.cast(o); // has the expected behaviour
    }

Second, this pattern causes unsafe casts to occur at invocations of the method. Considerthe following snippet, which uses the first (incorrect) implementation of 'doCast':

    this.<String>doCast(42);  // succeeds
    String s = doCast(42);  // fails at runtime

Finally, relying on the type parameter to be inferred can have surprising results, andinteracts badly with overloaded methods. Consider:

    <T> T getThing()
    void assertThat(int a, int b)
    void assertThat(Object a, Object b)

This invocation will be ambiguous:

    assertThat(42, getThing());// both method assertThat(int,int) and method assertThat(Object,Object) match


## Suppression
Suppress false positives by adding an `@SuppressWarnings("TypeParameterUnusedInFormals")` annotation to the enclosing element.
