package com.cobbletown.collectionexporter.report;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** UI-independent report options. Category selection is a logical OR. */
public record ReportRequest(
        Set<ReportCategory> categories,
        boolean includeCounts,
        boolean includeDiagnostics,
        boolean includeLocations
) {
    public ReportRequest {
        Objects.requireNonNull(categories, "categories");
        EnumSet<ReportCategory> copy = EnumSet.noneOf(ReportCategory.class);
        copy.addAll(categories);
        categories = java.util.Collections.unmodifiableSet(copy);
    }

    /** Backward-compatible constructor with PC locations disabled. */
    public ReportRequest(Set<ReportCategory> categories,
                         boolean includeCounts,
                         boolean includeDiagnostics) {
        this(categories, includeCounts, includeDiagnostics, false);
    }

    public static ReportRequest of(ReportCategory... categories) {
        EnumSet<ReportCategory> selected = EnumSet.noneOf(ReportCategory.class);
        for (ReportCategory category : categories) {
            selected.add(category);
        }
        return new ReportRequest(selected, true, false, false);
    }

    public boolean selects(ReportCategory category) {
        return categories.contains(category);
    }
}
