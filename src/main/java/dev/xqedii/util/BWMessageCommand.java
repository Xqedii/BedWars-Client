package dev.xqedii.command;

import dev.xqedii.NbtToolsMod;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import org.apache.commons.lang3.StringUtils;

public class BWMessageCommand extends CommandBase {
    @Override
    public String getCommandName() {
        return "bw-mess";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/bw-mess <message>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: " + getCommandUsage(sender)));
            return;
        }

        String message = StringUtils.join(args, " ");
        NbtToolsMod.gameEventHandler.sendMessage(message);
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Message sent to other clients."));
    }
}