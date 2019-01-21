The `@Mock` annotation is used to automatically initialize mocks using
`MockitoAnnotations.initMocks`, or `MockitoJUnitRunner`.

Variables annotated this way should not be explicitly initialized, as this will
be overwritten by automatic initialization.

