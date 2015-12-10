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

## On by default : WARNING

__[BadAnnotationImplementation](bugpattern/BadAnnotationImplementation)__\
Classes that implement Annotation must override equals and hashCode. Consider using AutoAnnotation instead of implementing Annotation by hand.

__[CannotMockFinalClass](bugpattern/CannotMockFinalClass)__\
Mockito cannot mock final classes

__[ElementsCountedInLoop](bugpattern/ElementsCountedInLoop)__\
This code, which counts elements using a loop, can be replaced by a simpler library method

__[EmptyTopLevelDeclaration](bugpattern/EmptyTopLevelDeclaration)__\
Empty top-level type declaration

__[EqualsHashCode](bugpattern/EqualsHashCode)__\
Classes that override equals should also override hashCode.

__[Finally](bugpattern/Finally)__\
If you return or throw from a finally, then values returned or thrown from the try-catch block will be ignored. Consider using try-with-resources instead.

__[IncompatibleModifiers](bugpattern/IncompatibleModifiers)__\
This annotation has incompatible modifiers as specified by its @IncompatibleModifiers annotation

__[JUnitAmbiguousTestClass](bugpattern/JUnitAmbiguousTestClass)__\
Test class inherits from JUnit 3&#39;s TestCase but has JUnit 4 @Test annotations.

__[MissingOverride](bugpattern/MissingOverride)__\
method overrides method in supertype; expected @Override

__[MultipleTopLevelClasses](bugpattern/MultipleTopLevelClasses)__\
Source files should not contain multiple top-level class declarations

__[NonAtomicVolatileUpdate](bugpattern/NonAtomicVolatileUpdate)__\
This update of a volatile variable is non-atomic

__[NonCanonicalStaticMemberImport](bugpattern/NonCanonicalStaticMemberImport)__\
Static import of member uses non-canonical name

__[NonOverridingEquals](bugpattern/NonOverridingEquals)__\
equals method doesn&#39;t override Object.equals

__[PreconditionsInvalidPlaceholder](bugpattern/PreconditionsInvalidPlaceholder)__\
Preconditions only accepts the %s placeholder in error message strings

__[ProtoFieldPreconditionsCheckNotNull](bugpattern/ProtoFieldPreconditionsCheckNotNull)__\
Protobuf fields cannot be null, so this check is redundant

__[RequiredModifiers](bugpattern/RequiredModifiers)__\
This annotation is missing required modifiers as specified by its @RequiredModifiers annotation

__[StaticAccessedFromInstance](bugpattern/StaticAccessedFromInstance)__\
A static variable or method should not be accessed from an object instance

__[StringEquality](bugpattern/StringEquality)__\
String comparison using reference equality instead of value equality

__[SynchronizeOnNonFinalField](bugpattern/SynchronizeOnNonFinalField)__\
Synchronizing on non-final fields is not safe: if the field is ever updated, different threads may end up locking on different objects.

__[TypeParameterUnusedInFormals](bugpattern/TypeParameterUnusedInFormals)__\
Declaring a type parameter that is only used in the return type is a misuse of generics: operations on the type parameter are unchecked, it hides unsafe casts at invocations of the method, and it interacts badly with method overload resolution. NOTE: correcting this issue is often an incompatible API change; you should check that all dependent code still compiles succesfully.

__[UnnecessaryStaticImport](bugpattern/UnnecessaryStaticImport)__\
Using static imports for types is unnecessary

__[WaitNotInLoop](bugpattern/WaitNotInLoop)__\
Because of spurious wakeups, Object.wait() and Condition.await() must always be called in a loop

## On by default : ERROR

__[ArrayEquals](bugpattern/ArrayEquals)__\
Reference equality used to compare arrays

__[ArrayHashCode](bugpattern/ArrayHashCode)__\
hashcode method on array does not hash array contents

__[ArrayToString](bugpattern/ArrayToString)__\
Calling toString on an array does not provide useful information

__[ArrayToStringCompoundAssignment](bugpattern/ArrayToStringCompoundAssignment)__\
Implicit toString used on an array (String += Array)

__[ArrayToStringConcatenation](bugpattern/ArrayToStringConcatenation)__\
Implicit toString used on an array (String + Array)

__[AsyncFunctionReturnsNull](bugpattern/AsyncFunctionReturnsNull)__\
AsyncFunction should not return a null Future, only a Future whose result is null.

__[BadComparable](bugpattern/BadComparable)__\
Possible sign flip from narrowing conversion

__[BadShiftAmount](bugpattern/BadShiftAmount)__\
Shift by an amount that is out of range

__[ChainingConstructorIgnoresParameter](bugpattern/ChainingConstructorIgnoresParameter)__\
The called constructor accepts a parameter with the same name and type as one of its caller&#39;s parameters, but its caller doesn&#39;t pass that parameter to it.  It&#39;s likely that it was intended to.

__[CheckReturnValue](bugpattern/CheckReturnValue)__\
Ignored return value of method that is annotated with @CheckReturnValue

__[ClassName](bugpattern/ClassName)__\
The source file name should match the name of the top-level class it contains

__[ComparisonOutOfRange](bugpattern/ComparisonOutOfRange)__\
Comparison to value that is out of range for the compared type

__[CompileTimeConstant](bugpattern/CompileTimeConstant)__\
Non-compile-time constant expression passed to parameter with @CompileTimeConstant type annotation.

__[DeadException](bugpattern/DeadException)__\
Exception created but not thrown

__[DepAnn](bugpattern/DepAnn)__\
Deprecated item is not annotated with @Deprecated

__[DoubleCheckedLocking](bugpattern/DoubleCheckedLocking)__\
Double-checked locking on non-volatile fields is unsafe

__[EqualsNaN](bugpattern/EqualsNaN)__\
== NaN always returns false; use the isNaN methods instead

__[ForOverride](bugpattern/ForOverride)__\
Method annotated @ForOverride must be protected or package-private and only invoked from declaring class

__[GetClassOnClass](bugpattern/GetClassOnClass)__\
Calling getClass() on an object of type Class returns the Class object for java.lang.Class; you probably meant to operate on the object directly

__[GuardedByChecker](bugpattern/GuardedByChecker)__\
Checks for unguarded accesses to fields and methods with @GuardedBy annotations

__[GuiceAssistedInjectScoping](bugpattern/GuiceAssistedInjectScoping)__\
Scope annotation on implementation class of AssistedInject factory is not allowed

__[HashtableContains](bugpattern/HashtableContains)__\
contains() is a legacy method that is equivalent to containsValue()

__[InvalidPatternSyntax](bugpattern/InvalidPatternSyntax)__\
Invalid syntax used for a regular expression

__[IsInstanceOfClass](bugpattern/IsInstanceOfClass)__\
The argument to Class#isInstance(Object) should not be a class literal

__[JUnit3TestNotRun](bugpattern/JUnit3TestNotRun)__\
Test method will not be run; please prefix name with &quot;test&quot;

__[JUnit4SetUpNotRun](bugpattern/JUnit4SetUpNotRun)__\
setUp() method will not be run; Please add a @Before annotation

__[JUnit4TearDownNotRun](bugpattern/JUnit4TearDownNotRun)__\
tearDown() method will not be run; Please add an @After annotation

__[JUnit4TestNotRun](bugpattern/JUnit4TestNotRun)__\
Test method will not be run; please add @Test annotation

__[LongLiteralLowerCaseSuffix](bugpattern/LongLiteralLowerCaseSuffix)__\
Prefer &#39;L&#39; to &#39;l&#39; for the suffix to long literals

__[MislabeledAndroidString](bugpattern/MislabeledAndroidString)__\
Certain resources in `android.R.string` have names that do not match their content

__[MisusedWeekYear](bugpattern/MisusedWeekYear)__\
Use of &quot;YYYY&quot; (week year) in a date pattern without &quot;ww&quot; (week in year). You probably meant to use &quot;yyyy&quot; (year) instead.

__[MockitoUsage](bugpattern/MockitoUsage)__\
Missing method call for verify(mock) here

__[NarrowingCompoundAssignment](bugpattern/NarrowingCompoundAssignment)__\
Compound assignments to bytes, shorts, chars, and floats hide dangerous casts

__[NonCanonicalStaticImport](bugpattern/NonCanonicalStaticImport)__\
Static import of type uses non-canonical name

__[NonFinalCompileTimeConstant](bugpattern/NonFinalCompileTimeConstant)__\
@CompileTimeConstant parameters should be final

__[OptionalEquality](bugpattern/OptionalEquality)__\
Comparison using reference equality instead of value equality

__[Overrides](bugpattern/Overrides)__\
Varargs doesn&#39;t agree for overridden method

__[PackageLocation](bugpattern/PackageLocation)__\
Package names should match the directory they are declared in

__[PreconditionsCheckNotNull](bugpattern/PreconditionsCheckNotNull)__\
Literal passed as first argument to Preconditions.checkNotNull() can never be null

__[PreconditionsCheckNotNullPrimitive](bugpattern/PreconditionsCheckNotNullPrimitive)__\
First argument to `Preconditions.checkNotNull()` is a primitive rather than an object reference

__[ProtoFieldNullComparison](bugpattern/ProtoFieldNullComparison)__\
Protobuf fields cannot be null

__[ReturnValueIgnored](bugpattern/ReturnValueIgnored)__\
Return value of this method must be used

__[SelfAssignment](bugpattern/SelfAssignment)__\
Variable assigned to itself

__[SelfEquals](bugpattern/SelfEquals)__\
An object is tested for equality to itself

__[SizeGreaterThanOrEqualsZero](bugpattern/SizeGreaterThanOrEqualsZero)__\
Comparison of a size &gt;= 0 is always true, did you intend to check for non-emptiness?

__[StringBuilderInitWithChar](bugpattern/StringBuilderInitWithChar)__\
StringBuilder does not have a char constructor; this invokes the int constructor.

__[SuppressWarningsDeprecated](bugpattern/SuppressWarningsDeprecated)__\
Suppressing &quot;deprecated&quot; is probably a typo for &quot;deprecation&quot;

__[TryFailThrowable](bugpattern/TryFailThrowable)__\
Catching Throwable masks failures from fail() or assert*() in the try block

__[UnnecessaryTypeArgument](bugpattern/UnnecessaryTypeArgument)__\
Non-generic methods should not be invoked with type arguments

__[UnsynchronizedOverridesSynchronized](bugpattern/UnsynchronizedOverridesSynchronized)__\
Unsynchronized method overrides a synchronized method.

__[UnusedAnonymousClass](bugpattern/UnusedAnonymousClass)__\
Instance created but never used

## Experimental : WARNING

__[AssertFalse](bugpattern/AssertFalse)__\
Assertions may be disabled at runtime and do not guarantee that execution will halt here; consider throwing an exception instead

__[AssistedInjectAndInjectOnConstructors](bugpattern/AssistedInjectAndInjectOnConstructors)__\
@AssistedInject and @Inject should not be used on different constructors in the same class.

__[CollectionIncompatibleType](bugpattern/CollectionIncompatibleType)__\
Incompatible type as argument to Object-accepting Java collections method

__[GuiceInjectOnFinalField](bugpattern/GuiceInjectOnFinalField)__\
Although Guice allows injecting final fields, doing so is not recommended because the injected value may not be visible to other threads.

__[MissingFail](bugpattern/MissingFail)__\
Not calling fail() when expecting an exception masks bugs

__[NullablePrimitive](bugpattern/NullablePrimitive)__\
@Nullable should not be used for primitive types.

__[OverridesGuiceInjectableMethod](bugpattern/OverridesGuiceInjectableMethod)__\
This method is not annotated with @Inject, but it overrides a method that is annotated with @com.google.inject.Inject. Guice will inject this method,and it is recommended to annotate it explicitly.

__[PreconditionsErrorMessageEagerEvaluation](bugpattern/PreconditionsErrorMessageEagerEvaluation)__\
Second argument to Preconditions.* is a call to String.format(), which can be unwrapped

__[PrimitiveArrayPassedToVarargsMethod](bugpattern/PrimitiveArrayPassedToVarargsMethod)__\
Passing a primitive array to a varargs method is usually wrong

## Experimental : NOT\_A\_PROBLEM

__[FallthroughSuppression](bugpattern/FallthroughSuppression)__\
Fallthrough warning suppression has no effect if warning is suppressed

## Experimental : ERROR

__[ArraysAsListPrimitiveArray](bugpattern/ArraysAsListPrimitiveArray)__\
Arrays.asList does not autobox primitive arrays, as one might expect.

__[AssistedInjectAndInjectOnSameConstructor](bugpattern/AssistedInjectAndInjectOnSameConstructor)__\
@AssistedInject and @Inject cannot be used on the same constructor.

__[ClassCanBeStatic](bugpattern/ClassCanBeStatic)__\
Inner class is non-static but does not reference enclosing class

__[DivZero](bugpattern/DivZero)__\
Division by integer literal zero

__[EmptyIf](bugpattern/EmptyIf)__\
Empty statement after if

__[GuardedByValidator](bugpattern/GuardedByValidator)__\
Invalid @GuardedBy expression

__[GuiceAssistedParameters](bugpattern/GuiceAssistedParameters)__\
A constructor cannot have two @Assisted parameters of the same type unless they are disambiguated with named @Assisted annotations. 

__[Immutable](bugpattern/Immutable)__\
Type declaration annotated with @Immutable is not immutable

__[InjectInvalidTargetingOnScopingAnnotation](bugpattern/InjectInvalidTargetingOnScopingAnnotation)__\
The target of a scoping annotation must be set to METHOD and/or TYPE.

__[InjectMoreThanOneQualifier](bugpattern/InjectMoreThanOneQualifier)__\
Using more than one qualifier annotation on the same element is not allowed.

__[InjectMoreThanOneScopeAnnotationOnClass](bugpattern/InjectMoreThanOneScopeAnnotationOnClass)__\
A class can be annotated with at most one scope annotation

__[InjectScopeAnnotationOnInterfaceOrAbstractClass](bugpattern/InjectScopeAnnotationOnInterfaceOrAbstractClass)__\
Scope annotation on an interface or abstact class is not allowed

__[InjectScopeOrQualifierAnnotationRetention](bugpattern/InjectScopeOrQualifierAnnotationRetention)__\
Scoping and qualifier annotations must have runtime retention.

__[InjectedConstructorAnnotations](bugpattern/InjectedConstructorAnnotations)__\
Injected constructors cannot be optional nor have binding annotations

__[InsecureCipherMode](bugpattern/InsecureCipherMode)__\
Cipher.getInstance() is invoked using either the default settings or ECB mode

__[JMockTestWithoutRunWithOrRuleAnnotation](bugpattern/JMockTestWithoutRunWithOrRuleAnnotation)__\
jMock tests must have a @RunWith(JMock.class) annotation, or the Mockery field must have a @Rule JUnit annotation

__[JavaxInjectOnAbstractMethod](bugpattern/JavaxInjectOnAbstractMethod)__\
Abstract methods are not injectable with javax.inject.Inject.

__[JavaxInjectOnFinalField](bugpattern/JavaxInjectOnFinalField)__\
@javax.inject.Inject cannot be put on a final field.

__[LockMethodChecker](bugpattern/LockMethodChecker)__\
This method does not acquire the locks specified by its @LockMethod annotation

__[MalformedFormatString](bugpattern/MalformedFormatString)__\
Printf-like format string does not match its arguments

__[MissingCasesInEnumSwitch](bugpattern/MissingCasesInEnumSwitch)__\
Enum switch statement is missing cases

__[ModifyingCollectionWithItself](bugpattern/ModifyingCollectionWithItself)__\
Modifying a collection with itself

__[MoreThanOneInjectableConstructor](bugpattern/MoreThanOneInjectableConstructor)__\
A class may not have more than one injectable constructor.

__[NoAllocation](bugpattern/NoAllocation)__\
@NoAllocation was specified on this method, but something was found that would trigger an allocation

__[NonRuntimeAnnotation](bugpattern/NonRuntimeAnnotation)__\
Calling getAnnotation on an annotation that is not retained at runtime.

__[NumericEquality](bugpattern/NumericEquality)__\
Numeric comparison using reference equality instead of value equality

__[OverlappingQualifierAndScopeAnnotation](bugpattern/OverlappingQualifierAndScopeAnnotation)__\
Annotations cannot be both Qualifiers/BindingAnnotations and Scopes

__[OverridesJavaxInjectableMethod](bugpattern/OverridesJavaxInjectableMethod)__\
This method is not annotated with @Inject, but it overrides a  method that is  annotated with @javax.inject.Inject.

__[ParameterPackage](bugpattern/ParameterPackage)__\
Method parameter has wrong package

__[ProtoStringFieldReferenceEquality](bugpattern/ProtoStringFieldReferenceEquality)__\
Comparing protobuf fields of type String using reference equality

__[SelfEquality](bugpattern/SelfEquality)__\
Variable compared to itself

__[UnlockMethod](bugpattern/UnlockMethod)__\
This method does not acquire the locks specified by its @UnlockMethod annotation

__[WildcardImport](bugpattern/WildcardImport)__\
Use of wildcard imports is forbidden

