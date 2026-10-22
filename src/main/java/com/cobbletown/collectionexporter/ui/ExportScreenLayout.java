package com.cobbletown.collectionexporter.ui;

/**
 * Pure layout for the Collection Exporter screen. The middle report viewer owns all remaining
 * vertical space between the compact filters and the footer buttons.
 */
public record ExportScreenLayout(
        int panelX,
        int panelY,
        int panelWidth,
        int panelHeight,
        int contentX,
        int contentWidth,
        int titleY,
        int filterTop,
        int filterRowHeight,
        int filterRows,
        int reportTop,
        int reportBottom,
        int buttonsY,
        int backX,
        int backWidth,
        int refreshX,
        int refreshWidth,
        int copyX,
        int copyWidth
) {
    public static ExportScreenLayout compute(int width, int height, int filterRows) {
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        int margin = Math.max(6, Math.min(14, safeWidth / 40));
        int panelWidth = Math.min(620, Math.max(1, safeWidth - margin * 2));
        int panelX = Math.max(0, (safeWidth - panelWidth) / 2);
        int contentX = panelX + 12;
        int contentWidth = Math.max(1, panelWidth - 24);

        int panelY = 4;
        int panelHeight = Math.max(1, safeHeight - panelY * 2);
        int titleY = panelY + 6;
        int filterTop = titleY + 13;
        int rowHeight = 12;
        int safeRows = Math.max(1, filterRows);
        int reportTop = filterTop + safeRows * rowHeight + 6;
        // Reserve a quiet bottom-right metadata line below the footer controls.
        int buttonsY = panelY + panelHeight - 20 - 20;
        int reportBottom = Math.max(reportTop + 1, buttonsY - 6);

        int gap = 6;
        int backWidth = Math.min(84, Math.max(54, contentWidth / 5));
        int refreshWidth = Math.min(92, Math.max(62, contentWidth / 5));
        int copyWidth = contentWidth - backWidth - refreshWidth - gap * 2;
        if (copyWidth < 84) {
            copyWidth = 84;
            int remaining = Math.max(54, contentWidth - copyWidth - gap * 2);
            backWidth = remaining / 2;
            refreshWidth = remaining - backWidth;
        }
        int refreshX = contentX;
        int copyX = refreshX + refreshWidth + gap;
        int backX = copyX + copyWidth + gap;

        return new ExportScreenLayout(
                panelX, panelY, panelWidth, panelHeight,
                contentX, contentWidth,
                titleY,
                filterTop, rowHeight, safeRows,
                reportTop, reportBottom,
                buttonsY,
                backX, backWidth,
                refreshX, refreshWidth,
                copyX, copyWidth);
    }
}
