package com.mpp.aedialsworks.cellterminal.screen;

import java.util.*;
import appeng.api.stacks.*;
import appeng.core.definitions.*;
import com.mpp.aedialsworks.cellterminal.client.*;
import com.mpp.aedialsworks.cellterminal.integration.ae2.TerminalScreenAdapter;
import com.mpp.aedialsworks.cellterminal.menu.*;
import com.mpp.aedialsworks.cellterminal.network.TerminalChannels;
import com.mpp.aedialsworks.common.config.AWConfigs;
import com.mpp.aedialsworks.common.registry.AWItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import org.lwjgl.glfw.GLFW;

/** Original compact 18px rows and atlas controls; server target/session validation is unchanged. */
public final class CellTerminalScreen extends TerminalScreenAdapter {
    private enum LineKind { STORAGE, CELL, CONTENT, SUBNET, TOOL, NOTE }
    private record ViewLine(LineKind kind, CompoundTag entry, int index, String text) {}
    private record Hit(int x, int y, int width, int height, Runnable action) {
        boolean contains(double mx, double my) { return mx >= x && mx < x + width && my >= y && my < y + height; }
    }
    private EditBox search, priority;
    private TerminalIconButton modeButton, styleButton, limitButton, visibilityButton, sourceButton, settingsButton, rootButton;
    private final List<TerminalIconButton> tabButtons = new ArrayList<>(), filterButtons = new ArrayList<>();
    private final Set<Long> favorites = new LinkedHashSet<>(), selectedIds = new LinkedHashSet<>();
    private final Set<String> collapsed = new HashSet<>();
    private final List<ViewLine> lines = new ArrayList<>();
    private final List<Hit> hits = new ArrayList<>();
    private List<CompoundTag> rows = List.of();
    private List<Component> hoverText = List.of();
    private CompoundTag selected;
    private TerminalTab shown = TerminalTab.TERMINAL;
    private SearchFilterMode searchMode = SearchFilterMode.MIXED;
    private String queryError = "", confirmAction = "";
    private long confirmUntil, focusTarget = -1, lastClickTarget = -1, lastClickTime;
    private int visibleRows = 8, scroll, filterIndex;
    private float uiScale = 1;
    private boolean restored, toolBuses, inspector, draggingScroll, tempPartition;

    public CellTerminalScreen(CellTerminalMenu menu, Inventory inv, Component title) { super(menu, inv, title); }
    private static Component text(String key, Object... args) { return Component.translatable("gui.aedialsworks.cellterminal." + key, args); }
    private int footer() { return TerminalAtlas.CONTENT_TOP + visibleRows * 18; }
    private int maxScroll() { return Math.max(0, lines.size() - visibleRows); }
    private boolean partitionView() { return shown == TerminalTab.PARTITION || shown == TerminalTab.BUS_PARTITION || shown == TerminalTab.TEMP && tempPartition; }
    private boolean busRows() { return shown.bus() || shown == TerminalTab.NETWORK_TOOLS && toolBuses; }

    @Override protected void init() {
        int pw = minecraft.getWindow().getGuiScaledWidth(), ph = minecraft.getWindow().getGuiScaledHeight();
        uiScale = Math.min(1f, Math.min(pw / 340f, ph / 286f));
        width = (int)(pw / uiScale); height = (int)(ph / uiScale);
        int available = Math.max(6, (height - 162) / 18);
        visibleRows = AWConfigs.CLIENT.cellterminal.gui.terminalStyle.get().equals("TALL") ? available : Math.min(8, available);
        prepareLayout(visibleRows);
        String previousSearch = search == null ? AWConfigs.CLIENT.cellterminal.gui.searchFilter.get() : search.getValue();
        super.init(); leftPos = Math.max(22, Math.min(leftPos, width - 315)); topPos = Math.max(24, (height - imageHeight + 22) / 2);
        tabButtons.clear(); filterButtons.clear(); hits.clear();
        searchMode = SearchFilterMode.fromName(AWConfigs.CLIENT.cellterminal.gui.searchMode.get());
        search = addRenderableWidget(new EditBox(font, leftPos + 23, topPos + 20, 158, 12, text("search")));
        search.setMaxLength(256); search.setValue(previousSearch); search.setResponder(value -> scroll = 0); search.setTextColor(0xE8E8E8);
        modeButton = icon(189, 21, 10, "mode", TerminalIconButton.Icon.SEARCH, () -> searchMode = searchMode.next());
        icon(183, 6, 10, "search_help", TerminalIconButton.Icon.HELP, () -> { setFocused(search); search.setFocused(true); });
        rootButton = icon(5, 5, 12, "root", TerminalIconButton.Icon.BACK, () -> {
            var payload = new CompoundTag(); payload.putLong("network", 0); menu.request("network", payload); selected = null; selectedIds.clear(); scroll = 0;
        });
        for (var tab : TerminalTab.values()) {
            var button = icon(4 + tab.ordinal() * 24, -22, 22, "tab." + tab.name().toLowerCase(Locale.ROOT), TerminalIconButton.Icon.TAB, () -> selectTab(tab, -1));
            button.item(tabIcon(tab)); tabButtons.add(button);
        }
        styleButton = icon(-18, 18, 16, "style.small", TerminalIconButton.Icon.STYLE, () -> {
            var gui = AWConfigs.CLIENT.cellterminal.gui; gui.terminalStyle.set(gui.terminalStyle.get().equals("SMALL") ? "TALL" : "SMALL"); gui.searchMode.set(searchMode.name());
            init(minecraft, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
        });
        limitButton = icon(-18, 36, 16, "slots", TerminalIconButton.Icon.LIMIT, () -> {
            var limit = limitSetting(); limit.set(switch (limit.get()) { case "LIMIT_8" -> "LIMIT_32"; case "LIMIT_32" -> "LIMIT_64"; case "LIMIT_64" -> "UNLIMITED"; default -> "LIMIT_8"; });
        });
        visibilityButton = icon(-18, 54, 16, "visibility.root", TerminalIconButton.Icon.VISIBILITY, () -> {
            var setting = AWConfigs.CLIENT.cellterminal.gui.subnetVisibility;
            setting.set(switch (setting.get()) { case "DONT_SHOW" -> "SHOW_FAVORITES"; case "SHOW_FAVORITES" -> "SHOW_ALL"; default -> "DONT_SHOW"; }); sendViewPreferences();
        });
        icon(-18, 72, 16, "refresh", TerminalIconButton.Icon.REFRESH, () -> menu.request("refresh", new CompoundTag()));
        for (int i = 0; i < 4; i++) {
            final int index = i;
            var button = icon(-18, 98 + i * 18, 16, "filter." + i, TerminalIconButton.Icon.FILTER, () -> { filterIndex = index; cycleFilter(); });
            button.item(switch (i) { case 0 -> new ItemStack(Items.IRON_INGOT); case 1 -> new ItemStack(Items.WATER_BUCKET); case 2 -> AEItems.ITEM_CELL_1K.stack(); default -> AEBlocks.CELL_WORKBENCH.stack(); }); filterButtons.add(button);
        }
        settingsButton = icon(-18, 174, 16, "target_settings", TerminalIconButton.Icon.SETTINGS, () -> inspector = !inspector); settingsButton.item(AEItems.NETWORK_TOOL.stack());
        sourceButton = icon(-18, 194, 16, "tool_source.cells", TerminalIconButton.Icon.SOURCE, () -> {
            if (shown == TerminalTab.TEMP) { tempPartition = !tempPartition; scroll = 0; rebuildLines(); }
            else { toolBuses = !toolBuses; scroll = 0; selected = null; selectedIds.clear(); confirmAction = ""; menu.request("refresh", new CompoundTag()); }
        });
        priority = addRenderableWidget(new EditBox(font, leftPos + 217, topPos + 67, 69, 12, text("priority")));
        priority.setMaxLength(11); priority.setFilter(value -> value.isEmpty() || value.equals("-") || value.matches("-?[0-9]{1,10}"));
        priority.setValue(selected == null ? "0" : Integer.toString(selected.getInt("priority"))); priority.visible = false;
        favorites.clear(); for (String id : AWConfigs.CLIENT.cellterminal.gui.subnetFavorites.get().stream().limit(128).toList()) favorites.add(Long.parseLong(id));
    }
    private TerminalIconButton icon(int x, int y, int size, String key, TerminalIconButton.Icon kind, Runnable action) {
        return addRenderableWidget(new TerminalIconButton(leftPos + x, topPos + y, size, text(key), kind, action));
    }
    private ItemStack tabIcon(TerminalTab tab) {
        return switch (tab) {
            case TERMINAL -> new ItemStack(AWItems.CELL_TERMINAL.get()); case INVENTORY -> new ItemStack(Items.CHEST);
            case PARTITION -> AEBlocks.CELL_WORKBENCH.stack(); case TEMP -> AEItems.ITEM_CELL_HOUSING.stack();
            case BUS_INVENTORY, BUS_PARTITION -> AEParts.STORAGE_BUS.stack();
            case NETWORK_TOOLS -> AEItems.NETWORK_TOOL.stack(); case SUBNETS -> AEBlocks.CONTROLLER.stack();
        };
    }
    private net.minecraftforge.common.ForgeConfigSpec.ConfigValue<String> limitSetting() {
        var gui = AWConfigs.CLIENT.cellterminal.gui; return shown == TerminalTab.SUBNETS ? gui.subnetSlotLimit : busRows() ? gui.busSlotLimit : gui.cellSlotLimit;
    }
    private int displayCount(CompoundTag entry, boolean partition) {
        if (partition) return entry.getInt("partitionSize");
        return Math.min(entry.getList("contents", Tag.TAG_COMPOUND).size(), switch (limitSetting().get()) { case "LIMIT_8" -> 8; case "LIMIT_32" -> 32; case "LIMIT_64" -> 64; default -> Integer.MAX_VALUE; });
    }
    private void sendViewPreferences() {
        var payload = new CompoundTag(); payload.putString("visibility", AWConfigs.CLIENT.cellterminal.gui.subnetVisibility.get());
        payload.putLongArray("favorites", favorites.stream().mapToLong(Long::longValue).toArray()); menu.request("view", payload);
    }
    private void selectTab(TerminalTab tab, long target) {
        var payload = new CompoundTag(); payload.putInt("tab", tab.ordinal()); menu.request("tab", payload);
        scroll = 0; selected = null; selectedIds.clear(); focusTarget = target; inspector = false; tempPartition = false; if (target >= 0) collapsed.clear();
    }
    private void select(CompoundTag entry) {
        if (selected == null || selected.getLong("id") != entry.getLong("id")) priority.setValue(Integer.toString(entry.getInt("priority"))); selected = entry;
    }
    private void action(CompoundTag entry, String action, CompoundTag payload) {
        if (entry == null) return;
        payload.putLong("id", entry.getLong("id")); payload.putLong("token", entry.getLong("token")); menu.request(action, payload);
    }
    private void action(String action, CompoundTag payload) { action(selected, action, payload); }

    @Override public void containerTick() {
        super.containerTick(); search.tick(); priority.tick(); var meta = menu.data(TerminalChannels.META);
        if (!restored && meta.contains("tab")) {
            restored = true; int tab = AWConfigs.CLIENT.cellterminal.gui.selectedTab.get();
            if (tab >= 0 && tab < TerminalTab.values().length && meta.getBoolean("tab" + tab)) selectTab(TerminalTab.values()[tab], -1);
            sendViewPreferences(); long previous = Long.parseLong(AWConfigs.CLIENT.cellterminal.gui.lastViewedNetworkId.get());
            if (previous != 0 && previous != meta.getLong("rootNetworkId")) { var payload = new CompoundTag(); payload.putLong("network", previous); menu.request("network", payload); }
        }
        if (shown != menu.activeTab()) { shown = menu.activeTab(); scroll = 0; selected = null; selectedIds.clear(); inspector = false; }
        for (int i = 0; i < tabButtons.size(); i++) { tabButtons.get(i).active = meta.getBoolean("tab" + i); tabButtons.get(i).selected(i == shown.ordinal()); }
        modeButton.label(text("search_mode." + searchMode.name().toLowerCase(Locale.ROOT)));
        modeButton.state(switch (searchMode) { case INVENTORY -> 0; case PARTITION -> 1; default -> 2; });
        boolean tall = AWConfigs.CLIENT.cellterminal.gui.terminalStyle.get().equals("TALL"); styleButton.label(text(tall ? "style.tall" : "style.small")); styleButton.state(tall ? 1 : 0);
        String limit = limitSetting().get().equals("UNLIMITED") ? "∞" : limitSetting().get().substring(6); limitButton.label(text("slots", limit)); limitButton.badge(limit);
        String visibility = AWConfigs.CLIENT.cellterminal.gui.subnetVisibility.get();
        visibilityButton.label(text("visibility." + switch (visibility) { case "SHOW_ALL" -> "all"; case "SHOW_FAVORITES" -> "favorites"; default -> "root"; }));
        visibilityButton.state(visibility.equals("SHOW_ALL") ? 2 : visibility.equals("SHOW_FAVORITES") ? 1 : 0);
        rootButton.active = meta.getLong("networkId") != meta.getLong("rootNetworkId");
        sourceButton.visible = shown == TerminalTab.NETWORK_TOOLS || shown == TerminalTab.TEMP;
        sourceButton.label(text(shown == TerminalTab.TEMP ? tempPartition ? "contents" : "partition" : toolBuses ? "tool_source.buses" : "tool_source.cells"));
        sourceButton.item(shown == TerminalTab.TEMP ? tempPartition ? new ItemStack(Items.CHEST) : AEBlocks.CELL_WORKBENCH.stack() : toolBuses ? AEParts.STORAGE_BUS.stack() : AEBlocks.DRIVE.stack());
        for (int i = 0; i < filterButtons.size(); i++) {
            filterIndex = i; String state = filterSetting().get(); var button = filterButtons.get(i);
            button.state(state.equals("SHOW_ALL") ? 0 : state.equals("SHOW_ONLY") ? 1 : 2);
            button.label(text("filter_state", text("filter." + i), text("filter_mode." + state.toLowerCase(Locale.ROOT)))); button.visible = shown != TerminalTab.SUBNETS;
        }
        updateRows(); settingsButton.visible = shown != TerminalTab.SUBNETS; settingsButton.active = selected != null;
        priority.visible = inspector && selected != null && shown != TerminalTab.SUBNETS; if (!priority.visible) priority.setFocused(false);
    }
    private void updateRows() {
        String channel = shown == TerminalTab.TEMP ? TerminalChannels.TEMP_CELLS : busRows() ? TerminalChannels.BUSES : shown == TerminalTab.SUBNETS ? TerminalChannels.SUBNETS : TerminalChannels.STORAGES;
        var next = new ArrayList<CompoundTag>(); String query = search.getValue().trim(); queryError = "";
        var parsed = query.isEmpty() ? null : AdvancedSearchParser.parse(query.startsWith("?") ? query : "?" + query);
        if (parsed != null && !parsed.isSuccess()) queryError = parsed.getErrorMessage();
        for (var value : menu.data(channel).getList("entries", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag)value; boolean matches = query.isEmpty();
            if (parsed != null && parsed.isSuccess()) {
                var matcher = parsed.getMatcher();
                if (shown == TerminalTab.SUBNETS) matches = matcher.matchesSubnetConnectionFilter(new SubnetInfo(entry), new SubnetInfo.ConnectionPoint(entry), false, searchMode);
                else if (busRows()) matches = matcher.matchesStorageBusFilter(new StorageBusInfo(entry), searchMode);
                else matches = matcher.matchesCellFilter(new CellInfo(entry), new StorageInfo(entry), searchMode);
                if (!query.startsWith("?")) matches |= entry.getString("name").toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)) || entry.getString("storageName").toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
            }
            if (matches && matchesConfiguredFilters(entry)) next.add(entry);
        }
        rows = next;
        if (selected != null) { long id = selected.getLong("id"); selected = rows.stream().filter(row -> row.getLong("id") == id).findFirst().orElse(null); }
        if (selected == null && !rows.isEmpty()) select(rows.get(0)); rebuildLines();
    }
    private String groupKey(CompoundTag entry) { return entry.getString("dimension") + ":" + entry.getLong("pos") + ":" + entry.getString("kind"); }
    private void rebuildLines() {
        lines.clear();
        if (shown == TerminalTab.NETWORK_TOOLS) {
            lines.add(new ViewLine(LineKind.TOOL, null, 0, "mass_partition")); if (!toolBuses) lines.add(new ViewLine(LineKind.TOOL, null, 1, "attribute_unique")); lines.add(new ViewLine(LineKind.NOTE, null, 0, "tools_help"));
        }
        if (shown == TerminalTab.SUBNETS) {
            for (var entry : rows) { lines.add(new ViewLine(LineKind.SUBNET, entry, 0, "")); addContents(entry, false); }
        } else {
            var groups = new LinkedHashMap<String, List<CompoundTag>>();
            for (var entry : rows) groups.computeIfAbsent(groupKey(entry), ignored -> new ArrayList<>()).add(entry);
            for (var group : groups.entrySet()) {
                var first = group.getValue().get(0); boolean grouped = !busRows() && shown != TerminalTab.TEMP;
                if (grouped) lines.add(new ViewLine(LineKind.STORAGE, first, group.getValue().size(), group.getKey()));
                if (grouped && collapsed.contains(group.getKey())) continue;
                for (var entry : group.getValue()) {
                    boolean empty = ItemStack.of(entry.getCompound("item")).isEmpty();
                    if (empty && !busRows() && (shown == TerminalTab.INVENTORY || shown == TerminalTab.PARTITION)) continue;
                    lines.add(new ViewLine(LineKind.CELL, entry, 0, ""));
                    if (shown == TerminalTab.INVENTORY || shown.bus() || shown == TerminalTab.PARTITION || shown == TerminalTab.TEMP && !empty) addContents(entry, partitionView());
                }
            }
        }
        if (lines.isEmpty()) lines.add(new ViewLine(LineKind.NOTE, null, 0, "no_results"));
        if (focusTarget >= 0) for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i);
            if (line.kind == LineKind.CELL && line.entry.getLong("id") == focusTarget) { scroll = i; select(line.entry); focusTarget = -1; break; }
        }
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }
    private int columns(CompoundTag entry) { return entry.getString("kind").equals("bus") ? 9 : 8; }
    private int slotX(CompoundTag entry) { return columns(entry) == 9 ? 22 : 34; }
    private void addContents(CompoundTag entry, boolean partition) {
        int count = displayCount(entry, partition), columns = columns(entry);
        for (int index = 0; index < count; index += columns) lines.add(new ViewLine(LineKind.CONTENT, entry, index, ""));
    }

    @Override public void drawBG(GuiGraphics g, int x, int y, int mouseX, int mouseY, float partial) {
        hits.clear(); hoverText = List.of(); TerminalAtlas.panel(g, x, y, imageWidth, imageHeight);
        g.drawString(font, trim(title.getString(), 155), x + 22, y + 6, TerminalAtlas.TEXT, false);
        TerminalAtlas.inset(g, x + 21, y + 33, 162, visibleRows * 18 + 2);
        for (int i = 0; i < visibleRows && scroll + i < lines.size(); i++) {
            var line = lines.get(scroll + i); int ry = y + 34 + i * 18;
            switch (line.kind) {
                case STORAGE -> drawStorage(g, line, x, ry, mouseX, mouseY);
                case CELL -> drawCell(g, line.entry, x, ry, mouseX, mouseY);
                case CONTENT -> drawContents(g, line, x, ry, mouseX, mouseY);
                case SUBNET -> drawSubnet(g, line.entry, x, ry, mouseX, mouseY);
                case TOOL -> drawTool(g, line.text, x, ry, mouseX, mouseY);
                case NOTE -> { g.drawString(font, trim(text(line.text).getString(), 156), x + 24, ry + 5, TerminalAtlas.SECONDARY, false); hover(mouseX, mouseY, x + 22, ry, 160, 18, text(line.text)); }
            }
        }
        drawScrollbar(g, x, y);
        String status = queryError.isEmpty() ? menu.feedback() : queryError;
        Component footerText = status.isEmpty() ? Component.translatable("container.inventory") : queryError.isEmpty() ? text("feedback." + status) : Component.literal(status);
        int footerColor = status.isEmpty() ? TerminalAtlas.TEXT : status.equals("done") ? 0x287A2D : 0xA32C2C;
        g.drawString(font, trim(footerText.getString(), 162), x + 22, y + footer() + 5, footerColor, false);
        hover(mouseX, mouseY, x + 22, y + footer() + 2, 162, 13, footerText);
        for (var slot : menu.slots) TerminalAtlas.slot(g, x + slot.x - 1, y + slot.y - 1, false);
        drawToolbox(g, x, y, mouseX, mouseY);
        if (inspector && selected != null && shown != TerminalTab.SUBNETS) drawInspector(g, x, y, mouseX, mouseY);
    }
    private void drawStorage(GuiGraphics g, ViewLine line, int x, int y, int mx, int my) {
        var entry = line.entry; var pos = BlockPos.of(entry.getLong("pos"));
        g.fill(x + 22, y, x + 182, y + 18, 0xFFD1D1D1); g.fill(x + 22, y + 17, x + 182, y + 18, 0xFF8B8B8B);
        g.renderItem(entry.getString("kind").equals("chest") ? AEBlocks.CHEST.stack() : AEBlocks.DRIVE.stack(), x + 23, y + 1);
        g.drawString(font, trim(entry.getString("storageName"), 95), x + 42, y + 1, TerminalAtlas.TEXT, false);
        g.drawString(font, trim(pos.getX() + ", " + pos.getY() + ", " + pos.getZ(), 136), x + 42, y + 10, TerminalAtlas.SECONDARY, false);
        g.drawString(font, trim(Integer.toString(entry.getInt("priority")), 24), x + 141, y + 4, 0x555555, false);
        g.drawString(font, collapsed.contains(line.text) ? "+" : "−", x + 172, y + 4, TerminalAtlas.TEXT, false);
        hit(x + 167, y, 15, 18, () -> { if (!collapsed.remove(line.text)) collapsed.add(line.text); rebuildLines(); });
        hit(x + 140, y, 27, 18, () -> { select(entry); inspector = true; priority.visible = true; setFocused(priority); priority.setFocused(true); });
        hover(mx, my, x + 22, y, 118, 18, Component.literal(entry.getString("storageName")), Component.literal(entry.getString("dimension") + "  " + pos.toShortString()), text("group_help"));
        hover(mx, my, x + 140, y, 27, 18, text("priority_value", entry.getInt("priority"))); hover(mx, my, x + 167, y, 15, 18, text(collapsed.contains(line.text) ? "expand" : "collapse"));
    }
    private void drawCell(GuiGraphics g, CompoundTag entry, int x, int y, int mx, int my) {
        long id = entry.getLong("id"); boolean chosen = selected != null && selected.getLong("id") == id;
        if (chosen || selectedIds.contains(id)) g.fill(x + 22, y, x + 182, y + 18, selectedIds.contains(id) ? 0x5055AA55 : 0x405599DD);
        boolean bus = entry.getString("kind").equals("bus"); ItemStack item = ItemStack.of(entry.getCompound("item")); if (bus) item = AEParts.STORAGE_BUS.stack();
        int cellX = bus ? 22 : 34;
        if (!bus) { g.fill(x + 27, y, x + 28, y + 18, 0xFF808080); g.fill(x + 27, y + 8, x + 32, y + 9, 0xFF808080); }
        if (!item.isEmpty()) g.renderItem(item, x + cellX, y + 1);
        else { TerminalAtlas.slot(g, x + cellX - 1, y, false); g.drawString(font, Integer.toString(entry.getInt("slot") + 1), x + cellX + 3, y + 5, 0xAFAFAF, false); }
        String name = item.isEmpty() ? text("empty_slot").getString() : entry.getString("name");
        g.drawString(font, trim(name, 85), x + 52, y + 1, entry.getBoolean("customName") ? 0x2E7D32 : TerminalAtlas.TEXT, false);
        long total = entry.getLong("totalBytes"), used = entry.getLong("usedBytes");
        if (total > 0) {
            g.fill(x + 52, y + 12, x + 136, y + 16, 0xFF555555);
            int fill = (int)Math.min(82, Math.max(0, used * 82d / total)); int color = used * 1d / total < .75 ? 0xFF55B855 : used < total ? 0xFFE5A345 : 0xFFD95555;
            g.fill(x + 53, y + 13, x + 53 + fill, y + 15, color);
        } else if (bus) g.drawString(font, trim(text("priority_value", entry.getInt("priority")).getString(), 84), x + 52, y + 10, TerminalAtlas.SECONDARY, false);
        else g.drawString(font, text("slot_number", entry.getInt("slot") + 1), x + 52, y + 10, TerminalAtlas.SECONDARY, false);
        hover(mx, my, x + 32, y, 107, 18, Component.literal(name), text("capacity", used, total), text("cell_help"));
        if (shown == TerminalTab.NETWORK_TOOLS) {
            TerminalAtlas.sprite(g, x + 166, y + 4, selectedIds.contains(id) ? 0 : 16, 0, 8); hover(mx, my, x + 141, y, 40, 18, text("tools_help"));
        } else if (partitionView()) {
            smallAction(g, x + 143, y + 5, 0, "partition_contents", "partition", entry, mx, my);
            smallAction(g, x + 155, y + 5, 8, "clear_partition", "clear", entry, mx, my);
            smallAction(g, x + 167, y + 5, 32, "fuzzy", "fuzzy", entry, mx, my);
        } else {
            atlasAction(g, x + 141, y + 3, 0, "pickup", entry, mx, my, () -> action(entry, "pickup", new CompoundTag()));
            atlasAction(g, x + 154, y + 3, 24, "contents", entry, mx, my, () -> showDetails(entry, false));
            atlasAction(g, x + 167, y + 3, 12, "partition", entry, mx, my, () -> showDetails(entry, true));
        }
        var upgrades = entry.getList("upgrades", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(4, upgrades.size()); i++) {
            var card = ItemStack.of(upgrades.getCompound(i)); if (card.isEmpty()) continue; int cx = x + 3 + i % 2 * 8, cy = y + i / 2 * 8;
            g.pose().pushPose(); g.pose().translate(cx, cy, 0); g.pose().scale(.5f, .5f, 1); g.renderItem(card, 0, 0); g.pose().popPose();
            int index = i; hit(cx, cy, 8, 8, () -> { var payload = new CompoundTag(); payload.putInt("slot", index); action(entry, "upgrade_extract", payload); });
            hover(mx, my, cx, cy, 8, 8, card.getHoverName(), text("extract_upgrade"));
        }
    }
    private void showDetails(CompoundTag entry, boolean partition) {
        if (shown == TerminalTab.TEMP) { tempPartition = partition; focusTarget = entry.getLong("id"); rebuildLines(); }
        else selectTab(entry.getString("kind").equals("bus") ? partition ? TerminalTab.BUS_PARTITION : TerminalTab.BUS_INVENTORY : partition ? TerminalTab.PARTITION : TerminalTab.INVENTORY, entry.getLong("id"));
    }
    private void atlasAction(GuiGraphics g, int x, int y, int u, String label, CompoundTag entry, int mx, int my, Runnable action) {
        TerminalAtlas.sprite(g, x, y, u, 16 + (inside(mx, my, x, y, 12, 12) ? 12 : 0), 12);
        hit(x, y, 12, 12, () -> { select(entry); action.run(); }); hover(mx, my, x, y, 12, 12, text(label));
    }
    private void smallAction(GuiGraphics g, int x, int y, int u, String action, String label, CompoundTag entry, int mx, int my) {
        TerminalAtlas.sprite(g, x, y, u, inside(mx, my, x, y, 8, 8) ? 8 : 0, 8);
        hit(x, y, 8, 8, () -> { select(entry); action(entry, action, new CompoundTag()); }); hover(mx, my, x, y, 8, 8, text(label));
    }
    private CompoundTag contentEntry(CompoundTag entry, int index, boolean partition) {
        var list = entry.getList(partition ? "partition" : "contents", Tag.TAG_COMPOUND);
        if (partition) { for (var value : list) if (((CompoundTag)value).getInt("slot") == index) return (CompoundTag)value; }
        else if (index < list.size()) return list.getCompound(index); return null;
    }
    private void drawContents(GuiGraphics g, ViewLine line, int x, int y, int mx, int my) {
        boolean partition = partitionView(); int count = displayCount(line.entry, partition), columns = columns(line.entry);
        for (int i = 0; i < columns && line.index + i < count; i++) {
            int index = line.index + i, cx = x + slotX(line.entry) + i * 18; TerminalAtlas.slot(g, cx, y, partition);
            var value = contentEntry(line.entry, index, partition); var stack = value == null ? null : GenericStack.readTag(value);
            if (stack != null) {
                g.renderItem(stack.what().wrapForDisplayOrFilter(), cx + 1, y + 1);
                if (!partition) { String amount = shortAmount(stack.amount()); g.pose().pushPose(); g.pose().translate(cx + 17, y + 12, 200); g.pose().scale(.65f, .65f, 1); g.drawString(font, amount, -font.width(amount), 0, 0xFFFFFF, true); g.pose().popPose(); }
                hover(mx, my, cx, y, 18, 18, stack.what().getDisplayName(), Component.literal(stack.amount() + " " + stack.what().getUnitSymbol()), text(partition ? "partition_help" : "content_help"));
            } else hover(mx, my, cx, y, 18, 18, text("partition_help"));
            if (inside(mx, my, cx, y, 18, 18)) g.fill(cx + 1, y + 1, cx + 17, y + 17, 0x50FFFFFF);
        }
    }
    private void drawSubnet(GuiGraphics g, CompoundTag entry, int x, int y, int mx, int my) {
        long id = entry.getLong("id"); boolean root = id == menu.data(TerminalChannels.META).getLong("rootNetworkId");
        g.fill(x + 22, y, x + 182, y + 18, 0xFFD1D1D1);
        TerminalAtlas.sprite(g, x + 24, y + 3, root ? 30 : 40, root || entry.getBoolean("outbound") ? 40 : 50, 10);
        String name = root ? text("root").getString() : entry.getString("name");
        g.drawString(font, trim(name, 106), x + 38, y + 1, TerminalAtlas.TEXT, false); g.drawString(font, text("network_nodes", entry.getInt("nodes")), x + 38, y + 10, TerminalAtlas.SECONDARY, false);
        boolean favorite = favorites.contains(id);
        TerminalAtlas.sprite(g, x + 149, y + 1, favorite ? 96 : 112, 60 + (inside(mx, my, x + 149, y, 16, 18) ? 16 : 0), 16);
        TerminalAtlas.sprite(g, x + 168, y + 3, 48, 16 + (inside(mx, my, x + 168, y, 12, 18) ? 12 : 0), 12);
        hit(x + 149, y, 16, 18, () -> { if (!favorites.remove(id) && favorites.size() < 128) favorites.add(id); AWConfigs.CLIENT.cellterminal.gui.subnetFavorites.set(favorites.stream().map(String::valueOf).toList()); sendViewPreferences(); });
        hit(x + 168, y, 12, 18, () -> { var payload = new CompoundTag(); payload.putLong("network", id); menu.request("network", payload); selectTab(TerminalTab.TERMINAL, -1); });
        hover(mx, my, x + 22, y, 126, 18, Component.literal(entry.getString("name")), text("network_nodes", entry.getInt("nodes")));
        hover(mx, my, x + 149, y, 16, 18, text(favorite ? "favorite.remove" : "favorite.add")); hover(mx, my, x + 168, y, 12, 18, text("switch_network"));
    }
    private void drawTool(GuiGraphics g, String action, int x, int y, int mx, int my) {
        boolean confirming = action.equals(confirmAction) && System.currentTimeMillis() < confirmUntil;
        g.drawString(font, trim(text(confirming ? "confirm" : action).getString(), 138), x + 25, y + 5, confirming ? 0xA56500 : TerminalAtlas.TEXT, false);
        TerminalAtlas.sprite(g, x + 165, y + 1, 0, 60 + (inside(mx, my, x + 165, y, 16, 18) ? 16 : 0), 16);
        hit(x + 165, y, 16, 18, () -> runTool(action));
        hover(mx, my, x + 22, y, 160, 18, text(action), text("confirm_hint"), text("selected_count", selectedIds.isEmpty() ? rows.size() : selectedIds.size()));
    }

    private void drawToolbox(GuiGraphics g, int x, int y, int mx, int my) {
        var cards = menu.data(TerminalChannels.META).getList("toolboxCards", Tag.TAG_COMPOUND); if (cards.isEmpty()) return;
        int bx = x + 211, by = y + footer() + 6;
        TerminalAtlas.panel(g, bx, by, 62, 72); g.drawString(font, trim(text("toolbox").getString(), 54), bx + 4, by + 4, TerminalAtlas.TEXT, false);
        for (int i = 0; i < Math.min(9, cards.size()); i++) {
            int cx = bx + 4 + i % 3 * 18, cy = by + 14 + i / 3 * 18; TerminalAtlas.slot(g, cx, cy, false);
            var card = ItemStack.of(cards.getCompound(i)); g.renderItem(card, cx + 1, cy + 1);
            if (!card.isEmpty()) hover(mx, my, cx, cy, 18, 18, card.getHoverName(), text("toolbox_help"));
            int index = i; hit(cx, cy, 18, 18, () -> { var payload = new CompoundTag(); payload.putInt("toolSlot", index); action("upgrade_insert", payload); });
        }
    }
    private void drawInspector(GuiGraphics g, int x, int y, int mx, int my) {
        int px = x + 211, py = y + 34; TerminalAtlas.panel(g, px, py, 100, 134);
        g.drawString(font, trim(selected.getString("name"), 83), px + 5, py + 6, TerminalAtlas.TEXT, false);
        g.drawString(font, text("priority_label"), px + 5, py + 21, TerminalAtlas.SECONDARY, false);
        TerminalAtlas.sprite(g, px + 80, py + 33, 48, 16, 12);
        hit(px + 80, py + 33, 12, 12, this::applyPriority); hover(mx, my, px + 80, py + 33, 12, 12, text("priority"));
        smallAction(g, px + 7, py + 53, 0, "partition_contents", "partition", selected, mx, my);
        smallAction(g, px + 23, py + 53, 8, "clear_partition", "clear", selected, mx, my);
        smallAction(g, px + 39, py + 53, 32, "fuzzy", "fuzzy", selected, mx, my);
        g.drawString(font, trim(selected.getString("fuzzy"), 38), px + 54, py + 53, TerminalAtlas.SECONDARY, false);
        var upgrades = selected.getList("upgrades", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(8, upgrades.size()); i++) {
            int cx = px + 6 + i % 4 * 18, cy = py + 66 + i / 4 * 18; TerminalAtlas.slot(g, cx, cy, false);
            var card = ItemStack.of(upgrades.getCompound(i)); g.renderItem(card, cx + 1, cy + 1);
            int index = i; hit(cx, cy, 18, 18, () -> { var payload = new CompoundTag(); payload.putInt("slot", index); action("upgrade_extract", payload); });
            hover(mx, my, cx, cy, 18, 18, card.isEmpty() ? text("upgrade") : card.getHoverName(), text("extract_upgrade"));
        }
        atlasAction(g, px + 6, py + 112, 0, "pickup", selected, mx, my, () -> action("pickup", new CompoundTag()));
        atlasAction(g, px + 26, py + 112, 24, "highlight", selected, mx, my, () -> action("highlight", new CompoundTag()));
        atlasAction(g, px + 46, py + 112, 12, "upgrade", selected, mx, my, () -> action("upgrade_insert", new CompoundTag()));
        g.drawString(font, "×", px + 85, py + 115, TerminalAtlas.TEXT, false);
        hit(px + 80, py + 108, 16, 18, () -> inspector = false); hover(mx, my, px + 80, py + 108, 16, 18, text("close"));
    }
    private void applyPriority() { try { var payload = new CompoundTag(); payload.putInt("priority", Integer.parseInt(priority.getValue())); action("priority", payload); } catch (NumberFormatException ignored) {} }
    private void drawScrollbar(GuiGraphics g, int x, int y) {
        int height = visibleRows * 18, track = height - 2; TerminalAtlas.inset(g, x + 189, y + 34, 12, height);
        int thumb = Math.max(15, track * visibleRows / Math.max(visibleRows, lines.size()));
        int offset = maxScroll() == 0 ? 0 : (track - thumb) * scroll / maxScroll(); TerminalAtlas.panel(g, x + 190, y + 35 + offset, 10, thumb);
        int cy = y + 35 + offset + thumb / 2; g.fill(x + 192, cy - 2, x + 198, cy - 1, 0xFF808080); g.fill(x + 192, cy + 1, x + 198, cy + 2, 0xFF808080);
    }
    private void scrollToMouse(double y) {
        int track = visibleRows * 18 - 2, thumb = Math.max(15, track * visibleRows / Math.max(visibleRows, lines.size()));
        double fraction = (y - topPos - 35 - thumb / 2d) / Math.max(1, track - thumb); scroll = Math.max(0, Math.min(maxScroll(), (int)Math.round(fraction * maxScroll())));
    }
    private String trim(String value, int width) { return font.width(value) <= width ? value : font.plainSubstrByWidth(value, width - font.width("…")) + "…"; }
    private static String shortAmount(long amount) { if (amount >= 1_000_000_000) return amount / 1_000_000_000 + "G"; if (amount >= 1_000_000) return amount / 1_000_000 + "M"; if (amount >= 1_000) return amount / 1_000 + "k"; return Long.toString(amount); }
    private static boolean inside(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    private void hit(int x, int y, int w, int h, Runnable action) { hits.add(new Hit(x, y, w, h, action)); }
    private void hover(int mx, int my, int x, int y, int w, int h, Component... text) { if (inside(mx, my, x, y, w, h)) hoverText = List.of(text); }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        mx /= uiScale; my /= uiScale;
        if (button == 0) for (var hit : hits) if (hit.contains(mx, my)) { hit.action.run(); return true; }
        if (inside(mx, my, leftPos + 189, topPos + 34, 12, visibleRows * 18)) { draggingScroll = true; scrollToMouse(my); return true; }
        if (inside(mx, my, leftPos + 21, topPos + 34, 162, visibleRows * 18)) {
            int index = ((int)my - topPos - 34) / 18 + scroll; if (index >= lines.size()) return true;
            var line = lines.get(index); var entry = line.entry; if (entry == null) return true; select(entry);
            if (line.kind == LineKind.CONTENT && shown != TerminalTab.SUBNETS) {
                int column = ((int)mx - leftPos - slotX(entry)) / 18, slot = line.index + column;
                if (mx < leftPos + slotX(entry) || column >= columns(entry) || slot >= displayCount(entry, partitionView())) return true;
                if (partitionView()) { var payload = new CompoundTag(); payload.putInt("slot", slot); payload.putBoolean("clear", button == 1); payload.putBoolean("fluid", hasControlDown()); action("partition", payload); }
                else if (hasShiftDown()) { var payload = new CompoundTag(); payload.putInt("slot", 0); payload.putInt("contentIndex", slot); action("partition", payload); } return true;
            }
            if (line.kind == LineKind.CELL) {
                if (shown == TerminalTab.NETWORK_TOOLS || hasShiftDown()) { long id = entry.getLong("id"); if (!selectedIds.add(id)) selectedIds.remove(id); return true; }
                if (button == 1 || !menu.getCarried().isEmpty() && mx < leftPos + 51) { action("pickup", new CompoundTag()); return true; }
            }
            long now = System.currentTimeMillis(), id = entry.getLong("id");
            if (button == 0 && id == lastClickTarget && now - lastClickTime < 400 && shown != TerminalTab.SUBNETS) action("highlight", new CompoundTag());
            lastClickTarget = id; lastClickTime = now; return true;
        }
        if (inside(mx, my, leftPos + 211, topPos + footer() + 6, 62, 72) && !menu.data(TerminalChannels.META).getList("toolboxCards", Tag.TAG_COMPOUND).isEmpty()) return true;
        if (priority.visible && inside(mx, my, leftPos + 211, topPos + 34, 100, 134)) { if (priority.mouseClicked(mx, my, button)) { setFocused(priority); priority.setFocused(true); } return true; }
        return super.mouseClicked(mx, my, button);
    }
    private void runTool(String action) {
        long now = System.currentTimeMillis(); if (!action.equals(confirmAction) || now > confirmUntil) { confirmAction = action; confirmUntil = now + 5000; return; }
        var targets = rows.stream().filter(row -> selectedIds.isEmpty() || selectedIds.contains(row.getLong("id"))).limit(256).toList();
        var payload = new CompoundTag(); payload.putLongArray("ids", targets.stream().mapToLong(row -> row.getLong("id")).toArray());
        payload.putLongArray("tokens", targets.stream().mapToLong(row -> row.getLong("token")).toArray()); payload.putBoolean("confirmed", true); menu.request(action, payload); confirmAction = "";
    }
    @Override public boolean mouseScrolled(double mx, double my, double delta) {
        mx /= uiScale; my /= uiScale;
        if (inside(mx, my, leftPos, topPos + 34, 208, visibleRows * 18)) { scroll = Math.max(0, Math.min(maxScroll(), scroll - (int)Math.signum(delta) * 3)); return true; }
        return super.mouseScrolled(mx, my, delta);
    }
    @Override public void render(GuiGraphics g, int x, int y, float partial) {
        x = (int)(x / uiScale); y = (int)(y / uiScale); g.pose().pushPose(); g.pose().scale(uiScale, uiScale, 1); super.render(g, x, y, partial);
        for (var tab : List.of(TerminalTab.BUS_INVENTORY, TerminalTab.BUS_PARTITION)) {
            int tx = leftPos + 4 + tab.ordinal() * 24; TerminalAtlas.sprite(g, tx + 13, topPos - 10, tab == TerminalTab.BUS_PARTITION ? 16 : 24, 0, 8);
        }
        if (!hoverText.isEmpty()) g.renderTooltip(font, hoverText, Optional.empty(), x, y); g.pose().popPose();
    }
    @Override public boolean mouseReleased(double x, double y, int button) { draggingScroll = false; return super.mouseReleased(x / uiScale, y / uiScale, button); }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) { if (draggingScroll) { scrollToMouse(y / uiScale); return true; } return super.mouseDragged(x / uiScale, y / uiScale, button, dx / uiScale, dy / uiScale); }
    @Override public void mouseMoved(double x, double y) { super.mouseMoved(x / uiScale, y / uiScale); }
    private net.minecraftforge.common.ForgeConfigSpec.ConfigValue<String> filterSetting(){
        var f=AWConfigs.CLIENT.cellterminal.filters;
        return switch(filterIndex){case 0 -> busRows()?f.busItemCells:f.cellItemCells;case 1 -> busRows()?f.busFluidCells:f.cellFluidCells;case 2 -> busRows()?f.busHasItems:f.cellHasItems;default -> busRows()?f.busPartitioned:f.cellPartitioned;};
    }
    private void cycleFilter(){var setting=filterSetting();setting.set(switch(setting.get()){case "SHOW_ALL"->"SHOW_ONLY";case "SHOW_ONLY"->"HIDE";default->"SHOW_ALL";});scroll=0;}
    private boolean matchesConfiguredFilters(CompoundTag entry){
        if(shown==TerminalTab.SUBNETS)return true;
        var f=AWConfigs.CLIENT.cellterminal.filters;boolean bus=busRows();
        boolean item=entry.getString("keyType").equals("item"),fluid=entry.getString("keyType").equals("fluid");
        if(bus){for(var value:entry.getList("contents",Tag.TAG_COMPOUND)){var stack=GenericStack.readTag((CompoundTag)value);if(stack!=null){item|=stack.what() instanceof AEItemKey;fluid|=stack.what() instanceof AEFluidKey;}}}
        return filter((bus?f.busItemCells:f.cellItemCells).get(),item) && filter((bus?f.busFluidCells:f.cellFluidCells).get(),fluid)
            && filter((bus?f.busHasItems:f.cellHasItems).get(),!entry.getList("contents",Tag.TAG_COMPOUND).isEmpty())
            && filter((bus?f.busPartitioned:f.cellPartitioned).get(),!entry.getList("partition",Tag.TAG_COMPOUND).isEmpty());
    }
    private static boolean filter(String state,boolean value){return state.equals("SHOW_ALL") || state.equals("SHOW_ONLY")==value;}

    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE && inspector) { inspector = false; priority.visible = false; priority.setFocused(false); return true; }
        if (key != GLFW.GLFW_KEY_ESCAPE) {
            if (priority.visible && priority.isFocused()) { if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) applyPriority(); else priority.keyPressed(key, scan, modifiers); return true; }
            if (search.isFocused()) { search.keyPressed(key, scan, modifiers); return true; }
            if (key == GLFW.GLFW_KEY_PAGE_DOWN || key == GLFW.GLFW_KEY_PAGE_UP) { scroll = Math.max(0, Math.min(maxScroll(), scroll + (key == GLFW.GLFW_KEY_PAGE_DOWN ? visibleRows : -visibleRows))); return true; }
        }
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public void removed() {
        var gui = AWConfigs.CLIENT.cellterminal.gui;
        gui.selectedTab.set(shown.ordinal()); gui.searchFilter.set(search == null ? "" : search.getValue()); gui.searchMode.set(searchMode.name());
        long viewed = menu.data(TerminalChannels.META).getLong("networkId"); gui.lastViewedNetworkId.set(Long.toString(viewed == menu.data(TerminalChannels.META).getLong("rootNetworkId") ? 0 : viewed)); AWConfigs.CLIENT_SPEC.save(); super.removed();
    }
}
