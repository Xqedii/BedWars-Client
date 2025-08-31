package dev.xqedii;

import com.google.gson.JsonObject;
import dev.xqedii.util.GuiHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TeamHudManager {

    private final Minecraft mc;
    private final Map<String, ResourceLocation> playerHeads = new HashMap<>();

    private static class HealthData {
        float health;
        float maxHealth;
    }
    private final Map<String, HealthData> playerHealthData = new HashMap<>();

    private static class ResourceData {
        int iron = 0, gold = 0, diamond = 0, emerald = 0;
    }
    private final Map<String, ResourceData> playerResourceData = new HashMap<>();

    private static final int BOX_HEIGHT = 42;
    private static final int PADDING = 4;
    private static final int ICON_SIZE = 16;
    private static final int BASE_HEAD_SIZE = 32;

    public TeamHudManager(Minecraft mc) {
        this.mc = mc;
    }

    public void clearHealthData() {
        playerHealthData.clear();
    }

    public void clearResourceData() {
        playerResourceData.clear();
    }

    public void updatePlayerHealth(String playerName, float health, float maxHealth) {
        HealthData data = playerHealthData.computeIfAbsent(playerName, k -> new HealthData());
        data.health = health;
        data.maxHealth = maxHealth;
    }

    public void updatePlayerResources(String playerName, JsonObject resources) {
        ResourceData data = playerResourceData.computeIfAbsent(playerName, k -> new ResourceData());
        data.iron = resources.has("iron") ? resources.get("iron").getAsInt() : 0;
        data.gold = resources.has("gold") ? resources.get("gold").getAsInt() : 0;
        data.diamond = resources.has("diamond") ? resources.get("diamond").getAsInt() : 0;
        data.emerald = resources.has("emerald") ? resources.get("emerald").getAsInt() : 0;
    }

    public void render(Set<UUID> teammates) {
        if (mc.theWorld == null || teammates.isEmpty()) {
            return;
        }

        int yOffset = PADDING;

        for (UUID teammateUUID : teammates) {
            EntityPlayer player = mc.theWorld.getPlayerEntityByUUID(teammateUUID);
            if (player != null) {
                drawPlayerBox(PADDING, yOffset, player);
                yOffset += BOX_HEIGHT + PADDING;
            }
        }
    }

    private void drawPlayerBox(int x, int y, EntityPlayer player) {
        FontRenderer fr = mc.fontRendererObj;

        int calculatedItemRowWidth = calculateItemRowWidth(player, fr);
        int nameWidth = fr.getStringWidth(player.getName());
        int contentWidth = Math.max(calculatedItemRowWidth, nameWidth);
        int dynamicBoxWidth = PADDING + BASE_HEAD_SIZE + PADDING * 2 + contentWidth + PADDING;

        int solidBackgroundColor = 0xFF0D0D0D;
        GuiHelper.drawAntiAliasedRoundedRect(x, y, dynamicBoxWidth, BOX_HEIGHT, 5.0f, solidBackgroundColor);

        Gui.drawRect(x + PADDING, y + PADDING, x + PADDING + BASE_HEAD_SIZE, y + PADDING + BASE_HEAD_SIZE, 0xFF0D0D0D);

        int scaledHeadSize = (int) (BASE_HEAD_SIZE * 0.95f);
        int headOffset = (BASE_HEAD_SIZE - scaledHeadSize) / 2;
        int headX = x + PADDING + headOffset;
        int headY = y + PADDING + headOffset;

        ResourceLocation headTexture = getPlayerHead(player.getName());
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(headTexture);
        Gui.drawModalRectWithCustomSizedTexture(headX, headY, 0, 0, scaledHeadSize, scaledHeadSize, scaledHeadSize, scaledHeadSize);

        int textX = x + BASE_HEAD_SIZE + PADDING * 2;
        fr.drawStringWithShadow(player.getName(), textX, y + PADDING, 0xFFFFFF);

        int itemY = y + PADDING + fr.FONT_HEIGHT + 3;
        int itemX = textX;

        int spacing = 5;

        itemX = drawItemCount(Items.emerald, player, itemX, itemY, spacing);
        itemX = drawItemCount(Items.diamond, player, itemX, itemY, spacing);
        itemX = drawItemCount(Items.gold_ingot, player, itemX, itemY, spacing);
        drawItemCount(Items.iron_ingot, player, itemX, itemY, 0);

        HealthData data = playerHealthData.get(player.getName());
        float health, maxHealth;

        if (data != null) {
            health = data.health;
            maxHealth = data.maxHealth;
        } else {
            health = player.getMaxHealth();
            maxHealth = player.getMaxHealth();
        }

        float healthPercent = (maxHealth > 0) ? (health / maxHealth) : 0;
        int healthBarTotalWidth = dynamicBoxWidth - PADDING * 2;
        int healthBarWidth = (int) (healthBarTotalWidth * healthPercent);
        int healthBarY = y + BOX_HEIGHT - PADDING - 3;

        float healthBarRadius = 1.0f;
        GuiHelper.drawAntiAliasedRoundedRect(x + PADDING, healthBarY, healthBarTotalWidth, 2, healthBarRadius, 0xFF161616);
        if (healthBarWidth > 0) {
            GuiHelper.drawAntiAliasedRoundedRect(x + PADDING, healthBarY, healthBarWidth, 2, healthBarRadius, 0xFFFF5555);
        }
    }

    private int calculateItemRowWidth(EntityPlayer player, FontRenderer fr) {
        int width = 0;
        int spacing = 5;

        String countStr = String.valueOf(getResourceCount(player, Items.emerald));
        width += ICON_SIZE + 1 + fr.getStringWidth(countStr) + spacing;

        countStr = String.valueOf(getResourceCount(player, Items.diamond));
        width += ICON_SIZE + 1 + fr.getStringWidth(countStr) + spacing;

        countStr = String.valueOf(getResourceCount(player, Items.gold_ingot));
        width += ICON_SIZE + 1 + fr.getStringWidth(countStr) + spacing;

        countStr = String.valueOf(getResourceCount(player, Items.iron_ingot));
        width += ICON_SIZE + 1 + fr.getStringWidth(countStr);

        return width;
    }

    private int drawItemCount(Item item, EntityPlayer player, int x, int y, int spacingAfter) {
        int count = getResourceCount(player, item);
        ItemStack itemStack = new ItemStack(item);

        GlStateManager.pushMatrix();
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        mc.getRenderItem().renderItemAndEffectIntoGUI(itemStack, x, y);
        GlStateManager.popMatrix();

        String countStr = String.valueOf(count);
        int textX = x + ICON_SIZE + 1;
        mc.fontRendererObj.drawStringWithShadow(countStr, textX, y + (ICON_SIZE / 2f) - (mc.fontRendererObj.FONT_HEIGHT / 2f) , 0xFFFFFF);

        return textX + mc.fontRendererObj.getStringWidth(countStr) + spacingAfter;
    }

    public int getResourceCount(EntityPlayer player, Item item) {
        if (player.getName().equals(mc.thePlayer.getName())) {
            return countItems(player, item);
        }
        ResourceData data = playerResourceData.get(player.getName());
        if (data == null) return 0;

        if (item == Items.iron_ingot) return data.iron;
        if (item == Items.gold_ingot) return data.gold;
        if (item == Items.diamond) return data.diamond;
        if (item == Items.emerald) return data.emerald;

        return 0;
    }

    public int countItems(EntityPlayer player, Item item) {
        int count = 0;
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack != null && stack.getItem() == item) {
                count += stack.stackSize;
            }
        }
        return count;
    }

    private ResourceLocation getPlayerHead(String username) {
        String lowerCaseUsername = username.toLowerCase();
        if (playerHeads.containsKey(lowerCaseUsername)) {
            return playerHeads.get(lowerCaseUsername);
        }

        ResourceLocation playerHead = new ResourceLocation("bwhud/heads/" + lowerCaseUsername);
        TextureManager textureManager = mc.getTextureManager();
        ITextureObject texture = textureManager.getTexture(playerHead);

        if (texture == null) {
            String url = "https://cravatar.eu/helmhead/" + username + "/64.png";
            ThreadDownloadImageData textureObject = new ThreadDownloadImageData(null, url, null, null);
            textureManager.loadTexture(playerHead, textureObject);
        }

        playerHeads.put(lowerCaseUsername, playerHead);
        return playerHead;
    }
}