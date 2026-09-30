package com.mpp.aedialsworks.cells;

import appeng.api.config.FuzzyMode;
import appeng.api.networking.*;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.*;
import appeng.api.stacks.*;
import appeng.api.upgrades.*;
import appeng.core.definitions.AEItems;
import appeng.util.ConfigInventory;
import com.mpp.aedialsworks.cells.api.*;
import com.mpp.aedialsworks.cells.cell.ResourceFilters;
import com.mpp.aedialsworks.cells.upgrades.CellUpgrades;
import com.mpp.aedialsworks.common.config.AWConfigs;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.player.Player;

/** Independent domain lifecycle shared by cable parts and full blocks. */
public abstract class AbstractCellsLogic implements IGridTickable,ResourceFilter {
    protected final CellsHost host;
    protected final IActionSource source;
    public final IUpgradeInventory upgrades;
    public final ConfigInventory filters;
    public int polling,priority;
    public boolean items=true,fluids=true,inverse;
    public FuzzyMode fuzzyMode=FuzzyMode.IGNORE_ALL;
    public String tag="";
    private boolean unloaded,loading;
    private long elapsed;
    protected AbstractCellsLogic(CellsHost host){
        this.host=host;this.source=IActionSource.ofMachine(host);
        upgrades=UpgradeInventories.forMachine(host.cellsItem(),host.cellsKind().proxy()?AWConfigs.SERVER.cells.general.subnetProxyUpgradeSlots.get():4,this::settingsChanged);
        filters=ConfigInventory.configTypes(63,this::settingsChanged);
        host.cellsNode().addService(IGridTickable.class,this);
    }
    public final CellsHost host(){return host;}
    protected final IGrid grid(){return host.cellsNode().getGrid();}
    public boolean active(){return !unloaded&&host.cellsNode().isActive();}
    public boolean installed(String id){return CellUpgrades.has(upgrades,id);}
    public int pages(){return 1+Math.min(4,upgrades.getInstalledUpgrades(AEItems.CAPACITY_CARD));}
    public void settingsChanged(){if(!loading){changed();onSettingsChanged();}}
    protected void onSettingsChanged(){}
    protected final void changed(){host.cellsChanged();}
    public void resume(){unloaded=false;onSettingsChanged();}
    public void unload(){unloaded=true;}
    public boolean unloaded(){return unloaded;}
    @Override public TickingRequest getTickingRequest(IGridNode node){return new TickingRequest(1,60,false,true,1);}
    @Override public TickRateModulation tickingRequest(IGridNode node,int delta){
        if(!active())return TickRateModulation.SLOWER;
        elapsed+=delta;int rate=Math.max(polling,host.cellsKind().resourceInterface()?AWConfigs.SERVER.cells.interfaces.interfaceMinPollingRate.get():1);
        if(rate>0&&elapsed<rate)return TickRateModulation.URGENT;elapsed=0;
        boolean work=tick();return rate>0||work?TickRateModulation.URGENT:TickRateModulation.SLOWER;
    }
    public abstract boolean tick();
    @Override public boolean accepts(AEKey key){
        if(key instanceof AEItemKey&&!items||key instanceof AEFluidKey&&!fluids)return false;
        return ResourceFilters.matches(key,filters.keySet(),inverse||upgrades.isInstalled(AEItems.INVERTER_CARD),upgrades.isInstalled(AEItems.FUZZY_CARD),fuzzyMode,installed("tag_card")?tag:"");
    }
    public CompoundTag saveSettings(){var n=new CompoundTag();n.putInt("polling",polling);n.putInt("priority",priority);n.putBoolean("items",items);n.putBoolean("fluids",fluids);n.putBoolean("inverse",inverse);n.putString("fuzzy",fuzzyMode.name());n.putString("tag",tag);filters.writeToChildTag(n,"filters");saveExtraSettings(n);return n;}
    public void loadSettings(CompoundTag n){
        loading=true;
        try{polling=Math.max(0,n.getInt("polling"));priority=n.getInt("priority");items=!n.contains("items")||n.getBoolean("items");fluids=!n.contains("fluids")||n.getBoolean("fluids");inverse=n.getBoolean("inverse");tag=n.getString("tag");try{fuzzyMode=FuzzyMode.valueOf(n.getString("fuzzy"));}catch(IllegalArgumentException ex){fuzzyMode=FuzzyMode.IGNORE_ALL;}if(n.contains("filters"))filters.readFromChildTag(n,"filters");loadExtraSettings(n);}finally{loading=false;}
        settingsChanged();
    }
    public CompoundTag save(){var n=saveSettings();upgrades.writeToNBT(n,"upgrades");saveContents(n);return n;}
    public void load(CompoundTag n){loading=true;try{upgrades.readFromNBT(n,"upgrades");loadContents(n);}finally{loading=false;}loadSettings(n);}
    protected void saveExtraSettings(CompoundTag n){}
    protected void loadExtraSettings(CompoundTag n){}
    protected void saveContents(CompoundTag n){}
    protected void loadContents(CompoundTag n){}
    public CompoundTag snapshot(int port,int page){var n=saveSettings();n.putString("kind",host.cellsKind().id);n.putBoolean("active",active());n.putInt("pages",pages());return n;}
    public boolean configure(Player player,String action,CompoundTag n){
        switch(action){
            case "settings"->{String next=n.getString("tag");if(next.length()>128||!next.isEmpty()&&net.minecraft.resources.ResourceLocation.tryParse(next)==null)return false;polling=Math.max(0,n.getInt("polling"));priority=n.getInt("priority");items=n.getBoolean("items");fluids=n.getBoolean("fluids");inverse=n.getBoolean("inverse");tag=next;}
            case "fuzzy"->fuzzyMode=FuzzyMode.values()[(fuzzyMode.ordinal()+1)%FuzzyMode.values().length];
            case "filter"->{int i=n.getInt("slot");if(i<0||i>=filters.size())return false;var key=n.contains("key")?AEKey.fromTagGeneric(n.getCompound("key")):null;if(key!=null&&!(key instanceof AEItemKey||key instanceof AEFluidKey))return false;filters.setStack(i,key==null?null:new GenericStack(key,1));}
            default->{return false;}
        }settingsChanged();return true;
    }
    public CompoundTag memorySettings(boolean includeFilters){var n=saveSettings();if(!includeFilters){n.remove("filters");for(var raw:n.getList("ports",Tag.TAG_COMPOUND)){var port=(CompoundTag)raw;port.putBoolean("preserveFilters",true);for(var row:port.getList("slots",Tag.TAG_COMPOUND))((CompoundTag)row).remove("filter");}}return n;}
    /** Settings cards never include storage or upgrade items. Dismantled items do. */
    public void importData(appeng.util.SettingsFrom from,CompoundTag n){if(from==appeng.util.SettingsFrom.DISMANTLE_ITEM)load(n);else loadSettings(n);}
    public CompoundTag exportData(appeng.util.SettingsFrom from){return from==appeng.util.SettingsFrom.DISMANTLE_ITEM?save():saveSettings();}
}
