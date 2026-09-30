package com.mpp.aedialsworks.data;

import com.mpp.aedialsworks.Aedialsworks;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public final class AWBlockTags extends BlockTagsProvider {
    private final List<AWDataModule> modules;

    public AWBlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
            ExistingFileHelper existing, List<AWDataModule> modules) {
        super(output, lookup, Aedialsworks.MODID, existing);
        this.modules = List.copyOf(modules);
    }

    @Override protected void addTags(HolderLookup.Provider lookup) {
        modules.forEach(module -> module.blockTags(this));
    }

    public void add(TagKey<Block> key, Block... blocks) { tag(key).add(blocks); }
}
