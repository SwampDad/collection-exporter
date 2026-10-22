package com.cobbletown.collectionexporter.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure layout for the compact filter row. It prefers one row, then a balanced two-row
 * arrangement, then ordinary wrapped rows for very narrow windows.
 */
public final class ExportFilterLayout {
    public record Placement(int index, int x, int y, int row, int width) {
    }

    public record Result(List<Placement> placements, int rows) {
        public Result {
            placements = placements == null ? List.of() : List.copyOf(placements);
            rows = Math.max(1, rows);
        }

        public Placement get(int index) {
            return placements.stream()
                    .filter(placement -> placement.index() == index)
                    .findFirst()
                    .orElseThrow();
        }
    }

    private static final int[][] TWO_ROW_GROUPS = {{0, 1, 3}, {2, 4}};

    private ExportFilterLayout() {
    }

    public static Result layout(int x, int y, int maxWidth, List<Integer> widths,
                                int gap, int rowHeight) {
        if (widths.isEmpty()) {
            return new Result(List.of(), 1);
        }
        int total = totalWidth(widths, 0, widths.size() - 1, gap);
        if (total <= maxWidth) {
            return placeRows(x, y, List.of(range(widths.size())), widths, gap, rowHeight);
        }
        if (widths.size() == 5
                && rowWidth(widths, TWO_ROW_GROUPS[0], gap) <= maxWidth
                && rowWidth(widths, TWO_ROW_GROUPS[1], gap) <= maxWidth) {
            return placeRows(x, y, List.of(TWO_ROW_GROUPS[0], TWO_ROW_GROUPS[1]),
                    widths, gap, rowHeight);
        }
        return placeRows(x, y, wrap(widths, maxWidth, gap), widths, gap, rowHeight);
    }

    private static Result placeRows(int x, int y, List<int[]> rows,
                                    List<Integer> widths, int gap, int rowHeight) {
        List<Placement> placements = new ArrayList<>();
        for (int row = 0; row < rows.size(); row++) {
            int cursor = x;
            for (int index : rows.get(row)) {
                int width = widths.get(index);
                placements.add(new Placement(index, cursor, y + row * rowHeight, row, width));
                cursor += width + gap;
            }
        }
        return new Result(placements, rows.size());
    }

    private static List<int[]> wrap(List<Integer> widths, int maxWidth, int gap) {
        List<int[]> rows = new ArrayList<>();
        List<Integer> current = new ArrayList<>();
        int width = 0;
        for (int index = 0; index < widths.size(); index++) {
            int next = widths.get(index) + (current.isEmpty() ? 0 : gap);
            if (!current.isEmpty() && width + next > maxWidth) {
                rows.add(toArray(current));
                current = new ArrayList<>();
                width = 0;
            }
            current.add(index);
            width += widths.get(index) + (current.size() > 1 ? gap : 0);
        }
        if (!current.isEmpty()) {
            rows.add(toArray(current));
        }
        return rows;
    }

    private static int[] range(int size) {
        int[] values = new int[size];
        for (int index = 0; index < size; index++) {
            values[index] = index;
        }
        return values;
    }

    private static int[] toArray(List<Integer> values) {
        int[] result = new int[values.size()];
        for (int index = 0; index < values.size(); index++) {
            result[index] = values.get(index);
        }
        return result;
    }

    private static int rowWidth(List<Integer> widths, int[] indices, int gap) {
        int total = 0;
        for (int index : indices) {
            total += widths.get(index);
        }
        return total + gap * (indices.length - 1);
    }

    private static int totalWidth(List<Integer> widths, int from, int to, int gap) {
        int total = 0;
        for (int index = from; index <= to; index++) {
            total += widths.get(index);
        }
        return total + gap * Math.max(0, to - from);
    }
}
