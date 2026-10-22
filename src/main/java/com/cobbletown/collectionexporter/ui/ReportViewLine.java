package com.cobbletown.collectionexporter.ui;

/** One line in the native report viewer. */
public record ReportViewLine(String text, int indent, Tone tone) {
    public enum Tone {
        TEXT,
        MUTED,
        LEGENDARY,
        MYTHICAL,
        SKIN_HEADER,
        WARNING
    }

    public ReportViewLine {
        text = text == null ? "" : text;
        indent = Math.max(0, indent);
        tone = tone == null ? Tone.TEXT : tone;
    }
}
