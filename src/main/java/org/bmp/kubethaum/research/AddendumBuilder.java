package org.bmp.kubethaum.research;

import com.google.gson.JsonObject;

public final class AddendumBuilder extends PageBuilder<AddendumBuilder> {
    public AddendumBuilder() { super(new JsonObject()); }
    @Override protected AddendumBuilder self() { return this; }
}
