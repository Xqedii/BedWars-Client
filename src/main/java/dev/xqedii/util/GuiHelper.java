package dev.xqedii.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class GuiHelper {

    public static void drawTexture(ResourceLocation texture, int x, int y, int width, int height) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, width, height, width, height);
    }

    public static void drawAntiAliasedRoundedRect(float x, float y, float width, float height, float radius, int color) {
        if (width <= 0 || height <= 0) return;

        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        int scaleFactor = sr.getScaleFactor();

        GlStateManager.pushMatrix();
        GlStateManager.scale(1.0f / scaleFactor, 1.0f / scaleFactor, 1.0f / scaleFactor);

        float physicalX = x * scaleFactor;
        float physicalY = y * scaleFactor;
        float physicalWidth = width * scaleFactor;
        float physicalHeight = height * scaleFactor;
        float physicalRadius = radius * scaleFactor;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.disableTexture2D();

        float x2 = physicalX + physicalWidth;
        float y2 = physicalY + physicalHeight;
        physicalRadius = Math.min(Math.min(physicalWidth, physicalHeight) / 2.0f, physicalRadius);

        int solidColor = color | 0xFF000000;
        Gui.drawRect((int) (physicalX + physicalRadius), (int) physicalY, (int) (x2 - physicalRadius), (int) y2, solidColor);
        Gui.drawRect((int) physicalX, (int) (physicalY + physicalRadius), (int) x2, (int) (y2 - physicalRadius), solidColor);

        drawOptimizedAntiAliasedQuarterCircle(physicalX + physicalRadius, physicalY + physicalRadius, physicalRadius, 0, color);
        drawOptimizedAntiAliasedQuarterCircle(x2 - physicalRadius, physicalY + physicalRadius, physicalRadius, 1, color);
        drawOptimizedAntiAliasedQuarterCircle(physicalX + physicalRadius, y2 - physicalRadius, physicalRadius, 2, color);
        drawOptimizedAntiAliasedQuarterCircle(x2 - physicalRadius, y2 - physicalRadius, physicalRadius, 3, color);

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static void drawOptimizedAntiAliasedQuarterCircle(float centerX, float centerY, float radius, int quadrant, int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;
        if (a == 0) a = 255;

        float radiusWithFeatherSq = (radius + 1.0f) * (radius + 1.0f);
        for (int y_offset = 0; y_offset <= Math.ceil(radius) + 1; y_offset++) {
            float y_offset_sq = y_offset * y_offset;
            for (int x_offset = 0; x_offset <= Math.ceil(radius) + 1; x_offset++) {
                float distSq = x_offset * x_offset + y_offset_sq;
                if (distSq <= radiusWithFeatherSq) {
                    double dist = Math.sqrt(distSq);
                    double alphaFactor = MathHelper.clamp_double(1.0 - (dist - (radius - 0.5f)), 0.0, 1.0);
                    if (alphaFactor > 0) {
                        int finalAlpha = (int) (a * alphaFactor);
                        int finalColor = (finalAlpha << 24) | (r << 16) | (g << 8) | b;
                        float drawX, drawY;
                        switch (quadrant) {
                            case 0: drawX = centerX - x_offset; drawY = centerY - y_offset; break;
                            case 1: drawX = centerX + x_offset - 1; drawY = centerY - y_offset; break;
                            case 2: drawX = centerX - x_offset; drawY = centerY + y_offset - 1; break;
                            case 3: default: drawX = centerX + x_offset - 1; drawY = centerY + y_offset - 1; break;
                        }
                        Gui.drawRect((int) drawX, (int) drawY, (int) drawX + 1, (int) drawY + 1, finalColor);
                    }
                }
            }
        }
    }
}