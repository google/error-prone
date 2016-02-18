/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugCheckerInfo;
import com.google.errorprone.bugpatterns.ArrayEquals;
import com.google.errorprone.bugpatterns.ArrayHashCode;
import com.google.errorprone.bugpatterns.ArrayToString;
import com.google.errorprone.bugpatterns.ArrayToStringCompoundAssignment;
import com.google.errorprone.bugpatterns.ArrayToStringConcatenation;
import com.google.errorprone.bugpatterns.ArraysAsListPrimitiveArray;
import com.google.errorprone.bugpatterns.AssertFalse;
import com.google.errorprone.bugpatterns.AsyncFunctionReturnsNull;
import com.google.errorprone.bugpatterns.BadAnnotationImplementation;
import com.google.errorprone.bugpatterns.BadComparable;
import com.google.errorprone.bugpatterns.BadShiftAmount;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.CannotMockFinalClass;
import com.google.errorprone.bugpatterns.ChainingConstructorIgnoresParameter;
import com.google.errorprone.bugpatterns.CheckReturnValue;
import com.google.errorprone.bugpatterns.ClassCanBeStatic;
import com.google.errorprone.bugpatterns.ClassName;
import com.google.errorprone.bugpatterns.ComparisonOutOfRange;
import com.google.errorprone.bugpatterns.CompileTimeConstantChecker;
import com.google.errorprone.bugpatterns.DaggerProvidesNull;
import com.google.errorprone.bugpatterns.DeadException;
import com.google.errorprone.bugpatterns.DepAnn;
import com.google.errorprone.bugpatterns.DivZero;
import com.google.errorprone.bugpatterns.ElementsCountedInLoop;
import com.google.errorprone.bugpatterns.EmptyIfStatement;
import com.google.errorprone.bugpatterns.EmptyTopLevelDeclaration;
import com.google.errorprone.bugpatterns.EqualsHashCode;
import com.google.errorprone.bugpatterns.EqualsIncompatibleType;
import com.google.errorprone.bugpatterns.EqualsNaN;
import com.google.errorprone.bugpatterns.FallThroughSuppression;
import com.google.errorprone.bugpatterns.Finally;
import com.google.errorprone.bugpatterns.ForOverrideChecker;
import com.google.errorprone.bugpatterns.GetClassOnClass;
import com.google.errorprone.bugpatterns.GuiceAssistedInjectScoping;
import com.google.errorprone.bugpatterns.GuiceAssistedParameters;
import com.google.errorprone.bugpatterns.GuiceInjectOnFinalField;
import com.google.errorprone.bugpatterns.GuiceOverridesGuiceInjectableMethod;
import com.google.errorprone.bugpatterns.GuiceOverridesJavaxInjectableMethod;
import com.google.errorprone.bugpatterns.HashtableContains;
import com.google.errorprone.bugpatterns.IncompatibleModifiersChecker;
import com.google.errorprone.bugpatterns.InjectAssistedInjectAndInjectOnConstructors;
import com.google.errorprone.bugpatterns.InjectAssistedInjectAndInjectOnSameConstructor;
import com.google.errorprone.bugpatterns.InjectInvalidTargetingOnScopingAnnotation;
import com.google.errorprone.bugpatterns.InjectJavaxInjectOnAbstractMethod;
import com.google.errorprone.bugpatterns.InjectJavaxInjectOnFinalField;
import com.google.errorprone.bugpatterns.InjectMoreThanOneInjectableConstructor;
import com.google.errorprone.bugpatterns.InjectMoreThanOneQualifier;
import com.google.errorprone.bugpatterns.InjectMoreThanOneScopeAnnotationOnClass;
import com.google.errorprone.bugpatterns.InjectOverlappingQualifierAndScopeAnnotation;
import com.google.errorprone.bugpatterns.InjectScopeAnnotationOnInterfaceOrAbstractClass;
import com.google.errorprone.bugpatterns.InjectScopeOrQualifierAnnotationRetention;
import com.google.errorprone.bugpatterns.InjectedConstructorAnnotations;
import com.google.errorprone.bugpatterns.InsecureCipherMode;
import com.google.errorprone.bugpatterns.InvalidPatternSyntax;
import com.google.errorprone.bugpatterns.IsInstanceOfClass;
import com.google.errorprone.bugpatterns.JMockTestWithoutRunWithOrRuleAnnotation;
import com.google.errorprone.bugpatterns.JUnit3TestNotRun;
import com.google.errorprone.bugpatterns.JUnit4SetUpNotRun;
import com.google.errorprone.bugpatterns.JUnit4TearDownNotRun;
import com.google.errorprone.bugpatterns.JUnit4TestNotRun;
import com.google.errorprone.bugpatterns.JUnitAmbiguousTestClass;
import com.google.errorprone.bugpatterns.LongLiteralLowerCaseSuffix;
import com.google.errorprone.bugpatterns.MalformedFormatString;
import com.google.errorprone.bugpatterns.MissingCasesInEnumSwitch;
import com.google.errorprone.bugpatterns.MissingFail;
import com.google.errorprone.bugpatterns.MissingOverride;
import com.google.errorprone.bugpatterns.MisusedWeekYear;
import com.google.errorprone.bugpatterns.MockitoCast;
import com.google.errorprone.bugpatterns.MockitoUsage;
import com.google.errorprone.bugpatterns.ModifyingCollectionWithItself;
import com.google.errorprone.bugpatterns.MultipleTopLevelClasses;
import com.google.errorprone.bugpatterns.NarrowingCompoundAssignment;
import com.google.errorprone.bugpatterns.NoAllocationChecker;
import com.google.errorprone.bugpatterns.NonAtomicVolatileUpdate;
import com.google.errorprone.bugpatterns.NonCanonicalStaticImport;
import com.google.errorprone.bugpatterns.NonCanonicalStaticMemberImport;
import com.google.errorprone.bugpatterns.NonFinalCompileTimeConstant;
import com.google.errorprone.bugpatterns.NonOverridingEquals;
import com.google.errorprone.bugpatterns.NonRuntimeAnnotation;
import com.google.errorprone.bugpatterns.NullableConstructor;
import com.google.errorprone.bugpatterns.NullablePrimitive;
import com.google.errorprone.bugpatterns.NullableVoid;
import com.google.errorprone.bugpatterns.NumericEquality;
import com.google.errorprone.bugpatterns.OptionalEquality;
import com.google.errorprone.bugpatterns.Overrides;
import com.google.errorprone.bugpatterns.PackageLocation;
import com.google.errorprone.bugpatterns.PreconditionsCheckNotNull;
import com.google.errorprone.bugpatterns.PreconditionsCheckNotNullPrimitive;
import com.google.errorprone.bugpatterns.PreconditionsExpensiveString;
import com.google.errorprone.bugpatterns.PreconditionsInvalidPlaceholder;
import com.google.errorprone.bugpatterns.PrimitiveArrayPassedToVarargsMethod;
import com.google.errorprone.bugpatterns.ProtoFieldNullComparison;
import com.google.errorprone.bugpatterns.ProtoFieldPreconditionsCheckNotNull;
import com.google.errorprone.bugpatterns.ProtoStringFieldReferenceEquality;
import com.google.errorprone.bugpatterns.RequiredModifiersChecker;
import com.google.errorprone.bugpatterns.ReturnValueIgnored;
import com.google.errorprone.bugpatterns.SelfAssignment;
import com.google.errorprone.bugpatterns.SelfEquality;
import com.google.errorprone.bugpatterns.SelfEquals;
import com.google.errorprone.bugpatterns.SizeGreaterThanOrEqualsZero;
import com.google.errorprone.bugpatterns.StaticAccessedFromInstance;
import com.google.errorprone.bugpatterns.StringBuilderInitWithChar;
import com.google.errorprone.bugpatterns.StringEquality;
import com.google.errorprone.bugpatterns.SuppressWarningsDeprecated;
import com.google.errorprone.bugpatterns.TryFailThrowable;
import com.google.errorprone.bugpatterns.TypeParameterQualifier;
import com.google.errorprone.bugpatterns.TypeParameterUnusedInFormals;
import com.google.errorprone.bugpatterns.UnnecessaryStaticImport;
import com.google.errorprone.bugpatterns.UnnecessaryTypeArgument;
import com.google.errorprone.bugpatterns.UnsynchronizedOverridesSynchronized;
import com.google.errorprone.bugpatterns.UnusedAnonymousClass;
import com.google.errorprone.bugpatterns.VarChecker;
import com.google.errorprone.bugpatterns.WaitNotInLoop;
import com.google.errorprone.bugpatterns.WildcardImport;
import com.google.errorprone.bugpatterns.WrongParameterPackage;
import com.google.errorprone.bugpatterns.android.FragmentNotInstantiable;
import com.google.errorprone.bugpatterns.android.MislabeledAndroidString;
import com.google.errorprone.bugpatterns.android.RectIntersectReturnValueIgnored;
import com.google.errorprone.bugpatterns.collectionincompatibletype.CollectionIncompatibleType;
import com.google.errorprone.bugpatterns.threadsafety.DoubleCheckedLocking;
import com.google.errorprone.bugpatterns.threadsafety.GuardedByChecker;
import com.google.errorprone.bugpatterns.threadsafety.GuardedByValidator;
import com.google.errorprone.bugpatterns.threadsafety.ImmutableChecker;
import com.google.errorprone.bugpatterns.threadsafety.LockMethodChecker;
import com.google.errorprone.bugpatterns.threadsafety.SynchronizeOnNonFinalField;
import com.google.errorprone.bugpatterns.threadsafety.UnlockMethodChecker;

/**
 * Static helper class that provides {@link ScannerSupplier}s and {@link BugChecker}s for the
 * built-in Error Prone checks, as opposed to plugin checks or checks used in tests.
 */
public class BuiltInCheckerSuppliers {
  @SafeVarargs
  public static ImmutableSet<BugCheckerInfo> getSuppliers(Class<? extends BugChecker>... checkers) {
    ImmutableSet.Builder<BugCheckerInfo> result = ImmutableSet.builder();
    for (Class<? extends BugChecker> checker : checkers) {
      result.add(BugCheckerInfo.create(checker));
    }
    return result.build();
  }

  /**
   * Returns a {@link ScannerSupplier} with all {@link BugChecker}s in Error Prone.
   */
  public static ScannerSupplier allChecks() {
    return ScannerSupplier.fromBugCheckerInfos(
        Iterables.concat(ENABLED_ERRORS, ENABLED_WARNINGS, DISABLED_CHECKS));
  }

  /**
   * Returns a {@link ScannerSupplier} with the {@link BugChecker}s that are in the ENABLED lists.
   */
  public static ScannerSupplier defaultChecks() {
    return allChecks().filter(
        Predicates.or(
            Predicates.in(ENABLED_ERRORS),
            Predicates.in(ENABLED_WARNINGS)));
  }

  /**
   * Returns a {@link ScannerSupplier} with the {@link BugChecker}s that are in the ENABLED_ERRORS
   * list.
   */
  public static ScannerSupplier errorChecks() {
    return allChecks().filter(Predicates.in(ENABLED_ERRORS));
  }

  /**
   * A list of all checks with severity ERROR that are on by default.
   */
  @VisibleForTesting
  static final ImmutableSet<BugCheckerInfo> ENABLED_ERRORS =
      getSuppliers(
          ArrayEquals.class,
          ArrayHashCode.class,
          ArrayToString.class,
          ArrayToStringCompoundAssignment.class,
          ArrayToStringConcatenation.class,
          AsyncFunctionReturnsNull.class,
          BadShiftAmount.class,
          ChainingConstructorIgnoresParameter.class,
          CheckReturnValue.class,
          ClassName.class,
          ComparisonOutOfRange.class,
          CompileTimeConstantChecker.class,
          DeadException.class,
          DepAnn.class,
          EqualsNaN.class,
          ForOverrideChecker.class,
          GetClassOnClass.class,
          GuiceAssistedInjectScoping.class,
          GuardedByValidator.class,
          GuardedByChecker.class,
          HashtableContains.class,
          ImmutableChecker.class,
          InvalidPatternSyntax.class,
          IsInstanceOfClass.class,
          JUnit3TestNotRun.class,
          JUnit4SetUpNotRun.class,
          JUnit4TearDownNotRun.class,
          JUnit4TestNotRun.class,
          LongLiteralLowerCaseSuffix.class,
          MislabeledAndroidString.class,
          MisusedWeekYear.class,
          MockitoUsage.class,
          NonCanonicalStaticImport.class,
          NonFinalCompileTimeConstant.class,
          OptionalEquality.class,
          Overrides.class,
          PreconditionsCheckNotNull.class,
          PreconditionsCheckNotNullPrimitive.class,
          ProtoFieldNullComparison.class,
          RectIntersectReturnValueIgnored.class,
          ReturnValueIgnored.class,
          SelfAssignment.class,
          SelfEquals.class,
          SizeGreaterThanOrEqualsZero.class,
          StringBuilderInitWithChar.class,
          SuppressWarningsDeprecated.class,
          TryFailThrowable.class,
          TypeParameterQualifier.class,
          UnusedAnonymousClass.class);

  /**
   * A list of all checks with severity WARNING that are on by default.
   */
  @VisibleForTesting
  static final ImmutableSet<BugCheckerInfo> ENABLED_WARNINGS =
      getSuppliers(
          CannotMockFinalClass.class,
          ElementsCountedInLoop.class,
          EqualsHashCode.class,
          Finally.class,
          IncompatibleModifiersChecker.class,
          MultipleTopLevelClasses.class,
          NonAtomicVolatileUpdate.class,
          NonOverridingEquals.class,
          NullableConstructor.class,
          NullablePrimitive.class,
          NullableVoid.class,
          PreconditionsInvalidPlaceholder.class,
          RequiredModifiersChecker.class,
          StaticAccessedFromInstance.class,
          StringEquality.class,
          WaitNotInLoop.class,
          SynchronizeOnNonFinalField.class,
          TypeParameterUnusedInFormals.class);

  /**
   * A list of all checks that are off by default.
   */
  @VisibleForTesting
  static final ImmutableSet<BugCheckerInfo> DISABLED_CHECKS =
      getSuppliers(
          ArraysAsListPrimitiveArray.class,
          AssertFalse.class,
          BadAnnotationImplementation.class,
          BadComparable.class,
          ClassCanBeStatic.class,
          CollectionIncompatibleType.class,
          DivZero.class,
          DoubleCheckedLocking.class,
          EmptyIfStatement.class,
          EmptyTopLevelDeclaration.class,
          EqualsIncompatibleType.class,
          FallThroughSuppression.class,
          FragmentNotInstantiable.class,
          GuiceAssistedParameters.class,
          GuiceInjectOnFinalField.class,
          GuiceOverridesGuiceInjectableMethod.class,
          GuiceOverridesJavaxInjectableMethod.class,
          InjectAssistedInjectAndInjectOnConstructors.class,
          InjectAssistedInjectAndInjectOnSameConstructor.class,
          InjectInvalidTargetingOnScopingAnnotation.class,
          InjectJavaxInjectOnAbstractMethod.class,
          InjectJavaxInjectOnFinalField.class,
          InjectMoreThanOneInjectableConstructor.class,
          InjectMoreThanOneQualifier.class,
          InjectMoreThanOneScopeAnnotationOnClass.class,
          InjectOverlappingQualifierAndScopeAnnotation.class,
          InjectScopeAnnotationOnInterfaceOrAbstractClass.class,
          InjectScopeOrQualifierAnnotationRetention.class,
          InjectedConstructorAnnotations.class,
          InsecureCipherMode.class,
          JMockTestWithoutRunWithOrRuleAnnotation.class,
          JUnitAmbiguousTestClass.class,
          LockMethodChecker.class,
          MalformedFormatString.class,
          MissingCasesInEnumSwitch.class,
          MissingFail.class,
          MissingOverride.class,
          MockitoCast.class,
          ModifyingCollectionWithItself.class,
          NarrowingCompoundAssignment.class,
          NoAllocationChecker.class,
          NonCanonicalStaticMemberImport.class,
          NonRuntimeAnnotation.class,
          NumericEquality.class,
          PackageLocation.class,
          PreconditionsExpensiveString.class,
          PrimitiveArrayPassedToVarargsMethod.class,
          ProtoFieldPreconditionsCheckNotNull.class,
          ProtoStringFieldReferenceEquality.class,
          DaggerProvidesNull.class,
          SelfEquality.class,
          UnlockMethodChecker.class,
          UnnecessaryStaticImport.class,
          UnnecessaryTypeArgument.class,
          UnsynchronizedOverridesSynchronized.class,
          VarChecker.class,
          WildcardImport.class,
          WrongParameterPackage.class);

  // May not be instantiated
  private BuiltInCheckerSuppliers() {}
}
