package dev.xqedii;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class WebSocketManager extends WebSocketClient {

    // IP for xqedii.dev server (w port 3500)
    // Replace this with your IP if you want to run server.js on your VPS or smth
    private static final String SERVER_URI = "ws://5.75.146.59:3500";
    private final GameEventHandler eventHandler;
    private final Gson gson = new Gson();
    private final Minecraft mc;

    private Timer reconnectTimer;
    private boolean isReconnecting = false;
    private static final long RECONNECT_INTERVAL_MS = 5000;

    public WebSocketManager(GameEventHandler handler, Minecraft mc) throws URISyntaxException {
        super(new URI(SERVER_URI));
        this.eventHandler = handler;
        this.mc = mc;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("[BW-Helper] Connected to WebSocket server.");
        if (isReconnecting) {
            System.out.println("[BW-Helper] Reconnect successful.");
            isReconnecting = false;
            if (reconnectTimer != null) {
                reconnectTimer.cancel();
                reconnectTimer = null;
            }
        }
        sendJoinEvent();
    }

    @Override
    public void onMessage(String message) {
        this.mc.addScheduledTask(() -> {
            try {
                JsonObject data = gson.fromJson(message, JsonObject.class);
                String type = data.get("type").getAsString();
                eventHandler.handleIncomingEvent(type, data);
            } catch (Exception e) {
                System.err.println("[BW-Helper] Failed to process incoming message: " + message);
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("[BW-Helper] Disconnected. Reason: " + reason);
        if (!isReconnecting) {
            startReconnectTimer();
        }
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("[BW-Helper] WebSocket error: " + ex.getMessage());
        if (!isReconnecting && !isOpen()) {
            startReconnectTimer();
        }
    }

    private void startReconnectTimer() {
        System.out.println("[BW-Helper] Attempting to reconnect in " + (RECONNECT_INTERVAL_MS / 1000) + " seconds...");
        isReconnecting = true;
        if (reconnectTimer != null) {
            reconnectTimer.cancel();
        }
        reconnectTimer = new Timer("WebSocket-Reconnect-Timer");
        reconnectTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!isOpen()) {
                    System.out.println("[BW-Helper] Reconnecting...");
                    try {
                        reconnect();
                    } catch (Exception e) {
                        System.err.println("[BW-Helper] Reconnect attempt failed: " + e.getMessage());
                    }
                }
            }
        }, RECONNECT_INTERVAL_MS);
    }

    public void sendJoinEvent() {
        if (this.mc.getSession() == null || this.mc.getSession().getUsername() == null) return;
        String username = this.mc.getSession().getUsername();
        JsonObject json = new JsonObject();
        json.addProperty("type", "join");
        json.addProperty("player", username);
        send(json.toString());
    }

    public void sendHealthUpdate(float health, float maxHealth) {
        if (this.mc.getSession() == null || this.mc.getSession().getUsername() == null) return;
        JsonObject json = new JsonObject();
        json.addProperty("type", "health_update");
        json.addProperty("player", this.mc.getSession().getUsername());
        json.addProperty("health", health);
        json.addProperty("maxHealth", maxHealth);
        send(json.toString());
    }

    public void sendResourceUpdate(Map<String, Integer> resources) {
        if (this.mc.getSession() == null || this.mc.getSession().getUsername() == null) return;
        JsonObject json = new JsonObject();
        json.addProperty("type", "resource_update");
        json.addProperty("player", this.mc.getSession().getUsername());
        JsonObject resourcesJson = new JsonObject();
        for (Map.Entry<String, Integer> entry : resources.entrySet()) {
            resourcesJson.addProperty(entry.getKey(), entry.getValue());
        }
        json.add("resources", resourcesJson);
        send(json.toString());
    }

    public void sendPartyInviteEvent() {
        if (this.mc.getSession() == null || this.mc.getSession().getUsername() == null) return;
        String inviterName = this.mc.getSession().getUsername();
        JsonObject json = new JsonObject();
        json.addProperty("type", "party_invite");
        json.addProperty("inviter", inviterName);
        send(json.toString());
    }

    public void sendTacticVote(String tactic) {
        if (this.mc.getSession() == null || this.mc.getSession().getUsername() == null) return;
        JsonObject json = new JsonObject();
        json.addProperty("type", "tactic_vote");
        json.addProperty("player", this.mc.getSession().getUsername());
        json.addProperty("tactic", tactic);
        send(json.toString());
    }

    public void sendMessageEvent(String message) {
        if (this.mc.getSession() == null || this.mc.getSession().getUsername() == null) return;
        JsonObject json = new JsonObject();
        json.addProperty("type", "message");
        json.addProperty("player", this.mc.getSession().getUsername());
        json.addProperty("message", message);
        send(json.toString());
    }

    public void sendIntruderEvent(EntityPlayer player, EnumChatFormatting color) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "intruder");
        json.addProperty("player", player.getName());
        json.addProperty("color", color.name());
        send(json.toString());
    }

    public void sendTntEvent(EntityPlayer closestPlayer, int entityId, EnumChatFormatting color) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "tnt");
        json.addProperty("player", closestPlayer != null ? closestPlayer.getName() : "Unknown");
        json.addProperty("entityId", entityId);
        json.addProperty("color", color.name());
        send(json.toString());
    }

    public void sendFireballEvent(EntityPlayer closestPlayer, int entityId, EnumChatFormatting color) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "fireball");
        json.addProperty("player", closestPlayer != null ? closestPlayer.getName() : "Unknown");
        json.addProperty("entityId", entityId);
        json.addProperty("color", color.name());
        send(json.toString());
    }
}