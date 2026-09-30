package com.mpp.aedialsworks.powertools.integration.ae2;
import java.util.List;
import com.mpp.aedialsworks.powertools.*;
import com.mpp.aedialsworks.powertools.maintainer.MaintainerLogic;
import com.mpp.aedialsworks.powertools.crafter.CrafterLogic;
import com.mpp.aedialsworks.powertools.monitor.MonitorLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
public final class PowerBlockEntity extends AbstractPowerBlockEntity {
    public final MachineKind kind;
    public PowerBlockEntity(BlockEntityType<?> type,BlockPos pos,BlockState state){super(type,pos,state);kind=((PowerMachineBlock)state.getBlock()).kind;
        logic=switch(kind){case MAINTAINER->new MaintainerLogic(this);case CRAFTER->new CrafterLogic(this);default->new MonitorLogic(this,kind==MachineKind.ALARM);};
    }
    @Override public String powerKind(){return kind.id;}
    public int signal(){return kind==MachineKind.EMITTER && powerActive() && ((MonitorLogic)logic).settings.condition?((MonitorLogic)logic).settings.strength:0;}
    @Override public void addAdditionalDrops(Level level,BlockPos pos,List<ItemStack> drops){super.addAdditionalDrops(level,pos,drops);if(logic instanceof CrafterLogic crafter)crafter.drops(drops);}
    @Override public void clearContent(){if(logic instanceof CrafterLogic crafter)crafter.clear();}
}
