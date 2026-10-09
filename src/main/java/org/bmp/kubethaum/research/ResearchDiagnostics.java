package org.bmp.kubethaum.research;

import java.util.function.Supplier;

/** Adds script-facing context while retaining the original exception and stack trace. */
final class ResearchDiagnostics {
    private static final ThreadLocal<java.util.function.Consumer<String>> TRACE = new ThreadLocal<>();
    private ResearchDiagnostics() {}

    static void trace(java.util.function.Consumer<String> trace) {
        if (trace == null) TRACE.remove(); else TRACE.set(trace);
    }

    static void mark(String context) {
        var trace = TRACE.get();
        if (trace != null) trace.accept(context);
    }

    static <T> T at(String context, Supplier<T> action) {
        mark(context);
        try { return action.get(); }
        catch (RuntimeException ex) {
            if (ex instanceof DiagnosticException diagnostic) throw diagnostic;
            throw new DiagnosticException(context + ": " + ex.getMessage(), ex);
        }
    }

    static void at(String context, Runnable action) {
        at(context, () -> { action.run(); return null; });
    }
    private static final class DiagnosticException extends IllegalArgumentException {
        DiagnosticException(String message, Throwable cause) { super(message, cause); }
    }
}
