package xyz.lyki.friendguard.KeyUtils;

import java.util.ArrayList;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;
import xyz.lyki.friendguard.FriendGuard;
import xyz.lyki.friendguard.FriendGuardClient;

public class AddRemovePlayer {
    public static final String KEY_PLAYERADDREMOVE = "Add/Remove Players to Protected List";
    public static KeyBinding playerAddRemoveKey;
    public static ArrayList<String> playerList = FriendGuard.ProtectedPlayers;
    public static boolean isModEnabled;

    public static void registerKeyInputs() {
        ClientTickEvents.END_CLIENT_TICK.register((EndTick) client -> {
            if (playerAddRemoveKey.wasPressed()) {
                if (isModEnabled) {
                    if (client.crosshairTarget != null && client.crosshairTarget.getType() == Type.ENTITY) {
                        Entity entity = ((EntityHitResult) client.crosshairTarget).getEntity();
                        if (entity instanceof PlayerEntity) {
                            String playerName = entity.getName().getString();
                            if (!playerList.contains(playerName)) {
                                playerList.add(playerName);
                                String message = FriendGuardClient.messages.get("isNowAProtectedPlayer");
                                String playername = Formatting.GREEN + playerName;
                                client.inGameHud.getChatHud().addMessage(
                                    Text.literal(playername + Formatting.WHITE + message).formatted(new Formatting[]{Formatting.WHITE, Formatting.ITALIC})
                                );
                            } else {
                                playerList.remove(playerName);
                                String message = FriendGuardClient.messages.get("isNoLongerAProtectedPlayer");
                                String playername = Formatting.RED + playerName;
                                client.inGameHud.getChatHud().addMessage(
                                    Text.literal(playername + Formatting.WHITE + message).formatted(new Formatting[]{Formatting.WHITE, Formatting.ITALIC})
                                );
                            }
                        } else {
                            String errorMessage = FriendGuardClient.messages.get("notLookingAPlayer");
                            client.inGameHud.getChatHud().addMessage(Text.literal(errorMessage).formatted(Formatting.RED));
                        }
                    } else {
                        String errorMessage = FriendGuardClient.messages.get("notLookingAnything");
                        client.inGameHud.getChatHud().addMessage(Text.literal(errorMessage).formatted(Formatting.RED));
                    }
                } else {
                    String errorMessage = FriendGuardClient.messages.get("FriendGuardDisabled");
                    client.inGameHud.getChatHud().addMessage(Text.literal(errorMessage).formatted(Formatting.RED));
                }
            }
        });
    }

    public static void register() {
        playerAddRemoveKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("Add/Remove Players to Protected List", net.minecraft.client.util.InputUtil.Type.KEYSYM, 296, Category.MISC)
        );
        registerKeyInputs();
    }
}
