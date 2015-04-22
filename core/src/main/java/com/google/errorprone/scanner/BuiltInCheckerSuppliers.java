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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.bugpatterns.*;
import com.google.errorprone.bugpatterns.threadsafety.*;

/**
 * Static helper class that provides {@link ScannerSupplier}s and {@link BugChecker}s
 * for the built-in error-prone checks, as opposed to plugin checks or checks used in tests.
 */
public class BuiltInCheckerSuppliers {

  /**
   * Returns a {@link ScannerSupplier} with all {@link BugChecker}s in Error Prone.
   */
  public static ScannerSupplier allChecks() {
    return ScannerSupplier.fromBugCheckers(
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
  static final ImmutableList<BugChecker> ENABLED_ERRORS = ImmutableList.of(
      new ArrayEquals(),
      new ArrayHashCode(),
      new ArrayToString(),
      new ArrayToStringCompoundAssignment(),
      new ArrayToStringConcatenation(),
      new BadShiftAmount(),
      new ChainingConstructorIgnoresParameter(),
      new CheckReturnValue(),
      new ComparisonOutOfRange(),
      new CompileTimeConstantChecker(),
      new DeadException(),
      new DepAnn(),
      new EqualsNaN(),
      new ForOverrideChecker(),
      new GuiceAssistedInjectScoping(),
      new GuardedByValidator(),
      new GuardedByChecker(),
      new InvalidPatternSyntax(),
      new JUnit3TestNotRun(),
      new JUnit4SetUpNotRun(),
      new JUnit4TearDownNotRun(),
      new JUnit4TestNotRun(),
      new LongLiteralLowerCaseSuffix(),
      new MisusedWeekYear(),
      new NonFinalCompileTimeConstant(),
      new Overrides(),
      new PreconditionsCheckNotNull(),
      new PreconditionsCheckNotNullPrimitive(),
      new ProtoFieldNullComparison(),
      new ReturnValueIgnored(),
      new SelfAssignment(),
      new SelfEquals(),
      new SizeGreaterThanOrEqualsZero(),
      new StringBuilderInitWithChar(),
      new SuppressWarningsDeprecated(),
      new TryFailThrowable());

  /**
   * A list of all checks with severity WARNING that are on by default.
   */
  @VisibleForTesting
  static final ImmutableList<BugChecker> ENABLED_WARNINGS = ImmutableList.of(
      new CannotMockFinalClass(),
      new ElementsCountedInLoop(),
      new EqualsHashCode(),
      new Finally(),
      new IncompatibleModifiersChecker(),
      new NonAtomicVolatileUpdate(),
      new PreconditionsInvalidPlaceholder(),
      new RequiredModifiersChecker(),
      new StaticAccessedFromInstance(),
      new StringEquality(),
      new WaitNotInLoop(),
      new SynchronizeOnNonFinalField(),
      new TypeParameterUnusedInFormals());

  /**
   * A list of all checks that are off by default.
   */
  @VisibleForTesting
  static final ImmutableList<BugChecker> DISABLED_CHECKS = ImmutableList.<BugChecker>of(
      new ArraysAsListPrimitiveArray(),
      new AssertFalse(),
      new AsyncFunctionReturnsNull(),
      new ClassCanBeStatic(),
      new ClassName(),
      new CollectionIncompatibleType(),
      new CovariantEquals(),
      new DivZero(),
      new EmptyIfStatement(),
      new FallThroughSuppression(),
      new GuiceAssistedParameters(),
      new GuiceInjectOnFinalField(),
      new GuiceOverridesGuiceInjectableMethod(),
      new GuiceOverridesJavaxInjectableMethod(),
      new InjectAssistedInjectAndInjectOnConstructors(),
      new InjectAssistedInjectAndInjectOnSameConstructor(),
      new InjectedConstructorAnnotations(),
      new InjectInvalidTargetingOnScopingAnnotation(),
      new InjectJavaxInjectOnAbstractMethod(),
      new InjectJavaxInjectOnFinalField(),
      new InjectMoreThanOneInjectableConstructor(),
      new InjectMoreThanOneQualifier(),
      new InjectMoreThanOneScopeAnnotationOnClass(),
      new InjectOverlappingQualifierAndScopeAnnotation(),
      new InjectScopeAnnotationOnInterfaceOrAbstractClass(),
      new InjectScopeOrQualifierAnnotationRetention(),
      new JMockTestWithoutRunWithOrRuleAnnotation(),
      new JUnitAmbiguousTestClass(),
      new LockMethodChecker(),
      new MalformedFormatString(),
      new MissingCasesInEnumSwitch(),
      new MissingFail(),
      new ModifyingCollectionWithItself(),
      new NarrowingCompoundAssignment(),
      new NoAllocationChecker(),
      new NonCanonicalStaticImport(),
      new NonRuntimeAnnotation(),
      new NullablePrimitive(),
      new NumericEquality(),
      new PackageLocation(),
      new PreconditionsExpensiveString(),
      new PrimitiveArrayPassedToVarargsMethod(),
      new ProtoFieldPreconditionsCheckNotNull(),
      new ProtoStringFieldReferenceEquality(),
      new SelfEquality(),
      new UnlockMethodChecker(),
      new UnnecessaryStaticImport(),
      new UnnecessaryTypeArgument(),
      new WildcardImport(),
      new WrongParameterPackage());

  // May not be instantiated
  private BuiltInCheckerSuppliers() {}
}
