package com.mpp.aedialsworks.powertools.maintainer;
import appeng.api.networking.crafting.*;
import appeng.api.storage.StorageHelper;
import net.minecraft.nbt.CompoundTag;
public final class MaintainerTask extends AbstractCraftingTask {
    long nextAttempt;
    public void load(CompoundTag tag, ICraftingRequester owner) {
        unload(); link=null;
        if(tag.contains("link")) { link=StorageHelper.loadCraftingLink(tag.getCompound("link"),owner); state=State.CRAFTING; }
    }
    public void save(CompoundTag tag) {
        if(link!=null && !link.isDone() && !link.isCanceled()) { var n=new CompoundTag();link.writeToNBT(n);tag.put("link",n); }
        tag.putString("state",state.name());
    }
}
