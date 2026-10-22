package com.cobbletown.collectionexporter.report;

import com.cobbletown.collectionexporter.data.PokemonRecord;

/** Human-readable PC storage location. Box and slot indices are stored zero-based. */
public final class PcLocation {
    public static final String UNAVAILABLE = "Location unavailable";

    private PcLocation() {
    }

    public static boolean available(PokemonRecord pokemon) {
        return pokemon != null && pokemon.box() >= 0 && pokemon.slot() >= 0;
    }

    public static String format(PokemonRecord pokemon) {
        return available(pokemon)
                ? "Box " + (pokemon.box() + 1) + ", Pos. " + (pokemon.slot() + 1)
                : UNAVAILABLE;
    }
}
