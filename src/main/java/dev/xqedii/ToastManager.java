package dev.xqedii;

import dev.xqedii.util.GuiHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class ToastManager {

    private static class Toast {
        String line1, line2;
        EnumChatFormatting color;
        long duration;
    }

    private final Minecraft mc;
    private Toast persistentToast;
    private long persistentToastStartTime;
    private Toast temporaryToast;
    private long temporaryToastStartTime;

    private static final long TOAST_DURATION_MS = 5000;
    private static final long TEAMMATE_TOAST_DURATION_MS = 1000L;
    private static final long TOAST_ANIM_MS = 250;
    private static final int TOP_PADDING = 10;
    private static final Map<EnumChatFormatting, Color> TEAM_COLORS = new HashMap<>();

    static {
        TEAM_COLORS.put(EnumChatFormatting.RED, new Color(255, 85, 85));
        TEAM_COLORS.put(EnumChatFormatting.BLUE, new Color(85, 85, 255));
        TEAM_COLORS.put(EnumChatFormatting.GREEN, new Color(85, 255, 85));
        TEAM_COLORS.put(EnumChatFormatting.YELLOW, new Color(255, 255, 85));
        TEAM_COLORS.put(EnumChatFormatting.AQUA, new Color(85, 255, 255));
        TEAM_COLORS.put(EnumChatFormatting.WHITE, new Color(255, 255, 255));
        TEAM_COLORS.put(EnumChatFormatting.GRAY, new Color(170, 170, 170));
        TEAM_COLORS.put(EnumChatFormatting.LIGHT_PURPLE, new Color(255, 85, 255));
        TEAM_COLORS.put(EnumChatFormatting.GOLD, new Color(255, 170, 0));
    }

    public ToastManager(Minecraft mc) {
        this.mc = mc;
    }

    private void createToast(String line1, String line2, EnumChatFormatting color, long duration, boolean isTemporary) {
        Toast toast = new Toast();
        toast.line1 = line1;
        toast.line2 = line2;
        toast.color = color;
        toast.duration = duration;

        if (isTemporary) {
            temporaryToast = toast;
            temporaryToastStartTime = System.currentTimeMillis();
        } else {
            persistentToast = toast;
            persistentToastStartTime = System.currentTimeMillis();
            temporaryToast = null;
        }
        playSfx(5);
    }

    public void show(String line1, String line2, EnumChatFormatting color, long duration) {
        createToast(line1, line2, color, duration, false);
    }

    public void showTeammateAddedToast(EntityPlayer player) {
        if (!ConfigManager.showTeammateAlerts) return;
        createToast("Teammate Added", player.getName(), getPlayerArmorTeamColor(player), TEAMMATE_TOAST_DURATION_MS, true);
    }

    public void showIntruderToast(String playerName, EnumChatFormatting color) {
        if (!ConfigManager.showIntruderAlerts) return;
        createToast("Incomming", playerName, color, TOAST_DURATION_MS, false);
    }

    public void showTntToast(String playerName, EnumChatFormatting color) {
        if (!ConfigManager.showTntAlerts || "Unknown".equals(playerName)) return;
        createToast("TNT Placed!", playerName, color, TOAST_DURATION_MS, false);
    }

    public void showFireballToast(String playerName, EnumChatFormatting color) {
        if (!ConfigManager.showFireballAlerts || "Unknown".equals(playerName)) return;
        createToast("Fireball!", playerName, color, TOAST_DURATION_MS, false);
    }

    public void showJoinToast(String playerName) {
        if (!ConfigManager.showJoinAlerts) return;
        createToast("\u00a7e" + playerName, "\u00a7fJoined using the client", EnumChatFormatting.WHITE, TOAST_DURATION_MS, true);
    }

    public void showMessageToast(String playerName, String message) {
        if (!ConfigManager.showPlayerMessages) return;
        createToast("Message from " + playerName, message, EnumChatFormatting.YELLOW, 3000L, false);
    }

    public void showPartyCreatedToast() {
        createToast("\u00a7d\u00a7lParty", "\u00a7fSuccessfully created a party", EnumChatFormatting.WHITE, TOAST_DURATION_MS, false);
    }

    public void showPartyJoinedToast(String inviterName) {
        createToast("\u00a7d\u00a7lParty", "\u00a7fJoined party of \u00a7e" + inviterName, EnumChatFormatting.WHITE, TOAST_DURATION_MS, false);
    }

    public void showTacticVoteStartToast() {
        createToast("Choose a tactic", "Press \u00a7eRight Shift \u00a7fto choose a tactic", EnumChatFormatting.WHITE, 15000L, false);
    }

    public void showTacticVoteUpdateToast(String tacticName, int votes, int maxVotes) {
        createToast("\u00a7e\u00a7l" + tacticName, "Voted! (\u00a7e" + votes + "\u00a77/\u00a76" + maxVotes + "\u00a7f)", EnumChatFormatting.WHITE, 1500L, true);
    }

    public void showTacticChosenToast(String tacticName) {
        createToast("Tactic selected!", "\u00a7e" + tacticName, EnumChatFormatting.WHITE, 3000L, false);
    }

    public void showTacticFailedToast(String tacticName) {
        createToast("\u00a7c\u00a7l" + tacticName, "\u00a7fTactic failed", EnumChatFormatting.WHITE, 3000L, false);
    }

    public void showTacticSuccessToast(String tacticName) {
        createToast("\u00a7a\u00a7l" + tacticName, "\u00a7fTactic executed!", EnumChatFormatting.WHITE, 3000L, false);
    }

    public boolean isToastVisible() {
        return temporaryToast != null || persistentToast != null;
    }

    public void update() {
        long now = System.currentTimeMillis();
        if (temporaryToast != null && now - temporaryToastStartTime > temporaryToast.duration) {
            temporaryToast = null;
        }
        if (persistentToast != null && now - persistentToastStartTime > persistentToast.duration) {
            persistentToast = null;
        }
    }

    private void playSfx(int count) {
        for (int i = 0; i < count; i++) {
            mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord.create(new ResourceLocation("note.pling"), 1.0F));
        }
    }

    public void render() {
        Toast toastToRender = temporaryToast != null ? temporaryToast : persistentToast;
        long startTime = temporaryToast != null ? temporaryToastStartTime : persistentToastStartTime;

        if (toastToRender == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        FontRenderer fr = mc.fontRendererObj;
        long elapsedTime = System.currentTimeMillis() - startTime;
        float animProgress;
        if (elapsedTime < TOAST_ANIM_MS) {
            animProgress = (float) elapsedTime / TOAST_ANIM_MS;
        } else if (elapsedTime > toastToRender.duration - TOAST_ANIM_MS) {
            long timeSinceEnd = elapsedTime - (toastToRender.duration - TOAST_ANIM_MS);
            animProgress = 1.0f - ((float) timeSinceEnd / TOAST_ANIM_MS);
        } else {
            animProgress = 1.0f;
        }
        animProgress = MathHelper.clamp_float(animProgress, 0.0f, 1.0f);
        int textWidth1 = fr.getStringWidth(toastToRender.line1);
        int textWidth2 = fr.getStringWidth(toastToRender.line2);
        int maxWidth = Math.max(textWidth1, textWidth2);
        int boxWidth = maxWidth + 20;
        int boxHeight = 30;
        int boxX = (sr.getScaledWidth() - boxWidth) / 2;
        int boxY = (int) (-boxHeight + (boxHeight + TOP_PADDING) * animProgress);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GuiHelper.drawAntiAliasedRoundedRect(boxX, boxY, boxWidth, boxHeight, 5.0f, 0xFF0D0D0D);
        GlStateManager.disableBlend();
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0, 200);

        fr.drawStringWithShadow(toastToRender.line1, boxX + (boxWidth - textWidth1) / 2, boxY + 5, 0xFFFFFFFF);
        String coloredText = toastToRender.color + toastToRender.line2;
        fr.drawStringWithShadow(coloredText, boxX + (boxWidth - fr.getStringWidth(toastToRender.line2)) / 2, boxY + 17, 0xFFFFFFFF);

        GlStateManager.popMatrix();
    }

    public EnumChatFormatting getPlayerArmorTeamColor(EntityPlayer player) {
        if (player == null) return EnumChatFormatting.WHITE;
        int armorColor = -1;
        for (ItemStack stack : player.inventory.armorInventory) {
            if (stack != null && stack.getItem() instanceof ItemArmor) {
                ItemArmor armor = (ItemArmor) stack.getItem();
                if (armor.getArmorMaterial() == ItemArmor.ArmorMaterial.LEATHER) {
                    armorColor = armor.getColor(stack);
                    break;
                }
            }
        }
        if (armorColor == -1) return EnumChatFormatting.WHITE;
        Color playerColor = new Color(armorColor);
        EnumChatFormatting bestMatch = EnumChatFormatting.WHITE;
        double minDistance = Double.MAX_VALUE;
        for (Map.Entry<EnumChatFormatting, Color> entry : TEAM_COLORS.entrySet()) {
            Color teamColor = entry.getValue();
            double distance = Math.pow(playerColor.getRed() - teamColor.getRed(), 2)
                    + Math.pow(playerColor.getGreen() - teamColor.getGreen(), 2)
                    + Math.pow(playerColor.getBlue() - teamColor.getBlue(), 2);
            if (distance < minDistance) {
                minDistance = distance;
                bestMatch = entry.getKey();
            }
        }
        return bestMatch;
    }
}