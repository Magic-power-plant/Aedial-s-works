package com.mpp.aedialsworks.data;

import com.mpp.aedialsworks.Aedialsworks;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;

public final class AWItemTags extends ItemTagsProvider {
    private final List<AWDataModule> modules;

    public AWItemTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
            CompletableFuture<TagsProvider.TagLookup<Block>> blockTags, ExistingFileHelper existing,
            List<AWDataModule> modules) {
        super(output, lookup, blockTags, Aedialsworks.MODID, existing);
        this.modules = List.copyOf(modules);
    }

    @Override protected void addTags(HolderLookup.Provider lookup) {
        modules.forEach(module -> module.itemTags(this));
    }

    public void add(TagKey<Item> key, Item... items) { tag(key).add(items); }
    public void copyBlockTag(TagKey<Block> blocks, TagKey<Item> items) { copy(blocks, items); }
}
