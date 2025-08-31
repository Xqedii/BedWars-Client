package dev.xqedii;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.xqedii.gui.TacticVoteGui;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public class GameEventHandler {

    private enum TacticState {
        INACTIVE, VOTING, WAITING_FOR_RESOURCES, EXECUTED, FAILED
    }

    private final Minecraft mc;
    private final RegionManager regionManager = new RegionManager();
    private final ToastManager toastManager;
    private WebSocketManager webSocketManager;
    private final TeamHudManager teamHudManager;

    private boolean isGameActive = false;
    private Vec3 centerPos;
    private final Set<UUID> teammates = new HashSet<>();
    private final Set<UUID> playersCurrentlyInRegion = new HashSet<>();

    private long lastDetectionTime = 0;
    private static final long DETECTION_INTERVAL_MS = 100;
    private static final double REGION_SIZE = 80.0;

    private final List<HologramInfo> activeHolograms = new ArrayList<>();
    private boolean pendingTeammateScan = false;
    private long gracePeriodEndTime = 0;
    private int emptyMessageCounter = 0;

    private final Set<String> connectedUsers = new HashSet<>();
    private final Queue<String> commandQueue = new LinkedList<>();
    private long lastCommandSentTime = 0;
    private static final long COMMAND_INTERVAL_MS = 100;

    private String pendingDelayedCommand = null;
    private long delayedCommandTime = 0;

    private long lastHealthUpdateTime = 0;
    private static final long HEALTH_UPDATE_INTERVAL_MS = 250;

    private long lastResourceUpdateTime = 0;
    private static final long RESOURCE_UPDATE_INTERVAL_MS = 500;

    private TacticState tacticState = TacticState.INACTIVE;
    private long tacticVoteEndTime = 0;
    private long tacticFailTime = 0;
    private String chosenTactic = null;
    private final Map<String, String> tacticVotes = new HashMap<>();
    private int connectedClientCountForVote = 0;

    private final LinkedList<String> chatHistory = new LinkedList<>();


    public GameEventHandler(Minecraft mc) {
        this.mc = mc;
        this.toastManager = new ToastManager(mc);
        this.teamHudManager = new TeamHudManager(mc);

        try {
            webSocketManager = new WebSocketManager(this, mc);
            webSocketManager.connect();
        } catch (Exception e) {
            System.err.println("[BW-Helper] Failed to initialize WebSocket connection.");
            e.printStackTrace();
        }
    }

    public boolean isTacticVotingActive() {
        return tacticState == TacticState.VOTING;
    }

    public void openTacticGui() {
        if (isTacticVotingActive() && !tacticVotes.containsKey(mc.getSession().getUsername())) {
            mc.displayGuiScreen(new TacticVoteGui());
        }
    }

    public void castVote(String tactic) {
        if (webSocketManager != null && webSocketManager.isOpen()) {
            webSocketManager.sendTacticVote(tactic);
        }
    }

    private void endVoting() {
        if (tacticState != TacticState.VOTING) return;

        int tntVotes = 0;
        int bridgeVotes = 0;
        for (String vote : tacticVotes.values()) {
            if ("TNT_JUMP".equals(vote)) tntVotes++;
            else if ("BRIDGE".equals(vote)) bridgeVotes++;
        }

        if (tntVotes > bridgeVotes) {
            chosenTactic = "TNT Jump";
            tacticState = TacticState.WAITING_FOR_RESOURCES;
            toastManager.showTacticChosenToast(chosenTactic);
        } else if (bridgeVotes > tntVotes) {
            chosenTactic = "Bridge";
            tacticState = TacticState.WAITING_FOR_RESOURCES;
            toastManager.showTacticChosenToast(chosenTactic);
        } else {
            chosenTactic = null;
            tacticState = TacticState.FAILED;
        }
    }

    private void executeTactic() {
        if (mc.thePlayer == null) return;

        Item itemToDrop = null;
        if ("TNT Jump".equals(chosenTactic)) {
            itemToDrop = Items.gold_ingot;
        } else if ("Bridge".equals(chosenTactic)) {
            itemToDrop = Items.iron_ingot;
        }

        if (itemToDrop != null) {
            int originalSlot = mc.thePlayer.inventory.currentItem;
            boolean itemDropped = false;

            for (int i = 0; i < 36; i++) {
                ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
                if (stack != null && stack.getItem() == itemToDrop) {
                    int slotToUse = (i < 9) ? i : originalSlot;

                    if (i >= 9) {
                        mc.playerController.windowClick(0, i, slotToUse, 2, mc.thePlayer);
                    }

                    mc.thePlayer.inventory.currentItem = slotToUse;
                    mc.thePlayer.dropOneItem(true);
                    itemDropped = true;

                    if (i >= 9) {
                        mc.playerController.windowClick(0, i, slotToUse, 2, mc.thePlayer);
                    }

                    mc.thePlayer.inventory.currentItem = originalSlot;
                    break;
                }
            }

            if (itemDropped) {
                toastManager.showTacticSuccessToast(chosenTactic);
                tacticState = TacticState.EXECUTED;
            }
        }
    }

    public void startPartyInviteSequence() {
        if (webSocketManager != null && webSocketManager.isOpen()) {
            webSocketManager.sendPartyInviteEvent();
        }
    }

    private static class HologramInfo {
        Vec3 position;
        String[] lines;
        HologramInfo(Vec3 pos, String... text) { this.position = pos; this.lines = text; }
    }

    private void resetGameState() {
        isGameActive = false;
        teammates.clear();
        playersCurrentlyInRegion.clear();
        pendingTeammateScan = false;
        regionManager.stopVisualSequence();
        toastManager.show("Game Ended", "Detection disabled", EnumChatFormatting.YELLOW, 2000L);
        tacticState = TacticState.INACTIVE;
        tacticVotes.clear();
        chosenTactic = null;
    }

    public void triggerGameStartSequence() {
        if (mc.thePlayer != null) {
            isGameActive = true;
            centerPos = mc.thePlayer.getPositionVector();
            teammates.clear();
            playersCurrentlyInRegion.clear();
            teammates.add(mc.thePlayer.getUniqueID());
            pendingTeammateScan = true;
            gracePeriodEndTime = System.currentTimeMillis() + 1000;
            toastManager.show("Game Started!", "Good Luck!", EnumChatFormatting.GREEN, 2000L);
            regionManager.startVisualSequence(centerPos);
            teamHudManager.clearHealthData();
            teamHudManager.clearResourceData();

            tacticState = TacticState.VOTING;
            tacticVoteEndTime = System.currentTimeMillis() + 15000;
            tacticFailTime = System.currentTimeMillis() + 45000;
            tacticVotes.clear();
            chosenTactic = null;
            connectedClientCountForVote = connectedUsers.size();
            toastManager.showTacticVoteStartToast();
        }
    }

    public void sendMessage(String message) {
        if (webSocketManager != null && webSocketManager.isOpen()) {
            webSocketManager.sendMessageEvent(message);
        }
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (event.type == 2) return;
        String uncoloredMessage = event.message.getUnformattedText().trim();

        chatHistory.add(uncoloredMessage);
        if (chatHistory.size() > 5) {
            chatHistory.removeFirst();
        }

        if (ConfigManager.enableVictoryMessage && uncoloredMessage.startsWith("Top 1 - ") && chatHistory.size() >= 3) {
            String potentialPlayerNameLine = chatHistory.get(chatHistory.size() - 3);
            if (potentialPlayerNameLine.equals(mc.thePlayer.getName())) {
                mc.thePlayer.sendChatMessage(ConfigManager.victoryMessage);
            }
        }

        if (uncoloredMessage.isEmpty()) {
            emptyMessageCounter++;
        } else {
            emptyMessageCounter = 0;
        }
        if (emptyMessageCounter >= 20 || uncoloredMessage.contains("Top 1 - ")) {
            if (isGameActive) resetGameState();
            return;
        }
        if (uncoloredMessage.contains("Good luck with your BedWars Game") || uncoloredMessage.contains("Chron swoje lozko i niszcz je innym")) {
            triggerGameStartSequence();
        } else if (uncoloredMessage.contains("World shrinking started")) {
            toastManager.show("World shrinking started!", "", EnumChatFormatting.RED, 2000L);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        toastManager.update();

        if (!commandQueue.isEmpty() && System.currentTimeMillis() - lastCommandSentTime > COMMAND_INTERVAL_MS) {
            if (mc.thePlayer != null) {
                mc.thePlayer.sendChatMessage(commandQueue.poll());
                lastCommandSentTime = System.currentTimeMillis();
            }
        }

        if (pendingDelayedCommand != null && System.currentTimeMillis() >= delayedCommandTime) {
            if (mc.thePlayer != null) {
                mc.thePlayer.sendChatMessage(pendingDelayedCommand);
            }
            pendingDelayedCommand = null;
        }

        long now = System.currentTimeMillis();

        if (tacticState == TacticState.VOTING && now > tacticVoteEndTime) {
            endVoting();
        }

        if (tacticState == TacticState.WAITING_FOR_RESOURCES) {
            if (now > tacticFailTime) {
                tacticState = TacticState.FAILED;
                toastManager.showTacticFailedToast(chosenTactic);
            } else {
                int totalIron = 0;
                int totalGold = 0;
                for (UUID teammateUUID : teammates) {
                    EntityPlayer p = mc.theWorld.getPlayerEntityByUUID(teammateUUID);
                    if (p != null) {
                        totalIron += teamHudManager.getResourceCount(p, Items.iron_ingot);
                        totalGold += teamHudManager.getResourceCount(p, Items.gold_ingot);
                    }
                }

                if ("TNT Jump".equals(chosenTactic) && totalGold >= 8) {
                    executeTactic();
                } else if ("Bridge".equals(chosenTactic) && totalIron >= 32) {
                    executeTactic();
                }
            }
        }

        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (isGameActive && now - lastHealthUpdateTime > HEALTH_UPDATE_INTERVAL_MS) {
            webSocketManager.sendHealthUpdate(mc.thePlayer.getHealth(), mc.thePlayer.getMaxHealth());
            lastHealthUpdateTime = now;
        }

        if (isGameActive && now - lastResourceUpdateTime > RESOURCE_UPDATE_INTERVAL_MS) {
            Map<String, Integer> resources = new HashMap<>();
            resources.put("iron", teamHudManager.countItems(mc.thePlayer, Items.iron_ingot));
            resources.put("gold", teamHudManager.countItems(mc.thePlayer, Items.gold_ingot));
            resources.put("diamond", teamHudManager.countItems(mc.thePlayer, Items.diamond));
            resources.put("emerald", teamHudManager.countItems(mc.thePlayer, Items.emerald));
            webSocketManager.sendResourceUpdate(resources);
            lastResourceUpdateTime = now;
        }

        regionManager.update();

        if (pendingTeammateScan && now > gracePeriodEndTime) {
            findTeammates(mc.theWorld, toastManager);
            pendingTeammateScan = false;
        }

        if (ConfigManager.showHolograms) {
            activeHolograms.clear();
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (entity instanceof EntityItem) {
                    EntityItem itemEntity = (EntityItem) entity;
                    ItemStack stack = itemEntity.getEntityItem();
                    BlockPos blockBelowPos = new BlockPos(itemEntity).down();
                    Block blockBelow = mc.theWorld.getBlockState(blockBelowPos).getBlock();
                    if (stack.getItem() == Items.diamond && blockBelow == Blocks.diamond_block) {
                        activeHolograms.add(new HologramInfo(new Vec3(itemEntity.posX, itemEntity.posY + 3.0, itemEntity.posZ), "&bDiamond", "&fx" + stack.stackSize));
                    } else if (stack.getItem() == Items.emerald && blockBelow == Blocks.emerald_block) {
                        activeHolograms.add(new HologramInfo(new Vec3(itemEntity.posX, itemEntity.posY + 3.0, itemEntity.posZ), "&aEmerald", "&fx" + stack.stackSize));
                    }
                }
            }
        } else {
            activeHolograms.clear();
        }

        if (isGameActive && now - lastDetectionTime > DETECTION_INTERVAL_MS) {
            detectThreats(mc.theWorld, toastManager);
            lastDetectionTime = now;
        }
    }

    private void findTeammates(World world, ToastManager toastManager) {
        if (centerPos == null) return;
        AxisAlignedBB teammateBox = new AxisAlignedBB(centerPos.xCoord - 10, centerPos.yCoord - 10, centerPos.zCoord - 10, centerPos.xCoord + 10, centerPos.yCoord + 10, centerPos.zCoord + 10);
        for (EntityPlayer player : world.getEntitiesWithinAABB(EntityPlayer.class, teammateBox)) {
            if (!teammates.contains(player.getUniqueID())) {
                toastManager.showTeammateAddedToast(player);
                teammates.add(player.getUniqueID());
            }
        }
    }

    private void detectThreats(World world, ToastManager toastManager) {
        if (System.currentTimeMillis() < gracePeriodEndTime) return;
        AxisAlignedBB regionBox = new AxisAlignedBB(centerPos.xCoord - REGION_SIZE / 2, centerPos.yCoord - REGION_SIZE / 2, centerPos.zCoord - REGION_SIZE / 2, centerPos.xCoord + REGION_SIZE / 2, centerPos.yCoord + REGION_SIZE / 2, centerPos.zCoord + REGION_SIZE / 2);

        Set<UUID> playersFoundThisTick = new HashSet<>();
        for (EntityPlayer player : world.getEntitiesWithinAABB(EntityPlayer.class, regionBox)) {
            playersFoundThisTick.add(player.getUniqueID());
            if (!teammates.contains(player.getUniqueID()) && !playersCurrentlyInRegion.contains(player.getUniqueID())) {
                EnumChatFormatting color = toastManager.getPlayerArmorTeamColor(player);
                webSocketManager.sendIntruderEvent(player, color);
            }
        }
        playersCurrentlyInRegion.clear();
        playersCurrentlyInRegion.addAll(playersFoundThisTick);

        for (Entity tnt : world.getEntitiesWithinAABB(EntityTNTPrimed.class, regionBox)) {
            EntityPlayer closestPlayer = world.getClosestPlayerToEntity(tnt, 16.0);
            EnumChatFormatting color = toastManager.getPlayerArmorTeamColor(closestPlayer);
            webSocketManager.sendTntEvent(closestPlayer, tnt.getEntityId(), color);
        }
        for (Entity fireball : world.getEntitiesWithinAABB(EntityFireball.class, regionBox)) {
            EntityPlayer closestPlayer = world.getClosestPlayerToEntity(fireball, 10.0);
            EnumChatFormatting color = toastManager.getPlayerArmorTeamColor(closestPlayer);
            webSocketManager.sendFireballEvent(closestPlayer, fireball.getEntityId(), color);
        }
    }

    public void handleIncomingEvent(String type, JsonObject data) {
        if ("user_list_update".equals(type)) {
            connectedUsers.clear();
            JsonArray users = data.getAsJsonArray("users");
            for (JsonElement userElement : users) {
                connectedUsers.add(userElement.getAsString());
            }
            return;
        }

        if ("join".equals(type)) {
            String playerName = data.get("player").getAsString();
            if (mc.getSession() != null && !playerName.equals(mc.getSession().getUsername())) {
                toastManager.showJoinToast(playerName);
            }
            return;
        }

        if ("party_invite".equals(type)) {
            String inviterName = data.get("inviter").getAsString();
            String myName = mc.getSession().getUsername();
            if (inviterName.equals(myName)) {
                commandQueue.clear();
                for (String user : connectedUsers) {
                    if (!user.equals(myName)) {
                        commandQueue.add("/party invite " + user);
                    }
                }
                toastManager.showPartyCreatedToast();
            } else {
                this.pendingDelayedCommand = "/party accept " + inviterName;
                this.delayedCommandTime = System.currentTimeMillis() + 50;
                toastManager.showPartyJoinedToast(inviterName);
            }
            return;
        }

        if ("health_update".equals(type)) {
            String playerName = data.get("player").getAsString();
            float health = data.get("health").getAsFloat();
            float maxHealth = data.get("maxHealth").getAsFloat();
            teamHudManager.updatePlayerHealth(playerName, health, maxHealth);
            return;
        }

        if ("resource_update".equals(type)) {
            String playerName = data.get("player").getAsString();
            JsonObject resources = data.getAsJsonObject("resources");
            teamHudManager.updatePlayerResources(playerName, resources);
            return;
        }

        if ("tactic_vote".equals(type)) {
            if (tacticState != TacticState.VOTING) return;

            String playerName = data.get("player").getAsString();
            String tactic = data.get("tactic").getAsString();

            tacticVotes.put(playerName, tactic);

            int tntVotes = 0;
            int bridgeVotes = 0;
            for (String vote : tacticVotes.values()) {
                if ("TNT_JUMP".equals(vote)) tntVotes++;
                else if ("BRIDGE".equals(vote)) bridgeVotes++;
            }

            String leadingTacticName;
            if (tntVotes > bridgeVotes) {
                leadingTacticName = "TNT Jump";
            } else if (bridgeVotes > tntVotes) {
                leadingTacticName = "Bridge";
            } else {
                leadingTacticName = "Bridge".equals(tactic) ? "Bridge" : "TNT Jump";
            }

            int currentVotes = tacticVotes.size();
            toastManager.showTacticVoteUpdateToast(leadingTacticName, currentVotes, connectedClientCountForVote);

            if (currentVotes >= connectedClientCountForVote) {
                endVoting();
            }
            return;
        }

        String playerName = data.get("player").getAsString();

        if ("message".equals(type)) {
            String message = data.get("message").getAsString();
            toastManager.showMessageToast(playerName, message);
            return;
        }

        EnumChatFormatting color = EnumChatFormatting.WHITE;
        if (data.has("color")) {
            try {
                color = EnumChatFormatting.valueOf(data.get("color").getAsString());
            } catch (IllegalArgumentException e) {
            }
        }

        if ("intruder".equals(type)) {
            toastManager.showIntruderToast(playerName, color);
        } else if ("tnt".equals(type)) {
            toastManager.showTntToast(playerName, color);
        } else if ("fireball".equals(type)) {
            toastManager.showFireballToast(playerName, color);
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (mc.thePlayer != null) {
            regionManager.render(mc.thePlayer, event.partialTicks);
            if (ConfigManager.showHolograms) {
                for (HologramInfo hologram : activeHolograms) {
                    renderHologram(hologram, event.partialTicks);
                }
            }
        }
    }

    private void renderHologram(HologramInfo hologram, float partialTicks) {
        RenderManager renderManager = mc.getRenderManager();
        FontRenderer fontRenderer = mc.fontRendererObj;
        EntityPlayer player = mc.thePlayer;
        double distance = player.getDistance(hologram.position.xCoord, hologram.position.yCoord, hologram.position.zCoord);
        float maxRenderDistance = 140.0f;
        float scaleTransitionDistance = 6.0f;
        float fadeTransitionDistance = 2.0f;
        if (distance > maxRenderDistance) return;
        float scale;
        float alpha = 1.0f;
        if (distance > scaleTransitionDistance) {
            float maxScale = 7.0f;
            float minScale = 2.0f;
            float progress = (float)((distance - scaleTransitionDistance) / (maxRenderDistance - scaleTransitionDistance));
            scale = minScale + (maxScale - minScale) * progress;
        } else {
            scale = 2.0f;
            if (distance < scaleTransitionDistance) {
                float progress = (float)((distance - fadeTransitionDistance) / (scaleTransitionDistance - fadeTransitionDistance));
                alpha = MathHelper.clamp_float(progress, 0.0f, 1.0f);
            }
        }
        if (alpha <= 0.0f) return;
        double x = hologram.position.xCoord - renderManager.viewerPosX;
        double y = hologram.position.yCoord - renderManager.viewerPosY;
        double z = hologram.position.zCoord - renderManager.viewerPosZ;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-scale * 0.025f, -scale * 0.025f, scale * 0.025f);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        int yOffset = 0;
        for (String line : hologram.lines) {
            String formattedLine = line.replaceAll("&", "\u00a7");
            int textWidth = fontRenderer.getStringWidth(formattedLine);
            int color = 0xFFFFFF;
            int finalColor = ((int)(alpha * 255.0f) << 24) | (color & 0x00FFFFFF);
            fontRenderer.drawString(formattedLine, -textWidth / 2, yOffset, finalColor, true);
            yOffset += fontRenderer.FONT_HEIGHT;
        }
        GlStateManager.enableLighting();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type == RenderGameOverlayEvent.ElementType.TEXT) {
            toastManager.render();
            if (this.isGameActive && ConfigManager.showTeamHud) {
                teamHudManager.render(this.teammates);
            }
        }
    }
}