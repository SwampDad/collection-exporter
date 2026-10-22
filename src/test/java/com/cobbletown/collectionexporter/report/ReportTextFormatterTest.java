package com.cobbletown.collectionexporter.report;

import com.cobbletown.collectionexporter.TestFixtures;
import com.cobbletown.collectionexporter.classify.ClassificationCatalog;
import com.cobbletown.collectionexporter.classify.ClassificationCatalogLoader;
import com.cobbletown.collectionexporter.data.PcInventorySnapshot;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportTextFormatterTest {
    private final ClassificationCatalog catalog = ClassificationCatalogLoader.loadDefault();
    private final ReportEngine engine = new ReportEngine(catalog);
    private final ReportTextFormatter formatter =
            new ReportTextFormatter(ZoneId.of("America/New_York"));

    @Test
    void formatsShareableHeaderWithLocalCaptureTime() {
        ReportResult result = engine.build(
                TestFixtures.unresolvedShinyInventory(),
                ReportRequest.of(ReportCategory.SHINY));
        String text = formatter.format(result, ReportRequest.of(ReportCategory.SHINY));

        assertTrue(text.startsWith("━".repeat(44) + "\n"));
        assertTrue(text.contains("COBBLEMON TOWN"));
        assertTrue(text.contains("Pokémon Collection"));
        assertTrue(text.contains("October 21, 2026 · 1:20 AM EDT"));
        assertTrue(text.contains("PC Snapshot: Complete"));
        assertTrue(text.lines().filter(line -> line.startsWith("━")).allMatch(line -> line.length() == 44));
        assertFalse(text.contains("Completeness:"));
        assertFalse(text.contains("Captured: 2026-"));
        assertFalse(text.contains("Scope:"));
        assertFalse(text.contains("client-synchronized"));
    }

    @Test
    void formatsCustomSkinsWithClassificationLabelsAndNoShinySuffix() {
        String text = formatter.format(
                engine.build(TestFixtures.exampleInventory(), ReportRequest.of(ReportCategory.ALL)),
                ReportRequest.of(ReportCategory.ALL));

        assertTrue(text.contains("CUSTOM SKINS\n" + "─".repeat(44)));
        assertTrue(text.contains("Neon\n    Shiny Dragapult\n    Shiny Tyrantrum\n"
                + "    Entei (Legendary)\n    Mewtwo (Legendary)\n    Pelipper\n    Tropius"));
        assertTrue(text.contains("Western\n    Shiny Jynx\n    Jynx\n    Machamp\n"
                + "    Venusaur ×2"));
        assertFalse(text.contains("    Shiny:"));
        assertFalse(text.contains("    Non-Shiny:"));
        assertTrue(text.contains("SHINY\n" + "─".repeat(44) + "\n  Pikachu\n"));
        assertFalse(text.contains("Pikachu (Shiny)"));
        assertFalse(text.contains("Dragapult (Shiny)"));
        assertFalse(text.contains("Tron\n"));
    }

    @Test
    void splitsMythicalBeforeLegendaryAndSortsAlphabetically() {
        String text = formatter.format(
                engine.build(TestFixtures.legendaryAndMythicalInventory(),
                        ReportRequest.of(ReportCategory.LEGEND_MYTH)),
                ReportRequest.of(ReportCategory.LEGEND_MYTH));

        int mythical = text.indexOf("LEGENDARY & MYTHICAL\n" + "─".repeat(44) + "\nMythical\n");
        int legendary = text.indexOf("Legendary\n  Articuno\n  Azelf");
        assertTrue(mythical >= 0);
        assertTrue(legendary > mythical);
        assertTrue(text.contains("Mythical\n  Manaphy\n  Mew\nLegendary\n  Articuno\n  Azelf"));
    }

    @Test
    void unresolvedSectionNamesPokemonAndShowsRawAspect() {
        String text = formatter.format(
                engine.build(TestFixtures.unresolvedShinyInventory(),
                        ReportRequest.of(ReportCategory.SHINY)),
                ReportRequest.of(ReportCategory.SHINY));

        assertTrue(text.contains("UNRESOLVED\n" + "─".repeat(44)));
        assertTrue(text.contains("  Gyarados\n    Reason: Unrecognized aspect\n"
                + "    Aspect: mystery-token, second-mystery"));
    }

    @Test
    void detailedReportPreservesBothReasonsForTheSamePokemon() {
        String text = formatter.format(
                engine.build(TestFixtures.unresolvedMixedTokenInventory(),
                        ReportRequest.of(ReportCategory.ALL)),
                ReportRequest.of(ReportCategory.ALL));

        assertTrue(text.contains("  Eevee\n    Reason: Potential new skin (year token)\n"
                + "    Aspect: future2027skin"));
        assertTrue(text.contains("  Eevee\n    Reason: Unrecognized aspect\n"
                + "    Aspect: mystery-token"));
    }

    @Test
    void summaryUsesPlayerTerminologyAndOmitsZeroDiagnostics() {
        String text = formatter.format(
                engine.build(TestFixtures.exampleInventory(), ReportRequest.of(ReportCategory.ALL)),
                ReportRequest.of(ReportCategory.ALL));

        assertTrue(text.contains("SUMMARY\n" + "─".repeat(44)));
        assertTrue(text.contains("Total Pokémon: 37"));
        assertTrue(text.contains("Custom Skins: 30"));
        assertTrue(text.contains("  Shiny: 12"));
        assertTrue(text.contains("  Non-Shiny: 18"));
        assertTrue(text.contains("  Unique Species/Forms: 23"));
        assertTrue(text.contains("Legendary: 2"));
        assertTrue(text.contains("Mythical: 1"));
        assertTrue(text.contains("Shiny Pokémon: 14"));
        assertTrue(text.contains("Unresolved: 2"));
        assertTrue(text.contains("Aspect: totally_unknown_secondary"));
        assertTrue(text.contains("Aspect: totally_unknown_token"));
        assertTrue(text.contains("Other Pokémon: 5"));
        assertFalse(text.contains("Paradox: 0"));
        assertFalse(text.contains("Ultra Beasts: 0"));
        assertFalse(text.contains("Included instances"));
        assertFalse(text.contains("instances"));
    }

    @Test
    void unresolvedSectionAppearsEvenWhenNoPokemonMatchedFilters() {
        ReportRequest request = ReportRequest.of(
                ReportCategory.SKINS, ReportCategory.LEGEND_MYTH, ReportCategory.SHINY);
        String text = formatter.format(
                engine.build(TestFixtures.unselectedUnresolvedInventory(), request), request);
        assertTrue(text.contains("No Pokémon matched the selected categories."));
        assertTrue(text.contains("UNRESOLVED\n" + "─".repeat(44)));
        assertTrue(text.contains("  Eevee\n    Reason: Potential new skin (year token)\n    Aspect: future2027skin"));
        assertTrue(text.contains("Unresolved: 1"));
    }

    @Test
    void locationsAreIncludedInClipboardOutputWhenEnabled() {
        ReportRequest request = new ReportRequest(
                Set.of(ReportCategory.SKINS, ReportCategory.LEGEND_MYTH, ReportCategory.SHINY),
                true, false, true);
        String text = formatter.format(
                engine.build(TestFixtures.locationInventory(), request), request);

        assertTrue(text.contains("  Spooky\n    Tinkaton\n"
                + "      Box 3, Pos. 23\n      Box 7, Pos. 22"));
        assertTrue(text.contains("  Adventure\n    Zoroark — Box 3, Pos. 13"));
        assertTrue(text.contains("Zoroark — Box 3, Pos. 13"));
        assertFalse(text.contains("Tinkaton ×2"));
    }

    @Test
    void locationsAreAbsentFromClipboardOutputWhenDisabled() {
        ReportRequest request = new ReportRequest(
                Set.of(ReportCategory.SKINS, ReportCategory.LEGEND_MYTH, ReportCategory.SHINY),
                true, false, false);
        String text = formatter.format(
                engine.build(TestFixtures.locationInventory(), request), request);
        assertTrue(text.contains("Tinkaton ×2"));
        assertTrue(text.contains("Zoroark"));
        assertFalse(text.contains("Box "));
    }

    @Test
    void incompleteInventoryIsClearlyWarned() {
        String text = formatter.format(
                engine.build(TestFixtures.partialInventory(), ReportRequest.of(ReportCategory.ALL)),
                ReportRequest.of(ReportCategory.ALL));
        assertTrue(text.contains("PC Snapshot: Partial"));
        assertTrue(text.contains("WARNING: This is not a complete inventory."));
        assertFalse(text.contains("client-synchronized"));
    }

    @Test
    void unavailableSnapshotIsClearlyNotComplete() {
        String text = formatter.format(
                engine.build(PcInventorySnapshot.unavailable("2026-10-20T00:00:00Z", "no store"),
                        ReportRequest.of(ReportCategory.ALL)),
                ReportRequest.of(ReportCategory.ALL));
        assertTrue(text.contains("PC Snapshot: Unavailable"));
        assertTrue(text.contains("WARNING: This is not a complete inventory."));
    }

    @Test
    void emptyInventoryIsExplicit() {
        String text = formatter.format(
                engine.build(TestFixtures.emptyInventory(), ReportRequest.of(ReportCategory.ALL)),
                ReportRequest.of(ReportCategory.ALL));
        assertTrue(text.contains("No Pokémon matched the selected categories."));
        assertTrue(text.contains("Total Pokémon: 0"));
    }

    @Test
    void diagnosticsAreOptional() {
        ReportRequest quiet = new ReportRequest(Set.of(ReportCategory.ALL), true, false);
        ReportRequest verbose = new ReportRequest(Set.of(ReportCategory.ALL), true, true);
        String quietText = formatter.format(engine.build(TestFixtures.exampleInventory(), quiet), quiet);
        String verboseText = formatter.format(engine.build(TestFixtures.exampleInventory(), verbose), verbose);
        assertFalse(quietText.contains("DIAGNOSTICS"));
        assertTrue(verboseText.contains("DIAGNOSTICS"));
    }
}
