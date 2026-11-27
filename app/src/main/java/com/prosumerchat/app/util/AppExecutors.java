package com.prosumerchat.app.util;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AppExecutors — Quản lý các thread pool dùng chung cho toàn dự án:
 * - diskIO: single-thread executor cho Room/SQLCipher, Tink keyset và đọc/ghi file tuần tự.
 * - networkIO: fixed thread pool (3 threads) cho Firebase/Cloudinary/network callbacks.
 * - mainThread: đưa kết quả về Main/UI thread.
 */
public class AppExecutors {

    private static final int NETWORK_THREAD_COUNT = 3;
    private static volatile AppExecutors sInstance;

    private final ExecutorService diskIO;
    private final ExecutorService networkIO;
    private final Executor mainThread;

    public AppExecutors(
            @NonNull ExecutorService diskIO,
            @NonNull ExecutorService networkIO,
            @NonNull Executor mainThread) {
        this.diskIO = diskIO;
        this.networkIO = networkIO;
        this.mainThread = mainThread;
    }

    private AppExecutors() {
        this(
                Executors.newSingleThreadExecutor(new NamedThreadFactory("prosumerchat-disk-io")),
                Executors.newFixedThreadPool(
                        NETWORK_THREAD_COUNT,
                        new NamedThreadFactory("prosumerchat-net-io")),
                new MainThreadExecutor()
        );
    }

    @NonNull
    public static AppExecutors getInstance() {
        if (sInstance == null) {
            synchronized (AppExecutors.class) {
                if (sInstance == null) {
                    sInstance = new AppExecutors();
                }
            }
        }
        return sInstance;
    }

    @NonNull
    public ExecutorService diskIO() {
        return diskIO;
    }

    @NonNull
    public ExecutorService networkIO() {
        return networkIO;
    }

    @NonNull
    public Executor mainThread() {
        return mainThread;
    }

    public static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    private static final class MainThreadExecutor implements Executor {
        private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            mainThreadHandler.post(command);
        }
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger(1);

        NamedThreadFactory(@NonNull String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread thread = new Thread(r, prefix + "-" + counter.getAndIncrement());
            thread.setDaemon(false);
            return thread;
        }
    }
}
