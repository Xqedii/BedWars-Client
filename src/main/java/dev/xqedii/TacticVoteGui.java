package dev.xqedii.gui;

import dev.xqedii.NbtToolsMod;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

public class TacticVoteGui extends GuiScreen {

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.add(new GuiButton(0, this.width / 2 - 100, this.height / 2 - 24, "TNT Jump"));
        this.buttonList.add(new GuiButton(1, this.width / 2 - 100, this.height / 2 + 4, "Bridge"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.enabled) {
            if (button.id == 0) {
                NbtToolsMod.gameEventHandler.castVote("TNT_JUMP");
            } else if (button.id == 1) {
                NbtToolsMod.gameEventHandler.castVote("BRIDGE");
            }
            mc.displayGuiScreen(null);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, "Choose a Tactic", this.width / 2, this.height / 2 - 50, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}