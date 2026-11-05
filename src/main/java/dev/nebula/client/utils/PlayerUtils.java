package dev.nebula.client.utils;

import java.util.regex.Pattern;

import dev.nebula.client.mappings.Mappings;

import java.util.regex.Matcher;

public class PlayerUtils {

    public static String getDisplayName(Object player) {
        if (player == null) {
            return "";
        }

        try {
            if (!Mappings.ENTITY_PLAYER_CLASS.isInstance(player)) {
                return "";
            }

            Object chatComponent = ReflectionUtils.invokeMethod(Mappings.ENTITY_PLAYER_GET_DISPLAY_NAME, player);
            if (chatComponent == null) {
                return "";
            }

            Object name = ReflectionUtils.invokeMethod(Mappings.I_CHAT_COMPONENT_GET_FORMATTED_TEXT, chatComponent);
            String displayName = (name != null) ? name.toString() : "";
            return displayName;
        } catch (Exception e) {
            return "";
        }
    }

    public static String getClan(Object player) {
        try {
            String displayName = getDisplayName(player);
            if (displayName == null || displayName.isEmpty()) {
                return "";
            }

            Matcher matcher = Pattern.compile("\\[(.*?)\\]").matcher(displayName);
            if (matcher.find()) {
                String clan = matcher.group(1); 
                if (clan == null || clan.isEmpty()) {
                    return "";
                }

                clan = stripControlCodes(clan).replaceAll("[\\[\\]§]", "").trim();
                return clan;
            } else {
                return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    private static String stripControlCodes(String input) {
        if (input == null) return "";
        return input.replaceAll("§[0-9a-fk-or]", "");
    }
}
