Guice bindings are keyed by a pair of (optional Annotation, Type).

In most cirumstances, one doesn't need the annotation, as there's really just
one active implementation:

```java
bind(CoffeeMaker.class).to(RealCoffeeMaker.class);
...
@Inject Office(CoffeeMaker coffeeMaker) {}
```

However, in other circumstances, you want to bind a simple value (an integer,
String, double, etc.). You should use a Qualifier annotation to allow you to
get the *right* Integer back:

```java
bindConstant().annotatedWith(HttpPort.class).to(80);
...
@Inject MyWebServer(@HttpPort Integer httpPort) {}
```

This works great, but if your integer binding *doesn't* include a Qualifier, it
just means that you can ask Guice for "the Integer", and it will give you a
value back:

```java
bind(Integer.class).toInstance(80);
...
@Inject MyWebServer(Integer httpsPort) {}
```

To avoid confusion in these circumstances, please use a Qualifier annotation
when binding simple value types.
