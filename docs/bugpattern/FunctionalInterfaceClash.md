Passing lambdas to an overloaded method may be ambiguous if two overloads have
parameters that are functional interfaces with equivalent methods.

Prefer to avoid ambiguous overloads, and consider renaming one of the methods.

For example `Function<String, Integer>` and `IntFunction<String>` are both
compatible with the lambda `x -> x.hashCode()`.

```java
void f(Function<String, Integer> x) {}
void f(IntFunction<String> x) {}
```

```
error: reference to f is ambiguous
    f(x -> x.hashCode());
    ^
  both method f(Function<String,Integer>) in Test and method f(IntFunction<String>) in Test match
```

To avoid the ambiguity, callers will have to use an explicit cast:

```java
f((IntFunction<String>) x -> x.hashCode());
f((Function<String, Integer>) x -> x.hashCode());
```

## Expression-bodied lambdas

The situation is more complicated with expression-bodied lambdas. Consider:

```java
void doIt(Function<String, String> f);
void doIt(Consumer<String> c);
```

[JLS
15.12.2.1](https://docs.oracle.com/javase/specs/jls/se8/html/jls-15.html#jls-15.12.2.1)
says that lambdas whose body is a statement expression are compatible with
functional interfaces whose function type is void-returning _or_ value
returning:

> A lambda expression (§15.27) is potentially compatible with a functional
> interface type (§9.8) if all of the following are true:
>
> *   The arity of the target type's function type is the same as the arity of
>     the lambda expression.
>
> *   If the target type's function type has a void return, then the lambda body
>     is either a statement expression (§14.8) or a void-compatible block
>     (§15.27.2).
>
> *   If the target type's function type has a (non-void) return type, then the
>     lambda body is either an expression or a value-compatible block
>     (§15.27.2).

So, if you have:

```java
doIt(x -> System.gc());
```

it's an implicitly typed statement-expression-bodied lambda that's compatible
with both overloads.

Any of the following disambiguate the overloads:

```
doIt((String x) -> x.toString()); // explicitly typed
doIt(x -> (x.toString())); // non-statement expression body
doIt(x -> {x.toString();}); // statement body
```
