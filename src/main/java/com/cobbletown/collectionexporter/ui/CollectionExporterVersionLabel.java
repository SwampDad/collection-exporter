package com.cobbletown.collectionexporter.ui;

import net.fabricmc.loader.api.FabricLoader;

/** Builds the always-current version label from the installed Fabric mod metadata. */
final class CollectionExporterVersionLabel {
    private static final String MOD_ID = "collection-exporter";
    private static final String MOD_NAME = "ShareYourMons";

    private CollectionExporterVersionLabel() {
    }

    static String text() {
        String version = FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        return format(version);
    }

    static String format(String version) {
        String displayed = version == null || version.isBlank() ? "unknown" : version;
        return MOD_NAME + " · v" + displayed;
    }
}
