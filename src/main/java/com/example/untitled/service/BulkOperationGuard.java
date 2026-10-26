package com.example.untitled.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lets only one bulk operation (an import or random generation) run at a time,
 * so they cannot exhaust the connection pool or heap together.
 */
@Component
public class BulkOperationGuard {

    private final Semaphore semaphore = new Semaphore(1);

    /**
     * @return a permit that must be closed when the operation ends
     * @throws BulkOperationInProgressException if another bulk operation is running
     */
    public Permit acquire() {
        if (!semaphore.tryAcquire()) {
            throw new BulkOperationInProgressException();
        }
        return new Permit();
    }

    public boolean isBusy() {
        return semaphore.availablePermits() == 0;
    }

    /** Releases the guard on close; closing more than once is safe. */
    public final class Permit implements AutoCloseable {

        private final AtomicBoolean released = new AtomicBoolean();

        private Permit() {
        }

        @Override
        public void close() {
            if (released.compareAndSet(false, true)) {
                semaphore.release();
            }
        }
    }
}
