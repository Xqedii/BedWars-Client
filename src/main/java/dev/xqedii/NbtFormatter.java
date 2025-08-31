package dev.xqedii;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

public class NbtFormatter {

    private static final EnumChatFormatting COLOR_PUNCTUATION = EnumChatFormatting.GRAY;
    private static final EnumChatFormatting COLOR_KEY = EnumChatFormatting.GOLD;
    private static final EnumChatFormatting COLOR_VALUE = EnumChatFormatting.AQUA;

    private static final IChatComponent HOVER_TEXT = new ChatComponentText("Kliknij, aby wstawić do czatu").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GRAY));

    public static void sendFormattedNbt(NBTBase nbt, EntityPlayerSP player) {
        if (nbt == null) return;

        String prettyNbt = prettyPrintNbt(nbt.toString());
        String[] lines = prettyNbt.split("\n");

        for (String line : lines) {
            player.addChatMessage(formatLine(line));
        }
        player.playSound("note.pling", 1.0f, 1.5f);
    }

    private static IChatComponent formatLine(String line) {
        IChatComponent formattedLine = new ChatComponentText("");
        String trimmedLine = line.trim();
        int indentLength = line.indexOf(trimmedLine);
        if (indentLength > 0) {
            formattedLine.appendText(line.substring(0, indentLength));
        }

        boolean afterColon = false;

        for (int i = 0; i < trimmedLine.length(); i++) {
            char c = trimmedLine.charAt(i);

            if (c == '"') {
                int endIndex = i + 1;
                while (endIndex < trimmedLine.length()) {
                    if (trimmedLine.charAt(endIndex) == '"' && trimmedLine.charAt(endIndex - 1) != '\\') {
                        break;
                    }
                    endIndex++;
                }

                formattedLine.appendSibling(new ChatComponentText("\"").setChatStyle(new ChatStyle().setColor(COLOR_PUNCTUATION)));

                String content = trimmedLine.substring(i + 1, endIndex);

                if (afterColon) {
                    formattedLine.appendSibling(createClickableValueText(content));
                    afterColon = false;
                } else {
                    formattedLine.appendSibling(createClickableKeyText(content));
                }

                formattedLine.appendSibling(new ChatComponentText("\"").setChatStyle(new ChatStyle().setColor(COLOR_PUNCTUATION)));
                i = endIndex;
                continue;
            }

            if (c == ':' || c == ',' || c == '{' || c == '}' || c == '[' || c == ']') {
                formattedLine.appendSibling(new ChatComponentText(String.valueOf(c)).setChatStyle(new ChatStyle().setColor(COLOR_PUNCTUATION)));
                if (c == ':') afterColon = true;
                if (c == ',' || c == '{') afterColon = false;
                continue;
            }

            if (Character.isWhitespace(c)) {
                formattedLine.appendText(String.valueOf(c));
                continue;
            }

            int endIndex = i;
            while (endIndex < trimmedLine.length() && !(":,{}[] ".indexOf(trimmedLine.charAt(endIndex)) != -1)) {
                endIndex++;
            }
            String token = trimmedLine.substring(i, endIndex);

            if (afterColon) {
                formattedLine.appendSibling(createClickableValueText(token));
                afterColon = false;
            } else {
                formattedLine.appendSibling(createClickableKeyText(token));
            }
            i = endIndex - 1;
        }
        return formattedLine;
    }

    private static IChatComponent createClickableKeyText(String text) {
        ChatStyle style = new ChatStyle()
                .setColor(COLOR_KEY)
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, text))
                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, HOVER_TEXT));
        return new ChatComponentText(text).setChatStyle(style);
    }

    private static IChatComponent createClickableValueText(String text) {
        ChatStyle style = new ChatStyle()
                .setColor(COLOR_VALUE)
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, text))
                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, HOVER_TEXT));
        return new ChatComponentText(text).setChatStyle(style);
    }

    private static String prettyPrintNbt(String rawNbt) {
        StringBuilder pretty = new StringBuilder();
        int indentLevel = 0;
        boolean inString = false;
        boolean escaped = false;

        for (char c : rawNbt.toCharArray()) {
            if (escaped) {
                pretty.append(c);
                escaped = false;
                continue;
            }
            if (c == '\\') {
                pretty.append(c);
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
            }

            if (!inString) {
                if (c == '{' || c == '[') {
                    pretty.append(c).append('\n');
                    indentLevel++;
                    appendIndent(pretty, indentLevel);
                } else if (c == '}' || c == ']') {
                    pretty.append('\n');
                    indentLevel--;
                    appendIndent(pretty, indentLevel);
                    pretty.append(c);
                } else if (c == ',') {
                    pretty.append(c).append('\n');
                    appendIndent(pretty, indentLevel);
                } else if (c == ':') {
                    pretty.append(c).append(' ');
                } else if (!Character.isWhitespace(c)) {
                    pretty.append(c);
                }
            } else {
                pretty.append(c);
            }
        }
        return pretty.toString();
    }

    private static void appendIndent(StringBuilder builder, int level) {
        for (int i = 0; i < level; i++) {
            builder.append("  ");
        }
    }
}