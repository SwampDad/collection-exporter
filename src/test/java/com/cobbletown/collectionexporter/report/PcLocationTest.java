package com.cobbletown.collectionexporter.report;

import com.cobbletown.collectionexporter.data.PokemonRecord;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PcLocationTest {
    @Test
    void formatsZeroBasedStorageAsHumanReadableOneBasedNumbers() {
        PokemonRecord pokemon = pokemon(0, 0);
        assertTrue(PcLocation.available(pokemon));
        assertEquals("Box 1, Pos. 1", PcLocation.format(pokemon));
    }

    @Test
    void preservesActualBoxAndSlotAssociations() {
        assertEquals("Box 3, Pos. 13", PcLocation.format(pokemon(2, 12)));
        assertEquals("Box 7, Pos. 22", PcLocation.format(pokemon(6, 21)));
    }

    @Test
    void missingLocationIsExplicit() {
        PokemonRecord pokemon = pokemon(-1, -1);
        assertFalse(PcLocation.available(pokemon));
        assertEquals(PcLocation.UNAVAILABLE, PcLocation.format(pokemon));
    }

    private PokemonRecord pokemon(int box, int slot) {
        return new PokemonRecord(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                box, slot, "cobblemon:tinkaton", "Tinkaton", 959,
                "normal", "Normal", List.of(), false, 50, "female", null);
    }
}
