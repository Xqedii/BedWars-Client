package dev.xqedii;

import net.minecraftforge.common.config.Configuration;
import java.io.File;

public class ConfigManager {

    private static Configuration config;
    private static final String CATEGORY_GENERAL = "General";
    private static final String CATEGORY_MESSAGES = "Messages";

    public static boolean showRegion = true;
    public static boolean showIntruderAlerts = true;
    public static boolean showTntAlerts = true;
    public static boolean showTeammateAlerts = true;
    public static boolean showHolograms = true;
    public static boolean showFireballAlerts = true;
    public static boolean showJoinAlerts = true;
    public static boolean showPlayerMessages = true;
    public static boolean showTeamHud = false;
    public static boolean enableVictoryMessage = true;
    public static String victoryMessage = "!gg";

    public static void init(File configFile) {
        config = new Configuration(configFile);
        syncConfig();
    }

    public static void syncConfig() {
        config.load();

        showRegion = config.getBoolean("Show Visual Region", CATEGORY_GENERAL, showRegion, "Show the red 80x80 region on the ground.");
        showIntruderAlerts = config.getBoolean("Show Intruder Alerts", CATEGORY_GENERAL, showIntruderAlerts, "Show a toast when an enemy enters the region.");
        showTntAlerts = config.getBoolean("Show TNT Alerts", CATEGORY_GENERAL, showTntAlerts, "Show a toast when TNT is placed in the region.");
        showTeammateAlerts = config.getBoolean("Show Teammate Alerts", CATEGORY_GENERAL, showTeammateAlerts, "Show toasts when teammates are detected at the start of the game.");
        showHolograms = config.getBoolean("Show Item Holograms", CATEGORY_GENERAL, showHolograms, "Show holograms over diamond and emerald generators.");
        showFireballAlerts = config.getBoolean("Show Fireball Alerts", CATEGORY_GENERAL, showFireballAlerts, "Show a toast when a fireball is detected in the region.");
        showJoinAlerts = config.getBoolean("Show Join Alerts", CATEGORY_GENERAL, showJoinAlerts, "Show a toast when another player using the helper connects.");
        showPlayerMessages = config.getBoolean("Show Player Messages", CATEGORY_GENERAL, showPlayerMessages, "Show messages sent by other players via /bw-mess.");
        showTeamHud = config.getBoolean("Show Team HUD", CATEGORY_GENERAL, showTeamHud, "Show a HUD with teammate info (health, resources) during a game.");

        enableVictoryMessage = config.getBoolean("Enable Victory Message", CATEGORY_MESSAGES, enableVictoryMessage, "Automatically send a message after winning a game.");
        victoryMessage = config.getString("Victory Message", CATEGORY_MESSAGES, victoryMessage, "The message to send after winning a game.");

        if (config.hasChanged()) {
            config.save();
        }
    }

    public static void setShowRegion(boolean value) {
        showRegion = value;
        config.get(CATEGORY_GENERAL, "Show Visual Region", true).set(value);
        config.save();
    }

    public static void setShowIntruderAlerts(boolean value) {
        showIntruderAlerts = value;
        config.get(CATEGORY_GENERAL, "Show Intruder Alerts", true).set(value);
        config.save();
    }

    public static void setShowTntAlerts(boolean value) {
        showTntAlerts = value;
        config.get(CATEGORY_GENERAL, "Show TNT Alerts", true).set(value);
        config.save();
    }

    public static void setShowTeammateAlerts(boolean value) {
        showTeammateAlerts = value;
        config.get(CATEGORY_GENERAL, "Show Teammate Alerts", true).set(value);
        config.save();
    }

    public static void setShowHolograms(boolean value) {
        showHolograms = value;
        config.get(CATEGORY_GENERAL, "Show Item Holograms", true).set(value);
        config.save();
    }

    public static void setShowFireballAlerts(boolean value) {
        showFireballAlerts = value;
        config.get(CATEGORY_GENERAL, "Show Fireball Alerts", true).set(value);
        config.save();
    }

    public static void setShowJoinAlerts(boolean value) {
        showJoinAlerts = value;
        config.get(CATEGORY_GENERAL, "Show Join Alerts", true).set(value);
        config.save();
    }

    public static void setShowPlayerMessages(boolean value) {
        showPlayerMessages = value;
        config.get(CATEGORY_GENERAL, "Show Player Messages", true).set(value);
        config.save();
    }

    public static void setShowTeamHud(boolean value) {
        showTeamHud = value;
        config.get(CATEGORY_GENERAL, "Show Team HUD", false).set(value);
        config.save();
    }

    public static void setEnableVictoryMessage(boolean value) {
        enableVictoryMessage = value;
        config.get(CATEGORY_MESSAGES, "Enable Victory Message", true).set(value);
        config.save();
    }

    public static void setVictoryMessage(String value) {
        victoryMessage = value;
        config.get(CATEGORY_MESSAGES, "Victory Message", "!ez").set(value);
        config.save();
    }
}