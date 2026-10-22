package com.cobbletown.collectionexporter.classify;

import java.util.regex.Pattern;

/**
 * Conservative heuristics for review prioritization. They never promote an aspect to a skin;
 * they only distinguish an unrecognized token that looks like a seasonal release name from an
 * ordinary unknown appearance token.
 */
public final class AspectHeuristics {
    private static final Pattern YEAR_TOKEN = Pattern.compile("20\\d{2}");

    private AspectHeuristics() {
    }

    /**
     * True for tokens such as {@code adventure2026} or {@code october2027spooky}. The year is a
     * strong signal for a new seasonal family, but owner review is still required before it can
     * become a confirmed skin.
     */
    public static boolean looksLikeYearToken(String aspect) {
        return aspect != null && YEAR_TOKEN.matcher(aspect).find();
    }
}
