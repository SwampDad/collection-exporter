package com.cobbletown.collectionexporter.ui;

import com.cobbletown.collectionexporter.TestFixtures;
import com.cobbletown.collectionexporter.classify.ClassificationCatalogLoader;
import com.cobbletown.collectionexporter.data.InventoryCompleteness;
import com.cobbletown.collectionexporter.data.PcInventorySnapshot;
import com.cobbletown.collectionexporter.data.PokemonRecord;
import com.cobbletown.collectionexporter.report.ReportCategory;
import com.cobbletown.collectionexporter.report.ReportEngine;
import com.cobbletown.collectionexporter.report.ReportRequest;
import com.cobbletown.collectionexporter.report.ReportResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportViewBuilderTest {
    private final ReportEngine engine = new ReportEngine(ClassificationCatalogLoader.loadDefault());

    @Test
    void buildsTheCategorizedViewerFromTheStructuredReport() {
        ReportResult report = engine.build(TestFixtures.locationInventory(), defaults());
        ReportView view = ReportViewBuilder.build(report, false);

        assertEquals(List.of("CUSTOM SKINS", "SUMMARY"), view.sections().stream()
                .map(ReportSection::title).toList());
        ReportSection skins = find(view, "CUSTOM SKINS");
        assertFalse(skins.defaultCollapsed());
        assertTrue(lines(skins).contains("Adventure"));
        assertTrue(lines(skins).contains("Tinkaton ×2"));
        assertTrue(lines(skins).contains("Zoroark"));
        assertTrue(view.sections().stream().anyMatch(section ->
                section.title().equals("SUMMARY") && !section.defaultCollapsed()));
    }

    @Test
    void otherCategoryStartsCollapsedWhileInterestingCategoriesStartExpanded() {
        ReportResult report = engine.build(TestFixtures.exampleInventory(),
                ReportRequest.of(ReportCategory.ALL));
        ReportView view = ReportViewBuilder.build(report, false);
        ReportSection other = find(view, "OTHER");
        assertTrue(other.defaultCollapsed());
        assertFalse(find(view, "SHINY").defaultCollapsed());
        assertFalse(find(view, "UNRESOLVED").defaultCollapsed());
    }

    @Test
    void partialSnapshotShowsAWarningSection() {
        ReportResult report = engine.build(TestFixtures.partialInventory(),
                ReportRequest.of(ReportCategory.ALL));
        ReportSection warning = find(ReportViewBuilder.build(report, false), "PC WARNING");
        assertFalse(warning.defaultCollapsed());
        assertTrue(lines(warning).contains("This is not a complete PC snapshot."));
    }

    @Test
    void shinySkinEntriesUseAStarAndComeBeforeNonShinyEntries() {
        ReportResult report = engine.build(TestFixtures.exampleInventory(),
                ReportRequest.of(ReportCategory.ALL));
        List<String> lines = lines(find(ReportViewBuilder.build(report, false), "CUSTOM SKINS"));
        int petal = lines.indexOf("Petal");
        int shinyArmaldo = lines.indexOf("★ Armaldo");
        int shinyMachamp = lines.indexOf("★ Machamp");
        int nonShinyMachamp = lines.indexOf("Machamp");
        assertTrue(petal >= 0);
        assertTrue(shinyArmaldo > petal);
        assertTrue(shinyMachamp > shinyArmaldo);
        assertTrue(nonShinyMachamp > shinyMachamp);
    }

    @Test
    void skinFamilyHeadingIsRedToneAndShinyDoesNotOverrideNameTone() {
        ReportResult report = engine.build(TestFixtures.exampleInventory(),
                ReportRequest.of(ReportCategory.ALL));
        ReportSection skins = find(ReportViewBuilder.build(report, false), "CUSTOM SKINS");
        ReportViewLine petal = skins.lines().stream()
                .filter(line -> line.text().equals("Petal"))
                .findFirst().orElseThrow();
        ReportViewLine shinyArmaldo = skins.lines().stream()
                .filter(line -> line.text().equals("★ Armaldo"))
                .findFirst().orElseThrow();
        assertEquals(ReportViewLine.Tone.SKIN_HEADER, petal.tone());
        assertEquals(ReportViewLine.Tone.TEXT, shinyArmaldo.tone());
    }

    @Test
    void standaloneShinySectionAlsoUsesTheStar() {
        ReportResult report = engine.build(TestFixtures.exampleInventory(),
                ReportRequest.of(ReportCategory.SHINY));
        List<String> lines = lines(find(ReportViewBuilder.build(report, false), "SHINY"));
        assertTrue(lines.stream().anyMatch(line -> line.equals("★ Pikachu")));
    }

    @Test
    void locationsAreHumanReadableAndDuplicatesAreIndividual() {
        ReportResult report = engine.build(TestFixtures.locationInventory(), defaults());
        ReportView view = ReportViewBuilder.build(report, true);
        List<String> lines = lines(find(view, "CUSTOM SKINS"));

        assertTrue(lines.contains("Zoroark — Box 3, Pos. 13"));
        assertTrue(lines.contains("Tinkaton"));
        assertTrue(lines.contains("Box 3, Pos. 23"));
        assertTrue(lines.contains("Box 7, Pos. 22"));
        assertFalse(lines.contains("Tinkaton ×2"));
        assertFalse(lines.contains("Zoroark — Box 3, Pos. 13 — "));
    }

    @Test
    void unavailableLocationIsExplicitAndNotInvented() {
        ReportResult report = engine.build(TestFixtures.missingLocationInventory(), defaults());
        ReportView view = ReportViewBuilder.build(report, true);
        List<String> lines = lines(find(view, "CUSTOM SKINS"));
        assertTrue(lines.contains("Tinkaton — Location unavailable"));
    }

    @Test
    void locationToggleDoesNotChangeTheReportModel() {
        ReportResult report = engine.build(TestFixtures.locationInventory(), defaults());
        ReportView without = ReportViewBuilder.build(report, false);
        ReportView with = ReportViewBuilder.build(report, true);
        assertEquals(without.sections().size(), with.sections().size());
        assertTrue(lines(find(with, "CUSTOM SKINS")).stream().anyMatch(line -> line.contains("Box 3")));
        assertFalse(lines(find(without, "CUSTOM SKINS")).stream().anyMatch(line -> line.contains("Box")));
    }

    @Test
    void buildsAViewerForALargeSyntheticCollectionWithoutPrivateSnapshotData() {
        List<PokemonRecord> entries = new ArrayList<>();
        for (int index = 0; index < 619; index++) {
            entries.add(new PokemonRecord(new UUID(0L, index + 1L), index / 30, index % 30,
                    "test:species" + index, "Species " + index, 0,
                    "normal", "Normal", List.of("male"), false, 50, "male", null));
        }
        PcInventorySnapshot inventory = new PcInventorySnapshot("2026-10-22T00:00:00Z",
                "synthetic-test", null, InventoryCompleteness.COMPLETE_SYNCHRONIZED_STORE,
                21, 21, entries, List.of());

        ReportResult report = engine.build(inventory, ReportRequest.of(ReportCategory.ALL));
        ReportView view = ReportViewBuilder.build(report, true);
        ReportSection other = find(view, "OTHER");
        assertEquals(619, report.summary().includedInstances());
        assertTrue(other.lines().size() >= 619);
    }

    private ReportRequest defaults() {
        return ReportRequest.of(ReportCategory.SKINS, ReportCategory.LEGEND_MYTH, ReportCategory.SHINY);
    }

    private ReportSection find(ReportView view, String title) {
        return view.sections().stream()
                .filter(section -> section.title().equals(title))
                .findFirst()
                .orElseThrow();
    }

    private List<String> lines(ReportSection section) {
        return section.lines().stream().map(ReportViewLine::text).toList();
    }
}
