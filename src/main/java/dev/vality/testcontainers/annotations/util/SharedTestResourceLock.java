package dev.vality.testcontainers.annotations.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Serializes test classes that use process-wide singleton containers.
 * A semaphore is used intentionally because JUnit may invoke lifecycle callbacks on different threads.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SharedTestResourceLock {

    private static final Semaphore SINGLETON_TESTS = new Semaphore(1, true);
    private static final ConcurrentMap<Class<?>, Holder> HOLDERS = new ConcurrentHashMap<>();

    public static void acquire(Class<?> testClass) {
        var owner = new AtomicBoolean();
        var holder = HOLDERS.compute(testClass, (ignored, existing) -> {
            if (existing == null) {
                owner.set(true);
                return new Holder();
            }
            existing.references.incrementAndGet();
            return existing;
        });

        if (owner.get()) {
            try {
                SINGLETON_TESTS.acquire();
                holder.acquired.complete(null);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                HOLDERS.remove(testClass, holder);
                holder.acquired.completeExceptionally(ex);
                throw new IllegalStateException("Interrupted while waiting for singleton test resources", ex);
            }
            return;
        }

        try {
            holder.acquired.join();
        } catch (CompletionException ex) {
            throw new IllegalStateException("Unable to acquire singleton test resources", ex.getCause());
        }
    }

    public static void release(Class<?> testClass) {
        var releaseSemaphore = new AtomicBoolean();
        HOLDERS.computeIfPresent(testClass, (ignored, holder) -> {
            if (holder.references.decrementAndGet() == 0) {
                releaseSemaphore.set(true);
                return null;
            }
            return holder;
        });
        if (releaseSemaphore.get()) {
            SINGLETON_TESTS.release();
        }
    }

    private static final class Holder {

        private final AtomicInteger references = new AtomicInteger(1);
        private final CompletableFuture<Void> acquired = new CompletableFuture<>();
    }
}
