package org.kie.trustyai.connectors.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Convert a {@link ListenableFuture} into a {@link CompletableFuture}
 *
 * @param <T>
 */
public class ListenableFutureUtils<T> {

    private final CompletableFuture<T> completableFuture;

    public ListenableFutureUtils(ListenableFuture<T> listenableFuture) {
        this.completableFuture = new CompletableFuture<T>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                boolean cancelled = listenableFuture.cancel(mayInterruptIfRunning);
                super.cancel(cancelled);
                return cancelled;
            }
        };

        Futures.addCallback(listenableFuture, new FutureCallback<T>() {
            @Override
            public void onSuccess(T result) {
                completableFuture.complete(result);
            }

            @Override
            public void onFailure(Throwable ex) {
                completableFuture.completeExceptionally(ex);
            }
        }, ForkJoinPool.commonPool());
    }

    public static <T> CompletableFuture<T> asCompletableFuture(ListenableFuture<T> listenableFuture) {
        final ListenableFutureUtils<T> listenableFutureAdapter = new ListenableFutureUtils<>(listenableFuture);
        return listenableFutureAdapter.completableFuture;
    }

}
