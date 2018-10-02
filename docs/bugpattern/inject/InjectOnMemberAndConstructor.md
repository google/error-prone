When a class uses `@Inject` on a field, and that field is also assigned from an
`@Inject` constructor, then the field is assigned twice from the DI Injector. This
may result in 2 objects being created, where the first instance is assigned then
thrown away after the second injection.

A simple solution is to remove the `@Inject` annotation from the injected field.
