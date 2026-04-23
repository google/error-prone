`Type#toString` shouldn't be used for Type comparison as it is expensive and
fragile.

## Annotation Processors

There are a few potential options to use APIs to compare different type mirrors,
depending on the requirements.

Consider if the type being inspected will always be a
[`DeclaredType`][DeclaredType] (a class or interface), or if it can also by a
type variable, primitive type, or another [`TypeKind`][TypeKind].

Prefer using [auto-common][auto-common] if possible. To test a `DeclaredType`,
use:

*   [`MoreTypes.isTypeOf`][isTypeOf] - `isTypeOf(Foo.class, typeMirror)`
*   [`MoreTypes#asTypeElement`][asTypeElement] -
    `asTypeElement(typeMirror).getQualifiedName().contentEquals(expectedName)`

If you're not sure if the `TypeMirror` is a class or interface, add a
[`MoreTypes#isType`][isType] check before calling `isTypeOf` or `asTypeElement`.

To test primitive types, you can use `typeMirror.getKind() == TypeKind.INT`.

To compare arbitrary `TypeMirror`s, use [`MoreTypes#equivalence`][equivalence].

If `auto-common` isn't available, use [`javax.lang.model.util.Types`][Types] and
[`javax.lang.model.util.Elements`][Elements] (which are available from
[`ProcessingEnvironment`][ProcessingEnvironment]).

*   [`Types#isSameType`][isSameType] - `types.isSameType(typeMirror,
    elements.getTypeElement(expectedTypeName).asType())`
*   [`Types#asElement`][asElement] -
    `Objects.equals(types.asElement(typeMirror),
    elements.getTypeElement(expectedTypeName))`

To compare against a generic type with specific type arguments, use
[`getDeclaredType`][getDeclaredType] to construct an instantiation of a generic
type to compare against:

```
types.isSameType(
    typeMirror,
    types.getDeclaredType(
        elements.getTypeElement("java.util.List"),
        elements.getTypeElement("java.lang.String").asType()));
```

[Types]: https://docs.oracle.com/en/java/javase/25/docs/api/java.compiler/javax/lang/model/util/Types.html
[Elements]: https://docs.oracle.com/en/java/javase/25/docs/api/java.compiler/javax/lang/model/util/Elements.html
[ProcessingEnvironment]: https://docs.oracle.com/en/java/javase/25/docs/api/java.compiler/javax/annotation/processing/ProcessingEnvironment.html
[isSameType]: https://docs.oracle.com/en/java/javase/25/docs/api/java.compiler/javax/lang/model/util/Types.html#isSameType(javax.lang.model.type.TypeMirror,javax.lang.model.type.TypeMirror)
[asElement]: https://docs.oracle.com/en/java/javase/25/docs/api/java.compiler/javax/lang/model/util/Types.html#asElement(javax.lang.model.type.TypeMirror)
[DeclaredType]: https://docs.oracle.com/en/java/javase/25/docs/api/java.compiler/javax/lang/model/type/DeclaredType.html
[TypeKind]: https://docs.oracle.com/en/java/javase/25/docs/api/java.compiler/javax/lang/model/type/TypeKind.html
[auto-common]: https://github.com/google/auto/blob/main/common/README.md
[isTypeOf]: https://javadoc.io/static/com.google.auto/auto-common/1.2.2/com/google/auto/common/MoreTypes.html#isTypeOf(java.lang.Class,javax.lang.model.type.TypeMirror)
[asTypeElement]: https://javadoc.io/static/com.google.auto/auto-common/1.2.2/com/google/auto/common/MoreTypes.html#asTypeElement(javax.lang.model.type.TypeMirror)
[isType]: https://javadoc.io/static/com.google.auto/auto-common/1.2.2/com/google/auto/common/MoreTypes.html#isType(javax.lang.model.type.TypeMirror)
[equivalence]: https://javadoc.io/static/com.google.auto/auto-common/1.2.2/com/google/auto/common/MoreTypes.html#equivalence()
[getDeclaredType]: https://docs.oracle.com/en/java/javase/25/docs/api/java.compiler/javax/lang/model/util/Types.html#getDeclaredType(javax.lang.model.element.TypeElement,javax.lang.model.type.TypeMirror...)

## Error Prone

If this code is within an Error Prone check for comparing `Type`(s), all of the
same annotation processing APIs discussed above are available, but there are
some additional options.

*   Types can be looked up with `VisitorState`:

    ```
    ASTHelpers.isSameType(type, visitorState.getTypeFromString("com.package.SomeObject"), state)
    ```

*   Some types from `java.base` are available as cached instances in javac's
    symbol table:

    ```
    ASTHelpers.isSameType(type, state.getSymtab().objectType, state)
    ```
