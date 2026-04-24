package com.basti20999.customGUIs.config;

import org.bukkit.Sound;

import java.util.Map;

public record GUIConfig(
    String id,
    String command,
    String title,           // color-translated, may contain placeholders
    int size,
    boolean readonly,
    boolean closeOnClick,
    Sound openSound,        // null = no sound
    Map<Integer, ItemConfig> items
) {}
