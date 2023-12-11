package org.kie.trustyai.explainability;

import java.io.InputStream;
import java.lang.management.ManagementFactory;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import io.micrometer.core.instrument.util.IOUtils;

/**
 * This Annotation is intended to be used on test cases that might be having timeout issues
 * To use just annotate the test class with @ExtendWith(ThreadDumpOnTimeoutExtension.class)
 */
public class ThreadDumpOnTimeoutExtension implements TestExecutionExceptionHandler {

    @Override
    public void handleTestExecutionException(ExtensionContext context,
            Throwable throwable) throws Throwable {

        System.out.println("Thread dump after test execution, PID: " + ManagementFactory.getRuntimeMXBean().getPid());
        long pid = ManagementFactory.getRuntimeMXBean().getPid();
        System.out.println("PID: " + pid);
        InputStream in = Runtime.getRuntime().exec("jstack" + " " + pid).getInputStream();
        System.out.println(IOUtils.toString(in));
        System.out.println("################## finished");
        throw throwable;
    }
}
