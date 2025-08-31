package dev.xqedii;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

public class RegionManager {

    private enum State {
        INACTIVE, DELAY, ANIMATING_IN, ACTIVE, ANIMATING_OUT
    }

    private State currentState = State.INACTIVE;
    private long startTime;
    private Vec3 centerPos;

    private static final double INITIAL_REGION_SIZE = 3.0;
    private static final double REGION_SIZE = 80.0;
    private static final long DELAY_MS = 1000;
    private static final long ANIM_IN_MS = 1000;
    private static final long ACTIVE_MS = 3000;
    private static final long ANIM_OUT_MS = 1000;

    public void startVisualSequence(Vec3 center) {
        if (currentState != State.INACTIVE) return;
        this.currentState = State.DELAY;
        this.startTime = System.currentTimeMillis();
        this.centerPos = center;
    }

    public void stopVisualSequence() {
        this.currentState = State.INACTIVE;
    }

    public void update() {
        if (currentState == State.INACTIVE) return;
        long elapsedTime = System.currentTimeMillis() - startTime;
        switch (currentState) {
            case DELAY:
                if (elapsedTime >= DELAY_MS) {
                    currentState = State.ANIMATING_IN;
                    startTime = System.currentTimeMillis();
                }
                break;
            case ANIMATING_IN:
                if (elapsedTime >= ANIM_IN_MS) {
                    currentState = State.ACTIVE;
                    startTime = System.currentTimeMillis();
                }
                break;
            case ACTIVE:
                if (elapsedTime >= ACTIVE_MS) {
                    currentState = State.ANIMATING_OUT;
                    startTime = System.currentTimeMillis();
                }
                break;
            case ANIMATING_OUT:
                if (elapsedTime >= ANIM_OUT_MS) {
                    currentState = State.INACTIVE;
                }
                break;
        }
    }

    public void render(EntityPlayerSP player, float partialTicks) {
        if (!ConfigManager.showRegion) return;
        if (currentState == State.INACTIVE || centerPos == null) return;

        long elapsedTime = System.currentTimeMillis() - startTime;
        double currentSize;
        float alpha;

        switch (currentState) {
            case ANIMATING_IN:
                float progressIn = MathHelper.clamp_float((float) elapsedTime / ANIM_IN_MS, 0.0f, 1.0f);
                currentSize = INITIAL_REGION_SIZE + (REGION_SIZE - INITIAL_REGION_SIZE) * progressIn;
                alpha = 0.3f;
                break;
            case ACTIVE:
                currentSize = REGION_SIZE;
                alpha = 0.3f;
                break;
            case ANIMATING_OUT:
                float progressOut = 1.0f - MathHelper.clamp_float((float) elapsedTime / ANIM_OUT_MS, 0.0f, 1.0f);
                currentSize = REGION_SIZE;
                alpha = 0.3f * progressOut;
                break;
            default:
                return;
        }

        if (alpha <= 0) return;

        double renderX = centerPos.xCoord - (player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks);
        double renderY = centerPos.yCoord - (player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks);
        double renderZ = centerPos.zCoord - (player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks);

        GlStateManager.pushMatrix();
        GlStateManager.translate(renderX, renderY, renderZ);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        double s = currentSize / 2;
        float r = 1.0f, g = 0.0f, b = 0.0f;

        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(-s, -s, -s).color(r, g, b, alpha).endVertex();
        worldrenderer.pos( s, -s, -s).color(r, g, b, alpha).endVertex();
        worldrenderer.pos( s, -s,  s).color(r, g, b, alpha).endVertex();
        worldrenderer.pos(-s, -s,  s).color(r, g, b, alpha).endVertex();
        worldrenderer.pos(-s,  s, -s).color(r, g, b, alpha).endVertex();
        worldrenderer.pos(-s,  s,  s).color(r, g, b, alpha).endVertex();
        worldrenderer.pos( s,  s,  s).color(r, g, b, alpha).endVertex();
        worldrenderer.pos( s,  s, -s).color(r, g, b, alpha).endVertex();
        worldrenderer.pos(-s, -s,  s).color(r, g, b, alpha).endVertex();
        worldrenderer.pos( s, -s,  s).color(r, g, b, alpha).endVertex();
        worldrenderer.pos( s,  s,  s).color(r, g, b, alpha).endVertex();
        worldrenderer.pos(-s,  s,  s).color(r, g, b, alpha).endVertex();
        worldrenderer.pos(-s, -s, -s).color(r, g, b, alpha).endVertex();
        worldrenderer.pos(-s,  s, -s).color(r, g, b, alpha).endVertex();
        worldrenderer.pos( s,  s, -s).color(r, g, b, alpha).endVertex();
        worldrenderer.pos( s, -s, -s).color(r, g, b, alpha).endVertex();
        worldrenderer.pos(-s, -s, -s).color(r, g, b, alpha).endVertex();
        worldrenderer.pos(-s, -s,  s).color(r, g, b, alpha).endVertex();
        worldrenderer.pos(-s,  s,  s).color(r, g, b, alpha).endVertex();
        worldrenderer.pos(-s,  s, -s).color(r, g, b, alpha).endVertex();
        worldrenderer.pos( s, -s, -s).color(r, g, b, alpha).endVertex();
        worldrenderer.pos( s,  s, -s).color(r, g, b, alpha).endVertex();
        worldrenderer.pos( s,  s,  s).color(r, g, b, alpha).endVertex();
        worldrenderer.pos( s, -s,  s).color(r, g, b, alpha).endVertex();
        tessellator.draw();

        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}