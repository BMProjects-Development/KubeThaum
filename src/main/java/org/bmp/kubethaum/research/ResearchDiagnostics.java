package org.bmp.kubethaum.research;

import java.util.function.Supplier;

/** Adds script-facing context while retaining the original exception and stack trace. */
final class ResearchDiagnostics {
    private ResearchDiagnostics() {}

    static <T> T at(String context, Supplier<T> action) {
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
