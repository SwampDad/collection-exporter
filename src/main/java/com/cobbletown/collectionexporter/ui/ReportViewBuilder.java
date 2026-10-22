package com.cobbletown.collectionexporter.ui;

import com.cobbletown.collectionexporter.classify.PokemonCategory;
import com.cobbletown.collectionexporter.data.PokemonRecord;
import com.cobbletown.collectionexporter.report.PcLocation;
import com.cobbletown.collectionexporter.report.ReportEntry;
import com.cobbletown.collectionexporter.report.ReportIssue;
import com.cobbletown.collectionexporter.report.ReportResult;
import com.cobbletown.collectionexporter.report.ReportSummary;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the native viewer from the same structured result used by the clipboard formatter.
 * It never parses formatted report text and never reclassifies Pokémon.
 */
public final class ReportViewBuilder {
    private ReportViewBuilder() {
    }

    public static ReportView build(ReportResult report, boolean includeLocations) {
        List<ReportSection> sections = new ArrayList<>();
        if (report == null) {
            return new ReportView(List.of());
        }
        if (!report.completeness().isComplete()) {
            sections.add(new ReportSection("PC WARNING", false, false, List.of(
                    new ReportViewLine("This is not a complete PC snapshot.",
                            1, ReportViewLine.Tone.WARNING))));
        }

        List<ReportViewLine> skins = skins(report, includeLocations);
        add(sections, "CUSTOM SKINS", true, false, skins);

        List<ReportViewLine> legend = legendaryAndMythical(report, includeLocations);
        add(sections, "LEGENDARY & MYTHICAL", true, false, legend);

        List<ReportViewLine> shiny = new ArrayList<>();
        addEntries(shiny, report.shiny(), 1, includeLocations, false, false, true);
        add(sections, "SHINY", true, false, shiny);

        List<ReportViewLine> other = new ArrayList<>();
        addEntries(other, report.other(), 1, includeLocations, false, false, false);
        add(sections, "OTHER", true, true, other);

        add(sections, "UNRESOLVED", true, false, issues(report.unresolved()));
        add(sections, "CONFLICTS", true, false, issues(report.conflicts()));

        List<ReportViewLine> summary = summary(report.summary());
        add(sections, "SUMMARY", true, false, summary);

        return new ReportView(sections);
    }

    private static void add(List<ReportSection> sections,
                            String title,
                            boolean collapsible,
                            boolean defaultCollapsed,
                            List<ReportViewLine> lines) {
        if (!lines.isEmpty()) {
            sections.add(new ReportSection(title, collapsible, defaultCollapsed, lines));
        }
    }

    private static List<ReportViewLine> skins(ReportResult report, boolean includeLocations) {
        List<ReportViewLine> lines = new ArrayList<>();
        for (var group : report.skinGroups()) {
            lines.add(new ReportViewLine(group.family().displayName(), 1,
                    ReportViewLine.Tone.SKIN_HEADER));
            addEntries(lines, group.shiny(), 2, includeLocations, true, false, true);
            addEntries(lines, group.nonShiny(), 2, includeLocations, true, false, false);
        }
        return lines;
    }

    private static List<ReportViewLine> legendaryAndMythical(ReportResult report,
                                                             boolean includeLocations) {
        List<ReportViewLine> lines = new ArrayList<>();
        List<ReportEntry> mythical = report.mythicalEntries();
        List<ReportEntry> legendary = report.legendaryEntries();
        if (!mythical.isEmpty()) {
            lines.add(new ReportViewLine("Mythical", 1, ReportViewLine.Tone.MYTHICAL));
            addEntries(lines, mythical, 2, includeLocations, false, false, true);
        }
        if (!legendary.isEmpty()) {
            lines.add(new ReportViewLine("Legendary", 1, ReportViewLine.Tone.LEGENDARY));
            addEntries(lines, legendary, 2, includeLocations, false, false, true);
        }
        return lines;
    }

    private static void addEntries(List<ReportViewLine> lines,
                                   List<ReportEntry> entries,
                                   int indent,
                                   boolean includeLocations,
                                   boolean includeLegendSuffix,
                                   boolean includeShinySuffix,
                                   boolean starShiny) {
        for (ReportEntry entry : entries) {
            String label = entry.displayName();
            if (includeLegendSuffix) {
                if (entry.categories().contains(PokemonCategory.LEGENDARY)) {
                    label += " (Legendary)";
                } else if (entry.categories().contains(PokemonCategory.MYTHICAL)) {
                    label += " (Mythical)";
                }
            }
            if (includeShinySuffix && entry.shiny()) {
                label += " (Shiny)";
            }
            if (starShiny && entry.shiny()) {
                label = "★ " + label;
            }
            ReportViewLine.Tone tone = tone(entry);
            if (!includeLocations) {
                lines.add(new ReportViewLine(
                        entry.count() > 1 ? label + " ×" + entry.count() : label,
                        indent,
                        tone));
                continue;
            }

            if (entry.count() == 1) {
                lines.add(new ReportViewLine(
                        label + " — " + PcLocation.format(entry.instances().get(0)),
                        indent,
                        tone));
            } else {
                lines.add(new ReportViewLine(label, indent, tone));
                for (PokemonRecord pokemon : entry.instances()) {
                    lines.add(new ReportViewLine(PcLocation.format(pokemon),
                            indent + 1, ReportViewLine.Tone.MUTED));
                }
            }
        }
    }

    private static ReportViewLine.Tone tone(ReportEntry entry) {
        if (entry.categories().contains(PokemonCategory.LEGENDARY)) {
            return ReportViewLine.Tone.LEGENDARY;
        }
        if (entry.categories().contains(PokemonCategory.MYTHICAL)) {
            return ReportViewLine.Tone.MYTHICAL;
        }
        return ReportViewLine.Tone.TEXT;
    }

    private static List<ReportViewLine> issues(List<ReportIssue> issues) {
        List<ReportViewLine> lines = new ArrayList<>();
        for (ReportIssue issue : issues) {
            lines.add(new ReportViewLine(issue.quantityLabel(), 1, ReportViewLine.Tone.WARNING));
            lines.add(new ReportViewLine("Reason: " + issue.reason(), 2, ReportViewLine.Tone.MUTED));
            if (!issue.aspects().isEmpty()) {
                lines.add(new ReportViewLine("Aspect: " + String.join(", ", issue.aspects()),
                        2, ReportViewLine.Tone.MUTED));
            }
            if (issue.customSpecies()) {
                lines.add(new ReportViewLine("Species: custom/unknown",
                        2, ReportViewLine.Tone.MUTED));
            }
        }
        return lines;
    }

    private static List<ReportViewLine> summary(ReportSummary summary) {
        List<ReportViewLine> lines = new ArrayList<>();
        addCount(lines, "Total Pokémon", summary.includedInstances());
        addCount(lines, "Custom Skins", summary.skinInstances());
        addCount(lines, "Legendary", summary.legendaryInstances());
        addCount(lines, "Mythical", summary.mythicalInstances());
        addCount(lines, "Shiny", summary.shinyInstances());
        addCount(lines, "Other Pokémon", summary.otherInstances());
        addCount(lines, "Unresolved", summary.unresolvedInstances());
        addCount(lines, "Conflicts", summary.conflictInstances());
        return lines;
    }

    private static void addCount(List<ReportViewLine> lines, String label, int value) {
        if (value > 0 || "Total Pokémon".equals(label)) {
            lines.add(new ReportViewLine(label + ": " + value, 1, ReportViewLine.Tone.MUTED));
        }
    }
}
