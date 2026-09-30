package com.mpp.aedialsworks.cells.cell;
import java.util.*;
import appeng.api.config.FuzzyMode;
import appeng.api.stacks.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
/** Shared item/fluid filtering for cells, interfaces and network views. */
public final class ResourceFilters {
    private ResourceFilters(){}
    public static boolean matches(AEKey key,Collection<AEKey> selected,boolean inverse,boolean fuzzy,FuzzyMode mode,String tag){
        if(key==null)return false;
        boolean hasTag=tag!=null&&!tag.isBlank();
        boolean matched=selected.isEmpty()&&!hasTag;
        for(var wanted:selected)if(wanted.equals(key)||(fuzzy&&wanted.fuzzyEquals(key,mode))){matched=true;break;}
        if(hasTag){var id=ResourceLocation.tryParse(tag);if(id!=null)matched|=key.isTagged(key instanceof AEFluidKey?TagKey.create(Registries.FLUID,id):TagKey.create(Registries.ITEM,id));}
        return inverse?!matched:matched;
    }
}
