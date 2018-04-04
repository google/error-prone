Scopes on modules have no function and are often incorrectly assumed to apply
the scope to every binding method in the module. This check removes all scope
annotations from any class annotated with `@Module` or `@ProducerModule`.
