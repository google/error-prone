Modules that contain abstract binding methods (@Binds, @Multibinds) or only
static @Provides methods will not be instantiated by Dagger when they are
included in a component. Adding a private constructor clearly conveys that the
module will not be used as an instance.
