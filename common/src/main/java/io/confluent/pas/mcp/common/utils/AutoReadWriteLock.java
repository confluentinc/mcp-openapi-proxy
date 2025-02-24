package io.confluent.pas.mcp.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A utility class to manage a read-write lock.
 * This class provides methods to execute functions with read or write locks,
 * ensuring thread-safe access to shared resources.
 */
@Slf4j
public class AutoReadWriteLock {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Execute a function with a write lock.
     * This method acquires the write lock, executes the given callback, and then releases the lock.
     *
     * @param callback the function to execute
     */
    public void writeLockAndExecute(Runnable callback) {
        final Lock writeLock = lock.writeLock();

        writeLock.lock();
        try {
            callback.run();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Execute a function with a read lock.
     * This method acquires the read lock, executes the given callback, and then releases the lock.
     *
     * @param callback the function to execute
     */
    public void readLockAndExecute(Runnable callback) {
        final Lock readLock = lock.readLock();

        readLock.lock();
        try {
            callback.run();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Execute a function with a read lock and return a result.
     * This method acquires the read lock, executes the given callback, and then releases the lock.
     *
     * @param callback the function to execute
     * @param <V>      the type of the result
     * @return the result of the callback
     */
    public <V> V readLockAndExecute(Callable<V> callback) {
        final Lock readLock = lock.readLock();

        readLock.lock();
        try {
            return callback.call();
        } catch (Exception e) {
            log.error("Failed to execute callback", e);
            throw new RuntimeException(e);
        } finally {
            readLock.unlock();
        }
    }

}