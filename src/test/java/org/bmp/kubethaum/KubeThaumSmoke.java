package org.bmp.kubethaum;

import com.leclowndu93150.thaumaturge.api.capability.PlayerResearchAccess;
import com.leclowndu93150.thaumaturge.api.recipe.ResearchGate;
import com.leclowndu93150.thaumaturge.api.recipe.ResearchGated;
import com.leclowndu93150.thaumaturge.api.research.IResearchCategory;
import com.leclowndu93150.thaumaturge.api.research.IResearchEntry;
import com.leclowndu93150.thaumaturge.content.research.PlayerKnowledge;
import com.leclowndu93150.thaumaturge.content.research.ResearchEntry;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@EventBusSubscriber(modid = KubeThaum.ID)
@GameTestHolder(KubeThaum.ID)
@PrefixGameTestTemplate(false)
public final class KubeThaumSmoke {
    private KubeThaumSmoke() {}

    @SubscribeEvent public static void register(RegisterGameTestsEvent event) {
        if (Boolean.getBoolean("kubethaum.smokeTest")) event.register(KubeThaumSmoke.class);
    }

    @GameTest(template = "empty", timeoutTicks = 1200)
    public static void scriptedResearch(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var entries = registries.registryOrThrow(IResearchEntry.REGISTRY_KEY);
        var tagged = entries.get(ResourceLocation.parse("pack:tagged_research"));
        if (tagged == null || !tagged.stages().getFirst().obtain().getFirst().matches(new ItemStack(Items.OAK_PLANKS))) {
            helper.fail("Item tags were lost during research registry loading"); return;
        }
        var basic = entries.get(ResourceLocation.parse("kubejs:basic_metallurgy"));
        var advanced = entries.get(ResourceLocation.parse("kubejs:advanced_metallurgy"));
        var edited = entries.get(ResourceLocation.parse("thaumaturge:metallurgy"));
        if (basic == null || advanced == null || edited == null) { helper.fail("Scripted research missing"); return; }
        var source = entries.get(ResourceLocation.parse("pack:copy_source"));
        var copy = entries.get(ResourceLocation.parse("pack:copy_snapshot"));
        var nativeCopy = entries.get(ResourceLocation.parse("pack:native_copy"));
        if (source == null || copy == null || nativeCopy == null
            || !source.nameKey().equals("Edited after copy") || !copy.nameKey().equals("Independent copy")
            || copy.stages().size() != 1 || !source.stages().getFirst().obtain().getFirst().matches(new ItemStack(Items.OAK_PLANKS))
            || !copy.stages().getFirst().obtain().getFirst().matches(new ItemStack(Items.GOLD_INGOT))
            || nativeCopy.stages().size() != edited.stages().size()) {
            helper.fail("Research copy did not preserve independent stages/native data"); return;
        }
        if (basic.stages().size() != 2 || basic.stages().get(0).obtain().size() != 2 || basic.addenda().size() != 1) {
            helper.fail("Stage/item/addendum builders did not reach native registry"); return;
        }
        if (advanced.noteAspects().size() != 2 || advanced.complexity() != 2 || advanced.stages().getFirst().warp() != 1) {
            helper.fail("Note aspects/complexity/warp incorrect"); return;
        }
        if (edited.addenda().stream().noneMatch(page -> page.textKey().startsWith("Copper is now"))) {
            helper.fail("Existing research edit did not apply"); return;
        }
        var flagged = entries.get(ResourceLocation.parse("thaumaturge:base_auromancy"));
        if (flagged == null || flagged.addenda().stream().noneMatch(page -> page.textKey().equals("Progress flag regression test"))) {
            helper.fail("Edit of research with native progress flags did not apply"); return;
        }
        if (entries.get(ResourceLocation.parse("pack:runtime_flags")) == null) {
            helper.fail("Research requiring event, scan and ScanKeys progress flags is missing"); return;
        }
        var category = registries.registryOrThrow(IResearchCategory.REGISTRY_KEY).get(ResourceLocation.parse("kubejs:metallurgy"));
        if (category == null || category.formula().size() != 2 || !category.background().getPath().contains("back_3")) {
            helper.fail("Category style/formula incorrect"); return;
        }
        var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        var json = ResearchEntry.CODEC.encodeStart(ops, basic).getOrThrow();
        var decoded = ResearchEntry.CODEC.parse(ops, json).getOrThrow();
        if (decoded.stages().size() != basic.stages().size()) { helper.fail("Native codec roundtrip failed"); return; }
        var recipe = helper.getLevel().getServer().getRecipeManager()
            .byKey(ResourceLocation.parse("kubejs:gold_from_iron")).orElseThrow().value();
        if (!(recipe instanceof ResearchGated gated) || gated.researchGate().isEmpty()) {
            helper.fail("KubeJS recipe gate was lost"); return;
        }
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes("kubethaum-smoke".getBytes()), "KubeThaumTest"));
        var knowledge = (PlayerKnowledge)PlayerResearchAccess.of(player);
        knowledge.wipe();
        var gate = gated.researchGate().orElseThrow();
        if (ResearchGate.passes(player, gate)) { helper.fail("Recipe unlocked before research completion"); return; }
        knowledge.learn(gate.entry());
        knowledge.markComplete(gate.entry());
        if (!ResearchGate.passes(player, gate)) { helper.fail("Recipe still locked after research completion"); return; }
        // Exercise the native save and client-sync codecs without depending on a connected GUI client.
        var partialId = ResourceLocation.parse("kubejs:advanced_metallurgy");
        knowledge.learn(partialId);
        knowledge.setStage(partialId, 1);
        var saved = PlayerKnowledge.CODEC.codec().encodeStart(ops, knowledge).getOrThrow();
        var restored = PlayerKnowledge.CODEC.codec().parse(ops, saved).getOrThrow();
        if (!restored.hasCompleted(gate.entry()) || restored.stageOf(partialId) != 1) {
            helper.fail("Native save codec lost completed or partial scripted progress"); return;
        }
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            PlayerKnowledge.STREAM_CODEC.encode(buffer, restored);
            var synced = PlayerKnowledge.STREAM_CODEC.decode(buffer);
            if (!synced.hasCompleted(gate.entry()) || synced.stageOf(partialId) != 1) {
                helper.fail("Native network codec lost scripted progress"); return;
            }
        } finally { buffer.release(); }
        try {
            RecipeSmoke.verify(helper.getLevel().getServer());
            ScriptCheckSmoke.verify(helper.getLevel().getServer());
        }
        catch (Exception | AssertionError error) { helper.fail(error.getMessage()); return; }
        var server = helper.getLevel().getServer();
        var reload = server.reloadResources(server.getPackRepository().getSelectedIds());
        helper.succeedWhen(() -> {
            if (!reload.isDone()) throw new GameTestAssertException("Waiting for recipe/data-map reload");
            reload.join();
            try { RecipeSmoke.verify(server); }
            catch (AssertionError error) { helper.fail(error.getMessage()); }
        });
    }
}
