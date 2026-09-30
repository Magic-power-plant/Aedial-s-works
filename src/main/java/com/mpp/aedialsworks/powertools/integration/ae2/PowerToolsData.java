package com.mpp.aedialsworks.powertools.integration.ae2;
import java.util.function.Consumer;
import com.google.gson.*;
import com.mpp.aedialsworks.common.registry.*;
import com.mpp.aedialsworks.common.util.AWIds;
import com.mpp.aedialsworks.data.*;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.client.model.generators.*;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.registries.ForgeRegistries;
public final class PowerToolsData implements AWDataModule {
    @Override public void language(String locale,LanguageProvider p){boolean zh=locale.equals("zh_cn");
        p.add("item.aedialsworks.better_level_maintainer",zh?"\u6539\u8fdb\u7b49\u7ea7\u7ef4\u62a4\u5668":"Better Level Maintainer");
        p.add("block.aedialsworks.better_level_maintainer",zh?"\u6539\u8fdb\u7b49\u7ea7\u7ef4\u62a4\u5668":"Better Level Maintainer");
        p.add("item.aedialsworks.auto_crafter",zh?"\u81ea\u52a8\u5408\u6210\u5668":"Auto Crafter");
        p.add("block.aedialsworks.auto_crafter",zh?"\u81ea\u52a8\u5408\u6210\u5668":"Auto Crafter");
        p.add("item.aedialsworks.storage_level_emitter",zh?"\u5b58\u50a8\u80fd\u7ea7\u53d1\u4fe1\u5668":"Storage Level Emitter");
        p.add("block.aedialsworks.storage_level_emitter",zh?"\u5b58\u50a8\u80fd\u7ea7\u53d1\u4fe1\u5668":"Storage Level Emitter");
        p.add("item.aedialsworks.storage_display",zh?"\u5b58\u50a8\u663e\u793a\u5668":"Storage Display");
        p.add("block.aedialsworks.storage_display",zh?"\u5b58\u50a8\u663e\u793a\u5668":"Storage Display");
        p.add("item.aedialsworks.storage_level_alarm",zh?"\u5b58\u50a8\u76d1\u63a7\u8b66\u62a5\u5668":"Storage Level Alarm");
        p.add("block.aedialsworks.storage_level_alarm",zh?"\u5b58\u50a8\u76d1\u63a7\u8b66\u62a5\u5668":"Storage Level Alarm");
        p.add("item.aedialsworks.storage_level_emitter_part",zh?"\u5b58\u50a8\u80fd\u7ea7\u53d1\u4fe1\u5668\u90e8\u4ef6":"Storage Level Emitter Part");
        p.add("item.aedialsworks.storage_display_part",zh?"\u5b58\u50a8\u663e\u793a\u5668\u90e8\u4ef6":"Storage Display Part");
        p.add("item.aedialsworks.storage_display_part_smaller",zh?"\u5c0f\u578b\u5b58\u50a8\u663e\u793a\u5668\u90e8\u4ef6":"Small Storage Display Part");
        p.add("item.aedialsworks.storage_display_part_smallerer",zh?"\u5fae\u578b\u5b58\u50a8\u663e\u793a\u5668\u90e8\u4ef6":"Tiny Storage Display Part");
        p.add("item.aedialsworks.network_health_scanner",zh?"\u7f51\u7edc\u5065\u5eb7\u626b\u63cf\u5668":"Network Health Scanner");
        p.add("item.aedialsworks.network_component_locator",zh?"\u7f51\u7edc\u7ec4\u4ef6\u5b9a\u4f4d\u5668":"Network Component Locator");
        p.add("item.aedialsworks.priority_tuner",zh?"\u4f18\u5148\u7ea7\u8c03\u8282\u5668":"Priority Tuner");
        p.add("item.aedialsworks.cards_distributor",zh?"\u5347\u7ea7\u5361\u5206\u53d1\u5668":"Cards Distributor");
        p.add("item.aedialsworks.storage_level_alarm_locator",zh?"\u8b66\u62a5\u5668\u5b9a\u4f4d\u5668":"Alarm Locator");
        p.add("item.aedialsworks.remote_storage_monitor",zh?"\u8fdc\u7a0b\u5b58\u50a8\u76d1\u89c6\u5668":"Remote Storage Monitor");
        p.add("item.aedialsworks.crafter_speed_upgrade_i",zh?"\u5408\u6210\u901f\u5ea6\u5347\u7ea7\u5361 I":"Crafter Speed Upgrade I");
        p.add("item.aedialsworks.crafter_speed_upgrade_ii",zh?"\u5408\u6210\u901f\u5ea6\u5347\u7ea7\u5361 II":"Crafter Speed Upgrade II");
        p.add("item.aedialsworks.crafter_speed_upgrade_iii",zh?"\u5408\u6210\u901f\u5ea6\u5347\u7ea7\u5361 III":"Crafter Speed Upgrade III");
        p.add("item.aedialsworks.crafter_speed_upgrade_iv",zh?"\u5408\u6210\u901f\u5ea6\u5347\u7ea7\u5361 IV":"Crafter Speed Upgrade IV");
        p.add("gui.aedialsworks.powertools.entry",zh?"\u6761\u76ee":"Entry");
        p.add("gui.aedialsworks.powertools.target",zh?"\u76ee\u6807\u91cf":"Target");
        p.add("gui.aedialsworks.powertools.batch",zh?"\u6279\u91cf":"Batch");
        p.add("gui.aedialsworks.powertools.reset",zh?"\u590d\u4f4d\u91cf":"Reset");
        p.add("gui.aedialsworks.powertools.ticks",zh?"\u95f4\u9694\u523b":"Ticks");
        p.add("gui.aedialsworks.powertools.compare",zh?"\u6bd4\u8f83\u7b26":"Compare");
        p.add("gui.aedialsworks.powertools.enabled",zh?"\u542f\u7528\u5207\u6362":"Toggle");
        p.add("gui.aedialsworks.powertools.save",zh?"\u4fdd\u5b58":"Save");
        p.add("gui.aedialsworks.powertools.clear",zh?"\u6e05\u7a7a":"Clear");
        p.add("gui.aedialsworks.powertools.active",zh?"启用":"Active");
        p.add("gui.aedialsworks.powertools.back",zh?"返回":"Back");
        p.add("gui.aedialsworks.powertools.inventory",zh?"玩家物品栏":"Player inventory");
        p.add("gui.aedialsworks.powertools.choose_resource",zh?"选择资源":"Select resource");
        p.add("gui.aedialsworks.powertools.settings",zh?"设置":"Settings");
        p.add("gui.aedialsworks.powertools.recipes",zh?"配方":"Recipes");
        p.add("gui.aedialsworks.powertools.locate",zh?"定位":"Locate");
        p.add("gui.aedialsworks.powertools.networks",zh?"个网络":"networks");
        p.add("gui.aedialsworks.powertools.no_components",zh?"没有匹配的组件":"No matching components");
        p.add("gui.aedialsworks.powertools.match_mode",zh?"匹配模式：全部 / 任一":"Match mode: AND / OR");
        p.add("gui.aedialsworks.powertools.maintainer_hint",zh?"点击条目编辑；点击箭头请求合成。":"Click an entry to edit; arrow to request crafting.");
        p.add("gui.aedialsworks.powertools.remote_hint",zh?"点击资源配置监控。":"Click a resource to configure monitoring.");
        p.add("gui.aedialsworks.powertools.selector_hint",zh?"点击背包物品；Ctrl 点击容器选择流体。":"Click inventory; Ctrl-click a container for fluid.");
        p.add("gui.aedialsworks.powertools.machine",zh?"\u901f\u5ea6\uff0f\u6279\u91cf":"Speed / batch");
        p.add("gui.aedialsworks.powertools.run",zh?"\u7acb\u5373\u8fd0\u884c":"Run now");
        p.add("gui.aedialsworks.powertools.cancel",zh?"\u53d6\u6d88\u4efb\u52a1":"Cancel");
        p.add("gui.aedialsworks.powertools.hysteresis",zh?"\u8fdf\u6ede\u5f00\u5173":"Hysteresis");
        p.add("gui.aedialsworks.powertools.strength",zh?"\u4fe1\u53f7 1\u201315":"Signal 1\u201315");
        p.add("gui.aedialsworks.powertools.bind",zh?"\u7ed1\u5b9a\uff0f\u89e3\u7ed1\u81ea\u5df1":"Bind / unbind me");
        p.add("gui.aedialsworks.powertools.bound",zh?"\u8b66\u62a5\u5668\u5df2\u7ed1\u5b9a":"Alarm bound");
        p.add("gui.aedialsworks.powertools.unbound",zh?"\u8b66\u62a5\u5668\u5df2\u89e3\u7ed1":"Alarm unbound");
        p.add("gui.aedialsworks.powertools.hud",zh?"\u5207\u6362\u8fdc\u7a0b HUD":"Toggle remote HUD");
        p.add("gui.aedialsworks.powertools.upgrades",zh?"\u901f\u5ea6\u5347\u7ea7\u5361":"Speed upgrades");
        p.add("gui.aedialsworks.powertools.search",zh?"\u641c\u7d22\u7ec4\u4ef6":"Search components");
        p.add("gui.aedialsworks.powertools.scan",zh?"\u91cd\u65b0\u626b\u63cf":"Rescan");
        p.add("gui.aedialsworks.powertools.apply_priority",zh?"\u8bbe\u7f6e\u4f18\u5148\u7ea7":"Set priority");
        p.add("gui.aedialsworks.powertools.invalid",zh?"\u6570\u503c\u65e0\u6548":"Invalid number");
        p.add("gui.aedialsworks.powertools.saved",zh?"\u5df2\u4fdd\u5b58":"Saved");
        p.add("gui.aedialsworks.powertools.no_issues",zh?"\u65e0\u7b26\u5408\u6761\u4ef6\u7684\u7ed3\u679c":"No matching results");
        p.add("gui.aedialsworks.powertools.truncated",zh?"\u5df2\u8fbe\u626b\u63cf\u4e0a\u9650":"Scan limit reached");
        p.add("gui.aedialsworks.powertools.alarm",zh?"\u5b58\u50a8\u8b66\u62a5":"Storage alarm");
        p.add("gui.aedialsworks.powertools.remote",zh?"\u8fdc\u7a0b\u5b58\u50a8":"Remote storage");
        p.add("gui.aedialsworks.powertools.distributed",zh?"\u5df2\u5206\u53d1 %s \u5f20\u5347\u7ea7\u5361":"Distributed %s cards");
        p.add("gui.aedialsworks.powertools.tab_loops",zh?"\u73af\u8def":"Loops");
        p.add("gui.aedialsworks.powertools.tab_unloaded",zh?"\u672a\u52a0\u8f7d\u533a\u5757":"Unloaded chunks");
        p.add("gui.aedialsworks.powertools.tab_bottlenecks",zh?"\u901a\u9053\u74f6\u9888":"Bottlenecks");
        p.add("gui.aedialsworks.powertools.tab_channels",zh?"\u7f3a\u5c11\u9891\u9053":"Missing channels");
        p.add("gui.aedialsworks.powertools.tab_fatal",zh?"\u4e25\u91cd\u9519\u8bef":"Fatal errors");
        p.add("gui.aedialsworks.powertools.tab_patterns",zh?"\u6837\u677f\u95ee\u9898":"Patterns");
        p.add("gui.aedialsworks.powertools.issue_NETWORK_LOOP",zh?"\u7f51\u7edc\u73af\u8def":"Network loop");
        p.add("gui.aedialsworks.powertools.issue_CONTROLLER_CONFLICT",zh?"控制器冲突":"Controller conflict");
        p.add("gui.aedialsworks.powertools.issue_NO_POWER",zh?"\u7f51\u7edc\u672a\u4f9b\u7535":"No power");
        p.add("gui.aedialsworks.powertools.issue_MISSING_CHANNEL",zh?"\u7f3a\u5c11\u9891\u9053":"Missing channel");
        p.add("gui.aedialsworks.powertools.issue_CHANNEL_SATURATED",zh?"\u901a\u9053\u5df2\u6ee1":"Channels saturated");
        p.add("gui.aedialsworks.powertools.issue_UNLOADED_BOUNDARY",zh?"\u672a\u52a0\u8f7d\u533a\u5757\u8fb9\u754c":"Unloaded chunk boundary");
        p.add("gui.aedialsworks.powertools.issue_INVALID_PATTERN",zh?"\u5931\u6548\u6837\u677f":"Invalid pattern");
        p.add("gui.aedialsworks.powertools.issue_DUPLICATE_OUTPUT",zh?"\u91cd\u590d\u4ea7\u7269\u6837\u677f":"Duplicate output pattern");
        p.add("gui.aedialsworks.powertools.state_IDLE",zh?"\u7a7a\u95f2":"Idle");
        p.add("gui.aedialsworks.powertools.state_CALCULATING",zh?"\u8ba1\u7b97\u4e2d":"Calculating");
        p.add("gui.aedialsworks.powertools.state_WAITING_CPU",zh?"\u7b49\u5f85 CPU":"Waiting for CPU");
        p.add("gui.aedialsworks.powertools.state_CRAFTING",zh?"\u5408\u6210\u4e2d":"Crafting");
        p.add("gui.aedialsworks.powertools.state_MISSING_RESOURCES",zh?"\u7f3a\u5c11\u539f\u6599":"Missing resources");
        p.add("gui.aedialsworks.powertools.state_NOT_CRAFTABLE",zh?"\u4e0d\u53ef\u5408\u6210":"Not craftable");
        p.add("gui.aedialsworks.powertools.state_INVALID_PATTERN",zh?"\u65e0\u6548\u5408\u6210\u6837\u677f":"Invalid crafting pattern");
        p.add("gui.aedialsworks.powertools.state_ITEM_RECIPE_REQUIRED",zh?"\u8bf7\u4f7f\u7528\u7269\u54c1\u5408\u6210\u6837\u677f":"Use an item crafting pattern");
        p.add("gui.aedialsworks.powertools.state_INVALID_RECIPE",zh?"\u914d\u65b9\u5df2\u5931\u6548":"Recipe no longer matches");
        p.add("gui.aedialsworks.powertools.state_EMPTY_RESULT",zh?"\u914d\u65b9\u65e0\u4ea7\u7269":"Recipe has no result");
        p.add("gui.aedialsworks.powertools.state_SATISFIED",zh?"\u8fbe\u5230\u76ee\u6807":"Target reached");
        p.add("gui.aedialsworks.powertools.state_NO_POWER",zh?"\u80fd\u91cf\u4e0d\u8db3":"Not enough power");
        p.add("gui.aedialsworks.powertools.state_MISSING_INPUT",zh?"\u7f3a\u5c11\u539f\u6599":"Missing ingredients");
        p.add("gui.aedialsworks.powertools.state_CRAFTED",zh?"\u5df2\u5408\u6210":"Crafted");
        p.add("gui.aedialsworks.powertools.state_OUTPUT_FULL",zh?"\u5b58\u50a8\u5df2\u6ee1\uff0c\u4ea7\u7269\u5df2\u7f13\u5b58":"Storage full; output buffered");
    }
    @Override public void blockStates(BlockStateProvider p){
        p.directionalBlock(AWBlocks.BETTER_LEVEL_MAINTAINER.get(),p.models().cube("better_level_maintainer",AWIds.id("powertools/blocks/maintainer_top"),AWIds.id("powertools/blocks/maintainer_top"),AWIds.id("powertools/blocks/maintainer_side_assembler"),AWIds.id("powertools/blocks/maintainer_side"),AWIds.id("powertools/blocks/maintainer_side"),AWIds.id("powertools/blocks/maintainer_side")));
        p.directionalBlock(AWBlocks.AUTO_CRAFTER.get(),p.models().cube("auto_crafter",AWIds.id("powertools/blocks/maintainer_top"),AWIds.id("powertools/blocks/maintainer_top"),AWIds.id("powertools/blocks/interface_assembler"),AWIds.id("powertools/blocks/storage_assembler"),AWIds.id("powertools/blocks/storage_assembler"),AWIds.id("powertools/blocks/storage_assembler")));
        p.directionalBlock(AWBlocks.STORAGE_LEVEL_EMITTER.get(),p.models().cube("storage_level_emitter",AWIds.id("powertools/blocks/monitor_side"),AWIds.id("powertools/blocks/monitor_side"),AWIds.id("powertools/blocks/storage_level_emitter"),AWIds.id("powertools/blocks/monitor_side"),AWIds.id("powertools/blocks/monitor_side"),AWIds.id("powertools/blocks/monitor_side")));
        p.directionalBlock(AWBlocks.STORAGE_DISPLAY.get(),p.models().cube("storage_display",AWIds.id("powertools/blocks/monitor_side"),AWIds.id("powertools/blocks/monitor_side"),AWIds.id("powertools/blocks/monitor_front"),AWIds.id("powertools/blocks/monitor_side"),AWIds.id("powertools/blocks/monitor_side"),AWIds.id("powertools/blocks/monitor_side")));
        p.directionalBlock(AWBlocks.STORAGE_LEVEL_ALARM.get(),p.models().cube("storage_level_alarm",AWIds.id("powertools/blocks/storage_level_alarm_top"),AWIds.id("powertools/blocks/storage_level_alarm_top"),AWIds.id("powertools/blocks/storage_level_alarm_side"),AWIds.id("powertools/blocks/storage_level_alarm_side"),AWIds.id("powertools/blocks/storage_level_alarm_side"),AWIds.id("powertools/blocks/storage_level_alarm_side")));
    }
    @Override public void itemModels(ItemModelProvider p){
        p.withExistingParent("better_level_maintainer",AWIds.id("block/better_level_maintainer"));
        p.withExistingParent("auto_crafter",AWIds.id("block/auto_crafter"));
        p.withExistingParent("storage_level_emitter",AWIds.id("block/storage_level_emitter"));
        p.withExistingParent("storage_display",AWIds.id("block/storage_display"));
        p.withExistingParent("storage_level_alarm",AWIds.id("block/storage_level_alarm"));
        p.singleTexture("storage_level_emitter_part",p.mcLoc("item/generated"),"layer0",AWIds.id("powertools/parts/level_emitter_on"));
        p.singleTexture("storage_display_part",p.mcLoc("item/generated"),"layer0",AWIds.id("powertools/blocks/monitor_front"));
        p.singleTexture("storage_display_part_smaller",p.mcLoc("item/generated"),"layer0",AWIds.id("powertools/blocks/monitor_front_smaller"));
        p.singleTexture("storage_display_part_smallerer",p.mcLoc("item/generated"),"layer0",AWIds.id("powertools/blocks/monitor_front_smallerer"));
        p.singleTexture("network_health_scanner",p.mcLoc("item/generated"),"layer0",AWIds.id("powertools/items/network_health_scanner"));
        p.singleTexture("network_component_locator",p.mcLoc("item/generated"),"layer0",AWIds.id("powertools/items/network_component_locator"));
        p.singleTexture("priority_tuner",p.mcLoc("item/generated"),"layer0",AWIds.id("powertools/items/priority_tuner"));
        p.singleTexture("cards_distributor",p.mcLoc("item/generated"),"layer0",AWIds.id("powertools/items/cards_distributor"));
        p.singleTexture("storage_level_alarm_locator",p.mcLoc("item/generated"),"layer0",AWIds.id("powertools/items/level_monitor_alarm_locator"));
        p.singleTexture("remote_storage_monitor",p.mcLoc("item/generated"),"layer0",AWIds.id("powertools/items/remote_storage_monitor"));
        p.singleTexture("crafter_speed_upgrade_i",p.mcLoc("item/generated"),"layer0",AWIds.id("powertools/items/crafter_speed_upgrade_i"));
        p.singleTexture("crafter_speed_upgrade_ii",p.mcLoc("item/generated"),"layer0",AWIds.id("powertools/items/crafter_speed_upgrade_ii"));
        p.singleTexture("crafter_speed_upgrade_iii",p.mcLoc("item/generated"),"layer0",AWIds.id("powertools/items/crafter_speed_upgrade_iii"));
        p.singleTexture("crafter_speed_upgrade_iv",p.mcLoc("item/generated"),"layer0",AWIds.id("powertools/items/crafter_speed_upgrade_iv"));
    }
    @Override public void blockLoot(AWBlockLoot p){
        p.selfDrop(AWBlocks.BETTER_LEVEL_MAINTAINER.get());
        p.selfDrop(AWBlocks.AUTO_CRAFTER.get());
        p.selfDrop(AWBlocks.STORAGE_LEVEL_EMITTER.get());
        p.selfDrop(AWBlocks.STORAGE_DISPLAY.get());
        p.selfDrop(AWBlocks.STORAGE_LEVEL_ALARM.get());
    }
    @Override public void blockTags(AWBlockTags p){
        p.add(BlockTags.MINEABLE_WITH_PICKAXE,AWBlocks.BETTER_LEVEL_MAINTAINER.get());
        p.add(BlockTags.MINEABLE_WITH_PICKAXE,AWBlocks.AUTO_CRAFTER.get());
        p.add(BlockTags.MINEABLE_WITH_PICKAXE,AWBlocks.STORAGE_LEVEL_EMITTER.get());
        p.add(BlockTags.MINEABLE_WITH_PICKAXE,AWBlocks.STORAGE_DISPLAY.get());
        p.add(BlockTags.MINEABLE_WITH_PICKAXE,AWBlocks.STORAGE_LEVEL_ALARM.get());
    }
    @Override public void recipes(Consumer<FinishedRecipe> output){
        recipe(output,"auto_crafter","{\"result\":{\"item\":\"aedialsworks:auto_crafter\"},\"type\":\"minecraft:crafting_shaped\",\"pattern\":[\"msm\",\"fbf\",\"aca\"],\"key\":{\"m\":{\"item\":\"ae2:molecular_assembler\"},\"s\":{\"item\":\"ae2:64k_crafting_storage\"},\"f\":{\"item\":\"ae2:fluix_crystal\"},\"b\":{\"item\":\"aedialsworks:better_level_maintainer\"},\"a\":{\"item\":\"ae2:crafting_accelerator\"},\"c\":{\"item\":\"ae2:calculation_processor\"}}}");
        recipe(output,"better_level_maintainer","{\"result\":{\"item\":\"aedialsworks:better_level_maintainer\"},\"type\":\"minecraft:crafting_shaped\",\"pattern\":[\"aba\",\"cdc\",\"efe\"],\"key\":{\"a\":{\"tag\":\"forge:ingots/iron\"},\"b\":[{\"item\":\"ae2:semi_dark_monitor\"},{\"item\":\"ae2:monitor\"},{\"item\":\"ae2:dark_monitor\"}],\"c\":{\"item\":\"ae2:fluix_crystal\"},\"d\":{\"item\":\"ae2:quartz_pillar\"},\"e\":{\"item\":\"ae2:calculation_processor\"},\"f\":{\"item\":\"ae2:formation_core\"}}}");
        recipe(output,"cards_distributor","{\"result\":{\"item\":\"aedialsworks:cards_distributor\"},\"type\":\"minecraft:crafting_shaped\",\"pattern\":[\"sws\",\"bca\",\"sis\"],\"key\":{\"s\":{\"item\":\"ae2:silicon\"},\"w\":{\"item\":\"ae2:wireless_receiver\"},\"b\":{\"item\":\"ae2:basic_card\"},\"a\":{\"item\":\"ae2:advanced_card\"},\"c\":{\"item\":\"ae2:calculation_processor\"},\"i\":{\"tag\":\"forge:ingots/iron\"}}}");
        recipe(output,"crafter_speed_upgrade_i","{\"result\":{\"item\":\"aedialsworks:crafter_speed_upgrade_i\"},\"type\":\"minecraft:crafting_shaped\",\"pattern\":[\"msm\",\"aca\",\"mem\"],\"key\":{\"m\":{\"item\":\"ae2:molecular_assembler\"},\"s\":{\"item\":\"ae2:64k_crafting_storage\"},\"a\":{\"item\":\"ae2:crafting_accelerator\"},\"c\":{\"item\":\"ae2:speed_card\"},\"e\":{\"item\":\"ae2:energy_cell\"}}}");
        recipe(output,"crafter_speed_upgrade_ii","{\"result\":{\"item\":\"aedialsworks:crafter_speed_upgrade_ii\"},\"type\":\"minecraft:crafting_shaped\",\"pattern\":[\"uuu\",\"uiu\",\"uuu\"],\"key\":{\"u\":{\"item\":\"aedialsworks:crafter_speed_upgrade_i\"},\"i\":{\"item\":\"ae2:interface\"}}}");
        recipe(output,"crafter_speed_upgrade_iii","{\"result\":{\"item\":\"aedialsworks:crafter_speed_upgrade_iii\"},\"type\":\"minecraft:crafting_shaped\",\"pattern\":[\"uuu\",\"usu\",\"uuu\"],\"key\":{\"u\":{\"item\":\"aedialsworks:crafter_speed_upgrade_ii\"},\"s\":{\"item\":\"ae2:singularity\"}}}");
        recipe(output,"crafter_speed_upgrade_iv","{\"result\":{\"item\":\"aedialsworks:crafter_speed_upgrade_iv\"},\"type\":\"minecraft:crafting_shaped\",\"pattern\":[\"uuu\",\"ucu\",\"uuu\"],\"key\":{\"u\":{\"item\":\"aedialsworks:crafter_speed_upgrade_iii\"},\"c\":{\"item\":\"ae2:cell_component_64k\"}}}");
        recipe(output,"network_component_locator","{\"result\":{\"item\":\"aedialsworks:network_component_locator\"},\"type\":\"minecraft:crafting_shaped\",\"pattern\":[\"sws\",\"cnc\",\"ses\"],\"key\":{\"s\":{\"item\":\"ae2:smooth_sky_stone_block\"},\"w\":{\"item\":\"ae2:wireless_receiver\"},\"e\":{\"item\":\"ae2:engineering_processor\"},\"c\":{\"item\":\"ae2:calculation_processor\"},\"n\":{\"item\":\"ae2:network_tool\"}}}");
        recipe(output,"network_health_scanner","{\"result\":{\"item\":\"aedialsworks:network_health_scanner\"},\"type\":\"minecraft:crafting_shaped\",\"pattern\":[\"sws\",\"cpc\",\"scs\"],\"key\":{\"s\":{\"item\":\"ae2:smooth_sky_stone_block\"},\"w\":{\"item\":\"ae2:wireless_receiver\"},\"c\":{\"item\":\"ae2:calculation_processor\"},\"p\":[{\"item\":\"ae2:semi_dark_monitor\"},{\"item\":\"ae2:monitor\"},{\"item\":\"ae2:dark_monitor\"}]}}");
        recipe(output,"priority_tuner","{\"result\":{\"item\":\"aedialsworks:priority_tuner\"},\"type\":\"minecraft:crafting_shaped\",\"pattern\":[\"a a\",\"aba\",\" c \"],\"key\":{\"a\":{\"tag\":\"forge:ingots/iron\"},\"b\":{\"item\":\"ae2:charged_certus_quartz_crystal\"},\"c\":{\"item\":\"ae2:smooth_sky_stone_block\"}}}");
        recipe(output,"remote_storage_monitor","{\"result\":{\"item\":\"aedialsworks:remote_storage_monitor\"},\"type\":\"minecraft:crafting_shaped\",\"pattern\":[\"sws\",\"lml\",\"scs\"],\"key\":{\"s\":{\"item\":\"ae2:smooth_sky_stone_block\"},\"w\":{\"item\":\"ae2:wireless_receiver\"},\"l\":{\"item\":\"ae2:logic_processor\"},\"c\":{\"item\":\"ae2:calculation_processor\"},\"m\":{\"item\":\"aedialsworks:storage_display_part\"}}}");
        recipe(output,"storage_display","{\"result\":{\"item\":\"aedialsworks:storage_display\"},\"type\":\"minecraft:crafting_shaped\",\"pattern\":[\" a \",\"bcb\",\" d \"],\"key\":{\"a\":[{\"item\":\"ae2:semi_dark_monitor\"},{\"item\":\"ae2:monitor\"},{\"item\":\"ae2:dark_monitor\"}],\"b\":{\"tag\":\"forge:ingots/iron\"},\"c\":{\"item\":\"ae2:storage_monitor\"},\"d\":{\"item\":\"ae2:quartz_glass\"}}}");
        recipe(output,"storage_display_part","{\"result\":{\"item\":\"aedialsworks:storage_display_part\"},\"type\":\"minecraft:crafting_shapeless\",\"ingredients\":[{\"item\":\"aedialsworks:storage_display\"}]}");
        recipe(output,"storage_display_part_smaller","{\"result\":{\"item\":\"aedialsworks:storage_display_part_smaller\"},\"type\":\"minecraft:crafting_shapeless\",\"ingredients\":[{\"item\":\"aedialsworks:storage_display_part\"}]}");
        recipe(output,"storage_display_part_smallerer","{\"result\":{\"item\":\"aedialsworks:storage_display_part_smallerer\"},\"type\":\"minecraft:crafting_shapeless\",\"ingredients\":[{\"item\":\"aedialsworks:storage_display_part_smaller\"}]}");
        recipe(output,"storage_display_part_smallerer_restore","{\"result\":{\"item\":\"aedialsworks:storage_display\"},\"type\":\"minecraft:crafting_shapeless\",\"ingredients\":[{\"item\":\"aedialsworks:storage_display_part_smallerer\"}]}");
        recipe(output,"storage_level_alarm","{\"result\":{\"item\":\"aedialsworks:storage_level_alarm\"},\"type\":\"minecraft:crafting_shaped\",\"pattern\":[\"ipi\",\"cec\",\"ifi\"],\"key\":{\"c\":{\"item\":\"ae2:calculation_processor\"},\"e\":{\"item\":\"aedialsworks:storage_level_emitter\"},\"f\":{\"item\":\"ae2:fluix_crystal\"},\"i\":{\"tag\":\"forge:ingots/iron\"},\"p\":[{\"item\":\"ae2:semi_dark_monitor\"},{\"item\":\"ae2:monitor\"},{\"item\":\"ae2:dark_monitor\"}]}}");
        recipe(output,"storage_level_alarm_locator","{\"result\":{\"item\":\"aedialsworks:storage_level_alarm_locator\"},\"type\":\"aedialsworks:shapeless_reusable\",\"ingredients\":[{\"item\":\"aedialsworks:storage_level_alarm\",\"reusable\":true},{\"item\":\"minecraft:compass\"},{\"item\":\"ae2:smooth_sky_stone_block\"}]}");
        recipe(output,"storage_level_emitter","{\"result\":{\"item\":\"aedialsworks:storage_level_emitter\"},\"type\":\"minecraft:crafting_shaped\",\"pattern\":[\" a \",\"bcb\",\" d \"],\"key\":{\"a\":{\"item\":\"minecraft:redstone_torch\"},\"b\":{\"tag\":\"forge:ingots/iron\"},\"c\":{\"item\":\"ae2:storage_monitor\"},\"d\":{\"tag\":\"forge:dusts/redstone\"}}}");
        recipe(output,"storage_level_emitter_part","{\"result\":{\"item\":\"aedialsworks:storage_level_emitter_part\"},\"type\":\"minecraft:crafting_shapeless\",\"ingredients\":[{\"item\":\"aedialsworks:storage_level_emitter\"}]}");
    }
    private static void recipe(Consumer<FinishedRecipe> output,String id,String encoded){
        JsonObject json=JsonParser.parseString(encoded).getAsJsonObject();
        output.accept(new FinishedRecipe(){
            public void serializeRecipeData(JsonObject target){json.entrySet().forEach(e->{if(!e.getKey().equals("type"))target.add(e.getKey(),e.getValue());});}
            public ResourceLocation getId(){return AWIds.id(id);}
            public RecipeSerializer<?> getType(){return ForgeRegistries.RECIPE_SERIALIZERS.getValue(new ResourceLocation(json.get("type").getAsString()));}
            public JsonObject serializeAdvancement(){return null;}
            public ResourceLocation getAdvancementId(){return AWIds.id("recipes/"+id);}
        });
    }
}
