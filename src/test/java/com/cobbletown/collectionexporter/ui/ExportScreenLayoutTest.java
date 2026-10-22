package com.cobbletown.collectionexporter.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportScreenLayoutTest {
    @Test
    void compactTwoRowLayoutUsesMostOfTheWindowForTheReport() {
        ExportScreenLayout layout = ExportScreenLayout.compute(427, 240, 2);
        assertTrue(layout.reportTop() > layout.filterTop() + layout.filterRowHeight());
        assertTrue(layout.reportBottom() > layout.reportTop());
        assertTrue(layout.reportBottom() - layout.reportTop() >= 120);
        assertTrue(layout.panelY() + layout.panelHeight() <= 240);
        assertTrue(layout.buttonsY() + 20 <= layout.panelY() + layout.panelHeight() - 12);
    }

    @Test
    void oneRowLayoutGivesTheReportMoreVerticalSpace() {
        ExportScreenLayout one = ExportScreenLayout.compute(854, 480, 1);
        ExportScreenLayout two = ExportScreenLayout.compute(854, 480, 2);
        assertEquals(two.reportTop() - one.reportTop(), one.filterRowHeight());
        assertTrue(one.reportBottom() > one.reportTop());
    }

    @Test
    void buttonsDoNotOverlapAndStayInsideThePanel() {
        ExportScreenLayout layout = ExportScreenLayout.compute(320, 240, 2);
        assertTrue(layout.refreshX() + layout.refreshWidth() < layout.copyX());
        assertTrue(layout.copyX() + layout.copyWidth() < layout.backX());
        assertTrue(layout.backX() + layout.backWidth() <= layout.contentX() + layout.contentWidth());
        assertTrue(layout.buttonsY() + 20 <= layout.panelY() + layout.panelHeight() - 12);
    }

    @Test
    void smallWindowStillLeavesAResponsiveReportViewport() {
        ExportScreenLayout layout = ExportScreenLayout.compute(320, 240, 2);
        assertTrue(layout.panelX() >= 0);
        assertTrue(layout.panelX() + layout.panelWidth() <= 320);
        assertTrue(layout.panelY() + layout.panelHeight() <= 240);
        assertTrue(layout.reportBottom() - layout.reportTop() >= 100);
    }
}
