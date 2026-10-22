package com.cobbletown.collectionexporter.ui;

import com.cobbletown.collectionexporter.clipboard.ClipboardResult;
import com.cobbletown.collectionexporter.data.InventoryCompleteness;
import com.cobbletown.collectionexporter.report.ReportCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Native collection screen. The scrollable report is the primary surface; filters are compact
 * and the clipboard remains available from the footer.
 */
public final class CollectionExporterScreen extends Screen {
    private static final int PANEL = 0xC0080A0C;
    private static final int BORDER = 0x504A5562;
    private static final int TITLE = 0xFFFFFFFF;
    private static final int VERSION_LABEL = 0xFFA0A5AD;

    private final Screen parent;
    private final ExportController controller;
    private final ExportScreenModel model = new ExportScreenModel();

    private boolean loaded;
    private String loadFailure;
    private ButtonWidget copyButton;
    private ExportScreenLayout layout;
    private ReportViewerWidget viewer;
    private int copyFeedbackTicks;

    public CollectionExporterScreen(Screen parent, ExportController controller) {
        super(Text.literal("Collection Exporter"));
        this.parent = parent;
        this.controller = controller;
    }

    @Override
    protected void init() {
        ReportViewerWidget.State viewerState = viewer == null ? null : viewer.state();
        layout = ExportScreenLayout.compute(width, height, filterRows());
        ensureLoaded();

        addDrawable((context, mouseX, mouseY, delta) -> drawPanel(context));
        addFilters();
        addViewer(viewerState);
        addActions();
        updateCopyState();
    }

    private int filterRows() {
        ExportScreenLayout base = ExportScreenLayout.compute(width, height, 1);
        List<Integer> widths = filterSpecs().stream()
                .map(spec -> filterWidth(spec.label())).toList();
        return ExportFilterLayout.layout(base.contentX(), base.filterTop(),
                base.contentWidth(), widths, 6, base.filterRowHeight()).rows();
    }

    private List<FilterSpec> filterSpecs() {
        List<FilterSpec> specs = new ArrayList<>();
        specs.add(new FilterSpec("All", ReportCategory.ALL));
        specs.add(new FilterSpec("Custom Skins", ReportCategory.SKINS));
        specs.add(new FilterSpec("Legend & Mythic", ReportCategory.LEGEND_MYTH));
        specs.add(new FilterSpec("Shiny", ReportCategory.SHINY));
        specs.add(new FilterSpec("Show PC Locations", null));
        return specs;
    }

    private void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        readPc(true);
    }

    private void readPc(boolean freshRead) {
        try {
            if (freshRead) {
                model.refresh(controller);
            } else {
                model.rebuild(controller);
            }
            loadFailure = null;
        } catch (RuntimeException failure) {
            loadFailure = shortFailure(failure);
        }
    }

    private void addFilters() {
        List<FilterSpec> specs = filterSpecs();
        ExportScreenLayout base = ExportScreenLayout.compute(width, height, layout.filterRows());
        ExportFilterLayout.Result result = ExportFilterLayout.layout(
                base.contentX(), base.filterTop(), base.contentWidth(),
                specs.stream().map(spec -> filterWidth(spec.label())).toList(),
                6, base.filterRowHeight());
        for (int index = 0; index < specs.size(); index++) {
            FilterSpec spec = specs.get(index);
            ExportFilterLayout.Placement placement = result.get(index);
            CheckboxWidget.Builder builder = CheckboxWidget.builder(
                            Text.literal(spec.label()), textRenderer)
                    .pos(placement.x(), placement.y())
                    .maxWidth(base.contentWidth())
                    .checked(spec.category() == null
                            ? model.showLocations()
                            : model.isSelected(spec.category()));
            if (spec.category() == null) {
                builder.callback((widget, checked) -> onLocationsChanged(checked));
            } else {
                builder.callback((widget, checked) -> onFilterChanged(spec.category(), checked));
            }
            addDrawableChild(builder.build());
        }
    }

    private int filterWidth(String label) {
        return CheckboxWidget.getCheckboxSize(textRenderer) + 4 + textRenderer.getWidth(label);
    }

    private void addViewer(ReportViewerWidget.State state) {
        viewer = new ReportViewerWidget(
                layout.contentX(), layout.reportTop(),
                layout.contentWidth(), layout.reportBottom() - layout.reportTop(),
                textRenderer,
                currentView());
        viewer.setEmptyMessage(emptyMessage());
        if (state != null) {
            viewer.restoreState(state);
        }
        addDrawableChild(viewer);
    }

    private ReportView currentView() {
        if (loadFailure != null) {
            return new ReportView(List.of());
        }
        return ReportViewBuilder.build(model.report(), model.showLocations());
    }

    private String emptyMessage() {
        if (loadFailure != null) {
            return "PC data could not be read: " + loadFailure;
        }
        return "No Pokémon matched the selected categories.";
    }

    private void addActions() {
        String copyLabel = copyLabel();
        addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"), button -> refresh())
                .dimensions(layout.refreshX(), layout.buttonsY(), layout.refreshWidth(), 20).build());
        copyButton = addDrawableChild(ButtonWidget.builder(Text.literal(copyLabel), button -> copy())
                .dimensions(layout.copyX(), layout.buttonsY(), layout.copyWidth(), 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> close())
                .dimensions(layout.backX(), layout.buttonsY(), layout.backWidth(), 20).build());
    }

    private String copyLabel() {
        if (copyFeedbackTicks > 0) {
            return copyButton == null ? "Copied!" : copyButton.getMessage().getString();
        }
        return layout.copyWidth() < 100 ? "Copy" : "Copy to Clipboard";
    }

    private void onFilterChanged(ReportCategory category, boolean checked) {
        model.selectCategory(category, checked);
        readPc(false);
        rebuildWidgets();
        updateCopyState();
    }

    private void onLocationsChanged(boolean checked) {
        model.setShowLocations(checked);
        readPc(false);
        updateViewer();
        updateCopyState();
    }

    private void refresh() {
        readPc(true);
        updateViewer();
        if (viewer != null) {
            viewer.scrollToTop();
        }
    }

    private void updateViewer() {
        if (viewer == null) {
            return;
        }
        viewer.setView(currentView());
        viewer.setEmptyMessage(emptyMessage());
    }

    private void copy() {
        try {
            ClipboardResult result = model.copy(controller);
            if (result != null && result.success()) {
                setCopyFeedback("Copied!");
            } else {
                setCopyFeedback("Copy failed");
            }
        } catch (RuntimeException failure) {
            loadFailure = shortFailure(failure);
            setCopyFeedback("Copy failed");
            updateViewer();
        }
        updateCopyState();
    }

    private void setCopyFeedback(String label) {
        copyFeedbackTicks = 40;
        if (copyButton != null) {
            copyButton.setMessage(Text.literal(label));
        }
    }

    private void updateCopyState() {
        if (copyButton == null) {
            return;
        }
        InventoryCompleteness completeness = model.completeness();
        copyButton.active = model.report() != null
                && loadFailure == null
                && !model.selectedCategories().isEmpty()
                && completeness != InventoryCompleteness.UNAVAILABLE
                && completeness != InventoryCompleteness.AMBIGUOUS_MULTIPLE_STORES;
        if (copyFeedbackTicks == 0) {
            copyButton.setMessage(Text.literal(copyLabel()));
        }
    }

    private void rebuildWidgets() {
        ReportViewerWidget.State state = viewer == null ? null : viewer.state();
        clearChildren();
        init();
        if (viewer != null && state != null) {
            viewer.restoreState(state);
        }
    }

    private static String shortFailure(RuntimeException failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName()
                : failure.getClass().getSimpleName() + ": " + message;
    }

    @Override
    public void tick() {
        super.tick();
        if (copyFeedbackTicks > 0) {
            copyFeedbackTicks--;
            if (copyFeedbackTicks == 0) {
                updateCopyState();
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, layout.titleY(), TITLE);
        String version = CollectionExporterVersionLabel.text();
        int versionX = layout.panelX() + layout.panelWidth() - 10 - textRenderer.getWidth(version);
        int versionY = layout.panelY() + layout.panelHeight() - 13;
        context.drawTextWithShadow(textRenderer, version, versionX, versionY, VERSION_LABEL);
    }

    private void drawPanel(DrawContext context) {
        context.fill(layout.panelX(), layout.panelY(),
                layout.panelX() + layout.panelWidth(), layout.panelY() + layout.panelHeight(), PANEL);
        context.fill(layout.panelX(), layout.panelY(),
                layout.panelX() + layout.panelWidth(), layout.panelY() + 1, BORDER);
        context.fill(layout.panelX(), layout.panelY() + layout.panelHeight() - 1,
                layout.panelX() + layout.panelWidth(), layout.panelY() + layout.panelHeight(), BORDER);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private record FilterSpec(String label, ReportCategory category) {
    }
}
