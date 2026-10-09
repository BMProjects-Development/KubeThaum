package org.bmp.kubethaum;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import org.bmp.kubethaum.research.ResearchEventJS;

public final class ThaumaturgeEvents {
    public static final EventGroup GROUP = EventGroup.of("ThaumaturgeEvents");
    public static final EventHandler RESEARCH = GROUP.server("research", () -> ResearchEventJS.class)
        .exceptionHandler((event, handler, error) -> {
            ((ResearchEventJS)event).scriptFailed(error);
            return error;
        });
    private ThaumaturgeEvents() {}
}
