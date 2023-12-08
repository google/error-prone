/*
 * Copyright 2023 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.threadsafety;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.ErrorProneFlags;
import java.util.List;
import javax.inject.Inject;

/** A collection of types with known thread safety. */
public final class WellKnownThreadSafety implements ThreadSafety.KnownTypes {
  @Inject
  WellKnownThreadSafety(ErrorProneFlags flags, WellKnownMutability wellKnownMutability) {
    ImmutableList<String> knownThreadSafe = flags.getListOrEmpty("ThreadSafe:KnownThreadSafe");
    this.knownThreadSafeClasses = buildThreadSafeClasses(knownThreadSafe, wellKnownMutability);
    this.knownUnsafeClasses = wellKnownMutability.getKnownMutableClasses();
  }

  public static WellKnownThreadSafety fromFlags(ErrorProneFlags flags) {
    return new WellKnownThreadSafety(flags, WellKnownMutability.fromFlags(flags));
  }

  public ImmutableMap<String, AnnotationInfo> getKnownThreadSafeClasses() {
    return knownThreadSafeClasses;
  }

  @Override
  public ImmutableMap<String, AnnotationInfo> getKnownSafeClasses() {
    return getKnownThreadSafeClasses();
  }

  @Override
  public ImmutableSet<String> getKnownUnsafeClasses() {
    return knownUnsafeClasses;
  }

  /** Types that are known to be threadsafe. */
  private final ImmutableMap<String, AnnotationInfo> knownThreadSafeClasses;

  @SuppressWarnings("UnnecessarilyFullyQualified") // intentional
  private static ImmutableMap<String, AnnotationInfo> buildThreadSafeClasses(
      List<String> extraKnownThreadSafe, WellKnownMutability mutability) {
    return
    // Things should only be added here when there is a good reason to think that annotating
    // them is infeasible (e.g. the type is defined in the JDK or some third party package),
    // in all other cases the class itself should be annotated.
    new MapBuilder()
        .addAll(mutability.getKnownImmutableClasses())
        .addStrings(extraKnownThreadSafe)
        .add(ClassLoader.class)
        .add(Thread.class)
        .add(java.util.Random.class)
        .add(java.util.concurrent.atomic.AtomicBoolean.class)
        .add(java.util.concurrent.atomic.AtomicInteger.class)
        .add(java.util.concurrent.atomic.AtomicIntegerArray.class)
        .add(java.util.concurrent.atomic.AtomicLong.class)
        .add(java.util.concurrent.atomic.AtomicLongArray.class)
        .add(java.util.concurrent.atomic.AtomicMarkableReference.class)
        .add(java.util.concurrent.atomic.AtomicReference.class, "V")
        .add(java.util.concurrent.atomic.DoubleAccumulator.class)
        .add(java.util.concurrent.atomic.DoubleAdder.class)
        .add(java.util.concurrent.atomic.LongAccumulator.class)
        .add(java.util.concurrent.atomic.LongAdder.class)
        .add(java.util.concurrent.BlockingDeque.class, "E")
        .add(java.util.concurrent.BlockingQueue.class, "E")
        .add(java.util.concurrent.LinkedBlockingDeque.class, "E")
        .add(java.util.concurrent.LinkedBlockingQueue.class, "E")
        .add(java.util.concurrent.PriorityBlockingQueue.class, "E")
        .add(java.util.concurrent.ConcurrentLinkedDeque.class, "E")
        .add(java.util.concurrent.ConcurrentLinkedQueue.class, "E")
        .add(java.util.concurrent.ConcurrentHashMap.class, "K", "V")
        .add(java.util.concurrent.ConcurrentMap.class, "K", "V")
        .add(java.util.concurrent.ConcurrentNavigableMap.class, "K", "V")
        .add(java.util.concurrent.ConcurrentSkipListMap.class, "K", "V")
        .add(java.util.concurrent.ConcurrentSkipListSet.class, "E")
        .add(java.util.concurrent.CopyOnWriteArrayList.class, "E")
        .add(java.util.concurrent.CopyOnWriteArraySet.class, "E")
        .add(java.util.concurrent.CountDownLatch.class)
        .add(java.util.concurrent.Executor.class)
        .add(java.util.concurrent.ExecutorService.class)
        .add(java.util.concurrent.Future.class, "V")
        .add(java.util.concurrent.Semaphore.class)
        .add(java.util.concurrent.ScheduledExecutorService.class)
        .add(java.util.concurrent.locks.Condition.class)
        .add(java.util.concurrent.locks.Lock.class)
        .add(java.util.concurrent.locks.ReadWriteLock.class)
        .add(java.util.concurrent.locks.ReentrantLock.class)
        .add(java.util.concurrent.locks.ReentrantReadWriteLock.class)
        .add(java.security.cert.X509Certificate.class)
        .add(java.security.cert.TrustAnchor.class)
        .add(java.security.SecureRandom.class)
        .add("com.google.common.time.Clock")
        .add("com.google.common.time.TimeSource")
        .add("com.google.common.util.concurrent.AtomicLongMap", "K")
        .add("com.google.common.util.concurrent.CheckedFuture", "V", "X")
        .add("com.google.common.util.concurrent.ListeningExecutorService")
        .add("com.google.common.util.concurrent.ListenableFuture", "V")
        .add("com.google.common.util.concurrent.ListeningScheduledExecutorService")
        .add("com.google.common.util.concurrent.RateLimiter")
        .add("com.google.common.util.concurrent.RateObserver")
        .add("com.google.common.util.concurrent.SettableFuture", "V")
        .add("com.google.common.util.concurrent.Striped", "L")
        .add("com.google.common.cache.LoadingCache", "K", "V")
        .add("com.google.common.cache.AsyncLoadingCache", "K", "V")
        .add("com.google.common.cache.Cache", "K", "V")
        .add("com.google.common.collect.ConcurrentHashMultiset", "E")
        .add("dagger.Lazy", "T")
        .add("org.reactivestreams.Publisher", "T")
        .add("org.reactivestreams.Processor", "T", "R")
        .add("io.reactivex.Maybe", "T")
        .add("io.reactivex.Single", "T")
        .add("io.reactivex.Flowable", "T")
        .add(Throwable.class) // Unsafe due to initCause, but generally used across threads
        .add("java.lang.ThreadLocal")
        .add("java.lang.invoke.MethodHandle")
        .add(java.lang.reflect.Method.class)
        .add(java.lang.reflect.Field.class)
        .add("com.github.benmanes.caffeine.cache.Cache", "K", "V")
        .add("com.github.benmanes.caffeine.cache.LoadingCache", "K", "V")
        .add("com.github.benmanes.caffeine.cache.AsyncLoadingCache", "K", "V")
        .add("kotlinx.coroutines.CoroutineDispatcher")
        .add("kotlinx.coroutines.CoroutineScope")
        .add("kotlinx.coroutines.ExecutorCoroutineDispatcher")
        .add("kotlinx.coroutines.sync.Mutex")
        .add("kotlinx.coroutines.sync.Semaphore")
        .add("kotlin.Unit")
        .add("org.bouncycastle.cms.CMSSignedData")
        .add("org.bouncycastle.pkcs.PKCS10CertificationRequest")
        .build();
  }

  /** Types that are known to be mutable. */
  private final ImmutableSet<String> knownUnsafeClasses;
}
