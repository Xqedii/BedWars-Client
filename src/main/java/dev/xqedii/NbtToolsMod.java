package dev.xqedii;

import dev.xqedii.command.BWBaseCommand;
import dev.xqedii.command.BWMessageCommand;
import dev.xqedii.command.BWPartyCommand;
import dev.xqedii.gui.CustomGuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = NbtToolsMod.MODID, name = "BedWars Helper", version = "1.0")
public class NbtToolsMod {

    public static final String MODID = "nbtools";
    private static KeyBinding openGuiKey;

    private Minecraft mc;

    public static GameEventHandler gameEventHandler;

    private boolean guiKeyPressed = false;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ConfigManager.init(event.getSuggestedConfigurationFile());

        ClientCommandHandler.instance.registerCommand(new BWBaseCommand());
        ClientCommandHandler.instance.registerCommand(new BWMessageCommand());
        ClientCommandHandler.instance.registerCommand(new BWPartyCommand());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        this.mc = FMLClientHandler.instance().getClient();

        MinecraftForge.EVENT_BUS.register(this);

        gameEventHandler = new GameEventHandler(this.mc);
        MinecraftForge.EVENT_BUS.register(gameEventHandler);

        openGuiKey = new KeyBinding(
                "key.nbtools.open_gui",
                Keyboard.KEY_RSHIFT,
                "key.category.nbtools"
        );
        ClientRegistry.registerKeyBinding(openGuiKey);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (Keyboard.isKeyDown(openGuiKey.getKeyCode())) {
            if (!this.guiKeyPressed) {
                if (mc.currentScreen == null) {
                    if (gameEventHandler.isTacticVotingActive()) {
                        gameEventHandler.openTacticGui();
                    } else {
                        mc.displayGuiScreen(new CustomGuiScreen());
                    }
                }
                this.guiKeyPressed = true;
            }
        } else {
            this.guiKeyPressed = false;
        }
    }
}