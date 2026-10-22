package com.cobbletown.collectionexporter.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CollectionExporterVersionLabelTest {
    @Test
    void usesTheInstalledMetadataVersionWithoutNormalizingDevelopmentVersions() {
        assertEquals("ShareYourMons · v0.3.0-dev.9",
                CollectionExporterVersionLabel.format("0.3.0-dev.9"));
    }

    @Test
    void producesTheStableReleaseLabelFromStableMetadata() {
        assertEquals("ShareYourMons · v0.3.0",
                CollectionExporterVersionLabel.format("0.3.0"));
    }
}
