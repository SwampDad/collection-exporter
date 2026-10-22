package com.cobbletown.collectionexporter.ui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.client.sound.PositionedSoundInstance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Native, scrollable report viewer. It renders the structured report model and owns only
 * presentation state (scroll, collapsed sections, hover). It never reads the PC or formats
 * clipboard text.
 */
public final class ReportViewerWidget extends ClickableWidget {
    private static final int HEADER_HEIGHT = 15;
    private static final int LINE_HEIGHT = 11;
    private static final int INDENT_WIDTH = 8;
    private static final int BACKGROUND = 0xE0101418;
    private static final int BORDER = 0x504A5562;
    private static final int HEADER_BACKGROUND = 0x50242A32;
    private static final int HEADER_HOVER = 0x70404A56;
    private static final int TEXT = 0xFFE6EDF5;
    private static final int MUTED = 0xFF9CA9B8;
    private static final int SHINY_STAR = 0xFFFFD54A;
    private static final int SKIN_HEADER = 0xFFFF5555;
    private static final int LEGENDARY = 0xFFFFD166;
    private static final int MYTHICAL = 0xFFE59BFF;

    private static final int WARNING = 0xFFFFB86B;
    private static final int SCROLL_TRACK = 0x60000000;
    private static final int SCROLL_THUMB = 0xFF7A8490;
    private static final int SCROLL_THUMB_HOVER = 0xFFAAB6C4;

    private final TextRenderer textRenderer;
    private final Map<String, Boolean> collapsed = new LinkedHashMap<>();
    private ReportView view = new ReportView(List.of());
    private List<Row> rows = List.of();
    private int contentHeight;
    private double scroll;
    private boolean draggingScrollbar;
    private int dragOffset;
    private String emptyMessage = "No Pokémon matched the selected categories.";

    public ReportViewerWidget(int x, int y, int width, int height,
                              TextRenderer textRenderer, ReportView view) {
        super(x, y, width, height, Text.literal("Collection report"));
        this.textRenderer = textRenderer;
        setView(view);
    }

    public void setView(ReportView view) {
        this.view = view == null ? new ReportView(List.of()) : view;
        for (ReportSection section : this.view.sections()) {
            collapsed.putIfAbsent(section.title(), section.defaultCollapsed());
        }
        rebuildRows(this.view);
    }

    public void setEmptyMessage(String emptyMessage) {
        this.emptyMessage = emptyMessage == null || emptyMessage.isBlank()
                ? "No Pokémon matched the selected categories."
                : emptyMessage;
    }

    public State state() {
        return new State(scroll, new LinkedHashMap<>(collapsed));
    }

    public void restoreState(State state) {
        if (state == null) {
            return;
        }
        collapsed.clear();
        collapsed.putAll(state.collapsed());
        rebuildRows(view);
        scroll = state.scroll();
        clampScroll();
    }

    public void scrollToTop() {
        scroll = 0;
    }

    private void rebuildRows(ReportView view) {
        List<Row> next = new ArrayList<>();
        for (ReportSection section : view.sections()) {
            next.add(new Row(RowType.HEADER, section.title(), 0, ReportViewLine.Tone.TEXT,
                    HEADER_HEIGHT, section.title(), section.collapsible()));
            if (!collapsed.getOrDefault(section.title(), section.defaultCollapsed())) {
                for (ReportViewLine line : section.lines()) {
                    next.add(new Row(RowType.LINE, line.text(), line.indent(), line.tone(),
                            LINE_HEIGHT, section.title(), false));
                }
            }
        }
        rows = List.copyOf(next);
        contentHeight = rows.stream().mapToInt(Row::height).sum();
        clampScroll();
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(getX(), getY(), getX() + width, getY() + height, BACKGROUND);
        context.fill(getX(), getY(), getX() + width, getY() + 1, BORDER);
        context.fill(getX(), getY() + height - 1, getX() + width, getY() + height, BORDER);

        int contentTop = getY() + 3;
        int contentBottom = getY() + height - 3;
        int viewport = Math.max(1, contentBottom - contentTop);
        int textLeft = getX() + 8;
        int textRight = getX() + width - 12;

        context.enableScissor(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1);
        if (rows.isEmpty()) {
            context.drawTextWithShadow(textRenderer, ellipsize(emptyMessage, textRight - textLeft),
                    textLeft, contentTop + 4, MUTED);
        } else {
            int rowY = contentTop - (int) scroll;
            for (Row row : rows) {
                int bottom = rowY + row.height();
                if (bottom >= contentTop && rowY <= contentBottom) {
                    drawRow(context, row, textLeft, textRight, rowY, mouseX, mouseY);
                }
                rowY += row.height();
            }
        }
        context.disableScissor();
        drawScrollbar(context, viewport, contentTop, mouseX, mouseY);
    }

    private void drawRow(DrawContext context, Row row, int textLeft, int textRight,
                         int rowY, int mouseX, int mouseY) {
        if (row.type() == RowType.HEADER) {
            boolean hover = mouseX >= getX() && mouseX < getX() + width
                    && mouseY >= rowY && mouseY < rowY + row.height();
            context.fill(getX() + 1, rowY, getX() + width - 1, rowY + row.height(),
                    hover ? HEADER_HOVER : HEADER_BACKGROUND);
            int titleX = textLeft;
            if (row.collapsible()) {
                String arrow = collapsed.getOrDefault(row.sectionTitle(), false) ? "▸" : "▾";
                context.drawTextWithShadow(textRenderer, arrow, textLeft, rowY + 3, LEGENDARY);
                titleX += 9;
            }
            context.drawTextWithShadow(textRenderer, row.text(), titleX, rowY + 3, LEGENDARY);
            context.fill(getX() + 4, rowY + row.height() - 1, getX() + width - 4,
                    rowY + row.height(), 0x30FFFFFF);
            return;
        }
        int x = textLeft + row.indent() * INDENT_WIDTH;
        int color = color(row.tone());
        String rendered;
        if (row.text().startsWith("★ ")) {
            context.drawTextWithShadow(textRenderer, "★", x, rowY + 1, SHINY_STAR);
            int starWidth = textRenderer.getWidth("★ ");
            rendered = ellipsize(row.text().substring(2), textRight - x - starWidth);
            context.drawTextWithShadow(textRenderer, rendered,
                    x + starWidth, rowY + 1, color);
        } else {
            rendered = ellipsize(row.text(), textRight - x);
            context.drawTextWithShadow(textRenderer, rendered, x, rowY + 1, color);
        }
        if (row.tone() == ReportViewLine.Tone.SKIN_HEADER) {
            context.fill(x, rowY + 10, x + textRenderer.getWidth(rendered), rowY + 11, SKIN_HEADER);
        }
    }

    private int color(ReportViewLine.Tone tone) {
        return switch (tone) {
            case MUTED -> MUTED;
            case LEGENDARY -> LEGENDARY;
            case MYTHICAL -> MYTHICAL;
            case SKIN_HEADER -> SKIN_HEADER;
            case WARNING -> WARNING;
            case TEXT -> TEXT;
        };
    }

    private String ellipsize(String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        return textRenderer.trimToWidth(text, Math.max(0, maxWidth - textRenderer.getWidth(ellipsis)))
                + ellipsis;
    }

    private void drawScrollbar(DrawContext context, int viewport, int contentTop,
                               int mouseX, int mouseY) {
        if (contentHeight <= viewport) {
            return;
        }
        int trackX = getX() + width - 6;
        int trackY = contentTop;
        int trackHeight = viewport;
        int thumbHeight = Math.max(14,
                (int) ((long) trackHeight * viewport / Math.max(1, contentHeight)));
        int travel = Math.max(0, trackHeight - thumbHeight);
        int maxScroll = Math.max(1, contentHeight - viewport);
        int thumbY = trackY + (int) Math.round((double) scroll / maxScroll * travel);
        context.fill(trackX, trackY, trackX + 4, trackY + trackHeight, SCROLL_TRACK);
        boolean hover = mouseX >= trackX && mouseX < getX() + width
                && mouseY >= thumbY && mouseY < thumbY + thumbHeight;
        context.fill(trackX, thumbY, trackX + 4, thumbY + thumbHeight,
                hover || draggingScrollbar ? SCROLL_THUMB_HOVER : SCROLL_THUMB);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        Row row = rowAt(mouseY);
        if (row != null && row.type() == RowType.HEADER && row.collapsible()) {
            playDownSound(MinecraftClient.getInstance().getSoundManager());
            toggleSection(row);
        }
    }

    private void toggleSection(Row row) {
        int headerTop = rowTop(row);
        int headerScreenY = headerTop - (int) scroll;
        boolean nowCollapsed = !collapsed.getOrDefault(row.sectionTitle(), false);
        collapsed.put(row.sectionTitle(), nowCollapsed);
        rebuildRows(view);
        int offset = 0;
        int newTop = getY() + 3;
        for (Row candidate : rows) {
            if (candidate.type() == RowType.HEADER
                    && candidate.sectionTitle().equals(row.sectionTitle())) {
                newTop = getY() + 3 + offset;
                break;
            }
            offset += candidate.height();
        }
        scroll = newTop - headerScreenY;
        clampScroll();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible || button != 0 || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (scrollbarContains(mouseX, mouseY)) {
            draggingScrollbar = true;
            scrollFromThumb(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double deltaX, double deltaY) {
        if (draggingScrollbar) {
            scrollFromThumb(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                 double horizontalAmount, double verticalAmount) {
        if (!visible || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        scroll -= verticalAmount * 24;
        clampScroll();
        return true;
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        soundManager.play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }

    private boolean scrollbarContains(double mouseX, double mouseY) {
        if (contentHeight <= height - 6) {
            return false;
        }
        return mouseX >= getX() + width - 7 && mouseX < getX() + width
                && mouseY >= getY() + 3 && mouseY < getY() + height - 3;
    }

    private void scrollFromThumb(double mouseY) {
        int viewport = Math.max(1, height - 6);
        int trackY = getY() + 3;
        int thumbHeight = Math.max(14,
                (int) ((long) viewport * viewport / Math.max(1, contentHeight)));
        int travel = Math.max(1, viewport - thumbHeight);
        int maxScroll = Math.max(1, contentHeight - viewport);
        double ratio = (mouseY - trackY - thumbHeight / 2.0) / travel;
        scroll = ratio * maxScroll;
        clampScroll();
    }

    private void clampScroll() {
        int viewport = Math.max(1, height - 6);
        double max = Math.max(0, contentHeight - viewport);
        scroll = Math.max(0, Math.min(scroll, max));
    }

    private Row rowAt(double mouseY) {
        int rowY = getY() + 3 - (int) scroll;
        for (Row row : rows) {
            if (mouseY >= rowY && mouseY < rowY + row.height()) {
                return row;
            }
            rowY += row.height();
        }
        return null;
    }

    private int rowTop(Row target) {
        int top = getY() + 3;
        for (Row row : rows) {
            if (row == target) {
                return top;
            }
            top += row.height();
        }
        return top;
    }

    public record State(double scroll, Map<String, Boolean> collapsed) {
        public State {
            collapsed = collapsed == null ? Map.of() : Map.copyOf(collapsed);
        }
    }

    private enum RowType {
        HEADER,
        LINE
    }

    private record Row(RowType type, String text, int indent, ReportViewLine.Tone tone,
                       int height, String sectionTitle, boolean collapsible) {
    }
}
