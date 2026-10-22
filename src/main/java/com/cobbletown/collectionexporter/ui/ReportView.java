package com.cobbletown.collectionexporter.ui;

import java.util.List;

/** UI-independent structured report content for the in-game viewer. */
public record ReportView(List<ReportSection> sections) {
    public ReportView {
        sections = sections == null ? List.of() : List.copyOf(sections);
    }

    public boolean isEmpty() {
        return sections.isEmpty();
    }
}
