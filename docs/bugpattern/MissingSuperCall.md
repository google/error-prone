API providers may annotate a method with an annotation like
`android.support.annotation.CallSuper` or
`javax.annotation.OverridingMethodsMustInvokeSuper` to require that overriding
methods invoke the super method. This check enforces those annotations.
