package com.cobbletown.collectionexporter.classify;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AspectHeuristicsTest {
    @Test
    void seasonalYearTokensAreReviewCandidates() {
        assertTrue(AspectHeuristics.looksLikeYearToken("adventure2026"));
        assertTrue(AspectHeuristics.looksLikeYearToken("october2027spooky"));
        assertTrue(AspectHeuristics.looksLikeYearToken("mario2026"));
        assertTrue(AspectHeuristics.looksLikeYearToken("2028event"));
    }

    @Test
    void ordinaryAppearanceTokensAreNotYearCandidates() {
        assertFalse(AspectHeuristics.looksLikeYearToken("magikarp-jump-standard"));
        assertFalse(AspectHeuristics.looksLikeYearToken("gyarados-eye-color-red"));
        assertFalse(AspectHeuristics.looksLikeYearToken("regrown-tail-3"));
        assertFalse(AspectHeuristics.looksLikeYearToken(null));
    }
}
