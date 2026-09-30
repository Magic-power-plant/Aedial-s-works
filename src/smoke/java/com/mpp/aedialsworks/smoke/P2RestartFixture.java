package com.mpp.aedialsworks.smoke;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import appeng.api.config.Actionable;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import com.mpp.aedialsworks.common.registry.AWBlocks;
import com.mpp.aedialsworks.powertools.integration.ae2.PowerBlockEntity;
import com.mpp.aedialsworks.powertools.maintainer.MaintainerLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/** Separate JVM write/read acceptance run against a real saved world, without replacing loaded entities. */
public final class P2RestartFixture {
    private static final Path REPORT=Path.of(System.getProperty("aedialsworks.smokeReportDir"),"p2-restart");
    private static final BlockPos MACHINE=new BlockPos(1,64,1),DRIVE=new BlockPos(2,64,1),PROVIDER=new BlockPos(3,64,1),CHEST=new BlockPos(3,64,2),CPU=new BlockPos(4,64,1);
    private static String mode;
    private static long start;
    private static int phase;
    private static UUID expected;
    private static long writerPid;
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
    public static void begin(ServerPlayer player,String requested){
        try {
            require(requested.equals("write")||requested.equals("read"),"Restart mode must be write or read");mode=requested;var level=player.serverLevel();start=level.getGameTime();Files.createDirectories(REPORT);
            player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();player.teleportTo(level,2.5,66,-2.5,0,20);level.getChunkAt(MACHINE);
            if(mode.equals("write")){
                for(int x=0;x<7;x++)for(int z=0;z<5;z++)for(int y=63;y<68;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),y==63?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(MACHINE,AWBlocks.BETTER_LEVEL_MAINTAINER.get().defaultBlockState());
                level.setBlockAndUpdate(DRIVE,AEBlocks.DRIVE.block().defaultBlockState());
                level.setBlockAndUpdate(DRIVE.below(),AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
                level.setBlockAndUpdate(PROVIDER,AEBlocks.PATTERN_PROVIDER.block().defaultBlockState());
                level.setBlockAndUpdate(CHEST,Blocks.CHEST.defaultBlockState());level.setBlockAndUpdate(CPU,AEBlocks.CRAFTING_STORAGE_1K.block().defaultBlockState());
                ((DriveBlockEntity)level.getBlockEntity(DRIVE)).getInternalInventory().setItemDirect(0,AEItems.ITEM_CELL_64K.stack());
            }else{
                var saved=Files.readAllLines(REPORT.resolve("checkpoint.txt"),StandardCharsets.UTF_8);expected=UUID.fromString(saved.get(0));writerPid=Long.parseLong(saved.get(1));
                require(writerPid!=ProcessHandle.current().pid(),"Verification must run in a new JVM");
                require(level.getBlockEntity(MACHINE) instanceof PowerBlockEntity,"Saved maintainer exists after restart");
            }
        }catch(Exception e){throw new IllegalStateException("Restart fixture setup",e);}
    }
    public static boolean step(ServerPlayer player){
        try {
            var level=player.serverLevel();long age=level.getGameTime()-start;if(age<80)return false;
            require(age<1600,"Persistent CPU job did not recover before timeout");
            var be=(PowerBlockEntity)level.getBlockEntity(MACHINE);var logic=(MaintainerLogic)be.logic();var grid=be.getMainNode().getGrid();
            require(grid!=null&&be.getMainNode().isActive(),"Physical network survives construction/restart");
            var inv=grid.getStorageService().getInventory();
            if(mode.equals("write")){
                if(phase==0){
                    var provider=(PatternProviderLogicHost)level.getBlockEntity(PROVIDER);
                    provider.getLogic().getPatternInv().setItemDirect(0,PatternDetailsHelper.encodeProcessingPattern(new GenericStack[]{new GenericStack(AEItemKey.of(Items.IRON_INGOT),1)},new GenericStack[]{new GenericStack(AEItemKey.of(Items.DIAMOND),1)}));
                    inv.insert(AEItemKey.of(Items.IRON_INGOT),1,Actionable.MODULATE,IActionSource.empty());logic.entries[0].key=AEItemKey.of(Items.DIAMOND);logic.entries[0].threshold=1;logic.batch[0]=1;be.powerChanged();phase=1;return false;
                }
                if(logic.getRequestedJobs().isEmpty())return false;
                var chest=(ChestBlockEntity)level.getBlockEntity(CHEST);int iron=0;for(int i=0;i<chest.getContainerSize();i++)if(chest.getItem(i).is(Items.IRON_INGOT))iron+=chest.getItem(i).getCount();if(iron!=1)return false;
                var link=logic.getRequestedJobs().iterator().next();require(!link.isDone()&&!link.isCanceled(),"CPU request remains in progress before shutdown");
                Files.writeString(REPORT.resolve("checkpoint.txt"),link.getCraftingID()+"\n"+ProcessHandle.current().pid()+"\n",StandardCharsets.UTF_8);return true;
            }
            if(phase==0){
                require(logic.getRequestedJobs().size()==1,"Exactly one request restored from disk");
                require(logic.getRequestedJobs().iterator().next().getCraftingID().equals(expected),"Original crafting UUID restored in the new JVM");
                require(grid.getStorageService().getCachedInventory().get(AEItemKey.of(Items.DIAMOND))==0,"CPU still waits for processing output");
                require(inv.insert(AEItemKey.of(Items.DIAMOND),1,Actionable.MODULATE,IActionSource.empty())==1,"Resumed CPU accepts processing result");phase=1;return false;
            }
            if(!logic.getRequestedJobs().isEmpty())return false;
            require(grid.getStorageService().getCachedInventory().get(AEItemKey.of(Items.DIAMOND))==1,"Exactly one output returned after full restart");
            Files.writeString(REPORT.resolve("restart.txt"),"PASS: full JVM shutdown and saved-world restart; original CPU link UUID "+expected+" restored; one processing output returned; completed link retired. Writer PID="+writerPid+", reader PID="+ProcessHandle.current().pid()+"\n",StandardCharsets.UTF_8);return true;
        }catch(Exception e){throw new IllegalStateException("Persistent crafting verification",e);}
    }
}
