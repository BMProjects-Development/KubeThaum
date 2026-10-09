package org.bmp.kubethaum;

import com.leclowndu93150.thaumaturge.api.capability.KnowledgeAccess;
import com.leclowndu93150.thaumaturge.api.recipe.ResearchGate;
import com.leclowndu93150.thaumaturge.api.recipe.ResearchGated;
import com.leclowndu93150.thaumaturge.api.research.IResearchCategory;
import com.leclowndu93150.thaumaturge.api.research.IResearchEntry;
import com.leclowndu93150.thaumaturge.content.research.PlayerKnowledge;
import com.leclowndu93150.thaumaturge.content.research.ResearchEntry;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@EventBusSubscriber(modid = KubeThaum.ID)
public final class KubeThaumSmoke extends GameTestInstance {
    public KubeThaumSmoke(TestData<Holder<TestEnvironmentDefinition<?>>> data) { super(data); }

    @SubscribeEvent public static void register(RegisterGameTestsEvent event) {
        if (!Boolean.getBoolean("kubethaum.smokeTest")) return;
        var environment = event.registerEnvironment(Identifier.parse("kubethaum:smoke"), new TestEnvironmentDefinition.AllOf(List.of()));
        var data = new TestData<>(environment, Identifier.parse("thaumaturge:empty_5x5x7"), 40, 0, true, Rotation.NONE);
        event.registerTest(Identifier.parse("kubethaum:scripted_research"), new KubeThaumSmoke(data));
    }

    @Override public void run(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var entries = registries.lookupOrThrow(IResearchEntry.REGISTRY_KEY);
        var tagged = entries.getValue(Identifier.parse("pack:tagged_research"));
        if (tagged == null || !tagged.stages().getFirst().obtain().getFirst().matches(new ItemStack(Items.OAK_PLANKS))) {
            helper.fail("Item tags were lost during research registry loading"); return;
        }
        var basic = entries.getValue(Identifier.parse("pack:basic_metallurgy"));
        var advanced = entries.getValue(Identifier.parse("pack:advanced_metallurgy"));
        var edited = entries.getValue(Identifier.parse("thaumaturge:metallurgy"));
        if (basic == null || advanced == null || edited == null) { helper.fail("Scripted research missing"); return; }
        var source = entries.getValue(Identifier.parse("pack:copy_source"));
        var copy = entries.getValue(Identifier.parse("pack:copy_snapshot"));
        var nativeCopy = entries.getValue(Identifier.parse("pack:native_copy"));
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
        var category = registries.lookupOrThrow(IResearchCategory.REGISTRY_KEY).getValue(Identifier.parse("pack:metallurgy"));
        if (category == null || category.formula().size() != 2 || !category.background().getPath().contains("back_3")) {
            helper.fail("Category style/formula incorrect"); return;
        }
        var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        var json = ResearchEntry.CODEC.encodeStart(ops, basic).getOrThrow();
        var decoded = ResearchEntry.CODEC.parse(ops, json).getOrThrow();
        if (decoded.stages().size() != basic.stages().size()) { helper.fail("Native codec roundtrip failed"); return; }
        var recipe = helper.getLevel().getServer().getRecipeManager()
            .byKey(ResourceKey.create(Registries.RECIPE, Identifier.parse("pack:enchanted_alloy"))).orElseThrow().value();
        if (!(recipe instanceof ResearchGated gated) || gated.researchGate().isEmpty()) {
            helper.fail("KubeJS recipe gate was lost"); return;
        }
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes("kubethaum-smoke".getBytes()), "KubeThaumTest"));
        var knowledge = (PlayerKnowledge)KnowledgeAccess.of(player);
        knowledge.clear();
        var gate = gated.researchGate().orElseThrow();
        if (ResearchGate.passes(player, gate)) { helper.fail("Recipe unlocked before research completion"); return; }
        knowledge.addResearch(gate.entry());
        knowledge.markComplete(gate.entry());
        if (!ResearchGate.passes(player, gate)) { helper.fail("Recipe still locked after research completion"); return; }
        // Exercise the native save and client-sync codecs without depending on a connected GUI client.
        var partialId = Identifier.parse("pack:advanced_metallurgy");
        knowledge.addResearch(partialId);
        knowledge.setResearchStage(partialId, 1);
        var saved = PlayerKnowledge.CODEC.codec().encodeStart(ops, knowledge).getOrThrow();
        var restored = PlayerKnowledge.CODEC.codec().parse(ops, saved).getOrThrow();
        if (!restored.isResearchComplete(gate.entry()) || restored.researchStage(partialId) != 1) {
            helper.fail("Native save codec lost completed or partial scripted progress"); return;
        }
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            PlayerKnowledge.STREAM_CODEC.encode(buffer, restored);
            var synced = PlayerKnowledge.STREAM_CODEC.decode(buffer);
            if (!synced.isResearchComplete(gate.entry()) || synced.researchStage(partialId) != 1) {
                helper.fail("Native network codec lost scripted progress"); return;
            }
        } finally { buffer.release(); }
        helper.succeed();
    }
    @Override public MapCodec<? extends GameTestInstance> codec() { return MapCodec.unit(this); }
    @Override protected MutableComponent typeDescription() { return Component.literal("kubethaum:smoke"); }
}
