package org.bmp.kubethaum.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.util.JsonUtils;
import dev.latvian.mods.rhino.BaseFunction;
import dev.latvian.mods.rhino.Callable;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ContextFactory;
import dev.latvian.mods.rhino.Function;
import dev.latvian.mods.rhino.NativeJavaObject;
import dev.latvian.mods.rhino.RhinoException;
import dev.latvian.mods.rhino.Script;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.ScriptableObject;
import dev.latvian.mods.rhino.Undefined;
import dev.latvian.mods.rhino.type.TypeInfo;
import dev.latvian.mods.rhino.util.HideFromJS;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;

/** Evaluates fresh research scripts using detached builders and no game/Java bindings. */
public final class ResearchScriptCheck {
    public record Source(String file, String text) {}
    public record Location(String file, int line) {
        @Override public String toString() { return file + (line > 0 ? ":" + line : ""); }
    }
    public record Problem(Location location, String message) {
        @Override public String toString() { return location + ": " + message; }
    }
    public record Result(int files, int handlers, int skippedHandlers, List<Problem> problems,
                         ResearchEventJS.Changes changes, Map<String, Location> locations) {
        public boolean successful() { return problems.isEmpty(); }
    }
    private record Compiled(Source source, Script script) {}
    private record Handler(Function function, Location location) {}
    private ResearchScriptCheck() {}

    public static Result check(List<Source> sources, Map<String, JsonObject> entries, Map<String, JsonObject> categories,
                               Predicate<Identifier> knownItem, ProgressFlags progressFlags) {
        var problems = new ArrayList<Problem>();
        var locations = new LinkedHashMap<String, Location>();
        var handlers = new ArrayList<Handler>();
        var skipped = new int[1];
        var event = new ResearchEventJS(entries, categories, progressFlags);
        var factory = new ContextFactory() {
            @Override protected Context createContext() { return new CheckContext(this, locations); }
        };
        factory.getTypeWrappers().register(JsonObject.class, (cx, value, type) -> JsonUtils.objectOf(cx, value));
        factory.getTypeWrappers().register(JsonArray.class, (cx, value, type) -> JsonUtils.arrayOf(cx, value));
        var cx = (CheckContext)factory.enter();
        var scope = cx.initSafeStandardObjects();
        var researchEvents = cx.newObject(scope);
        researchEvents.put(cx, "research", researchEvents, function(cx, scope, (context, args) -> {
            if (args.length != 1 || !(args[0] instanceof Function callback)) {
                throw Context.reportRuntimeError("research() expects one callback", context);
            }
            handlers.add(new Handler(callback, position(context)));
            return Undefined.INSTANCE;
        }));
        scope.put(cx, "ThaumaturgeEvents", scope, researchEvents);
        // Only registrations are accepted here. Game/recipe callbacks are never invoked.
        scope.put(cx, "ServerEvents", scope, new ScriptableObject() {
            { setParentScope(scope); }
            @Override public String getClassName() { return "UncheckedServerEvents"; }
            @Override public Object get(Context context, String name, Scriptable start) {
                return function(context, scope, (c, args) -> { skipped[0]++; return Undefined.INSTANCE; });
            }
        });
        var console = cx.newObject(scope);
        for (String name : List.of("info", "log", "warn", "error", "debug")) {
            console.put(cx, name, console, function(cx, scope, (context, args) -> Undefined.INSTANCE));
        }
        scope.put(cx, "console", scope, console);
        var compiled = new ArrayList<Compiled>();
        ResearchEventJS.Changes changes = null;
        try {
            // Compile every file first, so one syntax error does not hide errors in other files.
            for (var source : sources) {
                try {
                    compiled.add(new Compiled(source, cx.compileString(source.text(), source.file(), 1, null)));
                } catch (RuntimeException error) { problems.add(problem(error, new Location(source.file(), 0))); }
            }
            if (problems.isEmpty()) {
                for (var file : compiled) {
                    try { file.script().exec(cx, scope); }
                    catch (RuntimeException error) { problems.add(problem(error, new Location(file.source().file(), 0))); }
                }
            }
            if (problems.isEmpty()) {
                ResearchDiagnostics.trace(context -> {
                    var location = position(cx);
                    if (location.line() > 0) locations.put(context, location);
                });
                for (var handler : handlers) {
                    try { cx.callSync(handler.function(), scope, scope, new Object[]{cx.wrap(scope, event)}); }
                    catch (RuntimeException error) { problems.add(problem(error, handler.location())); }
                }
                if (problems.isEmpty()) {
                    try {
                        changes = event.finish();
                        ResearchValidator.validateItems(changes.entries(), knownItem);
                    } catch (RuntimeException error) {
                        problems.add(problem(error, locate(error.getMessage(), locations)));
                    }
                }
            }
        } catch (CheckAborted error) {
            problems.add(new Problem(cx.lastLocation, "Check stopped: execution limit exceeded (possible infinite loop)."));
        } finally { ResearchDiagnostics.trace(null); }
        return new Result(sources.size(), handlers.size(), skipped[0], List.copyOf(problems), changes, Map.copyOf(locations));
    }

    static Location locate(String message, Map<String, Location> locations) {
        Location fallback = new Location("research validation", 0);
        int best = -1;
        for (var entry : locations.entrySet()) {
            String context = entry.getKey();
            int quote = context.indexOf("'");
            int end = quote < 0 ? -1 : context.indexOf("'", quote + 1);
            if (end > quote && message.contains(context.substring(quote + 1, end))) {
                String field = context.contains("field '") ? context.substring(context.indexOf("field '") + 7).replace("'", "") : "";
                int score = message.startsWith(context) || !field.isEmpty() && message.contains(field) ? context.length() : 0;
                if (score > best) { best = score; fallback = entry.getValue(); }
            }
        }
        return fallback;
    }

    private static Problem problem(Throwable error, Location fallback) {
        RhinoException origin = null;
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof RhinoException rhino && rhino.lineNumber() > 0) {
                origin = rhino;
            }
        }
        if (origin != null) {
            String message = origin instanceof dev.latvian.mods.rhino.WrappedException wrapped
                ? wrapped.getWrappedException().getMessage() : origin.details();
            return new Problem(new Location(origin.sourceName(), origin.lineNumber()), message);
        }
        return new Problem(fallback, error.getMessage() == null ? error.toString() : error.getMessage());
    }

    private static Location position(Context context) {
        int[] line = {0};
        String file = Context.getSourcePositionFromStack(context, line);
        return new Location(file == null ? "research validation" : file, line[0]);
    }

    @FunctionalInterface private interface Call { Object call(Context context, Object[] args); }
    private static BaseFunction function(Context context, Scriptable scope, Call call) {
        return new BaseFunction(scope, ScriptableObject.getFunctionPrototype(scope, context)) {
            @Override public Object call(Context cx, Scriptable scope, Scriptable self, Object[] args) { return call.call(cx, args); }
        };
    }

    private static final class CheckAborted extends Error {}
    private static final class CheckContext extends Context {
        private final long deadline = System.nanoTime() + 2_000_000_000L;
        private int instructions;
        private final Map<String, Location> locations;
        private Location lastLocation = new Location("research validation", 0);
        CheckContext(ContextFactory factory, Map<String, Location> locations) {
            super(factory);
            this.locations = locations;
            setInstructionObserverThreshold(10_000);
        }
        @Override protected void observeInstructionCount(int count) {
            lastLocation = position(this);
            instructions += count;
            if (instructions > 1_000_000 || System.nanoTime() > deadline) throw new CheckAborted();
        }
        @Override public Scriptable wrapAsJavaObject(Scriptable scope, Object value, TypeInfo type) {
            boolean builder = value instanceof ResearchEventJS || value instanceof ResearchBuilder
                || value instanceof PageBuilder<?> || value instanceof CategoryBuilder;
            if (!builder && !(value instanceof JsonElement)) {
                // Rhino passes the Java Consumer adapter as callback `this`. It has no DSL
                // members and must not expose its Java implementation to the script.
                return new ScriptableObject() {
                    { setParentScope(scope); }
                    @Override public String getClassName() { return "UnavailableJavaObject"; }
                };
            }
            // Expose only the existing DSL/JSON methods. Object.getClass(), wait(), fields,
            // hidden Java helpers, reflection, and the game are not reachable from this view.
            var methods = Arrays.stream(value.getClass().getMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers()))
                .filter(method -> method.getDeclaringClass() != Object.class && !method.isAnnotationPresent(HideFromJS.class))
                .filter(method -> method.getDeclaringClass().getPackageName().equals("org.bmp.kubethaum.research")
                    || method.getDeclaringClass().getPackageName().equals("com.google.gson"))
                .map(java.lang.reflect.Method::getName).collect(java.util.stream.Collectors.toSet());
            return new NativeJavaObject(scope, value, type, this) {
                @Override public Object get(Context context, String name, Scriptable start) {
                    if (!methods.contains(name)) return NOT_FOUND;
                    Object member = super.get(context, name, start);
                    if (!(member instanceof Callable callable)) return NOT_FOUND;
                    return function(context, scope, (cx, args) -> {
                        lastLocation = position(cx);
                        if (value instanceof ResearchEventJS && args.length > 0 && args[0] instanceof CharSequence) {
                            String id = String.valueOf(args[name.equals("copy") && args.length > 1 ? 1 : 0]);
                            String owner = name.toLowerCase(java.util.Locale.ROOT).contains("category") ? "Category" : "Research";
                            locations.put(owner + " '" + id + "'", lastLocation);
                        }
                        return callable.call(cx, scope, start, args);
                    });
                }
                @Override public boolean has(Context context, String name, Scriptable start) { return methods.contains(name); }
                @Override public Object[] getIds(Context context) { return methods.toArray(); }
            };
        }
        @Override public Scriptable wrapJavaClass(Scriptable scope, Class<?> type) {
            throw Context.reportRuntimeError("Java classes are unavailable in research check mode", this);
        }
    }
}
