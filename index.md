#summary Bugs caught by error-prone
==On by default==
  * [ArrayEquals]: {{{Reference equality used to compare arrays}}} _(error)_
  * [ArrayHashCode]: {{{hashcode method on array does not hash array contents}}} _(error)_
  * [ArrayToString]: {{{Calling toString on an array does not provide useful information}}} _(error)_
  * [ArrayToStringCompoundAssignment]: {{{Implicit toString used on an array (String += Array)}}} _(error)_
  * [ArrayToStringConcatenation]: {{{Implicit toString used on an array (String + Array)}}} _(error)_
  * [BadShiftAmount]: {{{Shift by an amount that is out of range}}} _(error)_
  * [CheckReturnValue]: {{{Ignored return value of method that is annotated with @CheckReturnValue}}} _(error)_
  * [ComparisonOutOfRange]: {{{Comparison to value that is out of range for the compared type}}} _(error)_
  * [DeadException]: {{{Exception created but not thrown}}} _(error)_
  * [DepAnn]: {{{Deprecated item is not annotated with @Deprecated}}} _(error)_
  * [EqualsNaN]: {{{== NaN always returns false; use the isNaN methods instead}}} _(error)_
  * [GuiceAssistedInjectScoping]: {{{Scope annotation on implementation class of AssistedInject factory is not allowed}}} _(error)_
  * [InvalidPatternSyntax]: {{{Invalid syntax used for a regular expression}}} _(error)_
  * [JUnit3TestNotRun]: {{{Test method will not be run; please prefix name with "test"}}} _(error)_
  * [JUnit4SetUpNotRun]: {{{setUp() method will not be run; Please add a @Before annotation}}} _(error)_
  * [JUnit4TearDownNotRun]: {{{tearDown() method will not be run; Please add an @After annotation}}} _(error)_
  * [JUnit4TestNotRun]: {{{Test method will not be run; please add @Test annotation}}} _(error)_
  * [LongLiteralLowerCaseSuffix]: {{{Prefer 'L' to 'l' for the suffix to long literals}}} _(error)_
  * [Overrides]: {{{Varargs doesn't agree for overridden method}}} _(error)_
  * [PreconditionsCheckNotNull]: {{{Literal passed as first argument to Preconditions.checkNotNull() can never be null}}} _(error)_
  * [PreconditionsCheckNotNullPrimitive]: {{{First argument to Preconditions.checkNotNull() is a primitive rather than an object reference}}} _(error)_
  * [ProtoFieldNullComparison]: {{{Protobuf fields cannot be null}}} _(error)_
  * [ReturnValueIgnored]: {{{Return value of this method must be used}}} _(error)_
  * [SelfAssignment]: {{{Variable assigned to itself}}} _(error)_
  * [SelfEquals]: {{{An object is tested for equality to itself}}} _(error)_
  * [StringBuilderInitWithChar]: {{{StringBuilder does not have a char constructor; this invokes the int constructor.}}} _(error)_
  * [SuppressWarningsDeprecated]: {{{Suppressing "deprecated" is probably a typo for "deprecation"}}} _(error)_
  * [TryFailThrowable]: {{{Catching Throwable masks failures from fail() or assert*() in the try block}}} _(error)_
  * [ElementsCountedInLoop]: {{{This code, which counts elements using a loop, can be replaced by a simpler library method}}} _(warning)_
  * [Finally]: {{{If you return or throw from a finally, then values returned or thrown from the try-catch block will be ignored}}} _(warning)_
  * [MisusedFormattingLogger]: {{{FormattingLogger uses wrong or mismatched format string}}} _(warning)_
  * [NonAtomicVolatileUpdate]: {{{This update of a volatile variable is non-atomic}}} _(warning)_
  * [PreconditionsInvalidPlaceholder]: {{{Preconditions only accepts the %s placeholder in error message strings}}} _(warning)_
  * [StaticAccessedFromInstance]: {{{A static variable or method should not be accessed from an object instance}}} _(warning)_
  * [StringEquality]: {{{String comparison using reference equality instead of value equality}}} _(warning)_
  * [SynchronizeOnNonFinalField]: {{{Synchronizing on non-final fields is not safe: if the field is ever updated, different threads may end up locking on different objects.}}} _(warning)_
  * [WaitNotInLoop]: {{{Object.wait() should always be called in a loop}}} _(warning)_
==Experimental==
  * [ArraysAsListPrimitiveArray]: {{{Arrays.asList does not autobox primitive arrays, as one might expect.}}} _(error)_
  * [AssistedInjectAndInjectOnSameConstructor]: {{{@AssistedInject and @Inject cannot be used on the same constructor.}}} _(error)_
  * [ClassCanBeStatic]: {{{Inner class is non-static but does not reference enclosing class}}} _(error)_
  * [CollectionIncompatibleType]: {{{Incompatible type as argument to non-generic Java collections method.}}} _(error)_
  * [CovariantEquals]: {{{equals() method doesn't override Object.equals()}}} _(error)_
  * [DivZero]: {{{Division by integer literal zero}}} _(error)_
  * [EmptyIf]: {{{Empty statement after if}}} _(error)_
  * [GuardedBy]: {{{Checks for unguarded accesses to fields and methods with @GuardedBy annotations}}} _(error)_
  * [GuardedByChecker]: {{{Checks for unguarded accesses to fields and methods with @GuardedBy annotations}}} _(error)_
  * [GuardedByValidator]: {{{Invalid @GuardedBy expression}}} _(error)_
  * [GuiceAssistedParameters]: {{{A constructor cannot have two @Assisted parameters of the same type unless they are disambiguated with named @Assisted annotations. }}} _(error)_
  * [InjectInvalidTargetingOnScopingAnnotation]: {{{The target of a scoping annotation must be set to METHOD and/or TYPE.}}} _(error)_
  * [InjectMoreThanOneQualifier]: {{{Using more than one qualifier annotation on the same element is not allowed.}}} _(error)_
  * [InjectMoreThanOneScopeAnnotationOnClass]: {{{A class can be annotated with at most one scope annotation}}} _(error)_
  * [InjectScopeAnnotationOnInterfaceOrAbstractClass]: {{{Scope annotation on an interface or abstact class is not allowed}}} _(error)_
  * [InjectScopeOrQualifierAnnotationRetention]: {{{Scoping and qualifier annotations must have runtime retention.}}} _(error)_
  * [InjectedConstructorAnnotations]: {{{Injected constructors cannot be optional nor have binding annotations}}} _(error)_
  * [JavaxInjectOnAbstractMethod]: {{{Abstract methods are not injectable with javax.inject.Inject.}}} _(error)_
  * [JavaxInjectOnFinalField]: {{{@javac.inject.Inject cannot be put on a final field.}}} _(error)_
  * [LockMethodChecker]: {{{This method does not acquire the locks specified by its @LockMethod annotation}}} _(error)_
  * [MalformedFormatString]: {{{Printf-like format string does not match its arguments}}} _(error)_
  * [MissingCasesInEnumSwitch]: {{{Enum switch statement is missing cases}}} _(error)_
  * [ModifyingCollectionWithItself]: {{{Modifying a collection with itself}}} _(error)_
  * [MoreThanOneInjectableConstructor]: {{{A class may not have more than one injectable constructor.}}} _(error)_
  * [NonRuntimeAnnotation]: {{{Calling getAnnotation on an annotation that is not retained at runtime.}}} _(error)_
  * [NumericEquality]: {{{Numeric comparison using reference equality instead of value equality}}} _(error)_
  * [OverlappingQualifierAndScopeAnnotation]: {{{Annotations cannot be both Qualifiers/BindingAnnotations and Scopes}}} _(error)_
  * [OverridesJavaxInjectableMethod]: {{{This method is not annotated with @Inject, but it overrides a  method that is  annotated with @javax.inject.Inject.}}} _(error)_
  * [ParameterPackage]: {{{Method parameter has wrong package}}} _(error)_
  * [ProtoStringFieldReferenceEquality]: {{{Comparing protobuf fields of type String using reference equality}}} _(error)_
  * [SelfEquality]: {{{Variable compared to itself}}} _(error)_
  * [UnlockMethod]: {{{This method does not acquire the locks specified by its @UnlockMethod annotation}}} _(error)_
  * [AssistedInjectAndInjectOnConstructors]: {{{@AssistedInject and @Inject should not be used on different constructors in the same class.}}} _(warning)_
  * [ChainingConstructorIgnoresParameter]: {{{The called constructor accepts a parameter with the same name and type as one of its caller's parameters, but its caller doesn't pass that parameter to it.  It's likely that it was intended to.}}} _(warning)_
  * [GuiceInjectOnFinalField]: {{{Although Guice allows injecting final fields, doing so is not recommended because the injected value may not be visible to other threads.}}} _(warning)_
  * [JUnitAmbiguousTestClass]: {{{Test class mixes JUnit 3 and JUnit 4 idioms}}} _(warning)_
  * [OrderingFrom]: {{{Ordering.from(new Comparator<T>() { }) can be refactored to cleaner form}}} _(warning)_
  * [OverridesGuiceInjectableMethod]: {{{This method is not annotated with @Inject, but it overrides a method that is annotated with @com.google.inject.Inject. Guice will inject this method,and it is recommended to annotate it explicitly.}}} _(warning)_
  * [PreconditionsErrorMessageEagerEvaluation]: {{{Second argument to Preconditions.* is a call to String.format(), which can be unwrapped}}} _(warning)_
  * [PrimitiveArrayPassedToVarargsMethod]: {{{Passing a primitive array to a varargs method is usually wrong}}} _(warning)_
  * [FallthroughSuppression]: {{{Fallthrough warning suppression has no effect if warning is suppressed}}} _(not a problem)_
