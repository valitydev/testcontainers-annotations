package dev.vality.testcontainers.annotations.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Serializes test methods that mutate the same class-scoped test resources.
 * The lock is released automatically when the JUnit method context is closed.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestExecutionLock {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(TestExecutionLock.class);
    private static final String METHOD_LOCK = "method-lock";

    public static void acquire(ExtensionContext context) {
        var methodStore = context.getStore(NAMESPACE);
        if (methodStore.get(METHOD_LOCK) != null) {
            return;
        }

        var semaphore = context.getRoot().getStore(NAMESPACE).getOrComputeIfAbsent(
                context.getRequiredTestClass(),
                ignored -> new Semaphore(1, true),
                Semaphore.class);
        try {
            semaphore.acquire();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for test resource", ex);
        }
        methodStore.put(METHOD_LOCK, new LockHandle(semaphore));
    }

    public static void release(ExtensionContext context) {
        var handle = context.getStore(NAMESPACE).remove(METHOD_LOCK, LockHandle.class);
        if (handle != null) {
            handle.close();
        }
    }

    private static final class LockHandle implements ExtensionContext.Store.CloseableResource {

        private final Semaphore semaphore;
        private final AtomicBoolean closed = new AtomicBoolean();

        private LockHandle(Semaphore semaphore) {
            this.semaphore = semaphore;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                semaphore.release();
            }
        }
    }
}
