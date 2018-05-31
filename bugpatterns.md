---
title: Bug Patterns
layout: bugpatterns
---


# Bug patterns

This list is auto-generated from our sources. Each bug pattern includes code
examples of both positive and negative cases; these examples are used in our
regression test suite.

Patterns which are marked __Experimental__ will not be evaluated against your
code, unless you specifically configure Error Prone. The default checks are
marked __On by default__, and each release promotes some experimental checks
after we've vetted them against Google's codebase.

## On by default : ERROR

__[AndroidInjectionBeforeSuper](bugpattern/AndroidInjectionBeforeSuper)__<br>
AndroidInjection.inject() should always be invoked before calling super.lifecycleMethod()

__[ArrayEquals](bugpattern/ArrayEquals)__<br>
Reference equality used to compare arrays

__[ArrayFillIncompatibleType](bugpattern/ArrayFillIncompatibleType)__<br>
Arrays.fill(Object[], Object) called with incompatible types.

__[ArrayHashCode](bugpattern/ArrayHashCode)__<br>
hashcode method on array does not hash array contents

__[ArrayToString](bugpattern/ArrayToString)__<br>
Calling toString on an array does not provide useful information

__[ArraysAsListPrimitiveArray](bugpattern/ArraysAsListPrimitiveArray)__<br>
Arrays.asList does not autobox primitive arrays, as one might expect.

__[AsyncCallableReturnsNull](bugpattern/AsyncCallableReturnsNull)__<br>
AsyncCallable should not return a null Future, only a Future whose result is null.

__[AsyncFunctionReturnsNull](bugpattern/AsyncFunctionReturnsNull)__<br>
AsyncFunction should not return a null Future, only a Future whose result is null.

__[AutoValueConstructorOrderChecker](bugpattern/AutoValueConstructorOrderChecker)__<br>
Arguments to AutoValue constructor are in the wrong order

__[BadShiftAmount](bugpattern/BadShiftAmount)__<br>
Shift by an amount that is out of range

__[BundleDeserializationCast](bugpattern/BundleDeserializationCast)__<br>
Object serialized in Bundle may have been flattened to base type.

__[ChainingConstructorIgnoresParameter](bugpattern/ChainingConstructorIgnoresParameter)__<br>
The called constructor accepts a parameter with the same name and type as one of its caller&#39;s parameters, but its caller doesn&#39;t pass that parameter to it.  It&#39;s likely that it was intended to.

__[CheckReturnValue](bugpattern/CheckReturnValue)__<br>
Ignored return value of method that is annotated with @CheckReturnValue

__[CollectionIncompatibleType](bugpattern/CollectionIncompatibleType)__<br>
Incompatible type as argument to Object-accepting Java collections method

__[ComparableType](bugpattern/ComparableType)__<br>
 Implementing &#39;Comparable&lt;T&gt;&#39; where T is not compatible with the implementing class.

__[ComparisonOutOfRange](bugpattern/ComparisonOutOfRange)__<br>
Comparison to value that is out of range for the compared type

__[CompatibleWithAnnotationMisuse](bugpattern/CompatibleWithAnnotationMisuse)__<br>
@CompatibleWith&#39;s value is not a type argument.

__[CompileTimeConstant](bugpattern/CompileTimeConstant)__<br>
Non-compile-time constant expression passed to parameter with @CompileTimeConstant type annotation.

__[ComplexBooleanConstant](bugpattern/ComplexBooleanConstant)__<br>
Non-trivial compile time constant boolean expressions shouldn&#39;t be used.

__[ConditionalExpressionNumericPromotion](bugpattern/ConditionalExpressionNumericPromotion)__<br>
A conditional expression with numeric operands of differing types will perform binary numeric promotion of the operands; when these operands are of reference types, the expression&#39;s result may not be of the expected type.

__[ConstantOverflow](bugpattern/ConstantOverflow)__<br>
Compile-time constant expression overflows

__[DaggerProvidesNull](bugpattern/DaggerProvidesNull)__<br>
Dagger @Provides methods may not return null unless annotated with @Nullable

__[DeadException](bugpattern/DeadException)__<br>
Exception created but not thrown

__[DeadThread](bugpattern/DeadThread)__<br>
Thread created but not started

__[DoNotCall](bugpattern/DoNotCall)__<br>
This method should not be called.

__[EqualsNaN](bugpattern/EqualsNaN)__<br>
== NaN always returns false; use the isNaN methods instead

__[EqualsReference](bugpattern/EqualsReference)__<br>
== must be used in equals method to check equality to itself or an infinite loop will occur.

__[ForOverride](bugpattern/ForOverride)__<br>
Method annotated @ForOverride must be protected or package-private and only invoked from declaring class, or from an override of the method

__[FormatString](bugpattern/FormatString)__<br>
Invalid printf-style format string

__[FormatStringAnnotation](bugpattern/FormatStringAnnotation)__<br>
Invalid format string passed to formatting method.

__[FunctionalInterfaceMethodChanged](bugpattern/FunctionalInterfaceMethodChanged)__<br>
Casting a lambda to this @FunctionalInterface can cause a behavior change from casting to a functional superinterface, which is surprising to users.  Prefer decorator methods to this surprising behavior.

__[FuturesGetCheckedIllegalExceptionType](bugpattern/FuturesGetCheckedIllegalExceptionType)__<br>
Futures.getChecked requires a checked exception type with a standard constructor.

__[GetClassOnAnnotation](bugpattern/GetClassOnAnnotation)__<br>
Calling getClass() on an annotation may return a proxy class

__[GetClassOnClass](bugpattern/GetClassOnClass)__<br>
Calling getClass() on an object of type Class returns the Class object for java.lang.Class; you probably meant to operate on the object directly

__[GuardedBy](bugpattern/GuardedBy)__<br>
Checks for unguarded accesses to fields and methods with @GuardedBy annotations

__[GuiceAssistedInjectScoping](bugpattern/GuiceAssistedInjectScoping)__<br>
Scope annotation on implementation class of AssistedInject factory is not allowed

__[GuiceAssistedParameters](bugpattern/GuiceAssistedParameters)__<br>
A constructor cannot have two @Assisted parameters of the same type unless they are disambiguated with named @Assisted annotations.

__[GuiceInjectOnFinalField](bugpattern/GuiceInjectOnFinalField)__<br>
Although Guice allows injecting final fields, doing so is disallowed because the injected value may not be visible to other threads.

__[HashtableContains](bugpattern/HashtableContains)__<br>
contains() is a legacy method that is equivalent to containsValue()

__[IdentityBinaryExpression](bugpattern/IdentityBinaryExpression)__<br>
A binary expression where both operands are the same is usually incorrect.

__[Immutable](bugpattern/Immutable)__<br>
Type declaration annotated with @Immutable is not immutable

__[ImmutableModification](bugpattern/ImmutableModification)__<br>
Modifying an immutable collection is guaranteed to throw an exception and leave the collection unmodified

__[IncompatibleArgumentType](bugpattern/IncompatibleArgumentType)__<br>
Passing argument to a generic method with an incompatible type.

__[IndexOfChar](bugpattern/IndexOfChar)__<br>
The first argument to indexOf is a Unicode code point, and the second is the index to start the search from

__[InexactVarargsConditional](bugpattern/InexactVarargsConditional)__<br>
Conditional expression in varargs call contains array and non-array arguments

__[InfiniteRecursion](bugpattern/InfiniteRecursion)__<br>
This method always recurses, and will cause a StackOverflowError

__[InjectMoreThanOneScopeAnnotationOnClass](bugpattern/InjectMoreThanOneScopeAnnotationOnClass)__<br>
A class can be annotated with at most one scope annotation.

__[InvalidPatternSyntax](bugpattern/InvalidPatternSyntax)__<br>
Invalid syntax used for a regular expression

__[InvalidTimeZoneID](bugpattern/InvalidTimeZoneID)__<br>
Invalid time zone identifier. TimeZone.getTimeZone(String) will silently return GMT instead of the time zone you intended.

__[IsInstanceOfClass](bugpattern/IsInstanceOfClass)__<br>
The argument to Class#isInstance(Object) should not be a Class

__[IsLoggableTagLength](bugpattern/IsLoggableTagLength)__<br>
Log tag too long, cannot exceed 23 characters.

__[JUnit3TestNotRun](bugpattern/JUnit3TestNotRun)__<br>
Test method will not be run; please correct method signature (Should be public, non-static, and method name should begin with &quot;test&quot;).

__[JUnit4ClassAnnotationNonStatic](bugpattern/JUnit4ClassAnnotationNonStatic)__<br>
This method should be static

__[JUnit4SetUpNotRun](bugpattern/JUnit4SetUpNotRun)__<br>
setUp() method will not be run; please add JUnit&#39;s @Before annotation

__[JUnit4TearDownNotRun](bugpattern/JUnit4TearDownNotRun)__<br>
tearDown() method will not be run; please add JUnit&#39;s @After annotation

__[JUnit4TestNotRun](bugpattern/JUnit4TestNotRun)__<br>
This looks like a test method but is not run; please add @Test and @Ignore, or, if this is a helper method, reduce its visibility.

__[JUnitAssertSameCheck](bugpattern/JUnitAssertSameCheck)__<br>
An object is tested for reference equality to itself using JUnit library.

__[JavaxInjectOnAbstractMethod](bugpattern/JavaxInjectOnAbstractMethod)__<br>
Abstract and default methods are not injectable with javax.inject.Inject

__[LiteByteStringUtf8](bugpattern/LiteByteStringUtf8)__<br>
This pattern will silently corrupt certain byte sequences from the serialized protocol message. Use ByteString or byte[] directly

__[LoopConditionChecker](bugpattern/LoopConditionChecker)__<br>
Loop condition is never modified in loop body.

__[MislabeledAndroidString](bugpattern/MislabeledAndroidString)__<br>
Certain resources in `android.R.string` have names that do not match their content

__[MissingSuperCall](bugpattern/MissingSuperCall)__<br>
Overriding method is missing a call to overridden super method

__[MisusedWeekYear](bugpattern/MisusedWeekYear)__<br>
Use of &quot;YYYY&quot; (week year) in a date pattern without &quot;ww&quot; (week in year). You probably meant to use &quot;yyyy&quot; (year) instead.

__[MockitoCast](bugpattern/MockitoCast)__<br>
A bug in Mockito will cause this test to fail at runtime with a ClassCastException

__[MockitoUsage](bugpattern/MockitoUsage)__<br>
Missing method call for verify(mock) here

__[ModifyingCollectionWithItself](bugpattern/ModifyingCollectionWithItself)__<br>
Using a collection function with itself as the argument.

__[MoreThanOneInjectableConstructor](bugpattern/MoreThanOneInjectableConstructor)__<br>
This class has more than one @Inject-annotated constructor. Please remove the @Inject annotation from all but one of them.

__[MustBeClosedChecker](bugpattern/MustBeClosedChecker)__<br>
The result of this method must be closed.

__[NCopiesOfChar](bugpattern/NCopiesOfChar)__<br>
The first argument to nCopies is the number of copies, and the second is the item to copy

__[NonCanonicalStaticImport](bugpattern/NonCanonicalStaticImport)__<br>
Static import of type uses non-canonical name

__[NonFinalCompileTimeConstant](bugpattern/NonFinalCompileTimeConstant)__<br>
@CompileTimeConstant parameters should be final or effectively final

__[NonRuntimeAnnotation](bugpattern/NonRuntimeAnnotation)__<br>
Calling getAnnotation on an annotation that is not retained at runtime.

__[NullTernary](bugpattern/NullTernary)__<br>
This conditional expression may evaluate to null, which will result in an NPE when the result is unboxed.

__[OptionalEquality](bugpattern/OptionalEquality)__<br>
Comparison using reference equality instead of value equality

__[OverlappingQualifierAndScopeAnnotation](bugpattern/OverlappingQualifierAndScopeAnnotation)__<br>
Annotations cannot be both Scope annotations and Qualifier annotations: this causes confusion when trying to use them.

__[OverridesJavaxInjectableMethod](bugpattern/OverridesJavaxInjectableMethod)__<br>
This method is not annotated with @Inject, but it overrides a method that is  annotated with @javax.inject.Inject. The method will not be Injected.

__[PackageInfo](bugpattern/PackageInfo)__<br>
Declaring types inside package-info.java files is very bad form

__[ParcelableCreator](bugpattern/ParcelableCreator)__<br>
Detects classes which implement Parcelable but don&#39;t have CREATOR

__[PreconditionsCheckNotNull](bugpattern/PreconditionsCheckNotNull)__<br>
Literal passed as first argument to Preconditions.checkNotNull() can never be null

__[PreconditionsCheckNotNullPrimitive](bugpattern/PreconditionsCheckNotNullPrimitive)__<br>
First argument to `Preconditions.checkNotNull()` is a primitive rather than an object reference

__[PredicateIncompatibleType](bugpattern/PredicateIncompatibleType)__<br>
Using ::equals as an incompatible Predicate; the predicate will always return false

__[PrivateSecurityContractProtoAccess](bugpattern/PrivateSecurityContractProtoAccess)__<br>
Access to a private protocol buffer field is forbidden. This protocol buffer carries a security contract, and can only be created using an approved library. Direct access to the fields is forbidden.

__[ProtoFieldNullComparison](bugpattern/ProtoFieldNullComparison)__<br>
Protobuf fields cannot be null

__[ProtocolBufferOrdinal](bugpattern/ProtocolBufferOrdinal)__<br>
To get the tag number of a protocol buffer enum, use getNumber() instead.

__[ProvidesMethodOutsideOfModule](bugpattern/ProvidesMethodOutsideOfModule)__<br>
@Provides methods need to be declared in a Module to have any effect.

__[RandomCast](bugpattern/RandomCast)__<br>
Casting a random number in the range [0.0, 1.0) to an integer or long always results in 0.

__[RandomModInteger](bugpattern/RandomModInteger)__<br>
Use Random.nextInt(int).  Random.nextInt() % n can have negative results

__[RectIntersectReturnValueIgnored](bugpattern/RectIntersectReturnValueIgnored)__<br>
Return value of android.graphics.Rect.intersect() must be checked

__[RestrictedApiChecker](bugpattern/RestrictedApiChecker)__<br>
 Check for non-whitelisted callers to RestrictedApiChecker.

__[ReturnValueIgnored](bugpattern/ReturnValueIgnored)__<br>
Return value of this method must be used

__[SelfAssignment](bugpattern/SelfAssignment)__<br>
Variable assigned to itself

__[SelfComparison](bugpattern/SelfComparison)__<br>
An object is compared to itself

__[SelfEquals](bugpattern/SelfEquals)__<br>
Testing an object for equality with itself will always be true.

__[ShouldHaveEvenArgs](bugpattern/ShouldHaveEvenArgs)__<br>
This method must be called with an even number of arguments.

__[SizeGreaterThanOrEqualsZero](bugpattern/SizeGreaterThanOrEqualsZero)__<br>
Comparison of a size &gt;= 0 is always true, did you intend to check for non-emptiness?

__[StreamToString](bugpattern/StreamToString)__<br>
Calling toString on a Stream does not provide useful information

__[StringBuilderInitWithChar](bugpattern/StringBuilderInitWithChar)__<br>
StringBuilder does not have a char constructor; this invokes the int constructor.

__[SuppressWarningsDeprecated](bugpattern/SuppressWarningsDeprecated)__<br>
Suppressing &quot;deprecated&quot; is probably a typo for &quot;deprecation&quot;

__[ThrowIfUncheckedKnownChecked](bugpattern/ThrowIfUncheckedKnownChecked)__<br>
throwIfUnchecked(knownCheckedException) is a no-op.

__[ThrowNull](bugpattern/ThrowNull)__<br>
Throwing &#39;null&#39; always results in a NullPointerException being thrown.

__[TruthSelfEquals](bugpattern/TruthSelfEquals)__<br>
isEqualTo should not be used to test an object for equality with itself; the assertion will never fail.

__[TryFailThrowable](bugpattern/TryFailThrowable)__<br>
Catching Throwable/Error masks failures from fail() or assert*() in the try block

__[TypeParameterQualifier](bugpattern/TypeParameterQualifier)__<br>
Type parameter used as type qualifier

__[UnnecessaryTypeArgument](bugpattern/UnnecessaryTypeArgument)__<br>
Non-generic methods should not be invoked with type arguments

__[UnusedAnonymousClass](bugpattern/UnusedAnonymousClass)__<br>
Instance created but never used

__[UnusedCollectionModifiedInPlace](bugpattern/UnusedCollectionModifiedInPlace)__<br>
Collection is modified in place, but the result is not used

__[VarTypeName](bugpattern/VarTypeName)__<br>
`var` should not be used as a type name.

## On by default : WARNING

__[AmbiguousMethodReference](bugpattern/AmbiguousMethodReference)__<br>
Method reference is ambiguous

__[ArgumentSelectionDefectChecker](bugpattern/ArgumentSelectionDefectChecker)__<br>
Arguments are in the wrong order or could be commented for clarity.

__[AssertEqualsArgumentOrderChecker](bugpattern/AssertEqualsArgumentOrderChecker)__<br>
Arguments are swapped in assertEquals-like call

__[AssertThrowsMultipleStatements](bugpattern/AssertThrowsMultipleStatements)__<br>
The lambda passed to assertThows should contain exactly one statement

__[AssertionFailureIgnored](bugpattern/AssertionFailureIgnored)__<br>
This assertion throws an AssertionError if it fails, which will be caught by an enclosing try block.

__[BadAnnotationImplementation](bugpattern/BadAnnotationImplementation)__<br>
Classes that implement Annotation must override equals and hashCode. Consider using AutoAnnotation instead of implementing Annotation by hand.

__[BadComparable](bugpattern/BadComparable)__<br>
Possible sign flip from narrowing conversion

__[BadNestedImport](bugpattern/BadNestedImport)__<br>
Importing nested classes with commonly-used names can make code harder to read, because it may not be clear from the context exactly which type is being referred to. Qualifying the name with that of the containing class can make the code clearer.

__[BoxedPrimitiveConstructor](bugpattern/BoxedPrimitiveConstructor)__<br>
valueOf or autoboxing provides better time and space performance

__[ByteBufferBackingArray](bugpattern/ByteBufferBackingArray)__<br>
ByteBuffer.array() shouldn&#39;t be called unless ByteBuffer.arrayOffset() is used or if the ByteBuffer was initialized using ByteBuffer.wrap() or ByteBuffer.allocate().

__[CannotMockFinalClass](bugpattern/CannotMockFinalClass)__<br>
Mockito cannot mock final classes

__[CanonicalDuration](bugpattern/CanonicalDuration)__<br>
Duration can be expressed more clearly with different units

__[CatchAndPrintStackTrace](bugpattern/CatchAndPrintStackTrace)__<br>
Logging or rethrowing exceptions should usually be preferred to catching and calling printStackTrace

__[CatchFail](bugpattern/CatchFail)__<br>
Ignoring exceptions and calling fail() is unnecessary, and makes test output less useful

__[ClassCanBeStatic](bugpattern/ClassCanBeStatic)__<br>
Inner class is non-static but does not reference enclosing class

__[ClassNewInstance](bugpattern/ClassNewInstance)__<br>
Class.newInstance() bypasses exception checking; prefer getDeclaredConstructor().newInstance()

__[CloseableProvides](bugpattern/CloseableProvides)__<br>
Providing Closeable resources makes their lifecycle unclear

__[CollectionToArraySafeParameter](bugpattern/CollectionToArraySafeParameter)__<br>
The type of the array parameter of Collection.toArray needs to be compatible with the array type

__[CollectorShouldNotUseState](bugpattern/CollectorShouldNotUseState)__<br>
Collector.of() should not use state

__[ComparableAndComparator](bugpattern/ComparableAndComparator)__<br>
Class should not implement both `Comparable` and `Comparator`

__[DateFormatConstant](bugpattern/DateFormatConstant)__<br>
DateFormat is not thread-safe, and should not be used as a constant field.

__[DefaultCharset](bugpattern/DefaultCharset)__<br>
Implicit use of the platform default charset, which can result in differing behaviour between JVM executions or incorrect behavior if the encoding of the data source doesn&#39;t match expectations.

__[DoubleBraceInitialization](bugpattern/DoubleBraceInitialization)__<br>
Prefer collection factory methods or builders to the double-brace initialization pattern.

__[DoubleCheckedLocking](bugpattern/DoubleCheckedLocking)__<br>
Double-checked locking on non-volatile fields is unsafe

__[EqualsHashCode](bugpattern/EqualsHashCode)__<br>
Classes that override equals should also override hashCode.

__[EqualsIncompatibleType](bugpattern/EqualsIncompatibleType)__<br>
An equality test between objects with incompatible types always returns false

__[FallThrough](bugpattern/FallThrough)__<br>
Switch case may fall through

__[Finally](bugpattern/Finally)__<br>
If you return or throw from a finally, then values returned or thrown from the try-catch block will be ignored. Consider using try-with-resources instead.

__[FloatCast](bugpattern/FloatCast)__<br>
Use parentheses to make the precedence explicit

__[FloatingPointAssertionWithinEpsilon](bugpattern/FloatingPointAssertionWithinEpsilon)__<br>
This fuzzy equality check is using a tolerance less than the gap to the next number. You may want a less restrictive tolerance, or to assert equality.

__[FloatingPointLiteralPrecision](bugpattern/FloatingPointLiteralPrecision)__<br>
Floating point literal loses precision

__[FragmentInjection](bugpattern/FragmentInjection)__<br>
Classes extending PreferenceActivity must implement isValidFragment such that it does not unconditionally return true to prevent vulnerability to fragment injection attacks.

__[FragmentNotInstantiable](bugpattern/FragmentNotInstantiable)__<br>
Subclasses of Fragment must be instantiable via Class#newInstance(): the class must be public, static and have a public nullary constructor

__[FutureReturnValueIgnored](bugpattern/FutureReturnValueIgnored)__<br>
Return value of methods returning Future must be checked. Ignoring returned Futures suppresses exceptions thrown from the code that completes the Future.

__[GetClassOnEnum](bugpattern/GetClassOnEnum)__<br>
Calling getClass() on an enum may return a subclass of the enum type

__[HidingField](bugpattern/HidingField)__<br>
Hiding fields of superclasses may cause confusion and errors

__[ImmutableAnnotationChecker](bugpattern/ImmutableAnnotationChecker)__<br>
Annotations should always be immutable

__[ImmutableEnumChecker](bugpattern/ImmutableEnumChecker)__<br>
Enums should always be immutable

__[IncompatibleModifiers](bugpattern/IncompatibleModifiers)__<br>
This annotation has incompatible modifiers as specified by its @IncompatibleModifiers annotation

__[InconsistentCapitalization](bugpattern/InconsistentCapitalization)__<br>
It is confusing to have a field and a parameter under the same scope that differ only in capitalization.

__[IncrementInForLoopAndHeader](bugpattern/IncrementInForLoopAndHeader)__<br>
This for loop increments the same variable in the header and in the body

__[InjectOnConstructorOfAbstractClass](bugpattern/InjectOnConstructorOfAbstractClass)__<br>
Constructors on abstract classes are never directly @Injected, only the constructors of their subclasses can be @Inject&#39;ed.

__[InputStreamSlowMultibyteRead](bugpattern/InputStreamSlowMultibyteRead)__<br>
Please also override int read(byte[], int, int), otherwise multi-byte reads from this input stream are likely to be slow.

__[InstanceOfAndCastMatchWrongType](bugpattern/InstanceOfAndCastMatchWrongType)__<br>
Casting inside an if block should be plausibly consistent with the instanceof type

__[IntLongMath](bugpattern/IntLongMath)__<br>
Expression of type int may overflow before being assigned to a long

__[IterableAndIterator](bugpattern/IterableAndIterator)__<br>
Class should not implement both `Iterable` and `Iterator`

__[JUnit3FloatingPointComparisonWithoutDelta](bugpattern/JUnit3FloatingPointComparisonWithoutDelta)__<br>
Floating-point comparison without error tolerance

__[JUnit4ClassUsedInJUnit3](bugpattern/JUnit4ClassUsedInJUnit3)__<br>
Some JUnit4 construct cannot be used in a JUnit3 context. Convert your class to JUnit4 style to use them.

__[JUnitAmbiguousTestClass](bugpattern/JUnitAmbiguousTestClass)__<br>
Test class inherits from JUnit 3&#39;s TestCase but has JUnit 4 @Test annotations.

__[JavaLangClash](bugpattern/JavaLangClash)__<br>
Never reuse class names from java.lang

__[JdkObsolete](bugpattern/JdkObsolete)__<br>
Suggests alternatives to obsolete JDK classes.

__[LogicalAssignment](bugpattern/LogicalAssignment)__<br>
Assignment where a boolean expression was expected; use == if this assignment wasn&#39;t expected or add parentheses for clarity.

__[MissingCasesInEnumSwitch](bugpattern/MissingCasesInEnumSwitch)__<br>
Switches on enum types should either handle all values, or have a default case.

__[MissingFail](bugpattern/MissingFail)__<br>
Not calling fail() when expecting an exception masks bugs

__[MissingOverride](bugpattern/MissingOverride)__<br>
method overrides method in supertype; expected @Override

__[ModifyCollectionInEnhancedForLoop](bugpattern/ModifyCollectionInEnhancedForLoop)__<br>
Modifying a collection while iterating over it in a loop may cause a ConcurrentModificationException to be thrown.

__[MultipleParallelOrSequentialCalls](bugpattern/MultipleParallelOrSequentialCalls)__<br>
Multiple calls to either parallel or sequential are unnecessary and cause confusion.

__[MutableConstantField](bugpattern/MutableConstantField)__<br>
Constant field declarations should use the immutable type (such as ImmutableList) instead of the general collection interface type (such as List)

__[NarrowingCompoundAssignment](bugpattern/NarrowingCompoundAssignment)__<br>
Compound assignments may hide dangerous casts

__[NestedInstanceOfConditions](bugpattern/NestedInstanceOfConditions)__<br>
Nested instanceOf conditions of disjoint types create blocks of code that never execute

__[NonAtomicVolatileUpdate](bugpattern/NonAtomicVolatileUpdate)__<br>
This update of a volatile variable is non-atomic

__[NonOverridingEquals](bugpattern/NonOverridingEquals)__<br>
equals method doesn&#39;t override Object.equals

__[NullableConstructor](bugpattern/NullableConstructor)__<br>
Constructors should not be annotated with @Nullable since they cannot return null

__[NullablePrimitive](bugpattern/NullablePrimitive)__<br>
@Nullable should not be used for primitive types since they cannot be null

__[NullableVoid](bugpattern/NullableVoid)__<br>
void-returning methods should not be annotated with @Nullable, since they cannot return null

__[ObjectToString](bugpattern/ObjectToString)__<br>
Calling toString on Objects that don&#39;t override toString() doesn&#39;t provide useful information

__[OperatorPrecedence](bugpattern/OperatorPrecedence)__<br>
Use grouping parenthesis to make the operator precedence explicit

__[OptionalNotPresent](bugpattern/OptionalNotPresent)__<br>
One should not call optional.get() inside an if statement that checks !optional.isPresent

__[OrphanedFormatString](bugpattern/OrphanedFormatString)__<br>
String literal contains format specifiers, but is not passed to a format method

__[OverrideThrowableToString](bugpattern/OverrideThrowableToString)__<br>
To return a custom message with a Throwable class, one should override getMessage() instead of toString() for Throwable.

__[Overrides](bugpattern/Overrides)__<br>
Varargs doesn&#39;t agree for overridden method

__[OverridesGuiceInjectableMethod](bugpattern/OverridesGuiceInjectableMethod)__<br>
This method is not annotated with @Inject, but it overrides a method that is annotated with @com.google.inject.Inject. Guice will inject this method, and it is recommended to annotate it explicitly.

__[ParameterName](bugpattern/ParameterName)__<br>
Detects `/* name= */`-style comments on actual parameters where the name doesn&#39;t match the formal parameter

__[PreconditionsInvalidPlaceholder](bugpattern/PreconditionsInvalidPlaceholder)__<br>
Preconditions only accepts the %s placeholder in error message strings

__[ProtoFieldPreconditionsCheckNotNull](bugpattern/ProtoFieldPreconditionsCheckNotNull)__<br>
Protobuf fields cannot be null, so this check is redundant

__[ProtoRedundantSet](bugpattern/ProtoRedundantSet)__<br>
A field on a protocol buffer was set twice in the same chained expression.

__[QualifierOrScopeOnInjectMethod](bugpattern/QualifierOrScopeOnInjectMethod)__<br>
Qualifiers/Scope annotations on @Inject methods don&#39;t have any effect. Move the qualifier annotation to the binding location.

__[ReachabilityFenceUsage](bugpattern/ReachabilityFenceUsage)__<br>
reachabilityFence should always be called inside a finally block

__[ReferenceEquality](bugpattern/ReferenceEquality)__<br>
Comparison using reference equality instead of value equality

__[RequiredModifiers](bugpattern/RequiredModifiers)__<br>
This annotation is missing required modifiers as specified by its @RequiredModifiers annotation

__[ShortCircuitBoolean](bugpattern/ShortCircuitBoolean)__<br>
Prefer the short-circuiting boolean operators &amp;&amp; and || to &amp; and |.

__[StaticGuardedByInstance](bugpattern/StaticGuardedByInstance)__<br>
Writes to static fields should not be guarded by instance locks

__[StreamResourceLeak](bugpattern/StreamResourceLeak)__<br>
Streams that encapsulate a closeable resource should be closed using try-with-resources

__[StringSplitter](bugpattern/StringSplitter)__<br>
String.split(String) has surprising behavior

__[SwigMemoryLeak](bugpattern/SwigMemoryLeak)__<br>
SWIG generated code that can&#39;t call a C++ destructor will leak memory

__[SynchronizeOnNonFinalField](bugpattern/SynchronizeOnNonFinalField)__<br>
Synchronizing on non-final fields is not safe: if the field is ever updated, different threads may end up locking on different objects.

__[ThreadJoinLoop](bugpattern/ThreadJoinLoop)__<br>
Thread.join needs to be surrounded by a loop until it succeeds, as in Uninterruptibles.joinUninterruptibly.

__[ThreadLocalUsage](bugpattern/ThreadLocalUsage)__<br>
ThreadLocals should be stored in static fields

__[ThreeLetterTimeZoneID](bugpattern/ThreeLetterTimeZoneID)__<br>
Three-letter time zone identifiers are deprecated, may be ambiguous, and might not do what you intend; the full IANA time zone ID should be used instead.

__[TruthAssertExpected](bugpattern/TruthAssertExpected)__<br>
The actual and expected values appear to be swapped, which results in poor assertion failure messages.

__[TruthConstantAsserts](bugpattern/TruthConstantAsserts)__<br>
Truth Library assert is called on a constant.

__[TruthIncompatibleType](bugpattern/TruthIncompatibleType)__<br>
Argument is not compatible with the subject&#39;s type.

__[TypeParameterShadowing](bugpattern/TypeParameterShadowing)__<br>
Type parameter declaration overrides another type parameter already declared

__[TypeParameterUnusedInFormals](bugpattern/TypeParameterUnusedInFormals)__<br>
Declaring a type parameter that is only used in the return type is a misuse of generics: operations on the type parameter are unchecked, it hides unsafe casts at invocations of the method, and it interacts badly with method overload resolution.

__[URLEqualsHashCode](bugpattern/URLEqualsHashCode)__<br>
Avoid hash-based containers of java.net.URL--the containers rely on equals() and hashCode(), which cause java.net.URL to make blocking internet connections.

__[UnsafeFinalization](bugpattern/UnsafeFinalization)__<br>
Finalizer may run before native code finishes execution

__[UnsynchronizedOverridesSynchronized](bugpattern/UnsynchronizedOverridesSynchronized)__<br>
Unsynchronized method overrides a synchronized method.

__[UseCorrectAssertInTests](bugpattern/UseCorrectAssertInTests)__<br>
Java assert is used in test. For testing purposes Assert.* matchers should be used.

__[WaitNotInLoop](bugpattern/WaitNotInLoop)__<br>
Because of spurious wakeups, Object.wait() and Condition.await() must always be called in a loop

__[WakelockReleasedDangerously](bugpattern/WakelockReleasedDangerously)__<br>
A wakelock acquired with a timeout may be released by the system before calling `release`, even after checking `isHeld()`. If so, it will throw a RuntimeException. Please wrap in a try/catch block.

## Experimental : ERROR

__[AndroidJdkLibsChecker](bugpattern/AndroidJdkLibsChecker)__<br>
Use of class, field, or method that is not compatible with legacy Android devices

__[AssistedInjectAndInjectOnSameConstructor](bugpattern/AssistedInjectAndInjectOnSameConstructor)__<br>
@AssistedInject and @Inject cannot be used on the same constructor.

__[AutoFactoryAtInject](bugpattern/AutoFactoryAtInject)__<br>
@AutoFactory and @Inject should not be used in the same type.

__[ClassName](bugpattern/ClassName)__<br>
The source file name should match the name of the top-level class it contains

__[ComparisonContractViolated](bugpattern/ComparisonContractViolated)__<br>
This comparison method violates the contract

__[DepAnn](bugpattern/DepAnn)__<br>
Deprecated item is not annotated with @Deprecated

__[DivZero](bugpattern/DivZero)__<br>
Division by integer literal zero

__[EmptyIf](bugpattern/EmptyIf)__<br>
Empty statement after if

__[FuzzyEqualsShouldNotBeUsedInEqualsMethod](bugpattern/FuzzyEqualsShouldNotBeUsedInEqualsMethod)__<br>
DoubleMath.fuzzyEquals should never be used in an Object.equals() method

__[InjectInvalidTargetingOnScopingAnnotation](bugpattern/InjectInvalidTargetingOnScopingAnnotation)__<br>
A scoping annotation&#39;s Target should include TYPE and METHOD.

__[InjectMoreThanOneQualifier](bugpattern/InjectMoreThanOneQualifier)__<br>
Using more than one qualifier annotation on the same element is not allowed.

__[InjectScopeAnnotationOnInterfaceOrAbstractClass](bugpattern/InjectScopeAnnotationOnInterfaceOrAbstractClass)__<br>
Scope annotation on an interface or abstact class is not allowed

__[InjectScopeOrQualifierAnnotationRetention](bugpattern/InjectScopeOrQualifierAnnotationRetention)__<br>
Scoping and qualifier annotations must have runtime retention.

__[InjectedConstructorAnnotations](bugpattern/InjectedConstructorAnnotations)__<br>
Injected constructors cannot be optional nor have binding annotations

__[InsecureCryptoUsage](bugpattern/InsecureCryptoUsage)__<br>
A standard cryptographic operation is used in a mode that is prone to vulnerabilities

__[IterablePathParameter](bugpattern/IterablePathParameter)__<br>
Path implements Iterable&lt;Path&gt;; prefer Collection&lt;Path&gt; for clarity

__[JMockTestWithoutRunWithOrRuleAnnotation](bugpattern/JMockTestWithoutRunWithOrRuleAnnotation)__<br>
jMock tests must have a @RunWith(JMock.class) annotation, or the Mockery field must have a @Rule JUnit annotation

__[Java7ApiChecker](bugpattern/Java7ApiChecker)__<br>
Use of class, field, or method that is not compatible with JDK 7

__[JavaxInjectOnFinalField](bugpattern/JavaxInjectOnFinalField)__<br>
@javax.inject.Inject cannot be put on a final field.

__[LockMethodChecker](bugpattern/LockMethodChecker)__<br>
This method does not acquire the locks specified by its @LockMethod annotation

__[LongLiteralLowerCaseSuffix](bugpattern/LongLiteralLowerCaseSuffix)__<br>
Prefer &#39;L&#39; to &#39;l&#39; for the suffix to long literals

__[NoAllocation](bugpattern/NoAllocation)__<br>
@NoAllocation was specified on this method, but something was found that would trigger an allocation

__[NumericEquality](bugpattern/NumericEquality)__<br>
Numeric comparison using reference equality instead of value equality

__[ParameterPackage](bugpattern/ParameterPackage)__<br>
Method parameter has wrong package

__[ProtoStringFieldReferenceEquality](bugpattern/ProtoStringFieldReferenceEquality)__<br>
Comparing protobuf fields of type String using reference equality

__[RestrictTo](bugpattern/RestrictTo)__<br>
Use of method or class annotated with @RestrictTo

__[StaticOrDefaultInterfaceMethod](bugpattern/StaticOrDefaultInterfaceMethod)__<br>
Static and default interface methods are not natively supported on older Android devices. 

__[UnlockMethod](bugpattern/UnlockMethod)__<br>
This method does not acquire the locks specified by its @UnlockMethod annotation

## Experimental : WARNING

__[AnnotateFormatMethod](bugpattern/AnnotateFormatMethod)__<br>
This method passes a pair of parameters through to String.format, but the enclosing method wasn&#39;t annotated @FormatMethod. Doing so gives compile-time rather than run-time protection against malformed format strings.

__[AssertFalse](bugpattern/AssertFalse)__<br>
Assertions may be disabled at runtime and do not guarantee that execution will halt here; consider throwing an exception instead

__[AssistedInjectAndInjectOnConstructors](bugpattern/AssistedInjectAndInjectOnConstructors)__<br>
@AssistedInject and @Inject should not be used on different constructors in the same class.

__[BigDecimalLiteralDouble](bugpattern/BigDecimalLiteralDouble)__<br>
BigDecimal(double) and BigDecimal.valueOf(double) may lose precision, prefer BigDecimal(String) or BigDecimal(long)

__[BinderIdentityRestoredDangerously](bugpattern/BinderIdentityRestoredDangerously)__<br>
A call to Binder.clearCallingIdentity() should be followed by Binder.restoreCallingIdentity() in a finally block. Otherwise the wrong Binder identity may be used by subsequent code.

__[BindingToUnqualifiedCommonType](bugpattern/BindingToUnqualifiedCommonType)__<br>
This code declares a binding for a common value type without a Qualifier annotation.

__[ConstructorInvokesOverridable](bugpattern/ConstructorInvokesOverridable)__<br>
Constructors should not invoke overridable methods.

__[ConstructorLeaksThis](bugpattern/ConstructorLeaksThis)__<br>
Constructors should not pass the &#39;this&#39; reference out in method invocations, since the object may not be fully constructed.

__[EmptyTopLevelDeclaration](bugpattern/EmptyTopLevelDeclaration)__<br>
Empty top-level type declaration

__[EqualsBrokenForNull](bugpattern/EqualsBrokenForNull)__<br>
equals() implementation throws NullPointerException when given null

__[ExpectedExceptionChecker](bugpattern/ExpectedExceptionChecker)__<br>
Calls to ExpectedException#expect should always be followed by exactly one statement.

__[FunctionalInterfaceClash](bugpattern/FunctionalInterfaceClash)__<br>
Overloads will be ambiguous when passing lambda arguments

__[HardCodedSdCardPath](bugpattern/HardCodedSdCardPath)__<br>
Hardcoded reference to /sdcard

__[InconsistentOverloads](bugpattern/InconsistentOverloads)__<br>
The ordering of parameters in overloaded methods should be as consistent as possible (when viewed from left to right)

__[MissingDefault](bugpattern/MissingDefault)__<br>
The Google Java Style Guide requires that each switch statement includes a default statement group, even if it contains no code. (This requirement is lifted for any switch statement that covers all values of an enum.)

__[MutableMethodReturnType](bugpattern/MutableMethodReturnType)__<br>
Method return type should use the immutable type (such as ImmutableList) instead of the general collection interface type (such as List)

__[NonCanonicalStaticMemberImport](bugpattern/NonCanonicalStaticMemberImport)__<br>
Static import of member uses non-canonical name

__[PrimitiveArrayPassedToVarargsMethod](bugpattern/PrimitiveArrayPassedToVarargsMethod)__<br>
Passing a primitive array to a varargs method is usually wrong

__[ProvidesFix](bugpattern/ProvidesFix)__<br>
BugChecker has incorrect ProvidesFix tag, please update

__[QualifierWithTypeUse](bugpattern/QualifierWithTypeUse)__<br>
Injection frameworks currently don&#39;t understand Qualifiers in TYPE_PARAMETER or TYPE_USE contexts.

__[RedundantThrows](bugpattern/RedundantThrows)__<br>
Thrown exception is a subtype of another

__[StaticQualifiedUsingExpression](bugpattern/StaticQualifiedUsingExpression)__<br>
A static variable or method should be qualified with a class name, not expression

__[StringEquality](bugpattern/StringEquality)__<br>
String comparison using reference equality instead of value equality

__[TestExceptionChecker](bugpattern/TestExceptionChecker)__<br>
Using @Test(expected=...) is discouraged, since the test will pass if *any* statement in the test method throws the expected exception

__[UnnecessaryDefaultInEnumSwitch](bugpattern/UnnecessaryDefaultInEnumSwitch)__<br>
Switch handles all enum values; an explicit default case is unnecessary and defeats error checking for non-exhaustive switches.

__[UnusedException](bugpattern/UnusedException)__<br>
This catch block catches an symbol and re-throws another, but swallows the caught symbol rather than setting it as a cause. This can make debugging harder.

__[Var](bugpattern/Var)__<br>
Non-constant variable missing @Var annotation

## Experimental : SUGGESTION

__[BooleanParameter](bugpattern/BooleanParameter)__<br>
Use parameter comments to document ambiguous literals

__[ClassNamedLikeTypeParameter](bugpattern/ClassNamedLikeTypeParameter)__<br>
This class&#39;s name looks like a Type Parameter.

__[ConstantField](bugpattern/ConstantField)__<br>
Field name is CONSTANT_CASE, but field is not static and final

__[EmptySetMultibindingContributions](bugpattern/EmptySetMultibindingContributions)__<br>
@Multibinds is a more efficient and declarative mechanism for ensuring that a set multibinding is present in the graph.

__[FieldCanBeFinal](bugpattern/FieldCanBeFinal)__<br>
This field is only assigned during initialization; consider making it final

__[FieldMissingNullable](bugpattern/FieldMissingNullable)__<br>
Fields that can be null should be annotated @Nullable

__[ImmutableRefactoring](bugpattern/ImmutableRefactoring)__<br>
Refactors uses of the JSR 305 @Immutable to Error Prone&#39;s annotation

__[LambdaFunctionalInterface](bugpattern/LambdaFunctionalInterface)__<br>
Use Java&#39;s utility functional interfaces instead of Function&lt;A, B&gt; for primitive types.

__[MethodCanBeStatic](bugpattern/MethodCanBeStatic)__<br>
A private method that does not reference the enclosing instance can be static

__[MixedArrayDimensions](bugpattern/MixedArrayDimensions)__<br>
C-style array declarations should not be used

__[MultiVariableDeclaration](bugpattern/MultiVariableDeclaration)__<br>
Variable declarations should declare only one variable

__[MultipleTopLevelClasses](bugpattern/MultipleTopLevelClasses)__<br>
Source files should not contain multiple top-level class declarations

__[MultipleUnaryOperatorsInMethodCall](bugpattern/MultipleUnaryOperatorsInMethodCall)__<br>
Avoid having multiple unary operators acting on the same variable in a method call

__[PackageLocation](bugpattern/PackageLocation)__<br>
Package names should match the directory they are declared in

__[ParameterComment](bugpattern/ParameterComment)__<br>
Non-standard parameter comment; prefer `/*paramName=*/ arg`

__[ParameterNotNullable](bugpattern/ParameterNotNullable)__<br>
Method parameters that aren&#39;t checked for null shouldn&#39;t be annotated @Nullable

__[PrivateConstructorForNoninstantiableModule](bugpattern/PrivateConstructorForNoninstantiableModule)__<br>
Add a private constructor to modules that will not be instantiated by Dagger.

__[PrivateConstructorForUtilityClass](bugpattern/PrivateConstructorForUtilityClass)__<br>
Utility classes (only static members) are not designed to be instantiated and should be made noninstantiable with a default constructor.

__[RemoveUnusedImports](bugpattern/RemoveUnusedImports)__<br>
Unused imports

__[ReturnMissingNullable](bugpattern/ReturnMissingNullable)__<br>
Methods that can return null should be annotated @Nullable

__[ScopeOnModule](bugpattern/ScopeOnModule)__<br>
Scopes on modules have no function and will soon be an error.

__[SwitchDefault](bugpattern/SwitchDefault)__<br>
The default case of a switch should appear at the end of the last statement group

__[ThrowsUncheckedException](bugpattern/ThrowsUncheckedException)__<br>
Unchecked exceptions do not need to be declared in the method signature.

__[TypeParameterNaming](bugpattern/TypeParameterNaming)__<br>
Type parameters must be a single letter with an optional numeric suffix, or an UpperCamelCase name followed by the letter &#39;T&#39;.

__[UngroupedOverloads](bugpattern/UngroupedOverloads)__<br>
Constructors and methods with the same name should appear sequentially with no other code in between. Please re-order or re-name methods.

__[UnnecessarySetDefault](bugpattern/UnnecessarySetDefault)__<br>
Unnecessary call to NullPointerTester#setDefault

__[UnnecessaryStaticImport](bugpattern/UnnecessaryStaticImport)__<br>
Using static imports for types is unnecessary

__[UseBinds](bugpattern/UseBinds)__<br>
@Binds is a more efficient and declarative mechanism for delegating a binding.

__[WildcardImport](bugpattern/WildcardImport)__<br>
Wildcard imports, static or otherwise, should not be used

