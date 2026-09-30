package com.mpp.aedialsworks.data;

import com.mpp.aedialsworks.common.registry.AWBlocks;
import java.util.List;
import java.util.Set;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;

/** Missing loot for any registered block fails datagen instead of silently shipping a broken block. */
public final class AWBlockLoot extends BlockLootSubProvider {
    private final List<AWDataModule> modules;

    public AWBlockLoot(List<AWDataModule> modules) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
        this.modules = List.copyOf(modules);
    }

    @Override protected void generate() {
        modules.forEach(module -> module.blockLoot(this));
    }

    @Override protected Iterable<Block> getKnownBlocks() {
        return AWBlocks.BLOCKS.getEntries().stream().map(entry -> entry.get()).toList();
    }

    public void selfDrop(Block block) { dropSelf(block); }
    public void table(Block block, LootTable.Builder table) { add(block, table); }
}
