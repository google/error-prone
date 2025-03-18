Class names from `java.lang` should never be reused. From
[Java Puzzlers](https://www.javapuzzlers.com/java-puzzlers-sampler.pdf):

> Avoid reusing the names of platform classes, and never reuse class names from
> `java.lang`, because these names are automatically imported everywhere.
> Programmers are used to seeing these names in their unqualified form and
> naturally assume that these names refer to the familiar classes from
> `java.lang`. If you reuse one of these names, the unqualified name will refer
> to the new definition any time it is used inside its own package.
