package com.cobbletown.collectionexporter.ui;

import java.util.List;

/** One collapsible report section and its native-viewer lines. */
public record ReportSection(
        String title,
        boolean collapsible,
        boolean defaultCollapsed,
        List<ReportViewLine> lines
) {
    public ReportSection {
        title = title == null || title.isBlank() ? "REPORT" : title;
        lines = lines == null ? List.of() : List.copyOf(lines);
    }
}
