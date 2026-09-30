package com.mpp.aedialsworks.cells.cell;
import java.util.List;
import appeng.api.config.FuzzyMode;
import appeng.api.stacks.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ResourceFiltersTest {
    private static final class FakeKey extends AEKey {
        @Override public AEKeyType getType(){return null;}
        @Override public AEKey dropSecondary(){return this;}
        @Override public CompoundTag toTag(){return new CompoundTag();}
        @Override public Object getPrimaryKey(){return this;}
        @Override public ResourceLocation getId(){return new ResourceLocation("test","key");}
        @Override public void writeToPacket(FriendlyByteBuf data){}
        @Override protected Component computeDisplayName(){return null;}
        @Override public void addDrops(long amount,List<ItemStack> drops,Level level,BlockPos pos){}
    }
    @Test void emptyFilterAlwaysMatches(){var key=new FakeKey();assertTrue(ResourceFilters.matches(key,List.of(),false,false,FuzzyMode.IGNORE_ALL,""));assertTrue(ResourceFilters.matches(key,List.of(),true,false,FuzzyMode.IGNORE_ALL,""));}
    @Test void nonEmptyInverseFlipsMatch(){var a=new FakeKey();var b=new FakeKey();assertTrue(ResourceFilters.matches(a,List.of(a),false,false,FuzzyMode.IGNORE_ALL,""));assertFalse(ResourceFilters.matches(b,List.of(a),false,false,FuzzyMode.IGNORE_ALL,""));assertFalse(ResourceFilters.matches(a,List.of(a),true,false,FuzzyMode.IGNORE_ALL,""));assertTrue(ResourceFilters.matches(b,List.of(a),true,false,FuzzyMode.IGNORE_ALL,""));}
    @Test void nullKeyNeverMatches(){assertFalse(ResourceFilters.matches(null,List.of(),false,false,FuzzyMode.IGNORE_ALL,""));}
}
