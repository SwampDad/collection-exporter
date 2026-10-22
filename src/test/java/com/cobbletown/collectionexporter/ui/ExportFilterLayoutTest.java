package com.cobbletown.collectionexporter.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportFilterLayoutTest {
    private static final List<Integer> WIDTHS = List.of(32, 84, 110, 48, 116);

    @Test
    void wideWindowUsesOneRow() {
        ExportFilterLayout.Result result = ExportFilterLayout.layout(10, 20, 600, WIDTHS, 6, 12);
        assertEquals(1, result.rows());
        for (int index = 0; index < WIDTHS.size(); index++) {
            assertEquals(0, result.get(index).row());
        }
    }

    @Test
    void normalMinecraftWidthUsesTwoBalancedRows() {
        ExportFilterLayout.Result result = ExportFilterLayout.layout(10, 20, 383, WIDTHS, 6, 12);
        assertEquals(2, result.rows());
        assertEquals(0, result.get(0).row());
        assertEquals(0, result.get(1).row());
        assertEquals(0, result.get(3).row());
        assertEquals(1, result.get(2).row());
        assertEquals(1, result.get(4).row());
        assertTrue(result.get(4).x() + result.get(4).width() <= 10 + 383);
    }

    @Test
    void narrowWindowStillFitsTheFiveControlsInTwoRows() {
        ExportFilterLayout.Result result = ExportFilterLayout.layout(10, 20, 280, WIDTHS, 6, 12);
        assertEquals(2, result.rows());
        assertTrue(result.get(3).x() + result.get(3).width() <= 10 + 280);
        assertTrue(result.get(4).x() + result.get(4).width() <= 10 + 280);
    }

    @Test
    void extremelyNarrowWindowWrapsWithoutOverlap() {
        ExportFilterLayout.Result result = ExportFilterLayout.layout(10, 20, 180, WIDTHS, 6, 12);
        assertTrue(result.rows() >= 3);
        for (int index = 1; index < WIDTHS.size(); index++) {
            ExportFilterLayout.Placement previous = result.get(index - 1);
            ExportFilterLayout.Placement current = result.get(index);
            if (previous.row() == current.row()) {
                assertTrue(previous.x() + previous.width() <= current.x());
            }
        }
    }
}
