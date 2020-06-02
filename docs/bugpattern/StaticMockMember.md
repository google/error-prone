Making a `@Mock` instance `static` will share the state across tests and can
make them order dependent and/or unclear to reader/maintainer. Removing
`static`, will ensure fresh mock instances are used for each tests.

Additionally, if the `@Mock` instance is marked as `static` to make it
serializable, there is a cleaner way to do it :
https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html#serilization_across_classloader
and
https://javadoc.io/static/org.mockito/mockito-core/3.3.3/org/mockito/Mock.html#serializable--
