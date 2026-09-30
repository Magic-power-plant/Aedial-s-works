package com.mpp.aedialsworks.cells.command;
import com.mojang.brigadier.arguments.*;
import com.mpp.aedialsworks.Aedialsworks;
import com.mpp.aedialsworks.cells.cell.*;
import com.mpp.aedialsworks.cells.api.CellsHost;
import com.mpp.aedialsworks.cells.integration.ae2.CellsPart;
import com.mpp.aedialsworks.cells.subnetproxy.ProxyLogic;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.*;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.*;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=Aedialsworks.MODID)
public final class CellsCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent e){var d=e.getDispatcher();
        d.register(Commands.literal("fillcell").requires(s->s.hasPermission(2)).then(Commands.literal("item").then(Commands.argument("resource",ItemArgument.item(e.getBuildContext())).then(Commands.argument("amount",LongArgumentType.longArg(1)).executes(c->fill(c.getSource(),AEItemKey.of(ItemArgument.getItem(c,"resource").createItemStack(1,false)),LongArgumentType.getLong(c,"amount")))))).then(Commands.literal("fluid").then(Commands.argument("resource",ResourceLocationArgument.id()).then(Commands.argument("amount",LongArgumentType.longArg(1)).executes(c->{var id=ResourceLocationArgument.getId(c,"resource");if(!BuiltInRegistries.FLUID.containsKey(id))return 0;return fill(c.getSource(),AEFluidKey.of(BuiltInRegistries.FLUID.get(id)),LongArgumentType.getLong(c,"amount"));})))));
        d.register(Commands.literal("inspectslot").requires(s->s.hasPermission(2)).then(Commands.argument("slot",IntegerArgumentType.integer(0,35)).executes(c->{var p=c.getSource().getPlayerOrException();var s=p.getInventory().getItem(IntegerArgumentType.getInteger(c,"slot"));var cell=CellHandler.open(s,null);String info=cell==null?s.toString():s.getHoverName().getString()+"; units="+cell.storedUnits()+"; bytes="+cell.usedBytes()+"/"+cell.totalBytes()+"; types="+cell.storedTypes()+"/"+cell.maximumTypes();c.getSource().sendSuccess(()->Component.literal(info),false);return 1;})));
        d.register(Commands.literal("inspectsubnetproxy").requires(s->s.hasPermission(2)).then(Commands.argument("pos",BlockPosArgument.blockPos()).then(Commands.argument("side",StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(new String[]{"north","south","east","west","up","down"},b)).executes(c->{var pos=BlockPosArgument.getLoadedBlockPos(c,"pos");var side=Direction.byName(StringArgumentType.getString(c,"side"));if(side==null)return 0;var part=appeng.api.parts.PartHelper.getPart(c.getSource().getLevel(),pos,side);if(!(part instanceof CellsPart p)||!(p.logic instanceof ProxyLogic logic))return 0;c.getSource().sendSuccess(()->Component.literal("CELLS proxy "+logic.guardStatus()+"; priority="+logic.priority+"; filter types="+logic.filters.keySet().size()+"; insertion="+logic.installed("insertion_card")),false);return 1;}))));
    }
    private static int fill(CommandSourceStack source,AEKey key,long amount)throws com.mojang.brigadier.exceptions.CommandSyntaxException{var p=source.getPlayerOrException();var stack=p.getMainHandItem();var cell=CellHandler.open(stack,null);if(cell==null||key==null)return 0;long inserted=cell.insert(key,amount,Actionable.MODULATE,IActionSource.ofPlayer(p));cell.persist();p.getInventory().setChanged();source.sendSuccess(()->Component.literal("Accepted "+inserted+"; stored units="+cell.storedUnits()),true);return inserted>0?1:0;}
}
