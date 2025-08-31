package dev.xqedii.command;

import dev.xqedii.NbtToolsMod;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class BWBaseCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "bw-base";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/bw-base";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        NbtToolsMod.gameEventHandler.triggerGameStartSequence();

        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "BedWars Helper sequence manually triggered."));
    }
}