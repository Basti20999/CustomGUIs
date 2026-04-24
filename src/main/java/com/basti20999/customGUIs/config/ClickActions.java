package com.basti20999.customGUIs.config;

/**
 * Actions resolved for a single click event.
 * Exactly one field should be non-null; all others are null.
 */
public record ClickActions(
    String command,         // run as player
    String consoleCommand,  // run as console (with %player% resolved)
    String openGui,         // open another GUI by id
    String message          // send chat message to player
) {
    /** A no-op action set (item exists but has no action). */
    public static final ClickActions NONE = new ClickActions(null, null, null, null);

    public boolean isEmpty() {
        return command == null && consoleCommand == null && openGui == null && message == null;
    }
}
