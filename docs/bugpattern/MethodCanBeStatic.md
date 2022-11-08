Consider an instance method that is not an override, is not overrideable itself,
and never accesses this (explicitly or implicitly) in its implementation. Such a
method can always be marked `static` without harm.

The main benefit of adding `static` is that a caller who wants to use the method
and doesn't already have an instance handy won't have to conjure one up
unnecessarily. Doing that is a pain, and in unit tests it also creates the false
impression that instances in multiple states need to be tested.

But adding `static` also benefits your implementation in some ways. It becomes a
little easier to read and to reason about, since you don't need to wonder how it
might be interacting with instance state. And auto-completion will stop
suggesting the names of instance fields and methods (which you probably don't
want to use).

This analogy might work for you: it's widely accepted that a method like this
shouldn't declare any parameters it doesn't actually use; such parameters should
normally be removed. This situation with `static` is fairly similar: in either
case there is one additional instance the caller needs to have in order to
access the method. So, adding `static` is conceptually similar to removing that
unused parameter.

## Suppression

Methods which are used by reflection can be annotated with `@Keep` to suppress
the warning.

The `@Keep` annotation can also be applied to annotations, to suppress the
warning for any member annotated with that annotation.

```java
import com.google.errorprone.annotations.Keep;

@Keep
@Retention(RetentionPolicy.RUNTIME)
@interface SomeAnnotation {}
...

public class Data {
  @SomeAnnotation
  int doSomething(int x) {
    return x;
  }
}
```

All false positives can be suppressed by annotating the variable with
`@SuppressWarnings("MethodCanBeStatic")`.
