/*
 * Copyright 2014 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.scanner;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.errorprone.BugCheckerInfo;
import com.google.errorprone.bugpatterns.ASTHelpersSuggestions;
import com.google.errorprone.bugpatterns.AddressSelection;
import com.google.errorprone.bugpatterns.AlreadyChecked;
import com.google.errorprone.bugpatterns.AlwaysThrows;
import com.google.errorprone.bugpatterns.AmbiguousMethodReference;
import com.google.errorprone.bugpatterns.AnnotationMirrorToString;
import com.google.errorprone.bugpatterns.AnnotationPosition;
import com.google.errorprone.bugpatterns.AnnotationValueToString;
import com.google.errorprone.bugpatterns.ArrayAsKeyOfSetOrMap;
import com.google.errorprone.bugpatterns.ArrayEquals;
import com.google.errorprone.bugpatterns.ArrayFillIncompatibleType;
import com.google.errorprone.bugpatterns.ArrayHashCode;
import com.google.errorprone.bugpatterns.ArrayRecordComponent;
import com.google.errorprone.bugpatterns.ArrayToString;
import com.google.errorprone.bugpatterns.ArraysAsListPrimitiveArray;
import com.google.errorprone.bugpatterns.AssertFalse;
import com.google.errorprone.bugpatterns.AssertThrowsMultipleStatements;
import com.google.errorprone.bugpatterns.AssertionFailureIgnored;
import com.google.errorprone.bugpatterns.AssignmentExpression;
import com.google.errorprone.bugpatterns.AsyncCallableReturnsNull;
import com.google.errorprone.bugpatterns.AsyncFunctionReturnsNull;
import com.google.errorprone.bugpatterns.AttemptedNegativeZero;
import com.google.errorprone.bugpatterns.AutoValueBoxedValues;
import com.google.errorprone.bugpatterns.AutoValueBuilderDefaultsInConstructor;
import com.google.errorprone.bugpatterns.AutoValueFinalMethods;
import com.google.errorprone.bugpatterns.AutoValueImmutableFields;
import com.google.errorprone.bugpatterns.AutoValueSubclassLeaked;
import com.google.errorprone.bugpatterns.AvoidObjectArrays;
import com.google.errorprone.bugpatterns.BadAnnotationImplementation;
import com.google.errorprone.bugpatterns.BadComparable;
import com.google.errorprone.bugpatterns.BadImport;
import com.google.errorprone.bugpatterns.BadInstanceof;
import com.google.errorprone.bugpatterns.BadShiftAmount;
import com.google.errorprone.bugpatterns.BanClassLoader;
import com.google.errorprone.bugpatterns.BanJNDI;
import com.google.errorprone.bugpatterns.BanSerializableRead;
import com.google.errorprone.bugpatterns.BareDotMetacharacter;
import com.google.errorprone.bugpatterns.BigDecimalEquals;
import com.google.errorprone.bugpatterns.BigDecimalLiteralDouble;
import com.google.errorprone.bugpatterns.BooleanLiteral;
import com.google.errorprone.bugpatterns.BooleanParameter;
import com.google.errorprone.bugpatterns.BoxedPrimitiveConstructor;
import com.google.errorprone.bugpatterns.BoxedPrimitiveEquality;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugPatternNaming;
import com.google.errorprone.bugpatterns.ByteBufferBackingArray;
import com.google.errorprone.bugpatterns.CacheLoaderNull;
import com.google.errorprone.bugpatterns.CannotMockFinalClass;
import com.google.errorprone.bugpatterns.CannotMockMethod;
import com.google.errorprone.bugpatterns.CanonicalDuration;
import com.google.errorprone.bugpatterns.CatchAndPrintStackTrace;
import com.google.errorprone.bugpatterns.CatchFail;
import com.google.errorprone.bugpatterns.CatchingUnchecked;
import com.google.errorprone.bugpatterns.ChainedAssertionLosesContext;
import com.google.errorprone.bugpatterns.ChainingConstructorIgnoresParameter;
import com.google.errorprone.bugpatterns.CharacterGetNumericValue;
import com.google.errorprone.bugpatterns.CheckNotNullMultipleTimes;
import com.google.errorprone.bugpatterns.CheckReturnValue;
import com.google.errorprone.bugpatterns.CheckedExceptionNotThrown;
import com.google.errorprone.bugpatterns.ClassCanBeStatic;
import com.google.errorprone.bugpatterns.ClassInitializationDeadlock;
import com.google.errorprone.bugpatterns.ClassName;
import com.google.errorprone.bugpatterns.ClassNamedLikeTypeParameter;
import com.google.errorprone.bugpatterns.ClassNewInstance;
import com.google.errorprone.bugpatterns.ClosingStandardOutputStreams;
import com.google.errorprone.bugpatterns.CollectionToArraySafeParameter;
import com.google.errorprone.bugpatterns.CollectorShouldNotUseState;
import com.google.errorprone.bugpatterns.ComparableAndComparator;
import com.google.errorprone.bugpatterns.ComparableType;
import com.google.errorprone.bugpatterns.CompareToZero;
import com.google.errorprone.bugpatterns.ComparingThisWithNull;
import com.google.errorprone.bugpatterns.ComparisonContractViolated;
import com.google.errorprone.bugpatterns.ComparisonOutOfRange;
import com.google.errorprone.bugpatterns.CompileTimeConstantChecker;
import com.google.errorprone.bugpatterns.ComplexBooleanConstant;
import com.google.errorprone.bugpatterns.ComputeIfAbsentAmbiguousReference;
import com.google.errorprone.bugpatterns.ConditionalExpressionNumericPromotion;
import com.google.errorprone.bugpatterns.ConstantField;
import com.google.errorprone.bugpatterns.ConstantOverflow;
import com.google.errorprone.bugpatterns.ConstantPatternCompile;
import com.google.errorprone.bugpatterns.DangerousLiteralNullChecker;
import com.google.errorprone.bugpatterns.DateFormatConstant;
import com.google.errorprone.bugpatterns.DeadException;
import com.google.errorprone.bugpatterns.DeadThread;
import com.google.errorprone.bugpatterns.DeduplicateConstants;
import com.google.errorprone.bugpatterns.DeeplyNested;
import com.google.errorprone.bugpatterns.DefaultCharset;
import com.google.errorprone.bugpatterns.DefaultLocale;
import com.google.errorprone.bugpatterns.DefaultPackage;
import com.google.errorprone.bugpatterns.DepAnn;
import com.google.errorprone.bugpatterns.DeprecatedVariable;
import com.google.errorprone.bugpatterns.DifferentNameButSame;
import com.google.errorprone.bugpatterns.DirectInvocationOnMock;
import com.google.errorprone.bugpatterns.DiscardedPostfixExpression;
import com.google.errorprone.bugpatterns.DistinctVarargsChecker;
import com.google.errorprone.bugpatterns.DoNotCallChecker;
import com.google.errorprone.bugpatterns.DoNotCallSuggester;
import com.google.errorprone.bugpatterns.DoNotClaimAnnotations;
import com.google.errorprone.bugpatterns.DoNotMockAutoValue;
import com.google.errorprone.bugpatterns.DoNotMockChecker;
import com.google.errorprone.bugpatterns.DoubleBraceInitialization;
import com.google.errorprone.bugpatterns.DuplicateBranches;
import com.google.errorprone.bugpatterns.DuplicateDateFormatField;
import com.google.errorprone.bugpatterns.DuplicateMapKeys;
import com.google.errorprone.bugpatterns.EffectivelyPrivate;
import com.google.errorprone.bugpatterns.EmptyCatch;
import com.google.errorprone.bugpatterns.EmptyIfStatement;
import com.google.errorprone.bugpatterns.EmptyTopLevelDeclaration;
import com.google.errorprone.bugpatterns.EnumOrdinal;
import com.google.errorprone.bugpatterns.EqualsGetClass;
import com.google.errorprone.bugpatterns.EqualsHashCode;
import com.google.errorprone.bugpatterns.EqualsIncompatibleType;
import com.google.errorprone.bugpatterns.EqualsNaN;
import com.google.errorprone.bugpatterns.EqualsNull;
import com.google.errorprone.bugpatterns.EqualsReference;
import com.google.errorprone.bugpatterns.EqualsUnsafeCast;
import com.google.errorprone.bugpatterns.EqualsUsingHashCode;
import com.google.errorprone.bugpatterns.EqualsWrongThing;
import com.google.errorprone.bugpatterns.ErroneousBitwiseExpression;
import com.google.errorprone.bugpatterns.ErroneousThreadPoolConstructorChecker;
import com.google.errorprone.bugpatterns.ExpectedExceptionChecker;
import com.google.errorprone.bugpatterns.ExpensiveLenientFormatString;
import com.google.errorprone.bugpatterns.ExplicitArrayForVarargs;
import com.google.errorprone.bugpatterns.ExtendingJUnitAssert;
import com.google.errorprone.bugpatterns.ExtendsAutoValue;
import com.google.errorprone.bugpatterns.FallThrough;
import com.google.errorprone.bugpatterns.FieldCanBeFinal;
import com.google.errorprone.bugpatterns.FieldCanBeLocal;
import com.google.errorprone.bugpatterns.FieldCanBeStatic;
import com.google.errorprone.bugpatterns.Finalize;
import com.google.errorprone.bugpatterns.Finally;
import com.google.errorprone.bugpatterns.FloatCast;
import com.google.errorprone.bugpatterns.FloatingPointAssertionWithinEpsilon;
import com.google.errorprone.bugpatterns.FloatingPointLiteralPrecision;
import com.google.errorprone.bugpatterns.ForEachIterable;
import com.google.errorprone.bugpatterns.ForOverrideChecker;
import com.google.errorprone.bugpatterns.FunctionalInterfaceClash;
import com.google.errorprone.bugpatterns.FunctionalInterfaceMethodChanged;
import com.google.errorprone.bugpatterns.FutureReturnValueIgnored;
import com.google.errorprone.bugpatterns.FutureTransformAsync;
import com.google.errorprone.bugpatterns.FuturesGetCheckedIllegalExceptionType;
import com.google.errorprone.bugpatterns.FuzzyEqualsShouldNotBeUsedInEqualsMethod;
import com.google.errorprone.bugpatterns.GetClassOnAnnotation;
import com.google.errorprone.bugpatterns.GetClassOnClass;
import com.google.errorprone.bugpatterns.GetClassOnEnum;
import com.google.errorprone.bugpatterns.GuiceNestedCombine;
import com.google.errorprone.bugpatterns.HashtableContains;
import com.google.errorprone.bugpatterns.HidingField;
import com.google.errorprone.bugpatterns.ICCProfileGetInstance;
import com.google.errorprone.bugpatterns.IdentifierName;
import com.google.errorprone.bugpatterns.IdentityBinaryExpression;
import com.google.errorprone.bugpatterns.IdentityHashMapBoxing;
import com.google.errorprone.bugpatterns.IdentityHashMapUsage;
import com.google.errorprone.bugpatterns.IgnoredPureGetter;
import com.google.errorprone.bugpatterns.ImmutableMemberCollection;
import com.google.errorprone.bugpatterns.ImmutableSetForContains;
import com.google.errorprone.bugpatterns.ImplementAssertionWithChaining;
import com.google.errorprone.bugpatterns.ImpossibleNullComparison;
import com.google.errorprone.bugpatterns.Incomparable;
import com.google.errorprone.bugpatterns.IncompatibleModifiersChecker;
import com.google.errorprone.bugpatterns.InconsistentCapitalization;
import com.google.errorprone.bugpatterns.InconsistentHashCode;
import com.google.errorprone.bugpatterns.IncorrectMainMethod;
import com.google.errorprone.bugpatterns.IncrementInForLoopAndHeader;
import com.google.errorprone.bugpatterns.IndexOfChar;
import com.google.errorprone.bugpatterns.InexactVarargsConditional;
import com.google.errorprone.bugpatterns.InfiniteRecursion;
import com.google.errorprone.bugpatterns.InitializeInline;
import com.google.errorprone.bugpatterns.InjectOnBugCheckers;
import com.google.errorprone.bugpatterns.InlineTrivialConstant;
import com.google.errorprone.bugpatterns.InputStreamSlowMultibyteRead;
import com.google.errorprone.bugpatterns.InsecureCipherMode;
import com.google.errorprone.bugpatterns.InstanceOfAndCastMatchWrongType;
import com.google.errorprone.bugpatterns.IntFloatConversion;
import com.google.errorprone.bugpatterns.IntLiteralCast;
import com.google.errorprone.bugpatterns.IntLongMath;
import com.google.errorprone.bugpatterns.InterfaceWithOnlyStatics;
import com.google.errorprone.bugpatterns.InterruptedExceptionSwallowed;
import com.google.errorprone.bugpatterns.Interruption;
import com.google.errorprone.bugpatterns.InvalidPatternSyntax;
import com.google.errorprone.bugpatterns.InvalidTimeZoneID;
import com.google.errorprone.bugpatterns.InvalidZoneId;
import com.google.errorprone.bugpatterns.IsInstanceIncompatibleType;
import com.google.errorprone.bugpatterns.IsInstanceOfClass;
import com.google.errorprone.bugpatterns.IterableAndIterator;
import com.google.errorprone.bugpatterns.IterablePathParameter;
import com.google.errorprone.bugpatterns.JUnit3FloatingPointComparisonWithoutDelta;
import com.google.errorprone.bugpatterns.JUnit3TestNotRun;
import com.google.errorprone.bugpatterns.JUnit4ClassAnnotationNonStatic;
import com.google.errorprone.bugpatterns.JUnit4ClassUsedInJUnit3;
import com.google.errorprone.bugpatterns.JUnit4EmptyMethods;
import com.google.errorprone.bugpatterns.JUnit4SetUpNotRun;
import com.google.errorprone.bugpatterns.JUnit4TearDownNotRun;
import com.google.errorprone.bugpatterns.JUnit4TestNotRun;
import com.google.errorprone.bugpatterns.JUnit4TestsNotRunWithinEnclosed;
import com.google.errorprone.bugpatterns.JUnitAmbiguousTestClass;
import com.google.errorprone.bugpatterns.JUnitAssertSameCheck;
import com.google.errorprone.bugpatterns.JUnitParameterMethodNotFound;
import com.google.errorprone.bugpatterns.JavaLangClash;
import com.google.errorprone.bugpatterns.JavaUtilDateChecker;
import com.google.errorprone.bugpatterns.JdkObsolete;
import com.google.errorprone.bugpatterns.LabelledBreakTarget;
import com.google.errorprone.bugpatterns.LambdaFunctionalInterface;
import com.google.errorprone.bugpatterns.LiteByteStringUtf8;
import com.google.errorprone.bugpatterns.LiteEnumValueOf;
import com.google.errorprone.bugpatterns.LiteProtoToString;
import com.google.errorprone.bugpatterns.LockNotBeforeTry;
import com.google.errorprone.bugpatterns.LockOnBoxedPrimitive;
import com.google.errorprone.bugpatterns.LockOnNonEnclosingClassLiteral;
import com.google.errorprone.bugpatterns.LogicalAssignment;
import com.google.errorprone.bugpatterns.LongDoubleConversion;
import com.google.errorprone.bugpatterns.LongFloatConversion;
import com.google.errorprone.bugpatterns.LongLiteralLowerCaseSuffix;
import com.google.errorprone.bugpatterns.LoopConditionChecker;
import com.google.errorprone.bugpatterns.LoopOverCharArray;
import com.google.errorprone.bugpatterns.LossyPrimitiveCompare;
import com.google.errorprone.bugpatterns.MathAbsoluteNegative;
import com.google.errorprone.bugpatterns.MathRoundIntLong;
import com.google.errorprone.bugpatterns.MemoizeConstantVisitorStateLookups;
import com.google.errorprone.bugpatterns.MethodCanBeStatic;
import com.google.errorprone.bugpatterns.MisformattedTestData;
import com.google.errorprone.bugpatterns.MisleadingEmptyVarargs;
import com.google.errorprone.bugpatterns.MisleadingEscapedSpace;
import com.google.errorprone.bugpatterns.MissingBraces;
import com.google.errorprone.bugpatterns.MissingCasesInEnumSwitch;
import com.google.errorprone.bugpatterns.MissingDefault;
import com.google.errorprone.bugpatterns.MissingFail;
import com.google.errorprone.bugpatterns.MissingImplementsComparable;
import com.google.errorprone.bugpatterns.MissingOverride;
import com.google.errorprone.bugpatterns.MissingRefasterAnnotation;
import com.google.errorprone.bugpatterns.MissingSuperCall;
import com.google.errorprone.bugpatterns.MissingTestCall;
import com.google.errorprone.bugpatterns.MisusedDayOfYear;
import com.google.errorprone.bugpatterns.MisusedWeekYear;
import com.google.errorprone.bugpatterns.MixedArrayDimensions;
import com.google.errorprone.bugpatterns.MixedDescriptors;
import com.google.errorprone.bugpatterns.MixedMutabilityReturnType;
import com.google.errorprone.bugpatterns.MockIllegalThrows;
import com.google.errorprone.bugpatterns.MockNotUsedInProduction;
import com.google.errorprone.bugpatterns.MockitoDoSetup;
import com.google.errorprone.bugpatterns.MockitoUsage;
import com.google.errorprone.bugpatterns.ModifiedButNotUsed;
import com.google.errorprone.bugpatterns.ModifyCollectionInEnhancedForLoop;
import com.google.errorprone.bugpatterns.ModifySourceCollectionInStream;
import com.google.errorprone.bugpatterns.ModifyingCollectionWithItself;
import com.google.errorprone.bugpatterns.MultiVariableDeclaration;
import com.google.errorprone.bugpatterns.MultimapKeys;
import com.google.errorprone.bugpatterns.MultipleParallelOrSequentialCalls;
import com.google.errorprone.bugpatterns.MultipleTopLevelClasses;
import com.google.errorprone.bugpatterns.MultipleUnaryOperatorsInMethodCall;
import com.google.errorprone.bugpatterns.MustBeClosedChecker;
import com.google.errorprone.bugpatterns.MutableGuiceModule;
import com.google.errorprone.bugpatterns.MutablePublicArray;
import com.google.errorprone.bugpatterns.NCopiesOfChar;
import com.google.errorprone.bugpatterns.NamedLikeContextualKeyword;
import com.google.errorprone.bugpatterns.NarrowCalculation;
import com.google.errorprone.bugpatterns.NarrowingCompoundAssignment;
import com.google.errorprone.bugpatterns.NegativeBoolean;
import com.google.errorprone.bugpatterns.NegativeCharLiteral;
import com.google.errorprone.bugpatterns.NestedInstanceOfConditions;
import com.google.errorprone.bugpatterns.NewFileSystem;
import com.google.errorprone.bugpatterns.NoAllocationChecker;
import com.google.errorprone.bugpatterns.NonApiType;
import com.google.errorprone.bugpatterns.NonAtomicVolatileUpdate;
import com.google.errorprone.bugpatterns.NonCanonicalStaticImport;
import com.google.errorprone.bugpatterns.NonCanonicalStaticMemberImport;
import com.google.errorprone.bugpatterns.NonCanonicalType;
import com.google.errorprone.bugpatterns.NonFinalCompileTimeConstant;
import com.google.errorprone.bugpatterns.NonFinalStaticField;
import com.google.errorprone.bugpatterns.NonOverridingEquals;
import com.google.errorprone.bugpatterns.NonRuntimeAnnotation;
import com.google.errorprone.bugpatterns.NullOptional;
import com.google.errorprone.bugpatterns.NullTernary;
import com.google.errorprone.bugpatterns.NullableConstructor;
import com.google.errorprone.bugpatterns.NullableOnContainingClass;
import com.google.errorprone.bugpatterns.NullableOptional;
import com.google.errorprone.bugpatterns.NullableVoid;
import com.google.errorprone.bugpatterns.ObjectEqualsForPrimitives;
import com.google.errorprone.bugpatterns.ObjectToString;
import com.google.errorprone.bugpatterns.ObjectsHashCodePrimitive;
import com.google.errorprone.bugpatterns.OperatorPrecedence;
import com.google.errorprone.bugpatterns.OptionalEquality;
import com.google.errorprone.bugpatterns.OptionalMapToOptional;
import com.google.errorprone.bugpatterns.OptionalMapUnusedValue;
import com.google.errorprone.bugpatterns.OptionalNotPresent;
import com.google.errorprone.bugpatterns.OptionalOfRedundantMethod;
import com.google.errorprone.bugpatterns.OrphanedFormatString;
import com.google.errorprone.bugpatterns.OutlineNone;
import com.google.errorprone.bugpatterns.OverrideThrowableToString;
import com.google.errorprone.bugpatterns.Overrides;
import com.google.errorprone.bugpatterns.OverridingMethodInconsistentArgumentNamesChecker;
import com.google.errorprone.bugpatterns.PackageInfo;
import com.google.errorprone.bugpatterns.PackageLocation;
import com.google.errorprone.bugpatterns.ParameterComment;
import com.google.errorprone.bugpatterns.ParameterName;
import com.google.errorprone.bugpatterns.ParametersButNotParameterized;
import com.google.errorprone.bugpatterns.PatternMatchingInstanceof;
import com.google.errorprone.bugpatterns.PreconditionsCheckNotNullRepeated;
import com.google.errorprone.bugpatterns.PreconditionsInvalidPlaceholder;
import com.google.errorprone.bugpatterns.PreferInstanceofOverGetKind;
import com.google.errorprone.bugpatterns.PreferredInterfaceType;
import com.google.errorprone.bugpatterns.PrimitiveArrayPassedToVarargsMethod;
import com.google.errorprone.bugpatterns.PrimitiveAtomicReference;
import com.google.errorprone.bugpatterns.PrivateConstructorForUtilityClass;
import com.google.errorprone.bugpatterns.PrivateSecurityContractProtoAccess;
import com.google.errorprone.bugpatterns.ProtectedMembersInFinalClass;
import com.google.errorprone.bugpatterns.ProtoBuilderReturnValueIgnored;
import com.google.errorprone.bugpatterns.ProtoStringFieldReferenceEquality;
import com.google.errorprone.bugpatterns.ProtoTruthMixedDescriptors;
import com.google.errorprone.bugpatterns.ProtocolBufferOrdinal;
import com.google.errorprone.bugpatterns.PublicApiNamedStreamShouldReturnStream;
import com.google.errorprone.bugpatterns.RandomCast;
import com.google.errorprone.bugpatterns.RandomModInteger;
import com.google.errorprone.bugpatterns.ReachabilityFenceUsage;
import com.google.errorprone.bugpatterns.RedundantControlFlow;
import com.google.errorprone.bugpatterns.RedundantOverride;
import com.google.errorprone.bugpatterns.RedundantSetterCall;
import com.google.errorprone.bugpatterns.RedundantThrows;
import com.google.errorprone.bugpatterns.ReferenceEquality;
import com.google.errorprone.bugpatterns.RemoveUnusedImports;
import com.google.errorprone.bugpatterns.RequiredModifiersChecker;
import com.google.errorprone.bugpatterns.RestrictedApiChecker;
import com.google.errorprone.bugpatterns.RethrowReflectiveOperationExceptionAsLinkageError;
import com.google.errorprone.bugpatterns.ReturnAtTheEndOfVoidFunction;
import com.google.errorprone.bugpatterns.ReturnValueIgnored;
import com.google.errorprone.bugpatterns.ReturnsNullCollection;
import com.google.errorprone.bugpatterns.RobolectricShadowDirectlyOn;
import com.google.errorprone.bugpatterns.RuleNotRun;
import com.google.errorprone.bugpatterns.RxReturnValueIgnored;
import com.google.errorprone.bugpatterns.SameNameButDifferent;
import com.google.errorprone.bugpatterns.SelfAlwaysReturnsThis;
import com.google.errorprone.bugpatterns.SelfAssertion;
import com.google.errorprone.bugpatterns.SelfAssignment;
import com.google.errorprone.bugpatterns.SelfComparison;
import com.google.errorprone.bugpatterns.SelfEquals;
import com.google.errorprone.bugpatterns.SelfSet;
import com.google.errorprone.bugpatterns.SetUnrecognized;
import com.google.errorprone.bugpatterns.ShortCircuitBoolean;
import com.google.errorprone.bugpatterns.ShouldHaveEvenArgs;
import com.google.errorprone.bugpatterns.SizeGreaterThanOrEqualsZero;
import com.google.errorprone.bugpatterns.StatementSwitchToExpressionSwitch;
import com.google.errorprone.bugpatterns.StaticAssignmentInConstructor;
import com.google.errorprone.bugpatterns.StaticAssignmentOfThrowable;
import com.google.errorprone.bugpatterns.StaticMockMember;
import com.google.errorprone.bugpatterns.StaticQualifiedUsingExpression;
import com.google.errorprone.bugpatterns.StreamResourceLeak;
import com.google.errorprone.bugpatterns.StreamToIterable;
import com.google.errorprone.bugpatterns.StreamToString;
import com.google.errorprone.bugpatterns.StringBuilderInitWithChar;
import com.google.errorprone.bugpatterns.StringCaseLocaleUsage;
import com.google.errorprone.bugpatterns.StringCharset;
import com.google.errorprone.bugpatterns.StringConcatToTextBlock;
import com.google.errorprone.bugpatterns.StringFormatWithLiteral;
import com.google.errorprone.bugpatterns.StringJoin;
import com.google.errorprone.bugpatterns.StringSplitter;
import com.google.errorprone.bugpatterns.StronglyTypeByteString;
import com.google.errorprone.bugpatterns.SubstringOfZero;
import com.google.errorprone.bugpatterns.SunApi;
import com.google.errorprone.bugpatterns.SuperCallToObjectMethod;
import com.google.errorprone.bugpatterns.SuppressWarningsDeprecated;
import com.google.errorprone.bugpatterns.SuppressWarningsWithoutExplanation;
import com.google.errorprone.bugpatterns.SwigMemoryLeak;
import com.google.errorprone.bugpatterns.SwitchDefault;
import com.google.errorprone.bugpatterns.SymbolToString;
import com.google.errorprone.bugpatterns.SystemConsoleNull;
import com.google.errorprone.bugpatterns.SystemExitOutsideMain;
import com.google.errorprone.bugpatterns.SystemOut;
import com.google.errorprone.bugpatterns.TestExceptionChecker;
import com.google.errorprone.bugpatterns.TestParametersNotInitialized;
import com.google.errorprone.bugpatterns.TheoryButNoTheories;
import com.google.errorprone.bugpatterns.ThreadBuilderNameWithPlaceholder;
import com.google.errorprone.bugpatterns.ThreadJoinLoop;
import com.google.errorprone.bugpatterns.ThreadLocalUsage;
import com.google.errorprone.bugpatterns.ThreeLetterTimeZoneID;
import com.google.errorprone.bugpatterns.ThrowIfUncheckedKnownChecked;
import com.google.errorprone.bugpatterns.ThrowIfUncheckedKnownUnchecked;
import com.google.errorprone.bugpatterns.ThrowNull;
import com.google.errorprone.bugpatterns.ThrowSpecificExceptions;
import com.google.errorprone.bugpatterns.ThrowsUncheckedException;
import com.google.errorprone.bugpatterns.ToStringReturnsNull;
import com.google.errorprone.bugpatterns.TooManyParameters;
import com.google.errorprone.bugpatterns.TraditionalSwitchExpression;
import com.google.errorprone.bugpatterns.TransientMisuse;
import com.google.errorprone.bugpatterns.TreeToString;
import com.google.errorprone.bugpatterns.TruthAssertExpected;
import com.google.errorprone.bugpatterns.TruthConstantAsserts;
import com.google.errorprone.bugpatterns.TruthContainsExactlyElementsInUsage;
import com.google.errorprone.bugpatterns.TruthGetOrDefault;
import com.google.errorprone.bugpatterns.TryFailRefactoring;
import com.google.errorprone.bugpatterns.TryFailThrowable;
import com.google.errorprone.bugpatterns.TryWithResourcesVariable;
import com.google.errorprone.bugpatterns.TypeEqualsChecker;
import com.google.errorprone.bugpatterns.TypeNameShadowing;
import com.google.errorprone.bugpatterns.TypeParameterNaming;
import com.google.errorprone.bugpatterns.TypeParameterQualifier;
import com.google.errorprone.bugpatterns.TypeParameterShadowing;
import com.google.errorprone.bugpatterns.TypeParameterUnusedInFormals;
import com.google.errorprone.bugpatterns.TypeToString;
import com.google.errorprone.bugpatterns.URLEqualsHashCode;
import com.google.errorprone.bugpatterns.UndefinedEquals;
import com.google.errorprone.bugpatterns.UngroupedOverloads;
import com.google.errorprone.bugpatterns.UnicodeDirectionalityCharacters;
import com.google.errorprone.bugpatterns.UnicodeEscape;
import com.google.errorprone.bugpatterns.UnicodeInCode;
import com.google.errorprone.bugpatterns.UnnecessarilyFullyQualified;
import com.google.errorprone.bugpatterns.UnnecessarilyVisible;
import com.google.errorprone.bugpatterns.UnnecessaryAnonymousClass;
import com.google.errorprone.bugpatterns.UnnecessaryAssignment;
import com.google.errorprone.bugpatterns.UnnecessaryAsync;
import com.google.errorprone.bugpatterns.UnnecessaryBoxedAssignment;
import com.google.errorprone.bugpatterns.UnnecessaryBoxedVariable;
import com.google.errorprone.bugpatterns.UnnecessaryBreakInSwitch;
import com.google.errorprone.bugpatterns.UnnecessaryCopy;
import com.google.errorprone.bugpatterns.UnnecessaryDefaultInEnumSwitch;
import com.google.errorprone.bugpatterns.UnnecessaryFinal;
import com.google.errorprone.bugpatterns.UnnecessaryLambda;
import com.google.errorprone.bugpatterns.UnnecessaryLongToIntConversion;
import com.google.errorprone.bugpatterns.UnnecessaryMethodInvocationMatcher;
import com.google.errorprone.bugpatterns.UnnecessaryMethodReference;
import com.google.errorprone.bugpatterns.UnnecessaryOptionalGet;
import com.google.errorprone.bugpatterns.UnnecessaryParentheses;
import com.google.errorprone.bugpatterns.UnnecessaryQualifier;
import com.google.errorprone.bugpatterns.UnnecessarySetDefault;
import com.google.errorprone.bugpatterns.UnnecessaryStaticImport;
import com.google.errorprone.bugpatterns.UnnecessaryStringBuilder;
import com.google.errorprone.bugpatterns.UnnecessaryTestMethodPrefix;
import com.google.errorprone.bugpatterns.UnnecessaryTypeArgument;
import com.google.errorprone.bugpatterns.UnsafeFinalization;
import com.google.errorprone.bugpatterns.UnsafeLocaleUsage;
import com.google.errorprone.bugpatterns.UnsafeReflectiveConstructionCast;
import com.google.errorprone.bugpatterns.UnsynchronizedOverridesSynchronized;
import com.google.errorprone.bugpatterns.UnusedAnonymousClass;
import com.google.errorprone.bugpatterns.UnusedCollectionModifiedInPlace;
import com.google.errorprone.bugpatterns.UnusedException;
import com.google.errorprone.bugpatterns.UnusedLabel;
import com.google.errorprone.bugpatterns.UnusedMethod;
import com.google.errorprone.bugpatterns.UnusedNestedClass;
import com.google.errorprone.bugpatterns.UnusedTypeParameter;
import com.google.errorprone.bugpatterns.UnusedVariable;
import com.google.errorprone.bugpatterns.UseCorrectAssertInTests;
import com.google.errorprone.bugpatterns.UseEnumSwitch;
import com.google.errorprone.bugpatterns.VarChecker;
import com.google.errorprone.bugpatterns.VarTypeName;
import com.google.errorprone.bugpatterns.VariableNameSameAsType;
import com.google.errorprone.bugpatterns.Varifier;
import com.google.errorprone.bugpatterns.VoidUsed;
import com.google.errorprone.bugpatterns.WaitNotInLoop;
import com.google.errorprone.bugpatterns.WildcardImport;
import com.google.errorprone.bugpatterns.WithSignatureDiscouraged;
import com.google.errorprone.bugpatterns.WrongOneof;
import com.google.errorprone.bugpatterns.XorPower;
import com.google.errorprone.bugpatterns.YodaCondition;
import com.google.errorprone.bugpatterns.android.BinderIdentityRestoredDangerously;
import com.google.errorprone.bugpatterns.android.BundleDeserializationCast;
import com.google.errorprone.bugpatterns.android.FragmentInjection;
import com.google.errorprone.bugpatterns.android.FragmentNotInstantiable;
import com.google.errorprone.bugpatterns.android.HardCodedSdCardPath;
import com.google.errorprone.bugpatterns.android.IsLoggableTagLength;
import com.google.errorprone.bugpatterns.android.MislabeledAndroidString;
import com.google.errorprone.bugpatterns.android.ParcelableCreator;
import com.google.errorprone.bugpatterns.android.RectIntersectReturnValueIgnored;
import com.google.errorprone.bugpatterns.android.StaticOrDefaultInterfaceMethod;
import com.google.errorprone.bugpatterns.android.WakelockReleasedDangerously;
import com.google.errorprone.bugpatterns.apidiff.AndroidJdkLibsChecker;
import com.google.errorprone.bugpatterns.apidiff.Java8ApiChecker;
import com.google.errorprone.bugpatterns.argumentselectiondefects.ArgumentSelectionDefectChecker;
import com.google.errorprone.bugpatterns.argumentselectiondefects.AssertEqualsArgumentOrderChecker;
import com.google.errorprone.bugpatterns.argumentselectiondefects.AutoValueConstructorOrderChecker;
import com.google.errorprone.bugpatterns.checkreturnvalue.BuilderReturnThis;
import com.google.errorprone.bugpatterns.checkreturnvalue.CanIgnoreReturnValueSuggester;
import com.google.errorprone.bugpatterns.checkreturnvalue.NoCanIgnoreReturnValueOnClasses;
import com.google.errorprone.bugpatterns.checkreturnvalue.UnnecessarilyUsedValue;
import com.google.errorprone.bugpatterns.checkreturnvalue.UsingJsr305CheckReturnValue;
import com.google.errorprone.bugpatterns.collectionincompatibletype.CollectionIncompatibleType;
import com.google.errorprone.bugpatterns.collectionincompatibletype.CollectionUndefinedEquality;
import com.google.errorprone.bugpatterns.collectionincompatibletype.CompatibleWithMisuse;
import com.google.errorprone.bugpatterns.collectionincompatibletype.IncompatibleArgumentType;
import com.google.errorprone.bugpatterns.collectionincompatibletype.JUnitIncompatibleType;
import com.google.errorprone.bugpatterns.collectionincompatibletype.TruthIncompatibleType;
import com.google.errorprone.bugpatterns.flogger.FloggerArgumentToString;
import com.google.errorprone.bugpatterns.flogger.FloggerFormatString;
import com.google.errorprone.bugpatterns.flogger.FloggerLogString;
import com.google.errorprone.bugpatterns.flogger.FloggerLogVarargs;
import com.google.errorprone.bugpatterns.flogger.FloggerLogWithCause;
import com.google.errorprone.bugpatterns.flogger.FloggerMessageFormat;
import com.google.errorprone.bugpatterns.flogger.FloggerPerWithoutRateLimit;
import com.google.errorprone.bugpatterns.flogger.FloggerRedundantIsEnabled;
import com.google.errorprone.bugpatterns.flogger.FloggerRequiredModifiers;
import com.google.errorprone.bugpatterns.flogger.FloggerSplitLogStatement;
import com.google.errorprone.bugpatterns.flogger.FloggerStringConcatenation;
import com.google.errorprone.bugpatterns.flogger.FloggerWithCause;
import com.google.errorprone.bugpatterns.flogger.FloggerWithoutCause;
import com.google.errorprone.bugpatterns.formatstring.AnnotateFormatMethod;
import com.google.errorprone.bugpatterns.formatstring.FormatString;
import com.google.errorprone.bugpatterns.formatstring.FormatStringAnnotationChecker;
import com.google.errorprone.bugpatterns.formatstring.InlineFormatString;
import com.google.errorprone.bugpatterns.formatstring.LenientFormatStringValidation;
import com.google.errorprone.bugpatterns.inject.AssistedInjectAndInjectOnConstructors;
import com.google.errorprone.bugpatterns.inject.AssistedInjectAndInjectOnSameConstructor;
import com.google.errorprone.bugpatterns.inject.AutoFactoryAtInject;
import com.google.errorprone.bugpatterns.inject.CloseableProvides;
import com.google.errorprone.bugpatterns.inject.InjectOnConstructorOfAbstractClass;
import com.google.errorprone.bugpatterns.inject.InjectOnMemberAndConstructor;
import com.google.errorprone.bugpatterns.inject.InjectedConstructorAnnotations;
import com.google.errorprone.bugpatterns.inject.InvalidTargetingOnScopingAnnotation;
import com.google.errorprone.bugpatterns.inject.JavaxInjectOnAbstractMethod;
import com.google.errorprone.bugpatterns.inject.JavaxInjectOnFinalField;
import com.google.errorprone.bugpatterns.inject.MisplacedScopeAnnotations;
import com.google.errorprone.bugpatterns.inject.MissingRuntimeRetention;
import com.google.errorprone.bugpatterns.inject.MoreThanOneInjectableConstructor;
import com.google.errorprone.bugpatterns.inject.MoreThanOneQualifier;
import com.google.errorprone.bugpatterns.inject.MoreThanOneScopeAnnotationOnClass;
import com.google.errorprone.bugpatterns.inject.OverlappingQualifierAndScopeAnnotation;
import com.google.errorprone.bugpatterns.inject.QualifierOrScopeOnInjectMethod;
import com.google.errorprone.bugpatterns.inject.QualifierWithTypeUse;
import com.google.errorprone.bugpatterns.inject.ScopeAnnotationOnInterfaceOrAbstractClass;
import com.google.errorprone.bugpatterns.inject.dagger.AndroidInjectionBeforeSuper;
import com.google.errorprone.bugpatterns.inject.dagger.EmptySetMultibindingContributions;
import com.google.errorprone.bugpatterns.inject.dagger.PrivateConstructorForNoninstantiableModule;
import com.google.errorprone.bugpatterns.inject.dagger.ProvidesNull;
import com.google.errorprone.bugpatterns.inject.dagger.RefersToDaggerCodegen;
import com.google.errorprone.bugpatterns.inject.dagger.ScopeOnModule;
import com.google.errorprone.bugpatterns.inject.dagger.UseBinds;
import com.google.errorprone.bugpatterns.inject.guice.AssistedInjectScoping;
import com.google.errorprone.bugpatterns.inject.guice.AssistedParameters;
import com.google.errorprone.bugpatterns.inject.guice.BindingToUnqualifiedCommonType;
import com.google.errorprone.bugpatterns.inject.guice.InjectOnFinalField;
import com.google.errorprone.bugpatterns.inject.guice.OverridesGuiceInjectableMethod;
import com.google.errorprone.bugpatterns.inject.guice.OverridesJavaxInjectableMethod;
import com.google.errorprone.bugpatterns.inject.guice.ProvidesMethodOutsideOfModule;
import com.google.errorprone.bugpatterns.inlineme.Inliner;
import com.google.errorprone.bugpatterns.inlineme.Suggester;
import com.google.errorprone.bugpatterns.inlineme.Validator;
import com.google.errorprone.bugpatterns.javadoc.AlmostJavadoc;
import com.google.errorprone.bugpatterns.javadoc.EmptyBlockTag;
import com.google.errorprone.bugpatterns.javadoc.EscapedEntity;
import com.google.errorprone.bugpatterns.javadoc.InheritDoc;
import com.google.errorprone.bugpatterns.javadoc.InvalidBlockTag;
import com.google.errorprone.bugpatterns.javadoc.InvalidInlineTag;
import com.google.errorprone.bugpatterns.javadoc.InvalidLink;
import com.google.errorprone.bugpatterns.javadoc.InvalidParam;
import com.google.errorprone.bugpatterns.javadoc.InvalidSnippet;
import com.google.errorprone.bugpatterns.javadoc.InvalidThrows;
import com.google.errorprone.bugpatterns.javadoc.InvalidThrowsLink;
import com.google.errorprone.bugpatterns.javadoc.MalformedInlineTag;
import com.google.errorprone.bugpatterns.javadoc.MissingSummary;
import com.google.errorprone.bugpatterns.javadoc.NotJavadoc;
import com.google.errorprone.bugpatterns.javadoc.ReturnFromVoid;
import com.google.errorprone.bugpatterns.javadoc.UnescapedEntity;
import com.google.errorprone.bugpatterns.javadoc.UnrecognisedJavadocTag;
import com.google.errorprone.bugpatterns.javadoc.UrlInSee;
import com.google.errorprone.bugpatterns.nullness.AddNullMarkedToPackageInfo;
import com.google.errorprone.bugpatterns.nullness.DereferenceWithNullBranch;
import com.google.errorprone.bugpatterns.nullness.EqualsBrokenForNull;
import com.google.errorprone.bugpatterns.nullness.EqualsMissingNullable;
import com.google.errorprone.bugpatterns.nullness.ExtendsObject;
import com.google.errorprone.bugpatterns.nullness.FieldMissingNullable;
import com.google.errorprone.bugpatterns.nullness.MultipleNullnessAnnotations;
import com.google.errorprone.bugpatterns.nullness.NullArgumentForNonNullParameter;
import com.google.errorprone.bugpatterns.nullness.NullablePrimitive;
import com.google.errorprone.bugpatterns.nullness.NullablePrimitiveArray;
import com.google.errorprone.bugpatterns.nullness.NullableTypeParameter;
import com.google.errorprone.bugpatterns.nullness.NullableWildcard;
import com.google.errorprone.bugpatterns.nullness.ParameterMissingNullable;
import com.google.errorprone.bugpatterns.nullness.RedundantNullCheck;
import com.google.errorprone.bugpatterns.nullness.ReturnMissingNullable;
import com.google.errorprone.bugpatterns.nullness.UnnecessaryCheckNotNull;
import com.google.errorprone.bugpatterns.nullness.UnsafeWildcard;
import com.google.errorprone.bugpatterns.nullness.VoidMissingNullable;
import com.google.errorprone.bugpatterns.overloading.InconsistentOverloads;
import com.google.errorprone.bugpatterns.threadsafety.DoubleCheckedLocking;
import com.google.errorprone.bugpatterns.threadsafety.GuardedByChecker;
import com.google.errorprone.bugpatterns.threadsafety.ImmutableAnnotationChecker;
import com.google.errorprone.bugpatterns.threadsafety.ImmutableChecker;
import com.google.errorprone.bugpatterns.threadsafety.ImmutableEnumChecker;
import com.google.errorprone.bugpatterns.threadsafety.ImmutableRefactoring;
import com.google.errorprone.bugpatterns.threadsafety.StaticGuardedByInstance;
import com.google.errorprone.bugpatterns.threadsafety.SynchronizeOnNonFinalField;
import com.google.errorprone.bugpatterns.threadsafety.ThreadPriorityCheck;
import com.google.errorprone.bugpatterns.threadsafety.ThreadSafeRefactoring;
import com.google.errorprone.bugpatterns.time.DateChecker;
import com.google.errorprone.bugpatterns.time.DurationFrom;
import com.google.errorprone.bugpatterns.time.DurationGetTemporalUnit;
import com.google.errorprone.bugpatterns.time.DurationTemporalUnit;
import com.google.errorprone.bugpatterns.time.DurationToLongTimeUnit;
import com.google.errorprone.bugpatterns.time.FromTemporalAccessor;
import com.google.errorprone.bugpatterns.time.InstantTemporalUnit;
import com.google.errorprone.bugpatterns.time.InvalidJavaTimeConstant;
import com.google.errorprone.bugpatterns.time.JavaDurationGetSecondsGetNano;
import com.google.errorprone.bugpatterns.time.JavaDurationGetSecondsToToSeconds;
import com.google.errorprone.bugpatterns.time.JavaDurationWithNanos;
import com.google.errorprone.bugpatterns.time.JavaDurationWithSeconds;
import com.google.errorprone.bugpatterns.time.JavaInstantGetSecondsGetNano;
import com.google.errorprone.bugpatterns.time.JavaLocalDateTimeGetNano;
import com.google.errorprone.bugpatterns.time.JavaLocalTimeGetNano;
import com.google.errorprone.bugpatterns.time.JavaPeriodGetDays;
import com.google.errorprone.bugpatterns.time.JavaTimeDefaultTimeZone;
import com.google.errorprone.bugpatterns.time.JodaConstructors;
import com.google.errorprone.bugpatterns.time.JodaDateTimeConstants;
import com.google.errorprone.bugpatterns.time.JodaDurationWithMillis;
import com.google.errorprone.bugpatterns.time.JodaInstantWithMillis;
import com.google.errorprone.bugpatterns.time.JodaNewPeriod;
import com.google.errorprone.bugpatterns.time.JodaPlusMinusLong;
import com.google.errorprone.bugpatterns.time.JodaTimeConverterManager;
import com.google.errorprone.bugpatterns.time.JodaToSelf;
import com.google.errorprone.bugpatterns.time.JodaWithDurationAddedLong;
import com.google.errorprone.bugpatterns.time.LocalDateTemporalAmount;
import com.google.errorprone.bugpatterns.time.PeriodFrom;
import com.google.errorprone.bugpatterns.time.PeriodGetTemporalUnit;
import com.google.errorprone.bugpatterns.time.PeriodTimeMath;
import com.google.errorprone.bugpatterns.time.PreferJavaTimeOverload;
import com.google.errorprone.bugpatterns.time.ProtoDurationGetSecondsGetNano;
import com.google.errorprone.bugpatterns.time.ProtoTimestampGetSecondsGetNano;
import com.google.errorprone.bugpatterns.time.StronglyTypeTime;
import com.google.errorprone.bugpatterns.time.TemporalAccessorGetChronoField;
import com.google.errorprone.bugpatterns.time.TimeInStaticInitializer;
import com.google.errorprone.bugpatterns.time.TimeUnitConversionChecker;
import com.google.errorprone.bugpatterns.time.TimeUnitMismatch;
import com.google.errorprone.bugpatterns.time.ZoneIdOfZ;
import java.util.Arrays;

/**
 * Static helper class that provides {@link ScannerSupplier}s and {@link BugChecker}s for the
 * built-in Error Prone checks, as opposed to plugin checks or checks used in tests.
 */
public class BuiltInCheckerSuppliers {
  @SafeVarargs
  public static ImmutableSet<BugCheckerInfo> getSuppliers(Class<? extends BugChecker>... checkers) {
    return getSuppliers(Arrays.asList(checkers));
  }

  public static ImmutableSet<BugCheckerInfo> getSuppliers(
      Iterable<Class<? extends BugChecker>> checkers) {
    return Streams.stream(checkers)
        .map(BugCheckerInfo::create)
        .collect(ImmutableSet.toImmutableSet());
  }

  /** Returns a {@link ScannerSupplier} with all {@link BugChecker}s in Error Prone. */
  public static ScannerSupplier allChecks() {
    return ScannerSupplier.fromBugCheckerInfos(
        Iterables.concat(ENABLED_ERRORS, ENABLED_WARNINGS, DISABLED_CHECKS));
  }

  /**
   * Returns a {@link ScannerSupplier} with the {@link BugChecker}s that are in the ENABLED lists.
   */
  public static ScannerSupplier defaultChecks() {
    return allChecks()
        .filter(Predicates.or(Predicates.in(ENABLED_ERRORS), Predicates.in(ENABLED_WARNINGS)));
  }

  /**
   * Returns a {@link ScannerSupplier} with the {@link BugChecker}s that are in the ENABLED_ERRORS
   * list.
   */
  public static ScannerSupplier errorChecks() {
    return allChecks().filter(Predicates.in(ENABLED_ERRORS));
  }

  /**
   * Returns a {@link ScannerSupplier} with the {@link BugChecker}s that are in the ENABLED_WARNINGS
   * list.
   */
  public static ScannerSupplier warningChecks() {
    return allChecks().filter(Predicates.in(ENABLED_WARNINGS));
  }

  /** A list of all checks with severity ERROR that are on by default. */
  public static final ImmutableSet<BugCheckerInfo> ENABLED_ERRORS =
      getSuppliers(
          // keep-sorted start
          AlwaysThrows.class,
          AndroidInjectionBeforeSuper.class,
          ArrayEquals.class,
          ArrayFillIncompatibleType.class,
          ArrayHashCode.class,
          ArrayToString.class,
          ArraysAsListPrimitiveArray.class,
          AssistedInjectScoping.class,
          AssistedParameters.class,
          AsyncCallableReturnsNull.class,
          AsyncFunctionReturnsNull.class,
          AutoValueBuilderDefaultsInConstructor.class,
          AutoValueConstructorOrderChecker.class,
          BadAnnotationImplementation.class,
          BadShiftAmount.class,
          BanJNDI.class,
          BoxedPrimitiveEquality.class,
          BundleDeserializationCast.class,
          ChainingConstructorIgnoresParameter.class,
          CheckNotNullMultipleTimes.class,
          CheckReturnValue.class,
          CollectionIncompatibleType.class,
          CollectionToArraySafeParameter.class,
          ComparableType.class,
          ComparingThisWithNull.class,
          ComparisonOutOfRange.class,
          CompatibleWithMisuse.class,
          CompileTimeConstantChecker.class,
          ComputeIfAbsentAmbiguousReference.class,
          ConditionalExpressionNumericPromotion.class,
          ConstantOverflow.class,
          DangerousLiteralNullChecker.class,
          DeadException.class,
          DeadThread.class,
          DereferenceWithNullBranch.class,
          DiscardedPostfixExpression.class,
          DoNotCallChecker.class,
          DoNotMockChecker.class,
          DoubleBraceInitialization.class,
          DuplicateMapKeys.class,
          DurationFrom.class,
          DurationGetTemporalUnit.class,
          DurationTemporalUnit.class,
          DurationToLongTimeUnit.class,
          EqualsHashCode.class,
          EqualsNaN.class,
          EqualsNull.class,
          EqualsReference.class,
          EqualsWrongThing.class,
          FloggerFormatString.class,
          FloggerLogString.class,
          FloggerLogVarargs.class,
          FloggerSplitLogStatement.class,
          ForOverrideChecker.class,
          FormatString.class,
          FormatStringAnnotationChecker.class,
          FromTemporalAccessor.class,
          FunctionalInterfaceMethodChanged.class,
          FuturesGetCheckedIllegalExceptionType.class,
          FuzzyEqualsShouldNotBeUsedInEqualsMethod.class,
          GetClassOnAnnotation.class,
          GetClassOnClass.class,
          GuardedByChecker.class,
          HashtableContains.class,
          IdentityBinaryExpression.class,
          IdentityHashMapBoxing.class,
          ImmutableChecker.class,
          ImpossibleNullComparison.class,
          Incomparable.class,
          IncompatibleArgumentType.class,
          IncompatibleModifiersChecker.class,
          IndexOfChar.class,
          InexactVarargsConditional.class,
          InfiniteRecursion.class,
          InjectOnFinalField.class,
          InjectOnMemberAndConstructor.class,
          InstantTemporalUnit.class,
          InvalidJavaTimeConstant.class,
          InvalidPatternSyntax.class,
          InvalidTimeZoneID.class,
          InvalidZoneId.class,
          IsInstanceIncompatibleType.class,
          IsInstanceOfClass.class,
          IsLoggableTagLength.class,
          JUnit3TestNotRun.class,
          JUnit4ClassAnnotationNonStatic.class,
          JUnit4SetUpNotRun.class,
          JUnit4TearDownNotRun.class,
          JUnit4TestNotRun.class,
          JUnit4TestsNotRunWithinEnclosed.class,
          JUnitAssertSameCheck.class,
          JUnitParameterMethodNotFound.class,
          JavaxInjectOnAbstractMethod.class,
          JodaToSelf.class,
          LenientFormatStringValidation.class,
          LiteByteStringUtf8.class,
          LocalDateTemporalAmount.class,
          LockOnBoxedPrimitive.class,
          LoopConditionChecker.class,
          LossyPrimitiveCompare.class,
          MathRoundIntLong.class,
          MislabeledAndroidString.class,
          MisleadingEmptyVarargs.class,
          MisleadingEscapedSpace.class,
          MisplacedScopeAnnotations.class,
          MissingSuperCall.class,
          MissingTestCall.class,
          MisusedDayOfYear.class,
          MisusedWeekYear.class,
          MixedDescriptors.class,
          MockitoUsage.class,
          ModifyingCollectionWithItself.class,
          MoreThanOneInjectableConstructor.class,
          MoreThanOneScopeAnnotationOnClass.class,
          MustBeClosedChecker.class,
          NCopiesOfChar.class,
          NoCanIgnoreReturnValueOnClasses.class,
          NonCanonicalStaticImport.class,
          NonFinalCompileTimeConstant.class,
          NonRuntimeAnnotation.class,
          NullArgumentForNonNullParameter.class,
          NullTernary.class,
          NullableOnContainingClass.class,
          OptionalEquality.class,
          OptionalMapUnusedValue.class,
          OptionalOfRedundantMethod.class,
          OverlappingQualifierAndScopeAnnotation.class,
          OverridesJavaxInjectableMethod.class,
          PackageInfo.class,
          ParametersButNotParameterized.class,
          ParcelableCreator.class,
          PeriodFrom.class,
          PeriodGetTemporalUnit.class,
          PeriodTimeMath.class,
          PreconditionsInvalidPlaceholder.class,
          PrivateSecurityContractProtoAccess.class,
          ProtoBuilderReturnValueIgnored.class,
          ProtoStringFieldReferenceEquality.class,
          ProtoTruthMixedDescriptors.class,
          ProtocolBufferOrdinal.class,
          ProvidesMethodOutsideOfModule.class,
          ProvidesNull.class,
          RandomCast.class,
          RandomModInteger.class,
          RectIntersectReturnValueIgnored.class,
          RedundantSetterCall.class,
          RequiredModifiersChecker.class,
          RestrictedApiChecker.class,
          ReturnValueIgnored.class,
          SelfAssertion.class,
          SelfAssignment.class,
          SelfComparison.class,
          SelfEquals.class,
          SetUnrecognized.class,
          ShouldHaveEvenArgs.class,
          SizeGreaterThanOrEqualsZero.class,
          StreamToString.class,
          StringBuilderInitWithChar.class,
          StringJoin.class,
          SubstringOfZero.class,
          SuppressWarningsDeprecated.class,
          TemporalAccessorGetChronoField.class,
          TestParametersNotInitialized.class,
          TheoryButNoTheories.class,
          ThreadBuilderNameWithPlaceholder.class,
          ThrowIfUncheckedKnownChecked.class,
          ThrowNull.class,
          TreeToString.class,
          TryFailThrowable.class,
          TypeParameterQualifier.class,
          UnicodeDirectionalityCharacters.class,
          UnicodeInCode.class,
          UnnecessaryCheckNotNull.class,
          UnnecessaryTypeArgument.class,
          UnsafeWildcard.class,
          UnusedAnonymousClass.class,
          UnusedCollectionModifiedInPlace.class,
          Validator.class,
          VarTypeName.class,
          WrongOneof.class,
          XorPower.class,
          ZoneIdOfZ.class
          // keep-sorted end
          );

  /** A list of all checks with severity WARNING that are on by default. */
  public static final ImmutableSet<BugCheckerInfo> ENABLED_WARNINGS =
      getSuppliers(
          // keep-sorted start
          ASTHelpersSuggestions.class,
          AddressSelection.class,
          AlmostJavadoc.class,
          AlreadyChecked.class,
          AmbiguousMethodReference.class,
          AnnotateFormatMethod.class,
          ArgumentSelectionDefectChecker.class,
          ArrayAsKeyOfSetOrMap.class,
          ArrayRecordComponent.class,
          AssertEqualsArgumentOrderChecker.class,
          AssertThrowsMultipleStatements.class,
          AssertionFailureIgnored.class,
          AssignmentExpression.class,
          AssistedInjectAndInjectOnSameConstructor.class,
          AttemptedNegativeZero.class,
          AutoValueBoxedValues.class,
          AutoValueFinalMethods.class,
          AutoValueImmutableFields.class,
          AutoValueSubclassLeaked.class,
          BadComparable.class,
          BadImport.class,
          BadInstanceof.class,
          BareDotMetacharacter.class,
          BigDecimalEquals.class,
          BigDecimalLiteralDouble.class,
          BooleanLiteral.class,
          BoxedPrimitiveConstructor.class,
          BugPatternNaming.class,
          ByteBufferBackingArray.class,
          CacheLoaderNull.class,
          CanonicalDuration.class,
          CatchAndPrintStackTrace.class,
          CatchFail.class,
          ChainedAssertionLosesContext.class,
          CharacterGetNumericValue.class,
          ClassCanBeStatic.class,
          ClassInitializationDeadlock.class,
          ClassNewInstance.class,
          CloseableProvides.class,
          ClosingStandardOutputStreams.class,
          CollectionUndefinedEquality.class,
          CollectorShouldNotUseState.class,
          ComparableAndComparator.class,
          CompareToZero.class,
          ComplexBooleanConstant.class,
          DateChecker.class,
          DateFormatConstant.class,
          DeeplyNested.class,
          DefaultCharset.class,
          DefaultPackage.class,
          DeprecatedVariable.class,
          DirectInvocationOnMock.class,
          DistinctVarargsChecker.class,
          DoNotCallSuggester.class,
          DoNotClaimAnnotations.class,
          DoNotMockAutoValue.class,
          DoubleCheckedLocking.class,
          DuplicateBranches.class,
          DuplicateDateFormatField.class,
          EffectivelyPrivate.class,
          EmptyBlockTag.class,
          EmptyCatch.class,
          EmptySetMultibindingContributions.class,
          EmptyTopLevelDeclaration.class,
          EnumOrdinal.class,
          EqualsGetClass.class,
          EqualsIncompatibleType.class,
          EqualsUnsafeCast.class,
          EqualsUsingHashCode.class,
          ErroneousBitwiseExpression.class,
          ErroneousThreadPoolConstructorChecker.class,
          EscapedEntity.class,
          ExpensiveLenientFormatString.class,
          ExtendingJUnitAssert.class,
          ExtendsObject.class,
          FallThrough.class,
          Finalize.class,
          Finally.class,
          FloatCast.class,
          FloatingPointAssertionWithinEpsilon.class,
          FloatingPointLiteralPrecision.class,
          FloggerArgumentToString.class,
          FloggerPerWithoutRateLimit.class,
          FloggerStringConcatenation.class,
          FragmentInjection.class,
          FragmentNotInstantiable.class,
          FutureReturnValueIgnored.class,
          FutureTransformAsync.class,
          GetClassOnEnum.class,
          GuiceNestedCombine.class,
          HidingField.class,
          ICCProfileGetInstance.class,
          IdentityHashMapUsage.class,
          IgnoredPureGetter.class,
          ImmutableAnnotationChecker.class,
          ImmutableEnumChecker.class,
          InconsistentCapitalization.class,
          InconsistentHashCode.class,
          IncorrectMainMethod.class,
          IncrementInForLoopAndHeader.class,
          InheritDoc.class,
          InjectOnBugCheckers.class,
          InjectOnConstructorOfAbstractClass.class,
          InjectedConstructorAnnotations.class,
          InlineFormatString.class,
          InlineTrivialConstant.class,
          Inliner.class,
          InputStreamSlowMultibyteRead.class,
          InstanceOfAndCastMatchWrongType.class,
          IntFloatConversion.class,
          IntLiteralCast.class,
          IntLongMath.class,
          InvalidBlockTag.class,
          InvalidInlineTag.class,
          InvalidLink.class,
          InvalidParam.class,
          InvalidSnippet.class,
          InvalidTargetingOnScopingAnnotation.class,
          InvalidThrows.class,
          InvalidThrowsLink.class,
          IterableAndIterator.class,
          JUnit3FloatingPointComparisonWithoutDelta.class,
          JUnit4ClassUsedInJUnit3.class,
          JUnit4EmptyMethods.class,
          JUnitAmbiguousTestClass.class,
          JUnitIncompatibleType.class,
          JavaDurationGetSecondsGetNano.class,
          JavaDurationGetSecondsToToSeconds.class,
          JavaDurationWithNanos.class,
          JavaDurationWithSeconds.class,
          JavaInstantGetSecondsGetNano.class,
          JavaLangClash.class,
          JavaLocalDateTimeGetNano.class,
          JavaLocalTimeGetNano.class,
          JavaPeriodGetDays.class,
          JavaTimeDefaultTimeZone.class,
          JavaUtilDateChecker.class,
          JavaxInjectOnFinalField.class,
          JdkObsolete.class,
          JodaConstructors.class,
          JodaDateTimeConstants.class,
          JodaDurationWithMillis.class,
          JodaInstantWithMillis.class,
          JodaNewPeriod.class,
          JodaPlusMinusLong.class,
          JodaTimeConverterManager.class,
          JodaWithDurationAddedLong.class,
          LabelledBreakTarget.class,
          LiteEnumValueOf.class,
          LiteProtoToString.class,
          LockNotBeforeTry.class,
          LockOnNonEnclosingClassLiteral.class,
          LogicalAssignment.class,
          LongDoubleConversion.class,
          LongFloatConversion.class,
          LoopOverCharArray.class,
          MalformedInlineTag.class,
          MathAbsoluteNegative.class,
          MemoizeConstantVisitorStateLookups.class,
          MisformattedTestData.class,
          MissingCasesInEnumSwitch.class,
          MissingFail.class,
          MissingImplementsComparable.class,
          MissingOverride.class,
          MissingRefasterAnnotation.class,
          MissingSummary.class,
          MixedMutabilityReturnType.class,
          MockIllegalThrows.class,
          MockNotUsedInProduction.class,
          ModifiedButNotUsed.class,
          ModifyCollectionInEnhancedForLoop.class,
          ModifySourceCollectionInStream.class,
          MultimapKeys.class,
          MultipleNullnessAnnotations.class,
          MultipleParallelOrSequentialCalls.class,
          MultipleUnaryOperatorsInMethodCall.class,
          MutablePublicArray.class,
          NamedLikeContextualKeyword.class,
          NarrowCalculation.class,
          NarrowingCompoundAssignment.class,
          NegativeCharLiteral.class,
          NestedInstanceOfConditions.class,
          NewFileSystem.class,
          NonApiType.class,
          NonAtomicVolatileUpdate.class,
          NonCanonicalType.class,
          NonOverridingEquals.class,
          NotJavadoc.class,
          NullOptional.class,
          NullableConstructor.class,
          NullableOptional.class,
          NullablePrimitive.class,
          NullablePrimitiveArray.class,
          NullableTypeParameter.class,
          NullableVoid.class,
          NullableWildcard.class,
          ObjectEqualsForPrimitives.class,
          ObjectToString.class,
          ObjectsHashCodePrimitive.class,
          OperatorPrecedence.class,
          OptionalMapToOptional.class,
          OptionalNotPresent.class,
          OrphanedFormatString.class,
          OutlineNone.class,
          OverrideThrowableToString.class,
          Overrides.class,
          OverridesGuiceInjectableMethod.class,
          OverridingMethodInconsistentArgumentNamesChecker.class,
          ParameterName.class,
          PatternMatchingInstanceof.class,
          PreconditionsCheckNotNullRepeated.class,
          PreferInstanceofOverGetKind.class,
          PrimitiveAtomicReference.class,
          ProtectedMembersInFinalClass.class,
          ProtoDurationGetSecondsGetNano.class,
          ProtoTimestampGetSecondsGetNano.class,
          QualifierOrScopeOnInjectMethod.class,
          ReachabilityFenceUsage.class,
          RedundantControlFlow.class,
          ReferenceEquality.class,
          RethrowReflectiveOperationExceptionAsLinkageError.class,
          ReturnAtTheEndOfVoidFunction.class,
          ReturnFromVoid.class,
          RobolectricShadowDirectlyOn.class,
          RuleNotRun.class,
          RxReturnValueIgnored.class,
          SameNameButDifferent.class,
          ScopeAnnotationOnInterfaceOrAbstractClass.class,
          SelfAlwaysReturnsThis.class,
          SelfSet.class,
          ShortCircuitBoolean.class,
          StatementSwitchToExpressionSwitch.class,
          StaticAssignmentInConstructor.class,
          StaticAssignmentOfThrowable.class,
          StaticGuardedByInstance.class,
          StaticMockMember.class,
          StreamResourceLeak.class,
          StreamToIterable.class,
          StringCaseLocaleUsage.class,
          StringCharset.class,
          StringConcatToTextBlock.class,
          StringSplitter.class,
          Suggester.class,
          SuperCallToObjectMethod.class,
          SwigMemoryLeak.class,
          SynchronizeOnNonFinalField.class,
          SystemConsoleNull.class,
          ThreadJoinLoop.class,
          ThreadLocalUsage.class,
          ThreadPriorityCheck.class,
          ThreeLetterTimeZoneID.class,
          ThrowIfUncheckedKnownUnchecked.class,
          TimeInStaticInitializer.class,
          TimeUnitConversionChecker.class,
          ToStringReturnsNull.class,
          TraditionalSwitchExpression.class,
          TruthAssertExpected.class,
          TruthConstantAsserts.class,
          TruthGetOrDefault.class,
          TruthIncompatibleType.class,
          TypeEqualsChecker.class,
          TypeNameShadowing.class,
          TypeParameterShadowing.class,
          TypeParameterUnusedInFormals.class,
          URLEqualsHashCode.class,
          UndefinedEquals.class,
          UnicodeEscape.class,
          UnnecessaryAssignment.class,
          UnnecessaryAsync.class,
          UnnecessaryBreakInSwitch.class,
          UnnecessaryCopy.class,
          UnnecessaryLambda.class,
          UnnecessaryLongToIntConversion.class,
          UnnecessaryMethodInvocationMatcher.class,
          UnnecessaryMethodReference.class,
          UnnecessaryParentheses.class,
          UnnecessaryQualifier.class,
          UnnecessaryStringBuilder.class,
          UnrecognisedJavadocTag.class,
          UnsafeFinalization.class,
          UnsafeReflectiveConstructionCast.class,
          UnsynchronizedOverridesSynchronized.class,
          UnusedLabel.class,
          UnusedMethod.class,
          UnusedNestedClass.class,
          UnusedTypeParameter.class,
          UnusedVariable.class,
          UseBinds.class,
          VariableNameSameAsType.class,
          VoidUsed.class,
          WaitNotInLoop.class,
          WakelockReleasedDangerously.class,
          WithSignatureDiscouraged.class
          // keep-sorted end
          );

  /** A list of all checks that are off by default. */
  public static final ImmutableSet<BugCheckerInfo> DISABLED_CHECKS =
      getSuppliers(
          // keep-sorted start
          AddNullMarkedToPackageInfo.class,
          AndroidJdkLibsChecker.class,
          AnnotationMirrorToString.class,
          AnnotationPosition.class,
          AnnotationValueToString.class,
          AssertFalse.class,
          AssistedInjectAndInjectOnConstructors.class,
          AutoFactoryAtInject.class,
          AvoidObjectArrays.class,
          BanClassLoader.class,
          BanSerializableRead.class,
          BinderIdentityRestoredDangerously.class, // TODO: enable this by default.
          BindingToUnqualifiedCommonType.class,
          BooleanParameter.class,
          BuilderReturnThis.class,
          CanIgnoreReturnValueSuggester.class,
          CannotMockFinalClass.class,
          CannotMockMethod.class,
          CatchingUnchecked.class,
          CheckedExceptionNotThrown.class,
          ClassName.class,
          ClassNamedLikeTypeParameter.class,
          ComparisonContractViolated.class,
          ConstantField.class,
          ConstantPatternCompile.class,
          DeduplicateConstants.class,
          DefaultLocale.class, // TODO: enable this by default.
          DepAnn.class,
          DifferentNameButSame.class,
          EmptyIfStatement.class,
          EqualsBrokenForNull.class,
          EqualsMissingNullable.class,
          ExpectedExceptionChecker.class,
          ExplicitArrayForVarargs.class,
          ExtendsAutoValue.class,
          FieldCanBeFinal.class,
          FieldCanBeLocal.class,
          FieldCanBeStatic.class,
          FieldMissingNullable.class,
          FloggerLogWithCause.class,
          FloggerMessageFormat.class,
          FloggerRedundantIsEnabled.class,
          FloggerRequiredModifiers.class,
          FloggerWithCause.class,
          FloggerWithoutCause.class,
          ForEachIterable.class,
          FunctionalInterfaceClash.class,
          HardCodedSdCardPath.class,
          IdentifierName.class,
          ImmutableMemberCollection.class,
          ImmutableRefactoring.class,
          ImmutableSetForContains.class,
          ImplementAssertionWithChaining.class,
          InconsistentOverloads.class,
          InitializeInline.class,
          InsecureCipherMode.class,
          InterfaceWithOnlyStatics.class,
          InterruptedExceptionSwallowed.class,
          Interruption.class,
          IterablePathParameter.class,
          Java8ApiChecker.class,
          LambdaFunctionalInterface.class,
          LongLiteralLowerCaseSuffix.class,
          MethodCanBeStatic.class,
          MissingBraces.class,
          MissingDefault.class,
          MissingRuntimeRetention.class,
          MixedArrayDimensions.class,
          MockitoDoSetup.class,
          MoreThanOneQualifier.class,
          MultiVariableDeclaration.class,
          MultipleTopLevelClasses.class,
          MutableGuiceModule.class,
          NegativeBoolean.class,
          NoAllocationChecker.class,
          NonCanonicalStaticMemberImport.class,
          NonFinalStaticField.class, // Intentionally disabled in OSS.
          PackageLocation.class,
          ParameterComment.class,
          ParameterMissingNullable.class,
          PreferJavaTimeOverload.class,
          PreferredInterfaceType.class,
          PrimitiveArrayPassedToVarargsMethod.class,
          PrivateConstructorForNoninstantiableModule.class,
          PrivateConstructorForUtilityClass.class,
          PublicApiNamedStreamShouldReturnStream.class,
          QualifierWithTypeUse.class,
          RedundantNullCheck.class,
          RedundantOverride.class,
          RedundantThrows.class,
          RefersToDaggerCodegen.class,
          RemoveUnusedImports.class,
          ReturnMissingNullable.class,
          ReturnsNullCollection.class,
          ScopeOnModule.class,
          StaticOrDefaultInterfaceMethod.class,
          StaticQualifiedUsingExpression.class,
          StringFormatWithLiteral.class,
          StronglyTypeByteString.class,
          StronglyTypeTime.class,
          SunApi.class,
          SuppressWarningsWithoutExplanation.class,
          SwitchDefault.class,
          SymbolToString.class,
          SystemExitOutsideMain.class,
          SystemOut.class,
          TestExceptionChecker.class,
          ThreadSafeRefactoring.class,
          ThrowSpecificExceptions.class,
          ThrowsUncheckedException.class,
          TimeUnitMismatch.class,
          TooManyParameters.class,
          TransientMisuse.class,
          TruthContainsExactlyElementsInUsage.class,
          TryFailRefactoring.class,
          TryWithResourcesVariable.class,
          TypeParameterNaming.class,
          TypeToString.class,
          UnescapedEntity.class, // TODO(b/263817298): re-enable
          UngroupedOverloads.class,
          UnnecessarilyFullyQualified.class,
          UnnecessarilyUsedValue.class,
          UnnecessarilyVisible.class,
          UnnecessaryAnonymousClass.class,
          UnnecessaryBoxedAssignment.class,
          UnnecessaryBoxedVariable.class,
          UnnecessaryDefaultInEnumSwitch.class,
          UnnecessaryFinal.class,
          UnnecessaryOptionalGet.class,
          UnnecessarySetDefault.class,
          UnnecessaryStaticImport.class,
          UnnecessaryTestMethodPrefix.class,
          UnsafeLocaleUsage.class,
          UnusedException.class,
          UrlInSee.class,
          UseCorrectAssertInTests.class,
          UseEnumSwitch.class,
          UsingJsr305CheckReturnValue.class,
          VarChecker.class,
          Varifier.class,
          VoidMissingNullable.class,
          WildcardImport.class,
          YodaCondition.class
          // keep-sorted end
          );

  // May not be instantiated
  private BuiltInCheckerSuppliers() {}
}
