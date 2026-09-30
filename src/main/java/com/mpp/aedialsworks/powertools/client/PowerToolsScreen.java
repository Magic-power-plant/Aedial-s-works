package com.mpp.aedialsworks.powertools.client;

import java.util.*;
import java.util.function.Supplier;
import appeng.api.stacks.*;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.StyleManager;
import appeng.client.gui.widgets.*;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import com.mpp.aedialsworks.powertools.menu.PowerToolsMenu;
import com.mpp.aedialsworks.powertools.menu.PowerToolsSlotSemantics;
import com.mpp.aedialsworks.powertools.scanner.NetworkScanner;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fluids.FluidUtil;

/** Original PowerTools layouts using AE2 rendering, input, slots and child-screen lifecycle. */
public final class PowerToolsScreen extends AEBaseScreen<PowerToolsMenu> {
    public enum View { MAINTAINER, MONITOR, REMOTE, CRAFTER, CRAFTER_OVERVIEW, ENTRY, SETTINGS,
        SELECTOR, INVENTORY, SCANNER, LOCATOR, LOCATIONS, PRIORITY }
    private static final String[] COMPARISONS = { "<", "<=", "=", ">=", ">", "!=" };
    private final View view;
    private final PowerToolsScreen parent;
    private int selected, tab, selectedTarget = -1;
    private String componentGroup = "";
    private boolean loaded;
    private AETextField search;
    private ConfirmableTextField threshold, secondary, interval, strength, priority;
    private Scrollbar scrollbar;
    private final List<Button> rowButtons = new ArrayList<>(), runButtons = new ArrayList<>();
    private Button comparisonButton;
    private final List<TabButton> tabs = new ArrayList<>();
    private List<Integer> visibleEntries = List.of();
    private List<CompoundTag> visibleRows = List.of();

    public PowerToolsScreen(PowerToolsMenu menu, Inventory inventory, Component title) {
        this(menu, inventory, title, initialView(menu), null, 0);
    }
    private PowerToolsScreen(PowerToolsMenu menu, Inventory inventory, Component title,
            View view, PowerToolsScreen parent, int selected) {
        super(menu, inventory, title, StyleManager.loadStyleDoc(
                "/screens/aedialsworks/powertools/" + view.name().toLowerCase(Locale.ROOT) + ".json"));
        this.view = view; this.parent = parent; this.selected = selected;
        var pageTitle = view == View.SELECTOR ? tr("choose_resource") : view == View.CRAFTER_OVERVIEW ? tr("recipes") : title;
        setTextContent("dialog_title", fit(pageTitle, view == View.MAINTAINER ? 145 : view == View.CRAFTER ? 120 : 172));
        switch (view) {
            case MAINTAINER -> setupMaintainer();
            case MONITOR, REMOTE -> setupMonitor();
            case CRAFTER -> setupCrafter();
            case CRAFTER_OVERVIEW -> backButton();
            case ENTRY -> setupEntry();
            case SETTINGS -> setupSettings();
            case SCANNER, LOCATOR, LOCATIONS -> setupTools();
            case PRIORITY -> setupPriority();
            case SELECTOR -> setupSelector();
            case INVENTORY -> { backButton(); widgets.addBackgroundPanel("inventoryPanel"); }
        }
    }
    private static View initialView(PowerToolsMenu menu) {
        return switch (menu.host.powerKind()) {
            case "better_level_maintainer" -> View.MAINTAINER;
            case "auto_crafter" -> View.CRAFTER;
            case "remote_storage_monitor" -> View.REMOTE;
            case "network_health_scanner" -> View.SCANNER;
            case "network_component_locator", "priority_tuner" -> View.LOCATOR;
            default -> View.MONITOR;
        };
    }
    public View currentView() { return view; }
    private boolean crafter() { return menu.host.powerKind().equals("auto_crafter"); }
    private boolean maintainer() { return menu.host.powerKind().equals("better_level_maintainer"); }
    private boolean remote() { return menu.host.powerKind().equals("remote_storage_monitor"); }
    private boolean emitter() { return menu.host.powerKind().equals("storage_level_emitter"); }
    private boolean tuner() { return menu.host.powerKind().equals("priority_tuner"); }
    private Component tr(String key) { return Component.translatable("gui.aedialsworks.powertools." + key); }
    private Component fit(Component label, int width) { return Component.literal(font.plainSubstrByWidth(label.getString(), width)); }
    private Button button(String id, Component label, Runnable action) { return widgets.addButton(id, label, action); }
    /** Action binding only; AE2 supplies painting, focus and tooltip behavior. */
    private Button icon(String id, Supplier<Icon> image, Component tooltip, Runnable action) {
        var button = new IconButton(ignored -> action.run()) { @Override protected Icon getIcon() { return image.get(); } };
        button.setMessage(tooltip); widgets.add(id, button); return button;
    }
    private Button icon(String id, Icon image, String tooltip, Runnable action) { return icon(id, () -> image, tr(tooltip), action); }
    private void backButton() { icon("back", Icon.ARROW_LEFT, "back", this::back); }
    private void inventoryButton() { icon("inventoryButton", Icon.VIEW_MODE_STORED, "inventory", () -> open(View.INVENTORY)); }
    private void search() {
        search = widgets.addTextField("search"); search.setMaxLength(64); search.setPlaceholder(fit(tr("search"), style.getWidget("search").getWidth()-8));
        search.setResponder(value -> { if (scrollbar != null) scrollbar.setCurrentScroll(0); });
    }
    private ConfirmableTextField number(String id, boolean signed, Runnable confirm) {
        var field = new ConfirmableTextField(style, font, 0, 0, 0, 0); field.setBordered(false); field.setMaxLength(20);
        field.setFilter(value -> value.isEmpty() || value.matches(signed ? "-?[0-9]*" : "[0-9]*"));
        field.setOnConfirm(confirm); widgets.add(id, field); return field;
    }
    private void setupMaintainer() {
        search(); scrollbar = widgets.addScrollBar("scrollbar"); inventoryButton();
        for (int i = 0; i < 18; i++) {
            final int cell = i;
            var run = (IconButton) icon("run" + i, Icon.ARROW_RIGHT, "run", () -> {
                if (cell < visibleEntries.size()) { selected = visibleEntries.get(cell); selectedAction("run"); }
            });
            run.setHalfSize(true); run.setDisableBackground(true); runButtons.add(run);
        }
    }
    private void setupMonitor() {
        inventoryButton(); widgets.add("settings", new TabButton(Icon.SCHEDULING_DEFAULT, tr("settings"), ignored -> open(View.SETTINGS)));
        if (view == View.MONITOR || view == View.REMOTE) {
            if (view == View.MONITOR) threshold = number("threshold", false, this::saveThreshold);
            icon("mode", () -> menu.data.getBoolean("any") ? Icon.REDSTONE_HIGH : Icon.REDSTONE_LOW, tr("match_mode"), () -> changeSetting("any"));
            icon("hysteresis", () -> menu.data.getBoolean("hysteresis") ? Icon.BLOCKING_MODE_YES : Icon.BLOCKING_MODE_NO, tr("hysteresis"), () -> changeSetting("hysteresis"));
        }
        if (remote()) icon("binding", Icon.VIEW_MODE_ALL, "hud", () -> changeSetting("hud"));
        else if (menu.host.powerKind().equals("storage_level_alarm")) icon("binding", Icon.REDSTONE_PULSE, "bind", () -> menu.action("bind", new CompoundTag()));
    }
    private void setupCrafter() {
        icon("overview", Icon.PATTERN_ACCESS_SHOW, "recipes", () -> open(View.CRAFTER_OVERVIEW));
        widgets.add("machine", new TabButton(Icon.SCHEDULING_DEFAULT, tr("ticks"), ignored -> open(View.SETTINGS)));
        widgets.add("batch", new TabButton(Icon.CRAFT_HAMMER, tr("batch"), ignored -> open(View.SETTINGS)));
        icon("enabled", () -> entry().getBoolean("enabled") ? Icon.VALID : Icon.INVALID, tr("enabled"), this::toggleEntry);
        icon("edit", Icon.LEVEL_ITEM, "target", () -> open(View.ENTRY));
        button("previousRecipe", Component.literal("<"), () -> selectRecipe(Math.floorMod(selected - 1, 12)));
        button("nextRecipe", Component.literal(">"), () -> selectRecipe((selected + 1) % 12));
    }
    private void setupEntry() {
        backButton(); threshold = number("threshold", false, this::saveEntry);
        secondary = number("secondaryInput", false, this::saveEntry); interval = number("interval", false, this::saveEntry);
        setTextContent("secondary", tr(maintainer() || crafter() ? "batch" : "reset")); setTextContent("ticks", tr("ticks"));
        icon("enabled", () -> entry().getBoolean("enabled") ? Icon.VALID : Icon.INVALID, tr("enabled"), this::toggleEntry);
        icon("save", Icon.ENTER, "save", () -> { if (saveEntry()) back(); });
        if (!crafter()) { icon("choose", Icon.SEARCH_DEFAULT, "choose_resource", () -> open(View.SELECTOR)); icon("clear", Icon.CLEAR, "clear", this::clearEntry); }
        if (!maintainer() && !crafter()) comparisonButton = button("compare", Component.empty(), () -> {
            var entry = entry(); entry.putInt("comparison", Math.floorMod(entry.getInt("comparison") + 1, 6)); sendEntry(entry);
        });
        if (maintainer()) { icon("run", Icon.ARROW_RIGHT, "run", () -> selectedAction("run")); icon("cancel", Icon.INVALID, "cancel", () -> selectedAction("cancel")); }
    }
    private void setupSettings() {
        backButton(); interval = number("interval", false, this::saveSettings); secondary = number("secondaryInput", false, this::saveSettings);
        strength = number("strengthInput", false, this::saveSettings);
        if (crafter()) { setTextContent("secondary", tr("batch")); strength.visible = false; setTextHidden("ticks", true); }
        else { if (!emitter()) strength.visible = false; if (remote()) setTextHidden("ticks", true); }
        icon("save", Icon.ENTER, "save", () -> { if (saveSettings()) back(); });
    }
    private void setupSelector() {
        backButton(); search(); scrollbar = widgets.addScrollBar("scrollbar"); widgets.addBackgroundPanel("inventoryPanel");
        for (int i = 0; i < 5; i++) { final int row = i; rowButtons.add(button("result" + i, Component.empty(), () -> {
            if (row < visibleRows.size()) choose(AEKey.fromTagGeneric(visibleRows.get(row).getCompound("key")));
        })); }
        menu.action("catalog", new CompoundTag());
    }
    private void setupTools() {
        search(); scrollbar = view == View.SCANNER ? widgets.addScrollBar("scrollbar", Scrollbar.SMALL) : widgets.addScrollBar("scrollbar");
        button("scan", tr("scan"), () -> menu.action("scan", new CompoundTag()));
        if (view == View.SCANNER) {
            Icon[] icons = { Icon.SCHEDULING_ROUND_ROBIN, Icon.VIEW_MODE_ALL, Icon.FULLNESS_FULL, Icon.POWER_UNIT_AE, Icon.INVALID, Icon.CRAFT_HAMMER };
            for (int i = 0; i < NetworkScanner.TABS.size(); i++) {
                final int category = i;
                var tabButton = new TabButton(icons[i], tr("tab_" + NetworkScanner.TABS.get(i)), ignored -> { tab = category; scrollbar.setCurrentScroll(0); });
                tabButton.setStyle(TabButton.Style.HORIZONTAL); tabs.add(tabButton); widgets.add("tab" + i, tabButton);
            }
        } else if (view == View.LOCATOR) {
            button("back", tr("back"), this::onClose);
            for (int i = 0; i < 20; i++) {
                final int cell = i;
                var component = new IconButton(ignored -> openGroup(cell)) {
                    @Override protected Icon getIcon() { return Icon.VIEW_MODE_STORED; }
                    @Override protected net.minecraft.world.item.Item getItemOverlay() {
                        if (cell >= visibleRows.size()) return null;
                        AEKey key = AEKey.fromTagGeneric(visibleRows.get(cell).getCompound("icon"));
                        return key instanceof AEItemKey item ? item.getItem() : Items.COMPASS;
                    }
                };
                component.setDisableBackground(true); widgets.add("component" + i, component); rowButtons.add(component);
            }
            return;
        } else { backButton(); button("locate", tr(tuner() ? "apply_priority" : "locate"), this::applySelectedLocation); }
        for (int i = 0; i < (view == View.SCANNER ? 10 : 5); i++) { final int row = i; rowButtons.add(button("result" + i, Component.empty(), () -> selectResult(row))); }
    }
    private void setupPriority() {
        backButton(); priority = number("priority", true, this::savePriority); icon("save", Icon.ENTER, "save", () -> { if (savePriority()) back(); });
        int[] deltas = { 1, 10, 100, 1000 };
        for (int i = 0; i < deltas.length; i++) { final int delta = deltas[i];
            button("plus" + i, Component.literal("+" + delta), () -> adjustPriority(delta));
            button("minus" + i, Component.literal("-" + delta), () -> adjustPriority(-delta));
        }
    }

    private void open(View next) {
        var screen = new PowerToolsScreen(menu, menu.getPlayerInventory(), title, next, this, selected);
        screen.selectedTarget = selectedTarget; screen.componentGroup = componentGroup; switchToScreen(screen);
    }
    private void back() {
        if (parent == null) { super.onClose(); return; }
        parent.loaded = false; switchToScreen(parent);
        if (parent.view == View.CRAFTER) parent.selectRecipe(parent.selected);
    }
    @Override public void onClose() { if (parent == null) super.onClose(); else back(); }
    private void position(SlotSemantic semantic, int x, int y) {
        var position = style.getSlots().get(semantic.id()); if (position == null) return;
        if (Objects.equals(position.getLeft(), x) && Objects.equals(position.getTop(), y)) return;
        position.setLeft(x); position.setTop(y); repositionSlots(semantic);
    }
    private void selectedAction(String action) { var payload = new CompoundTag(); payload.putInt("slot", selected); menu.action(action, payload); }
    private void selectRecipe(int page) { selected = page; loaded = false; menu.refreshRecipePreview(page); selectedAction("view_recipe"); }
    private CompoundTag settings() {
        var result = new CompoundTag();
        for (String key : List.of("any", "hysteresis", "hud")) result.putBoolean(key, menu.data.getBoolean(key));
        result.putInt("strength", Math.max(1, menu.data.getInt("strength"))); result.putInt("refresh", Math.max(20, menu.data.getInt("refresh"))); return result;
    }
    private void changeSetting(String name) {
        var value = settings(); value.putBoolean(name, !value.getBoolean(name)); menu.action("settings", value); menu.data.putBoolean(name, value.getBoolean(name));
    }
    private CompoundTag entry() {
        var result = menu.data.getList("entries", Tag.TAG_COMPOUND).getCompound(selected).copy(); result.putInt("slot", selected);
        if (crafter()) result.putLong("threshold", result.getLong("target")); return result;
    }
    private void sendEntry(CompoundTag entry) {
        entry.putInt("slot", selected); menu.action("entry", entry); var entries = menu.data.getList("entries", Tag.TAG_COMPOUND);
        if (selected < entries.size()) { if (crafter()) entry.putLong("target", entry.getLong("threshold")); entries.set(selected, entry.copy()); }
        menu.refreshConfigSlots();
    }
    private void clearEntry() { var entry = entry(); entry.remove("key"); entry.putBoolean("enabled", false); sendEntry(entry); loaded = false; }
    private void toggleEntry() { var entry = entry(); entry.putBoolean("enabled", !entry.getBoolean("enabled")); sendEntry(entry); }
    private void choose(AEKey key) {
        if (key == null) return;
        var entry = entry(); entry.put("key", key.toTagGeneric()); entry.putBoolean("enabled", true);
        if (entry.getLong("threshold") == 0) entry.putLong("threshold", 64);
        if (entry.getLong("batch") == 0) entry.putLong("batch", 64);
        sendEntry(entry); loaded = false; if (view == View.SELECTOR) back();
    }
    private void loadFields() {
        var entry = entry();
        if (threshold != null) threshold.setValue(Long.toString(entry.getLong("threshold")));
        if (secondary != null) secondary.setValue(Long.toString(crafter() ? Math.max(1, menu.data.getInt("batch"))
                : maintainer() ? Math.max(1, entry.getLong("batch")) : entry.getLong("reset")));
        if (interval != null) interval.setValue(Integer.toString(crafter() ? Math.max(1, menu.data.getInt("speed"))
                : maintainer() ? Math.max(20, entry.getInt("interval")) : Math.max(20, menu.data.getInt("refresh"))));
        if (strength != null) strength.setValue(Integer.toString(Math.max(1, menu.data.getInt("strength"))));
        if (priority != null) for (var raw : menu.data.getList("nodes", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) raw;
            if (row.getInt("id") == selectedTarget) { priority.setValue(Integer.toString(row.getInt("priority"))); setTextContent("resource", fit(rowName(row), 210)); break; }
        }
        loaded = true;
    }
    private boolean saveThreshold() {
        try { var entry = entry(); entry.putLong("threshold", Long.parseLong(threshold.getValue())); sendEntry(entry); return true; }
        catch (NumberFormatException exception) { invalid(); return false; }
    }
    private boolean saveEntry() {
        try {
            long amount = Long.parseLong(threshold.getValue()), value = Long.parseLong(secondary.getValue()); int ticks = Integer.parseInt(interval.getValue());
            if (ticks < (crafter() ? com.mpp.aedialsworks.powertools.crafter.CrafterLogic.MIN_SPEED_TICKS : 20) || ticks > (crafter() ? 72000 : maintainer() ? Integer.MAX_VALUE : 1200)
                    || (maintainer() || crafter()) && value < 1 || crafter() && value > 1000000) { invalid(); return false; }
            var entry = entry(); entry.putLong("threshold", amount); entry.putLong("reset", value); entry.putLong("batch", value); entry.putInt("interval", ticks); sendEntry(entry);
            if (crafter()) sendCrafterSettings(ticks, (int) value);
            else if (!maintainer()) { var settings = settings(); settings.putInt("refresh", ticks); menu.action("settings", settings); }
            setTextContent("message", tr("saved")); return true;
        } catch (NumberFormatException exception) { invalid(); return false; }
    }
    private void sendCrafterSettings(int ticks, int batch) {
        var settings = new CompoundTag(); settings.putInt("speed", ticks); settings.putInt("batch", batch); menu.action("settings", settings);
    }
    private boolean saveSettings() {
        try {
            int ticks = Integer.parseInt(interval.getValue()); long value = Long.parseLong(secondary.getValue());
            if (crafter()) {
                if (ticks < com.mpp.aedialsworks.powertools.crafter.CrafterLogic.MIN_SPEED_TICKS || ticks > 72000 || value < 1 || value > 1000000) { invalid(); return false; } sendCrafterSettings(ticks, (int) value);
            } else {
                int signal = Integer.parseInt(strength.getValue()); if (ticks < 20 || ticks > 1200 || signal < 1 || signal > 15) { invalid(); return false; }
                var settings = settings(); settings.putInt("refresh", ticks); settings.putInt("strength", signal); menu.action("settings", settings);
                var entry = entry(); entry.putLong("reset", value); sendEntry(entry);
            }
            setTextContent("message", tr("saved")); return true;
        } catch (NumberFormatException exception) { invalid(); return false; }
    }
    private void adjustPriority(int delta) {
        try { long value = Long.parseLong(priority.getValue()) + delta; priority.setValue(Long.toString(Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value)))); }
        catch (NumberFormatException exception) { invalid(); }
    }
    private boolean savePriority() {
        try {
            if (selectedTarget < 0) { invalid(); return false; }
            var payload = new CompoundTag(); payload.putInt("target", selectedTarget); payload.putInt("priority", Integer.parseInt(priority.getValue())); menu.action("priority", payload); return true;
        } catch (NumberFormatException exception) { invalid(); return false; }
    }
    private void invalid() { setTextContent("message", tr("invalid")); }
    private AEKey key(CompoundTag entry) { return entry.contains("key") ? AEKey.fromTagGeneric(entry.getCompound("key")) : null; }
    private Component rowName(CompoundTag row) {
        AEKey key = AEKey.fromTagGeneric(row.getCompound(row.contains("key") ? "key" : "icon")); return key == null ? Component.literal(row.getString("name")) : key.getDisplayName();
    }
    private String group(CompoundTag row) { return row.contains("icon") ? row.getCompound("icon").toString() : row.getString("name"); }
    private boolean matches(String name) { return search == null || name.toLowerCase(Locale.ROOT).contains(search.getValue().toLowerCase(Locale.ROOT)); }
    private List<CompoundTag> rows() {
        var result = new ArrayList<CompoundTag>(); String list = view == View.SELECTOR ? "catalog" : view == View.SCANNER ? "issues" : "nodes";
        for (var raw : menu.data.getList(list, Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) raw;
            if (view == View.SCANNER && !row.getString("tab").equals(NetworkScanner.TABS.get(tab))) continue;
            if ((view == View.LOCATOR || view == View.LOCATIONS) && tuner() && !row.getBoolean("tunable")) continue;
            if (view == View.LOCATIONS && !group(row).equals(componentGroup)) continue;
            if (!matches(rowName(row).getString())) continue; result.add(row);
        }
        return result;
    }
    private void openGroup(int cell) {
        if (cell >= visibleRows.size()) return; componentGroup = group(visibleRows.get(cell)); selectedTarget = visibleRows.get(cell).getInt("id"); open(View.LOCATIONS);
    }
    private void selectResult(int cell) {
        if (cell >= visibleRows.size()) return; selectedTarget = visibleRows.get(cell).getInt("id");
        if (view == View.SCANNER || !tuner()) locate(visibleRows.get(cell)); else open(View.PRIORITY);
    }
    private void applySelectedLocation() {
        if (selectedTarget < 0) return; if (tuner()) { open(View.PRIORITY); return; }
        for (var row : rows()) if (row.getInt("id") == selectedTarget) { locate(row); break; }
    }
    private void locate(CompoundTag row) {
        var payload = new CompoundTag(); payload.putInt("target", row.getInt("id")); payload.putLong("pos", row.getLong("pos")); menu.action("locate", payload);
    }
    private static String compact(long n) {
        if (n < 1000) return Long.toString(n); String[] suffix = { "k", "M", "G", "T", "P", "E" }; double value = n; int i = -1;
        do { value /= 1000; i++; } while (value >= 1000 && i < suffix.length - 1);
        return String.format(Locale.ROOT, value >= 100 ? "%.0f%s" : "%.1f%s", value, suffix[i]);
    }

    @Override protected void updateBeforeRender() {
        super.updateBeforeRender();
        if (view == View.SCANNER || view == View.LOCATOR || view == View.LOCATIONS || view == View.SELECTOR) { updateRows(); return; }
        if (!loaded && (menu.data.contains("entries") || view == View.PRIORITY)) loadFields();
        var entries = menu.data.getList("entries", Tag.TAG_COMPOUND);
        if (view == View.MAINTAINER) {
            var filtered = new ArrayList<Integer>();
            for (int i = 0; i < 24; i++) { var key = key(entries.getCompound(i)); if (matches(key == null ? "" : key.getDisplayName().getString())) filtered.add(i); }
            scrollbar.setRange(0, Math.max(0, (filtered.size() + 2) / 3 - 6), 1); int start = scrollbar.getCurrentScroll() * 3;
            visibleEntries = filtered.subList(Math.min(start, filtered.size()), Math.min(start + 18, filtered.size()));
            for (int i = 0; i < 24; i++) { int cell = visibleEntries.indexOf(i); position(PowerToolsSlotSemantics.CONFIG[i], cell < 0 ? -10000 : 12 + cell % 3 * 68, cell < 0 ? -10000 : 21 + cell / 3 * 23); }
            int configured = 0, active = 0;
            for (int i = 0; i < 24; i++) { var entry = entries.getCompound(i); if (entry.contains("key")) configured++; if (entry.getBoolean("enabled") && entry.contains("key")) active++; }
            setTextContent("summary", tr("recipes").copy().append(" " + configured + " / 24")); setTextContent("active", tr("active").copy().append(" " + active));
            for (int i = 0; i < 18; i++) {
                boolean visible = i < visibleEntries.size(); runButtons.get(i).visible = visible;
                var entry = visible ? entries.getCompound(visibleEntries.get(i)) : new CompoundTag();
                setTextContent("amount" + i, Component.literal(entry.contains("key") ? compact(entry.getLong("quantity")) + " / " + compact(entry.getLong("threshold")) : ""));
                setTextContent("state" + i, entry.contains("key") ? fit(tr("state_" + entry.getString("state")), 60) : Component.empty());
            }
        } else if (view == View.MONITOR) {
            for (int i = 0; i < 24; i++) { var entry = entries.getCompound(i);
                setTextContent("comparison" + i, Component.literal(COMPARISONS[Math.floorMod(entry.getInt("comparison"), 6)]));
                setTextContent("amount" + i, Component.literal(compact(entry.getLong("threshold"))));
            }
            setTextContent("entry", tr("entry").copy().append(" " + (selected + 1) + "  " + entry().getLong("quantity") + " / " + entry().getLong("threshold")));
        } else if (view == View.ENTRY) {
            if (comparisonButton != null) comparisonButton.setMessage(Component.literal(COMPARISONS[Math.floorMod(entry().getInt("comparison"), 6)]));
            if (!crafter()) position(PowerToolsSlotSemantics.CONFIG[selected], 6, 6);
            var key = key(entry()); setTextContent("resource", key == null ? tr("choose_resource") : fit(key.getDisplayName(), 167));
            setTextContent("quantity", Component.literal(compact(entry().getLong("quantity"))));
            setTextContent("state", entry().getString("state").isEmpty() ? Component.empty() : fit(tr("state_" + entry().getString("state")), 70));
        } else if (view == View.CRAFTER) {
            for (int i = 0; i < 12; i++) position(PowerToolsSlotSemantics.PATTERN[i], i == selected ? 17 : -10000, i == selected ? 43 : -10000);
            menu.refreshRecipePreview(selected); setTextContent("page", Component.literal((selected + 1) + " / 12"));
            setTextContent("speed", tr("ticks").copy().append(" " + menu.data.getInt("speed") + " · " + tr("batch").getString() + " " + menu.data.getInt("batch")));
            setTextContent("state", fit(tr("state_" + entry().getString("state")), 175));
        } else if (view == View.CRAFTER_OVERVIEW) {
            for (int i = 0; i < 12; i++) setTextContent("recipe" + i, Component.literal((i + 1) + " · ").append(fit(tr("state_" + entries.getCompound(i).getString("state")), 160)));
        }
    }
    private void updateRows() {
        var rows = rows();
        if (view == View.SELECTOR) setTextContent("message", menu.data.getBoolean("catalogTruncated") ? tr("truncated") : Component.empty());
        if (view == View.LOCATOR) {
            var groups = new LinkedHashMap<String, CompoundTag>();
            for (var row : rows) { var group = groups.computeIfAbsent(group(row), ignored -> row.copy()); group.putInt("count", group.getInt("count") + 1); }
            rows = new ArrayList<>(groups.values());
        }
        int columns = view == View.LOCATOR ? 5 : 1, capacity = rowButtons.size();
        scrollbar.setRange(0, Math.max(0, (rows.size() + columns - 1) / columns - capacity / columns), 1);
        int start = Math.min(scrollbar.getCurrentScroll() * columns, rows.size()); visibleRows = rows.subList(start, Math.min(start + capacity, rows.size()));
        for (int i = 0; i < capacity; i++) {
            var button = rowButtons.get(i); button.visible = i < visibleRows.size();
            if (!button.visible) { if (view == View.LOCATOR) setTextContent("count" + i, Component.empty()); continue; }
            var row = visibleRows.get(i); String detail = rowName(row).getString();
            if (view == View.LOCATOR) setTextContent("count" + i, Component.literal(Integer.toString(row.getInt("count"))));
            else if (view == View.LOCATIONS || view == View.SCANNER) {
                var pos = BlockPos.of(row.getLong("pos"));
                detail = (view == View.SCANNER ? tr("issue_" + row.getString("code")).getString() + " · " : "")
                        + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + (tuner() ? " · " + row.getInt("priority") : "");
            }
            button.setMessage(fit(Component.literal(detail), view == View.SCANNER ? 264 : 152)); button.setTooltip(Tooltip.create(rowName(row).copy().append("\n" + detail)));
        }
        if (view != View.SELECTOR) setTextContent("empty", rows.isEmpty() ? tr(view == View.SCANNER ? "no_issues" : "no_components") : Component.empty());
        if (view == View.SCANNER) {
            for (int i = 0; i < tabs.size(); i++) tabs.get(i).setSelected(i == tab);
            setTextContent("category", fit(tr("tab_" + NetworkScanner.TABS.get(tab)), 185));
            setTextContent("page", Component.literal(rows.size() + " · " + menu.data.getInt("grids") + " " + tr("networks").getString()
                    + (menu.data.getBoolean("truncated") ? " · " + tr("truncated").getString() : "")));
        }
    }
    private static AEKey carriedKey(ItemStack stack) {
        AEKey key = AEItemKey.of(stack);
        if (hasControlDown()) key = FluidUtil.getFluidContained(stack).<AEKey>map(AEFluidKey::of).orElse(key); return key;
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (view == View.SELECTOR) {
            for (Slot slot : menu.slots) if ((menu.getSlotSemantic(slot) == SlotSemantics.PLAYER_INVENTORY || menu.getSlotSemantic(slot) == SlotSemantics.PLAYER_HOTBAR) && isHovering(slot, mouseX, mouseY)) {
                if (button == 0 && !slot.getItem().isEmpty()) choose(carriedKey(slot.getItem())); return true;
            }
        }
        if (view == View.CRAFTER_OVERVIEW) {
            int x = (int) mouseX - getGuiLeft(), y = (int) mouseY - getGuiTop();
            if (button == 0 && x >= 7 && x < 170 && y >= 25 && y < 241) { int recipe = (y - 25) / 18; back(); parent.selectRecipe(recipe); return true; }
        }
        var slots = menu.getConfigurationSlots();
        for (int i = 0; i < slots.size(); i++) {
            if (!isHovering(slots.get(i), mouseX, mouseY)) continue;
            if (button != 0 && button != 1) return true; selected = i; loaded = false;
            if (button == 1) clearEntry();
            else if (!menu.getCarried().isEmpty()) choose(carriedKey(menu.getCarried()));
            else if (view == View.MONITOR || view == View.ENTRY) open(View.SELECTOR); else open(View.ENTRY);
            return true;
        }
        int x = (int) mouseX - getGuiLeft(), y = (int) mouseY - getGuiTop();
        if (view == View.MONITOR && x >= 9 && x < 213 && y >= 19 && y < 157) {
            selected = (y - 19) / 23 * 4 + (x - 9) / 51; loaded = false;
            if ((x - 9) % 51 < 31) { var entry = entry(); entry.putInt("comparison", Math.floorMod(entry.getInt("comparison") + (button == 1 ? -1 : 1), 6)); sendEntry(entry); }
            else if (button == 1) open(View.ENTRY);
            else { loadFields(); setFocused(threshold); threshold.setFocused(true); threshold.setHighlightPos(0); } return true;
        }
        if (view == View.MAINTAINER && x >= 9 && x < 213 && y >= 18 && y < 156 && (x - 9) % 68 < 56) {
            int cell = (y - 18) / 23 * 3 + (x - 9) / 68; if (cell < visibleEntries.size()) { selected = visibleEntries.get(cell); open(View.ENTRY); } return true;
        }
        for (Slot slot : menu.slots) {
            var semantic = menu.getSlotSemantic(slot);
            if ((semantic == SlotSemantics.CRAFTING_GRID || semantic == SlotSemantics.CRAFTING_RESULT || semantic == SlotSemantics.MISSING_INGREDIENT) && isHovering(slot, mouseX, mouseY)) return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
