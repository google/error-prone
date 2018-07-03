###UnusedInject
Fields that are annotated with @Inject and are not used in the class are reported by this error-prone checker. This is to prevent creation of unnecessary classes/methods by Dagger. In order to avoid this error, remove such unused fields.

###UnusedParam
Parameters in methods that are annotated with @Provides and that are not used in the method implementation are reported by this error-prone checker. This is to prevent creation of unnecessary classes/methods by Dagger. In order to avoid this error, remove unused parameters appropriately.
