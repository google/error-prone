The `@Inject` annotation is applied to **injection points** - methods,
constructors, or fields that a dependency injection tool like Guice or Dagger
will invoke or populate to construct an object.

Scope annotations (annotations that are themselves annotated with
`@ScopeAnnotation`) are used to denote the 'effective scope' of an object.
Examples of scope annotations include `@Singleton`, `@RequestScope`,
`@SessionScope`. These annotations can be used either on the declaration of a
type (so that every instantiation of that object will be handled with the
appropriate scope), or on a **provider method** (to declare that the particular
binding declared by that provider method is subject to that annotation's scoping
rules):

```java
@Singleton
class ExpensiveGlobalObject {
    @Inject ExpensiveGlobalObject(SomeDependencies stuff) {...}
}

class SomeModule extends AbstractModule {
    ...
    @Provides
    @Singleton
    OtherExpensiveObject provideOtherExpensiveObject() { return new Gold(); }
}
```

Qualifier annotations (annotations that are themselves annotated with
`@Qualifier` or `@BindingAnnotation`) are used to distinguish different
instances of the same type of object (the `@Red Robot` instead of the `@Blue
robot`). These annotations can be used _inside_ injection points, or on those
provider methods:

```java
class RobotArena {
    @Inject RobotArena(@Red Robot redBot, @Blue Robot blueBot) {...}
}

class SomeModule extends AbstractModule {
    ...
    @Provides
    @Red
    Robot provideRedRobot() { return new Robot("red"); }
}
```

Notably: there is no method declaration where `@Inject` and either a scope or a
qualifier annotation can reasonably coexist. Either the method is an **injection
point**, and the dependency injection system will ignore the scope or qualifier
annotation, or the method is a **provider** method, and the `@Inject` method
won't have any effect, as modules are constructed without a dependency injection
container.

```java {.bad}
class Example {
  // Here, @Singleton is ignored. Perhaps the scope should go onto Example, to make dependency
  // injection systems treat the Example type as a singleton.
  @Inject @Singleton
  Example(Robot something) {}
}
```

```java {.bad}
class MyModule extends AbstractModule {
  ...
  // The `@Inject` doesn't do anything: Guice will use this method to define a Singleton binding
  // for Database. When a Database is constructed by Guice, a DatabaseCredentials will be
  // constructed and this method will be invoked, but it isn't invoked until then.
  @Provides @Singleton @Inject
  Database providesSingletonDatabase(DatabaseCredentials db) { ... }
}
```
