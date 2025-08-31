package dev.xqedii.gui;

import dev.xqedii.ConfigManager;
import dev.xqedii.NbtToolsMod;
import dev.xqedii.util.GuiHelper;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

public class CustomGuiScreen extends GuiScreen {
    private static final float SAFE_AREA_PERCENTAGE = 0.90f;
    private static final ResourceLocation GUI_LOGO = new ResourceLocation(NbtToolsMod.MODID, "textures/gui/logo.png");
    private static final int MAIN_CONTENT_COLOR = 0xFF161616;
    private static final int TEXT_COLOR_PRIMARY = 0xFFFFFFFF;
    private static final float TITLE_SCALE = 2.5f;

    private static final int CHECKBOX_BG_COLOR = 0xFF333333;
    private static final int CHECKMARK_COLOR = 0xFFDEAC25;

    private static final long ANIMATION_DURATION_MS = 100;
    private static final float MAX_ALPHA_PERCENT = 0.30f;
    private static final float INITIAL_SCALE = 0.90f;
    private static final float FINAL_SCALE = 1.0f;
    private long animationStartTime;
    private int totalGameScale;

    private float currentAnimationScale;

    private float chkbox1_x, chkbox1_y, chkbox_size;
    private float chkbox2_x, chkbox2_y;
    private float chkbox3_x, chkbox3_y;
    private float chkbox4_x, chkbox4_y;
    private float chkbox5_x, chkbox5_y;
    private float chkbox6_x, chkbox6_y;
    private float chkbox7_x, chkbox7_y;
    private float chkbox8_x, chkbox8_y;
    private float chkbox9_x, chkbox9_y;
    private float chkbox10_x, chkbox10_y;

    private GuiTextField victoryMessageField;

    @Override
    public void initGui() {
        super.initGui();
        this.animationStartTime = System.currentTimeMillis();
        Keyboard.enableRepeatEvents(true);

        this.victoryMessageField = new GuiTextField(0, this.fontRendererObj, 0, 0, 100, 12);
        this.victoryMessageField.setText(ConfigManager.victoryMessage);
        this.victoryMessageField.setMaxStringLength(50);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        ConfigManager.setVictoryMessage(this.victoryMessageField.getText());
    }

    private void playClickSound() {
        if (mc != null) {
            mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long elapsedTime = System.currentTimeMillis() - this.animationStartTime;
        float progress = MathHelper.clamp_float((float) elapsedTime / (float) ANIMATION_DURATION_MS, 0.0f, 1.0f);
        int alpha = (int) (progress * 255 * MAX_ALPHA_PERCENT);
        if (alpha > 0) drawRect(0, 0, this.width, this.height, alpha << 24);
        super.drawScreen(mouseX, mouseY, partialTicks);
        ScaledResolution sr = new ScaledResolution(mc);
        FontRenderer textRenderer = mc.fontRendererObj;
        float framebufferWidth = mc.displayWidth, framebufferHeight = mc.displayHeight;
        float designWidth = 1366.0f, designHeight = 682.0f;
        float safeAreaWidth = framebufferWidth * SAFE_AREA_PERCENTAGE, safeAreaHeight = framebufferHeight * SAFE_AREA_PERCENTAGE;
        float scaleX = safeAreaWidth / designWidth, scaleY = safeAreaHeight / designHeight;
        float finalContentScale = Math.min(1.0f, Math.min(scaleX, scaleY));
        float finalPhysicalWidth = designWidth * finalContentScale, finalPhysicalHeight = designHeight * finalContentScale;
        float baseRatioWidth = 385.0f;
        float finalPhysicalRadius = (10.0f / baseRatioWidth) * finalPhysicalWidth;
        float finalPhysicalSidebarPadding = (9.0f / baseRatioWidth) * finalPhysicalWidth;
        float finalPhysicalSidebarWidth = (23.0f / baseRatioWidth) * finalPhysicalWidth;
        float finalPhysicalSidebarRadius = (6.0f / baseRatioWidth) * finalPhysicalWidth;
        float finalPhysicalLogoPadding = (3.0f / baseRatioWidth) * finalPhysicalWidth;
        float finalPhysicalLeft = (framebufferWidth - finalPhysicalWidth) / 2.0f;
        float finalPhysicalTop = (framebufferHeight - finalPhysicalHeight) / 2.0f;
        this.totalGameScale = sr.getScaleFactor();
        GlStateManager.pushMatrix();
        this.currentAnimationScale = INITIAL_SCALE + (FINAL_SCALE - INITIAL_SCALE) * progress;
        float centerX = this.width / 2.0f, centerY = this.height / 2.0f;
        GlStateManager.translate(centerX, centerY, 0);
        GlStateManager.scale(this.currentAnimationScale, this.currentAnimationScale, 1.0f);
        GlStateManager.translate(-centerX, -centerY, 0);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        float guiLeft = finalPhysicalLeft / totalGameScale, guiTop = finalPhysicalTop / totalGameScale;
        float guiWidth = finalPhysicalWidth / totalGameScale, guiHeight = finalPhysicalHeight / totalGameScale;
        float guiRadius = finalPhysicalRadius / totalGameScale, guiSidebarPadding = finalPhysicalSidebarPadding / totalGameScale;
        float guiSidebarWidth = finalPhysicalSidebarWidth / totalGameScale, guiSidebarRadius = finalPhysicalSidebarRadius / totalGameScale;
        float guiLogoPadding = finalPhysicalLogoPadding / totalGameScale;
        GuiHelper.drawAntiAliasedRoundedRect(guiLeft, guiTop, guiWidth, guiHeight, guiRadius, 0xFF0D0D0D);
        float guiSidebarLeft = guiLeft + guiSidebarPadding, guiSidebarTop = guiTop + guiSidebarPadding;
        float guiSidebarHeight = guiHeight - (guiSidebarPadding * 2);
        GuiHelper.drawAntiAliasedRoundedRect(guiSidebarLeft, guiSidebarTop, guiSidebarWidth, guiSidebarHeight, guiSidebarRadius, MAIN_CONTENT_COLOR);
        float logoContainerWidth = guiSidebarWidth;
        float iconX = guiSidebarLeft + guiLogoPadding, iconY = guiSidebarTop + guiLogoPadding;
        float iconSize = logoContainerWidth - (guiLogoPadding * 2);
        if (iconSize > 0) GuiHelper.drawTexture(new ResourceLocation(NbtToolsMod.MODID, "textures/gui/logo.png"), (int)iconX, (int)iconY, (int)iconSize, (int)iconSize);
        float mainContentGap = guiSidebarPadding;
        float mainContentLeft = guiSidebarLeft + guiSidebarWidth + mainContentGap, mainContentTop = guiSidebarTop;
        float mainContentWidth = guiWidth - (guiSidebarPadding * 3) - guiSidebarWidth;
        float mainContentHeight = guiSidebarHeight;
        GuiHelper.drawAntiAliasedRoundedRect(mainContentLeft, mainContentTop, mainContentWidth, mainContentHeight, guiSidebarRadius, MAIN_CONTENT_COLOR);
        String titleText = "BedWars Helper";
        float dynamicTitleScale = (TITLE_SCALE * finalContentScale) / totalGameScale;
        float titleTextWidth = textRenderer.getStringWidth(titleText) * dynamicTitleScale;
        float titleX = mainContentLeft + (mainContentWidth - titleTextWidth) / 2.0f;
        float titleY = mainContentTop + mainContentGap;
        GlStateManager.pushMatrix();
        GlStateManager.translate(titleX, titleY, 0);
        GlStateManager.scale(dynamicTitleScale, dynamicTitleScale, 1);
        textRenderer.drawStringWithShadow(titleText, 0, 0, TEXT_COLOR_PRIMARY);
        GlStateManager.popMatrix();
        float currentY = titleY + (textRenderer.FONT_HEIGHT * dynamicTitleScale) + mainContentGap;
        float optionHeight = 16f;
        chkbox_size = 12f;
        float labelX = mainContentLeft + mainContentGap;
        float checkboxX = mainContentLeft + mainContentWidth - mainContentGap - chkbox_size;

        drawCheckboxOption(textRenderer, "Show Visual Region", ConfigManager.showRegion, labelX, checkboxX, currentY, chkbox_size);
        chkbox1_x = checkboxX; chkbox1_y = currentY;
        currentY += optionHeight;
        drawCheckboxOption(textRenderer, "Show Intruder Alerts", ConfigManager.showIntruderAlerts, labelX, checkboxX, currentY, chkbox_size);
        chkbox2_x = checkboxX; chkbox2_y = currentY;
        currentY += optionHeight;
        drawCheckboxOption(textRenderer, "Show TNT Alerts", ConfigManager.showTntAlerts, labelX, checkboxX, currentY, chkbox_size);
        chkbox3_x = checkboxX; chkbox3_y = currentY;
        currentY += optionHeight;
        drawCheckboxOption(textRenderer, "Show Fireball Alerts", ConfigManager.showFireballAlerts, labelX, checkboxX, currentY, chkbox_size);
        chkbox4_x = checkboxX; chkbox4_y = currentY;
        currentY += optionHeight;
        drawCheckboxOption(textRenderer, "Show Teammate Alerts", ConfigManager.showTeammateAlerts, labelX, checkboxX, currentY, chkbox_size);
        chkbox5_x = checkboxX; chkbox5_y = currentY;
        currentY += optionHeight;
        drawCheckboxOption(textRenderer, "Show Item Holograms", ConfigManager.showHolograms, labelX, checkboxX, currentY, chkbox_size);
        chkbox6_x = checkboxX; chkbox6_y = currentY;
        currentY += optionHeight;
        drawCheckboxOption(textRenderer, "Show Join Alerts", ConfigManager.showJoinAlerts, labelX, checkboxX, currentY, chkbox_size);
        chkbox7_x = checkboxX; chkbox7_y = currentY;
        currentY += optionHeight;
        drawCheckboxOption(textRenderer, "Show Player Messages", ConfigManager.showPlayerMessages, labelX, checkboxX, currentY, chkbox_size);
        chkbox8_x = checkboxX; chkbox8_y = currentY;
        currentY += optionHeight;
        drawCheckboxOption(textRenderer, "Show Team HUD", ConfigManager.showTeamHud, labelX, checkboxX, currentY, chkbox_size);
        chkbox9_x = checkboxX; chkbox9_y = currentY;
        currentY += optionHeight;

        drawCheckboxOption(textRenderer, "Enable Victory Message", ConfigManager.enableVictoryMessage, labelX, checkboxX, currentY, chkbox_size);
        chkbox10_x = checkboxX; chkbox10_y = currentY;

        currentY += optionHeight;
        float labelY = currentY + (chkbox_size - textRenderer.FONT_HEIGHT) / 2.0f;
        textRenderer.drawStringWithShadow("Message:", labelX, labelY, TEXT_COLOR_PRIMARY);

        this.victoryMessageField.xPosition = (int)(mainContentLeft + mainContentWidth / 2f - 20);
        this.victoryMessageField.yPosition = (int)currentY;
        this.victoryMessageField.drawTextBox();

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void drawCheckboxOption(FontRenderer fr, String label, boolean isChecked, float labelX, float checkboxX, float y, float size) {
        float labelY = y + (size - fr.FONT_HEIGHT) / 2.0f;
        fr.drawStringWithShadow(label, labelX, labelY, TEXT_COLOR_PRIMARY);
        GuiHelper.drawAntiAliasedRoundedRect(checkboxX, y, size, size, 3, CHECKBOX_BG_COLOR);
        if (isChecked) {
            GlStateManager.pushMatrix();
            GlStateManager.disableTexture2D();
            GL11.glLineWidth(2.0f);
            GlStateManager.color(((CHECKMARK_COLOR >> 16) & 0xFF) / 255.0f, ((CHECKMARK_COLOR >> 8) & 0xFF) / 255.0f, (CHECKMARK_COLOR & 0xFF) / 255.0f);
            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex2f(checkboxX + size * 0.25f, y + size * 0.5f);
            GL11.glVertex2f(checkboxX + size * 0.45f, y + size * 0.75f);
            GL11.glVertex2f(checkboxX + size * 0.45f, y + size * 0.75f);
            GL11.glVertex2f(checkboxX + size * 0.75f, y + size * 0.3f);
            GL11.glEnd();
            GlStateManager.color(1,1,1,1);
            GlStateManager.enableTexture2D();
            GlStateManager.popMatrix();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        float centerX = this.width / 2.0f, centerY = this.height / 2.0f;
        float transformedMouseX = (mouseX - centerX) / this.currentAnimationScale + centerX;
        float transformedMouseY = (mouseY - centerY) / this.currentAnimationScale + centerY;

        this.victoryMessageField.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 0) {
            if (isMouseOver(transformedMouseX, transformedMouseY, chkbox1_x, chkbox1_y, chkbox_size, chkbox_size)) { ConfigManager.setShowRegion(!ConfigManager.showRegion); playClickSound(); }
            if (isMouseOver(transformedMouseX, transformedMouseY, chkbox2_x, chkbox2_y, chkbox_size, chkbox_size)) { ConfigManager.setShowIntruderAlerts(!ConfigManager.showIntruderAlerts); playClickSound(); }
            if (isMouseOver(transformedMouseX, transformedMouseY, chkbox3_x, chkbox3_y, chkbox_size, chkbox_size)) { ConfigManager.setShowTntAlerts(!ConfigManager.showTntAlerts); playClickSound(); }
            if (isMouseOver(transformedMouseX, transformedMouseY, chkbox4_x, chkbox4_y, chkbox_size, chkbox_size)) { ConfigManager.setShowFireballAlerts(!ConfigManager.showFireballAlerts); playClickSound(); }
            if (isMouseOver(transformedMouseX, transformedMouseY, chkbox5_x, chkbox5_y, chkbox_size, chkbox_size)) { ConfigManager.setShowTeammateAlerts(!ConfigManager.showTeammateAlerts); playClickSound(); }
            if (isMouseOver(transformedMouseX, transformedMouseY, chkbox6_x, chkbox6_y, chkbox_size, chkbox_size)) { ConfigManager.setShowHolograms(!ConfigManager.showHolograms); playClickSound(); }
            if (isMouseOver(transformedMouseX, transformedMouseY, chkbox7_x, chkbox7_y, chkbox_size, chkbox_size)) { ConfigManager.setShowJoinAlerts(!ConfigManager.showJoinAlerts); playClickSound(); }
            if (isMouseOver(transformedMouseX, transformedMouseY, chkbox8_x, chkbox8_y, chkbox_size, chkbox_size)) { ConfigManager.setShowPlayerMessages(!ConfigManager.showPlayerMessages); playClickSound(); }
            if (isMouseOver(transformedMouseX, transformedMouseY, chkbox9_x, chkbox9_y, chkbox_size, chkbox_size)) { ConfigManager.setShowTeamHud(!ConfigManager.showTeamHud); playClickSound(); }
            if (isMouseOver(transformedMouseX, transformedMouseY, chkbox10_x, chkbox10_y, chkbox_size, chkbox_size)) { ConfigManager.setEnableVictoryMessage(!ConfigManager.enableVictoryMessage); playClickSound(); }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private boolean isMouseOver(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.victoryMessageField.isFocused()) {
            this.victoryMessageField.textboxKeyTyped(typedChar, keyCode);
            ConfigManager.setVictoryMessage(this.victoryMessageField.getText());
        } else if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}