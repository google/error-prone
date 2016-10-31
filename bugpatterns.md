---
title: Bug Patterns
layout: master
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

__[ArrayEquals](bugpattern/ArrayEquals)__<br>
Reference equality used to compare arrays

__[ArrayHashCode](bugpattern/ArrayHashCode)__<br>
hashcode method on array does not hash array contents

__[ArrayToString](bugpattern/ArrayToString)__<br>
Calling toString on an array does not provide useful information

__[ArraysAsListPrimitiveArray](bugpattern/ArraysAsListPrimitiveArray)__<br>
Arrays.asList does not autobox primitive arrays, as one might expect.

__[AsyncFunctionReturnsNull](bugpattern/AsyncFunctionReturnsNull)__<br>
AsyncFunction should not return a null Future, only a Future whose result is null.

__[BadShiftAmount](bugpattern/BadShiftAmount)__<br>
Shift by an amount that is out of range

__[ChainingConstructorIgnoresParameter](bugpattern/ChainingConstructorIgnoresParameter)__<br>
The called constructor accepts a parameter with the same name and type as one of its caller&#39;s parameters, but its caller doesn&#39;t pass that parameter to it.  It&#39;s likely that it was intended to.

__[CheckReturnValue](bugpattern/CheckReturnValue)__<br>
Ignored return value of method that is annotated with @CheckReturnValue

__[ComparisonOutOfRange](bugpattern/ComparisonOutOfRange)__<br>
Comparison to value that is out of range for the compared type

__[CompileTimeConstant](bugpattern/CompileTimeConstant)__<br>
Non-compile-time constant expression passed to parameter with @CompileTimeConstant type annotation.

__[ConstantOverflow](bugpattern/ConstantOverflow)__<br>
Compile-time constant expression overflows

__[DaggerProvidesNull](bugpattern/DaggerProvidesNull)__<br>
Dagger @Provides methods may not return null unless annotated with @Nullable

__[DeadException](bugpattern/DeadException)__<br>
Exception created but not thrown

__[EqualsNaN](bugpattern/EqualsNaN)__<br>
== NaN always returns false; use the isNaN methods instead

__[ForOverride](bugpattern/ForOverride)__<br>
Method annotated @ForOverride must be protected or package-private and only invoked from declaring class

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

__[GuardedByChecker](bugpattern/GuardedByChecker)__<br>
Checks for unguarded accesses to fields and methods with @GuardedBy annotations

__[GuardedByValidator](bugpattern/GuardedByValidator)__<br>
Invalid @GuardedBy expression

__[GuavaSelfEquals](bugpattern/GuavaSelfEquals)__<br>
An object is tested for equality to itself using Guava Libraries

__[GuiceAssistedInjectScoping](bugpattern/GuiceAssistedInjectScoping)__<br>
Scope annotation on implementation class of AssistedInject factory is not allowed

__[GuiceAssistedParameters](bugpattern/GuiceAssistedParameters)__<br>
A constructor cannot have two @Assisted parameters of the same type unless they are disambiguated with named @Assisted annotations.

__[GuiceInjectOnFinalField](bugpattern/GuiceInjectOnFinalField)__<br>
Although Guice allows injecting final fields, doing so is disallowed because the injected value may not be visible to other threads.

__[HashtableContains](bugpattern/HashtableContains)__<br>
contains() is a legacy method that is equivalent to containsValue()

__[IdentityBinaryExpression](bugpattern/IdentityBinaryExpression)__<br>
Writing &quot;a &amp;&amp; a&quot;, &quot;a || a&quot;, &quot;a &amp; a&quot;, or &quot;a | a&quot; is equivalent to &quot;a&quot;.

__[Immutable](bugpattern/Immutable)__<br>
Type declaration annotated with @Immutable is not immutable

__[InfiniteRecursion](bugpattern/InfiniteRecursion)__<br>
This method always recurses, and will cause a StackOverflowError

__[InjectMoreThanOneScopeAnnotationOnClass](bugpattern/InjectMoreThanOneScopeAnnotationOnClass)__<br>
A class can be annotated with at most one scope annotation.

__[InsecureCryptoUsage](bugpattern/InsecureCryptoUsage)__<br>
A standard cryptographic operation is used in a mode that is prone to vulnerabilities

__[InvalidPatternSyntax](bugpattern/InvalidPatternSyntax)__<br>
Invalid syntax used for a regular expression

__[IsInstanceOfClass](bugpattern/IsInstanceOfClass)__<br>
The argument to Class#isInstance(Object) should not be a Class

__[JUnit3TestNotRun](bugpattern/JUnit3TestNotRun)__<br>
Test method will not be run; please prefix name with &quot;test&quot;

__[JUnit4SetUpNotRun](bugpattern/JUnit4SetUpNotRun)__<br>
setUp() method will not be run; Please add a @Before annotation

__[JUnit4TearDownNotRun](bugpattern/JUnit4TearDownNotRun)__<br>
tearDown() method will not be run; Please add an @After annotation

__[JUnit4TestNotRun](bugpattern/JUnit4TestNotRun)__<br>
Test method will not be run; please add @Test annotation

__[JavaxInjectOnAbstractMethod](bugpattern/JavaxInjectOnAbstractMethod)__<br>
Abstract and default methods are not injectable with javax.inject.Inject

__[MislabeledAndroidString](bugpattern/MislabeledAndroidString)__<br>
Certain resources in `android.R.string` have names that do not match their content

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

__[NonCanonicalStaticImport](bugpattern/NonCanonicalStaticImport)__<br>
Static import of type uses non-canonical name

__[NonFinalCompileTimeConstant](bugpattern/NonFinalCompileTimeConstant)__<br>
@CompileTimeConstant parameters should be final or effectively final

__[OptionalEquality](bugpattern/OptionalEquality)__<br>
Comparison using reference equality instead of value equality

__[OverlappingQualifierAndScopeAnnotation](bugpattern/OverlappingQualifierAndScopeAnnotation)__<br>
Annotations cannot be both Scope annotations and Qualifier annotations: this causes confusion when trying to use them.

__[Overrides](bugpattern/Overrides)__<br>
Varargs doesn&#39;t agree for overridden method

__[OverridesJavaxInjectableMethod](bugpattern/OverridesJavaxInjectableMethod)__<br>
This method is not annotated with @Inject, but it overrides a method that is  annotated with @javax.inject.Inject. The method will not be Injected.

__[PackageInfo](bugpattern/PackageInfo)__<br>
Declaring types inside package-info.java files is very bad form

__[PreconditionsCheckNotNull](bugpattern/PreconditionsCheckNotNull)__<br>
Literal passed as first argument to Preconditions.checkNotNull() can never be null

__[PreconditionsCheckNotNullPrimitive](bugpattern/PreconditionsCheckNotNullPrimitive)__<br>
First argument to `Preconditions.checkNotNull()` is a primitive rather than an object reference

__[ProtoFieldNullComparison](bugpattern/ProtoFieldNullComparison)__<br>
Protobuf fields cannot be null

__[ProvidesMethodOutsideOfModule](bugpattern/ProvidesMethodOutsideOfModule)__<br>
@Provides methods need to be declared in a Module to have any effect.

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

__[SelfEquality](bugpattern/SelfEquality)__<br>
Variable compared to itself

__[SelfEquals](bugpattern/SelfEquals)__<br>
An object is tested for equality to itself

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

__[TryFailThrowable](bugpattern/TryFailThrowable)__<br>
Catching Throwable/Error masks failures from fail() or assert*() in the try block

__[TypeParameterQualifier](bugpattern/TypeParameterQualifier)__<br>
Type parameter used as type qualifier

__[UnnecessaryTypeArgument](bugpattern/UnnecessaryTypeArgument)__<br>
Non-generic methods should not be invoked with type arguments

__[UnusedAnonymousClass](bugpattern/UnusedAnonymousClass)__<br>
Instance created but never used

## On by default : WARNING

__[AmbiguousMethodReference](bugpattern/AmbiguousMethodReference)__<br>
Method reference is ambiguous

__[BadAnnotationImplementation](bugpattern/BadAnnotationImplementation)__<br>
Classes that implement Annotation must override equals and hashCode. Consider using AutoAnnotation instead of implementing Annotation by hand.

__[BadComparable](bugpattern/BadComparable)__<br>
Possible sign flip from narrowing conversion

__[BoxedPrimitiveConstructor](bugpattern/BoxedPrimitiveConstructor)__<br>
valueOf or autoboxing provides better time and space performance

__[CannotMockFinalClass](bugpattern/CannotMockFinalClass)__<br>
Mockito cannot mock final classes

__[ClassCanBeStatic](bugpattern/ClassCanBeStatic)__<br>
Inner class is non-static but does not reference enclosing class

__[ClassNewInstance](bugpattern/ClassNewInstance)__<br>
Class.newInstance() bypasses exception checking; prefer getConstructor().newInstance()

__[CollectionIncompatibleType](bugpattern/CollectionIncompatibleType)__<br>
Incompatible type as argument to Object-accepting Java collections method

__[DefaultCharset](bugpattern/DefaultCharset)__<br>
Implicit use of the platform default charset, which can result in e.g. non-ASCII characters being silently replaced with &#39;?&#39; in many environments

__[DoubleCheckedLocking](bugpattern/DoubleCheckedLocking)__<br>
Double-checked locking on non-volatile fields is unsafe

__[ElementsCountedInLoop](bugpattern/ElementsCountedInLoop)__<br>
This code, which counts elements using a loop, can be replaced by a simpler library method

__[EqualsHashCode](bugpattern/EqualsHashCode)__<br>
Classes that override equals should also override hashCode.

__[EqualsIncompatibleType](bugpattern/EqualsIncompatibleType)__<br>
An equality test between objects with incompatible types always returns false

__[Finally](bugpattern/Finally)__<br>
If you return or throw from a finally, then values returned or thrown from the try-catch block will be ignored. Consider using try-with-resources instead.

__[FragmentNotInstantiable](bugpattern/FragmentNotInstantiable)__<br>
Subclasses of Fragment must be instantiable via Class#newInstance(): the class must be public, static and have a public nullary constructor

__[FunctionalInterfaceClash](bugpattern/FunctionalInterfaceClash)__<br>
Overloads will be ambiguous when passing lambda arguments

__[GetClassOnEnum](bugpattern/GetClassOnEnum)__<br>
Calling getClass() on an enum may return a subclass of the enum type

__[IncompatibleModifiers](bugpattern/IncompatibleModifiers)__<br>
This annotation has incompatible modifiers as specified by its @IncompatibleModifiers annotation

__[InjectOnConstructorOfAbstractClass](bugpattern/InjectOnConstructorOfAbstractClass)__<br>
Constructors on abstract classes are never directly @Injected, only the constructors of their subclasses can be @Inject&#39;ed.

__[InputStreamSlowMultibyteRead](bugpattern/InputStreamSlowMultibyteRead)__<br>
Please also override int read(byte[], int, int), otherwise multi-byte reads from this input stream are likely to be slow.

__[IterableAndIterator](bugpattern/IterableAndIterator)__<br>
Class should not implement both `Iterable` and `Iterator`

__[JUnit3FloatingPointComparisonWithoutDelta](bugpattern/JUnit3FloatingPointComparisonWithoutDelta)__<br>
Floating-point comparison without error tolerance

__[JUnitAmbiguousTestClass](bugpattern/JUnitAmbiguousTestClass)__<br>
Test class inherits from JUnit 3&#39;s TestCase but has JUnit 4 @Test annotations.

__[MissingCasesInEnumSwitch](bugpattern/MissingCasesInEnumSwitch)__<br>
The Google Java Style Guide requires switch statements to have an explicit default

__[MissingFail](bugpattern/MissingFail)__<br>
Not calling fail() when expecting an exception masks bugs

__[MissingOverride](bugpattern/MissingOverride)__<br>
method overrides method in supertype; expected @Override

__[NarrowingCompoundAssignment](bugpattern/NarrowingCompoundAssignment)__<br>
Compound assignments to bytes, shorts, chars, and floats hide dangerous casts

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

__[OperatorPrecedence](bugpattern/OperatorPrecedence)__<br>
Use grouping parenthesis to make the operator precedence explicit

__[OverridesGuiceInjectableMethod](bugpattern/OverridesGuiceInjectableMethod)__<br>
This method is not annotated with @Inject, but it overrides a method that is annotated with @com.google.inject.Inject. Guice will inject this method, and it is recommended to annotate it explicitly.

__[PreconditionsInvalidPlaceholder](bugpattern/PreconditionsInvalidPlaceholder)__<br>
Preconditions only accepts the %s placeholder in error message strings

__[ProtoFieldPreconditionsCheckNotNull](bugpattern/ProtoFieldPreconditionsCheckNotNull)__<br>
Protobuf fields cannot be null, so this check is redundant

__[ReferenceEquality](bugpattern/ReferenceEquality)__<br>
Comparison using reference equality instead of value equality

__[RequiredModifiers](bugpattern/RequiredModifiers)__<br>
This annotation is missing required modifiers as specified by its @RequiredModifiers annotation

__[StaticGuardedByInstance](bugpattern/StaticGuardedByInstance)__<br>
Writes to static fields should not be guarded by instance locks

__[SynchronizeOnNonFinalField](bugpattern/SynchronizeOnNonFinalField)__<br>
Synchronizing on non-final fields is not safe: if the field is ever updated, different threads may end up locking on different objects.

__[TruthConstantAsserts](bugpattern/TruthConstantAsserts)__<br>
Truth Library assert is called on a constant.

__[TruthSelfEquals](bugpattern/TruthSelfEquals)__<br>
An object is tested for equality to itself using Truth Libraries.

__[TypeParameterUnusedInFormals](bugpattern/TypeParameterUnusedInFormals)__<br>
Declaring a type parameter that is only used in the return type is a misuse of generics: operations on the type parameter are unchecked, it hides unsafe casts at invocations of the method, and it interacts badly with method overload resolution.

__[UnsynchronizedOverridesSynchronized](bugpattern/UnsynchronizedOverridesSynchronized)__<br>
Unsynchronized method overrides a synchronized method.

__[WaitNotInLoop](bugpattern/WaitNotInLoop)__<br>
Because of spurious wakeups, Object.wait() and Condition.await() must always be called in a loop

## Experimental : ERROR

__[ArgumentParameterSwap](bugpattern/ArgumentParameterSwap)__<br>
An argument is more similar to a different parameter; the arguments may have been swapped.

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

__[JMockTestWithoutRunWithOrRuleAnnotation](bugpattern/JMockTestWithoutRunWithOrRuleAnnotation)__<br>
jMock tests must have a @RunWith(JMock.class) annotation, or the Mockery field must have a @Rule JUnit annotation

__[JavaxInjectOnFinalField](bugpattern/JavaxInjectOnFinalField)__<br>
@javax.inject.Inject cannot be put on a final field.

__[LockMethodChecker](bugpattern/LockMethodChecker)__<br>
This method does not acquire the locks specified by its @LockMethod annotation

__[LongLiteralLowerCaseSuffix](bugpattern/LongLiteralLowerCaseSuffix)__<br>
Prefer &#39;L&#39; to &#39;l&#39; for the suffix to long literals

__[MultibindsInsteadOfMultibindings](bugpattern/MultibindsInsteadOfMultibindings)__<br>
`@Multibinds` is the new way to declare multibindings.

__[NoAllocation](bugpattern/NoAllocation)__<br>
@NoAllocation was specified on this method, but something was found that would trigger an allocation

__[NonRuntimeAnnotation](bugpattern/NonRuntimeAnnotation)__<br>
Calling getAnnotation on an annotation that is not retained at runtime.

__[NumericEquality](bugpattern/NumericEquality)__<br>
Numeric comparison using reference equality instead of value equality

__[ParameterPackage](bugpattern/ParameterPackage)__<br>
Method parameter has wrong package

__[ProtoStringFieldReferenceEquality](bugpattern/ProtoStringFieldReferenceEquality)__<br>
Comparing protobuf fields of type String using reference equality

__[QualifierOnMethodWithoutProvides](bugpattern/QualifierOnMethodWithoutProvides)__<br>
Qualifier applied to a method that isn&#39;t a @Provides method. This method won&#39;t be used for dependency injection

__[UnlockMethod](bugpattern/UnlockMethod)__<br>
This method does not acquire the locks specified by its @UnlockMethod annotation

## Experimental : WARNING

__[AssertFalse](bugpattern/AssertFalse)__<br>
Assertions may be disabled at runtime and do not guarantee that execution will halt here; consider throwing an exception instead

__[AssistedInjectAndInjectOnConstructors](bugpattern/AssistedInjectAndInjectOnConstructors)__<br>
@AssistedInject and @Inject should not be used on different constructors in the same class.

__[BigDecimalLiteralDouble](bugpattern/BigDecimalLiteralDouble)__<br>
BigDecimal(double) and BigDecimal.valueOf(double) may lose precision, prefer BigDecimal(String) or BigDecimal(long)

__[BindingToUnqualifiedCommonType](bugpattern/BindingToUnqualifiedCommonType)__<br>
This code declares a binding for a common value type without a Qualifier annotation.

__[EmptyTopLevelDeclaration](bugpattern/EmptyTopLevelDeclaration)__<br>
Empty top-level type declaration

__[HardCodedSdCardPath](bugpattern/HardCodedSdCardPath)__<br>
Hardcoded reference to /sdcard

__[NonCanonicalStaticMemberImport](bugpattern/NonCanonicalStaticMemberImport)__<br>
Static import of member uses non-canonical name

__[PrimitiveArrayPassedToVarargsMethod](bugpattern/PrimitiveArrayPassedToVarargsMethod)__<br>
Passing a primitive array to a varargs method is usually wrong

__[QualifierWithTypeUse](bugpattern/QualifierWithTypeUse)__<br>
Injection frameworks currently don&#39;t understand Qualifiers in TYPE_PARAMETER or TYPE_USE contexts.

__[RedundantThrows](bugpattern/RedundantThrows)__<br>
Thrown exception is a subtype of another

__[StaticAccessedFromInstance](bugpattern/StaticAccessedFromInstance)__<br>
A static variable or method should not be accessed from an object instance

__[StringEquality](bugpattern/StringEquality)__<br>
String comparison using reference equality instead of value equality

__[Var](bugpattern/Var)__<br>
Non-constant variable missing @Var annotation

## Experimental : SUGGESTION

__[ConstantField](bugpattern/ConstantField)__<br>
Field name is CONSTANT_CASE, but field is not static and final

__[EmptySetMultibindingContributions](bugpattern/EmptySetMultibindingContributions)__<br>
@Multibinds is a more efficient and declarative mechanism for ensuring that a set multibinding is present in the graph.

__[FieldMissingNullable](bugpattern/FieldMissingNullable)__<br>
Fields that can be null should be annotated @Nullable

__[MixedArrayDimensions](bugpattern/MixedArrayDimensions)__<br>
C-style array declarations should not be used

__[MultiVariableDeclaration](bugpattern/MultiVariableDeclaration)__<br>
Variable declarations should declare only one variable

__[MultipleTopLevelClasses](bugpattern/MultipleTopLevelClasses)__<br>
Source files should not contain multiple top-level class declarations

__[PackageLocation](bugpattern/PackageLocation)__<br>
Package names should match the directory they are declared in

__[ParameterNotNullable](bugpattern/ParameterNotNullable)__<br>
Method parameters that aren&#39;t checked for null shouldn&#39;t be annotated @Nullable

__[PrivateConstructorForNoninstantiableModuleTest](bugpattern/PrivateConstructorForNoninstantiableModuleTest)__<br>
Add a private constructor to modules that will not be instantiated by Dagger.

__[PrivateConstructorForUtilityClass](bugpattern/PrivateConstructorForUtilityClass)__<br>
Utility classes (only static members) are not designed to be instantiated and should be made noninstantiable with a default constructor.

__[RemoveUnusedImports](bugpattern/RemoveUnusedImports)__<br>
Unused imports

__[ReturnMissingNullable](bugpattern/ReturnMissingNullable)__<br>
Methods that can return null should be annotated @Nullable

__[ThrowsUncheckedException](bugpattern/ThrowsUncheckedException)__<br>
Unchecked exceptions do not need to be declared in the method signature.

__[UnnecessaryStaticImport](bugpattern/UnnecessaryStaticImport)__<br>
Using static imports for types is unnecessary

__[UseBinds](bugpattern/UseBinds)__<br>
@Binds is a more efficient and declarative mechanism for delegating a binding.

__[WildcardImport](bugpattern/WildcardImport)__<br>
Wildcard imports, static or otherwise, should not be used

