package org.kie.trustyai.explainability;

import io.micrometer.core.instrument.util.IOUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import java.io.InputStream;
import java.lang.management.ManagementFactory;

public class ThreadDumpOnTimeoutExtension implements TestExecutionExceptionHandler {

    @Override
    public void handleTestExecutionException(ExtensionContext context,
            Throwable throwable) throws Throwable {

        //        if (throwable instanceof TimeoutException) {
        //            Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        //            System.out.println("Thread dump after test execution:");
        //            for (Thread thread : allStackTraces.keySet()) {
        //                System.out.println("Thread: " + thread + ":");
        //                for (StackTraceElement element : allStackTraces.get(thread)) {
        //                    System.out.println("  " + element);
        //                }
        //            }
        //        }

        System.out.println("Thread dump after test execution, PID: " + ManagementFactory.getRuntimeMXBean().getPid());
        long pid = ManagementFactory.getRuntimeMXBean().getPid();
        System.out.println("PID: " + pid);
        InputStream in = Runtime.getRuntime().exec("jstack" + " " + pid).getInputStream();
        System.out.println(IOUtils.toString(in));
        System.out.println("################## finished ");
        throw throwable;
    }
}
