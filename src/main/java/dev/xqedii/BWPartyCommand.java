package dev.xqedii.command;

import dev.xqedii.NbtToolsMod;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class BWPartyCommand extends CommandBase {
    @Override
    public String getCommandName() {
        return "bw-party";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/bw-party";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        NbtToolsMod.gameEventHandler.startPartyInviteSequence();
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Sending party invites to all connected clients..."));
    }
}