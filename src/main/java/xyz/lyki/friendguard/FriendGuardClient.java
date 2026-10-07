package xyz.lyki.friendguard;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStarted;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStopping;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.apache.logging.log4j.Logger;
import xyz.lyki.friendguard.KeyUtils.AddRemovePlayer;
import xyz.lyki.friendguard.KeyUtils.ClearList;

public class FriendGuardClient implements ClientModInitializer {
    public static ArrayList<String> ProtectedPlayers = FriendGuard.ProtectedPlayers;
    private static final File configFile = new File("mods/FriendGuard/FriendGuardConfig.json");
    private static final List<String> ALL_PLAYER_NAMES = new ArrayList<>();

    /** Server-side player list populated from ClientPlayNetworkHandler.getPlayerList(). */
    public static List<String> getAllPlayers() {
        return Collections.unmodifiableList(ALL_PLAYER_NAMES);
    }

    /** Selected/tracked set used for the per-player action-bar arrows. */
    public static List<String> getTrackedPlayers() {
        return Collections.unmodifiableList(ProtectedPlayers);
    }

    public static final Logger LOGGER = FriendGuard.LOGGER;
    public static Map<String, String> messages = new HashMap<>();
    public static boolean isModEnabled;
    public static boolean isCompassEnabled;
    private String lastClientLanguage = "";
    private static final SoundEvent DIDGERIDOO_SOUND_EVENT = SoundEvent.of(Identifier.of("minecraft", "block.note_block.didgeridoo"));
    private static final String[] COMPASS_DIRECTIONS = new String[]{"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    @Override
    public void onInitializeClient() {
        isModEnabled = true;
        isCompassEnabled = true;
        AddRemovePlayer.register();
        ClearList.register();
        this.readConfigFile();

        // Tick loop: refresh the server player list and the tracked set every frame
        // so the tracker stays in sync across dimension switches.
        ClientTickEvents.END_CLIENT_TICK.register((EndTick) client -> {
            this.updatePlayerList(client);

            if (client.world != null && client.player != null && MinecraftClient.getInstance().getLanguageManager().getLanguage() != null) {
                String currentLanguage = client.getLanguageManager().getLanguage();
                if (!currentLanguage.equals(this.lastClientLanguage)) {
                    this.lastClientLanguage = currentLanguage;
                    this.loadLanguageMessages(currentLanguage);
                }
            }
        });

        HudRenderCallback.EVENT.register((matrixStack, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null && client.player != null && isModEnabled && isCompassEnabled) {
                List<PlayerTrackerInfo> allPlayers = this.collectAllPlayers(client);
                if (allPlayers.isEmpty()) {
                    return;
                }

                RegistryKey<World> ownDimensionKey = client.world.getRegistryKey();
                BlockPos ownPos = client.player.getBlockPos();

                int protectedCount = 0;
                List<PlayerTrackerInfo> trackedInfos = new ArrayList<>();

                for (PlayerTrackerInfo info : allPlayers) {
                    if (info.player == null) {
                        continue;
                    }

                    // Keep the original "see your friends through walls" behaviour for
                    // every player whose entity we actually have.
                    info.player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 2000, 0, false, false));

                    if (ProtectedPlayers.contains(info.name)) {
                        protectedCount++;
                        if (info.name != null && client.player != null) {
                            trackedInfos.add(info);
                        }
                        continue;
                    }

                    double distance = info.player.getBlockPos().getSquaredDistance(ownPos);
                    if (ProtectedPlayers.contains(info.name)) {
                        trackedInfos.add(info);
                    }
                }

                // Build one arrow per tracked player. Unreachable players (another
                // dimension, or outside entity range) are announced as "in another
                // dimension" and skipped for arrows because the client has no
                // position for them.
                StringBuilder actionBarMessage = new StringBuilder();
                boolean first = true;

                for (PlayerTrackerInfo info : trackedInfos) {
                    if (first) {
                        first = false;
                    } else {
                        actionBarMessage.append(" | ");
                    }

                    if (info.player == null) {
                        actionBarMessage.append(Formatting.GRAY);
                        actionBarMessage.append(info.name);
                        if (info.inOtherDimension) {
                            actionBarMessage.append(Formatting.WHITE).append(" [").append(info.dimensionLabel).append("]");
                        }
                        actionBarMessage.append(Formatting.RED).append(messages.get("inAnotherDimension"));
                        continue;
                    }

                    String direction = this.getCompassSymbol(client.player, info.player);
                    int meters = (int) Math.sqrt(info.player.getBlockPos().getSquaredDistance(ownPos));
                    String suffix = this.buildSuffix(info);

                    actionBarMessage.append(Formatting.GRAY).append(info.name);
                    actionBarMessage.append(Formatting.WHITE).append(" [").append(direction).append("]");
                    actionBarMessage.append(Formatting.WHITE).append(meters).append("m").append(suffix);
                }

                if (actionBarMessage.length() == 0) {
                    return;
                }

                if (protectedCount > 0) {
                    actionBarMessage.insert(0, Formatting.GREEN).append(" (").append(protectedCount).append(")").append(Formatting.RED);
                }

                client.inGameHud.setOverlayMessage(Text.literal(actionBarMessage.toString()).formatted(Formatting.RED), false);
            }
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity instanceof PlayerEntity targetPlayer && ProtectedPlayers.contains(targetPlayer.getName().getString()) && isModEnabled) {
                this.playDidgeridooSound(player);
                return ActionResult.FAIL;
            } else {
                return ActionResult.PASS;
            }
        });

        ClientLifecycleEvents.CLIENT_STARTED.register((ClientStarted) client -> {
            this.loadLanguageMessages(MinecraftClient.getInstance().options.language);
            if (isModEnabled) {
                LOGGER.info(messages.get("friendguardActivated"));
            } else {
                LOGGER.error(messages.get("friendguardDeactiveError"));
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register((ClientStopping) client -> saveConfig());
    }

    /**
     * Collects every player the server has told us about, not just the ones whose
     * entity is currently loaded in the world we are standing in.
     *
     * Players that are present as entities get a direction and a distance. Players
     * that only exist in the player list (another dimension, or outside the range the
     * server streams entities for) are kept too, so they are still announced, but we
     * cannot point at them because the vanilla client is never given their position.
     */
    private List<PlayerTrackerInfo> collectAllPlayers(MinecraftClient client) {
        List<PlayerTrackerInfo> result = new ArrayList<>();
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler == null) {
            return result;
        }

        String ownName = client.player.getName().getString();
        RegistryKey<World> ownDimensionKey = client.world.getRegistryKey();

        for (PlayerListEntry entry : handler.getPlayerList()) {
            if (entry.getProfile() == null) {
                continue;
            }

            String playerName = entry.getProfile().name();
            if (playerName == null || playerName.isEmpty() || playerName.equals(ownName)) {
                continue;
            }

            PlayerEntity entity = client.world.getPlayerAnyDimension(entry.getProfile().id());
            if (entity != null && entity != client.player) {
                result.add(new PlayerTrackerInfo(playerName, entity, false, this.getDimensionLabel(ownDimensionKey)));
            } else {
                result.add(new PlayerTrackerInfo(playerName, null, true, messages.get("dimensionOther")));
            }
        }

        return result;
    }

    /**
     * Updates the cached server player list from ClientPlayNetworkHandler.getPlayerList().
     * ClientPlayNetworkHandler must not be null here (client is connected and has a
     * player list). The method is idempotent: it clears the existing roster, then fills
     * it with a new copy. Only the player names are stored here so the tracker can
     * render the selection GUI even while switching dimensions.
     */
    public void updatePlayerList(MinecraftClient client) {
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler == null) {
            ALL_PLAYER_NAMES.clear();
            return;
        }

        List<String> fresh = new ArrayList<>();
        for (PlayerListEntry entry : handler.getPlayerList()) {
            if (entry.getProfile() == null) {
                continue;
            }

            String playerName = entry.getProfile().name();
            if (playerName == null || playerName.isEmpty()) {
                continue;
            }

            fresh.add(playerName);
        }

        ALL_PLAYER_NAMES.clear();
        ALL_PLAYER_NAMES.addAll(fresh);
    }

    private String buildSuffix(PlayerTrackerInfo info) {
        return info.inOtherDimension ? " [" + info.dimensionLabel + "]" : "";
    }

    private String joinNames(List<PlayerTrackerInfo> players) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < players.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            PlayerTrackerInfo info = players.get(i);
            builder.append(info.name);
            if (info.inOtherDimension) {
                builder.append(" [").append(info.dimensionLabel).append("]");
            }
        }
        return builder.toString();
    }

    private String getDimensionLabel(RegistryKey<World> worldKey) {
        if (World.OVERWORLD.equals(worldKey)) {
            return messages.get("dimensionOverworld");
        }
        if (World.NETHER.equals(worldKey)) {
            return messages.get("dimensionNether");
        }
        if (World.END.equals(worldKey)) {
            return messages.get("dimensionEnd");
        }
        return messages.get("dimensionOther");
    }

    private void loadLanguageMessages(String clientLanguage) {
        messages.clear();
        LOGGER.info("Language set to: " + clientLanguage);
        if ("tr_tr".equals(clientLanguage)) {
            messages.put("friendguardActivated", "No Friendly Fire is active!");
            messages.put(
                "friendguardDeactiveError",
                "FriendGuard yapılandırma ayarları nedeniyle devre dışı bırakıldı. Modu kullanmak için etkinleştirmeniz gerekiyor. ModMenu veya yapılandırma dosyası üzerinden etkinleştirebilirsiniz! lykiaofficial tarafından (https://lyki.dev)"
            );
            messages.put("isNearby", " yakınlarda! ");
            messages.put("closest", " en yakın! ");
            messages.put("morePlayersFound", " oyuncu daha bulundu!");
            messages.put("isNowAProtectedPlayer", " artık korunan bir oyuncu!");
            messages.put("isNoLongerAProtectedPlayer", " artık korunan bir oyuncu değil!");
            messages.put("notLookingAPlayer", "Bir oyuncuya bakmıyorsunuz!");
            messages.put("notLookingAnything", "Hiçbir şeye bakmıyorsunuz!");
            messages.put("listAlreadyEmpty", "Oyuncu listesi zaten boş!");
            messages.put("listCleared", "Oyuncu listesi temizlendi!");
            messages.put("FriendGuardDisabled", "FriendGuard aktif değil!");
            messages.put("compassSettingsLabel", "Pusula Ayarları");
            messages.put("enableCompassButton", "Göster");
            messages.put("disableCompassButton", "Gizle");
            messages.put("isInAnotherDimension", " başka bir boyutta!");
            messages.put("dimensionOverworld", "Overworld");
            messages.put("dimensionNether", "Nether");
            messages.put("dimensionEnd", "End");
            messages.put("dimensionOther", "başka boyut");
        } else if ("fr_fr".equals(clientLanguage)) {
            messages.put("friendguardActivated", "No Friendly Fire is active!");
            messages.put(
                "friendguardDeactiveError",
                "FriendGuard est désactivé en raison des paramètres de configuration. Vous devez l'activer pour utiliser le mod. Vous pouvez l'activer via ModMenu ou le fichier de configuration! par lykiaofficial (https://lyki.dev)"
            );
            messages.put("isNearby", " est à proximité! ");
            messages.put("closest", " le plus proche! ");
            messages.put("morePlayersFound", " joueurs trouvés de plus!");
            messages.put("isNowAProtectedPlayer", " est maintenant un joueur protégé!");
            messages.put("isNoLongerAProtectedPlayer", " n'est plus un joueur protégé!");
            messages.put("notLookingAPlayer", "Vous ne regardez pas un joueur!");
            messages.put("notLookingAnything", "Vous ne regardez rien!");
            messages.put("listAlreadyEmpty", "La liste des joueurs est déjà vide!");
            messages.put("listCleared", "La liste des joueurs a été effacée!");
            messages.put("FriendGuardDisabled", "FriendGuard est désactivé!");
            messages.put("compassSettingsLabel", "Paramètres de la Boussole");
            messages.put("enableCompassButton", "Afficher");
            messages.put("disableCompassButton", "Masquer");
            messages.put("isInAnotherDimension", " est dans une autre dimension!");
            messages.put("dimensionOverworld", "Overworld");
            messages.put("dimensionNether", "Nether");
            messages.put("dimensionEnd", "End");
            messages.put("dimensionOther", "autre dimension");
        } else if ("es_es".equals(clientLanguage)) {
            messages.put("friendguardActivated", "No Friendly Fire is active!");
            messages.put(
                "friendguardDeactiveError",
                "FriendGuard está desactivado debido a la configuración. Necesitas activarlo para usar el mod. Puedes activarlo a través de ModMenu o del archivo de configuración! por lykiaofficial (https://lyki.dev)"
            );
            messages.put("isNearby", " está cerca! ");
            messages.put("closest", " ¡más cercano! ");
            messages.put("morePlayersFound", " jugadores más encontrados!");
            messages.put("isNowAProtectedPlayer", " ahora es un jugador protegido!");
            messages.put("isNoLongerAProtectedPlayer", " ya no es un jugador protegido!");
            messages.put("notLookingAPlayer", "¡No estás mirando a un jugador!");
            messages.put("notLookingAnything", "¡No estás mirando nada!");
            messages.put("listAlreadyEmpty", "¡La lista de jugadores ya está vacía!");
            messages.put("listCleared", "¡La lista de jugadores ha sido limpia!");
            messages.put("FriendGuardDisabled", "¡FriendGuard está desactivado!");
            messages.put("compassSettingsLabel", "Ajustes de la Brújula");
            messages.put("enableCompassButton", "Mostrar");
            messages.put("disableCompassButton", "Ocultar");
            messages.put("isInAnotherDimension", " está en otra dimensión!");
            messages.put("dimensionOverworld", "Overworld");
            messages.put("dimensionNether", "Nether");
            messages.put("dimensionEnd", "End");
            messages.put("dimensionOther", "otra dimensión");
        } else if ("de_de".equals(clientLanguage)) {
            messages.put("friendguardActivated", "No Friendly Fire is active!");
            messages.put(
                "friendguardDeactiveError",
                "FriendGuard ist aufgrund der Konfigurationseinstellungen deaktiviert. Sie müssen es aktivieren, um das Mod zu verwenden. Sie können es über ModMenu oder die Konfigurationsdatei aktivieren! von lykiaofficial (https://lyki.dev)"
            );
            messages.put("isNearby", " ist in der Nähe! ");
            messages.put("closest", " am nächsten! ");
            messages.put("morePlayersFound", " weitere Spieler gefunden!");
            messages.put("isNowAProtectedPlayer", " ist jetzt ein geschützter Spieler!");
            messages.put("isNoLongerAProtectedPlayer", " ist kein geschützter Spieler mehr!");
            messages.put("notLookingAPlayer", "Du schaust keinen Spieler an!");
            messages.put("notLookingAnything", "Du schaust nichts an!");
            messages.put("listAlreadyEmpty", "Spielerliste ist bereits leer!");
            messages.put("listCleared", "Spielerliste wurde geleert!");
            messages.put("FriendGuardDisabled", "FriendGuard ist deaktiviert!");
            messages.put("compassSettingsLabel", "Kompass-Einstellungen");
            messages.put("enableCompassButton", "Anzeigen");
            messages.put("disableCompassButton", "Verstecken");
            messages.put("isInAnotherDimension", " ist in einer anderen Dimension!");
            messages.put("dimensionOverworld", "Overworld");
            messages.put("dimensionNether", "Nether");
            messages.put("dimensionEnd", "End");
            messages.put("dimensionOther", "andere Dimension");
        } else if ("pt_br".equals(clientLanguage)) {
            messages.put("friendguardActivated", "No Friendly Fire is active!");
            messages.put(
                "friendguardDeactiveError",
                "FriendGuard está desativado devido às configurações de configuração. Você precisa ativá-lo para usar o mod. Você pode ativá-lo através do ModMenu ou do arquivo de configuração! por lykiaofficial (https://lyki.dev)"
            );
            messages.put("isNearby", " está por perto! ");
            messages.put("closest", " mais próximo! ");
            messages.put("morePlayersFound", " mais jogadores encontrados!");
            messages.put("isNowAProtectedPlayer", " agora é um jogador protegido!");
            messages.put("isNoLongerAProtectedPlayer", " não é mais um jogador protegido!");
            messages.put("notLookingAPlayer", "Você não está olhando para um jogador!");
            messages.put("notLookingAnything", "Você não está olhando para nada!");
            messages.put("listAlreadyEmpty", "A lista de jogadores já está vazia!");
            messages.put("listCleared", "A lista de jogadores foi limpa!");
            messages.put("FriendGuardDisabled", "FriendGuard está desativado!");
            messages.put("compassSettingsLabel", "Configurações da Bússola");
            messages.put("enableCompassButton", "Mostrar");
            messages.put("disableCompassButton", "Ocultar");
            messages.put("isInAnotherDimension", " está em outra dimensão!");
            messages.put("dimensionOverworld", "Overworld");
            messages.put("dimensionNether", "Nether");
            messages.put("dimensionEnd", "End");
            messages.put("dimensionOther", "outra dimensão");
        } else if ("ru_ru".equals(clientLanguage)) {
            messages.put("friendguardActivated", "No Friendly Fire is active!");
            messages.put(
                "friendguardDeactiveError",
                "FriendGuard отключен из-за настроек конфигурации. Вам нужно включить его, чтобы использовать мод. Вы можете активировать его через ModMenu или файл конфигурации! от lykiaofficial (https://lyki.dev)"
            );
            messages.put("isNearby", " поблизости! ");
            messages.put("closest", " ближе всего! ");
            messages.put("morePlayersFound", " игроков найдено еще!");
            messages.put("isNowAProtectedPlayer", " теперь защищенный игрок!");
            messages.put("isNoLongerAProtectedPlayer", " больше не защищенный игрок!");
            messages.put("notLookingAPlayer", "Вы не смотрите на игрока!");
            messages.put("notLookingAnything", "Вы не смотрите ни на что!");
            messages.put("listAlreadyEmpty", "Список игроков уже пуст!");
            messages.put("listCleared", "Список игроков очищен!");
            messages.put("FriendGuardDisabled", "FriendGuard отключен!");
            messages.put("compassSettingsLabel", "Настройки Компаса");
            messages.put("enableCompassButton", "Показать");
            messages.put("disableCompassButton", "Скрыть");
            messages.put("isInAnotherDimension", " в другом измерении!");
            messages.put("dimensionOverworld", "Overworld");
            messages.put("dimensionNether", "Нижний мир");
            messages.put("dimensionEnd", "Энд");
            messages.put("dimensionOther", "другое измерение");
        } else if ("zh_cn".equals(clientLanguage)) {
            messages.put("friendguardActivated", "No Friendly Fire is active!");
            messages.put("friendguardDeactiveError", "由于配置设置，FriendGuard 已禁用。您需要启用它才能使用该模组。您可以通过 ModMenu 或配置文件启用它! 作者：lykiaofficial (https://lyki.dev)");
            messages.put("isNearby", " 附近! ");
            messages.put("closest", " 最近的! ");
            messages.put("morePlayersFound", " 更多玩家找到!");
            messages.put("isNowAProtectedPlayer", " 现在是受保护的玩家!");
            messages.put("isNoLongerAProtectedPlayer", " 不再是受保护的玩家!");
            messages.put("notLookingAPlayer", "你没有看着玩家!");
            messages.put("notLookingAnything", "你没有看着任何东西!");
            messages.put("listAlreadyEmpty", "玩家列表已经为空!");
            messages.put("listCleared", "玩家列表已清空!");
            messages.put("FriendGuardDisabled", "FriendGuard 已禁用!");
            messages.put("compassSettingsLabel", "指南针设置");
            messages.put("enableCompassButton", "显示");
            messages.put("disableCompassButton", "隐藏");
            messages.put("isInAnotherDimension", " 在另一个维度!");
            messages.put("dimensionOverworld", "主世界");
            messages.put("dimensionNether", "下界");
            messages.put("dimensionEnd", "末地");
            messages.put("dimensionOther", "其他维度");
        } else {
            messages.put("friendguardActivated", "No Friendly Fire is active!");
            messages.put(
                "friendguardDeactiveError",
                "FriendGuard is disabled due to configuration settings. You need to enable it to use the mod. You can activate it through ModMenu or the configuration file! by lykiaofficial (https://lyki.dev)"
            );
            messages.put("isNearby", " is nearby! ");
            messages.put("closest", " closest! ");
            messages.put("morePlayersFound", " more players found!");
            messages.put("isNowAProtectedPlayer", " is now a protected player!");
            messages.put("isNoLongerAProtectedPlayer", " is no longer a protected player!");
            messages.put("notLookingAPlayer", "You are not looking at a player!");
            messages.put("notLookingAnything", "You are not looking at anything!");
            messages.put("listAlreadyEmpty", "Player list is already empty!");
            messages.put("listCleared", "Player list cleared!");
            messages.put("FriendGuardDisabled", "FriendGuard is disabled!");
            messages.put("compassSettingsLabel", "Compass Settings");
            messages.put("enableCompassButton", "Show");
            messages.put("disableCompassButton", "Hide");
            messages.put("isInAnotherDimension", " is in another dimension!");
            messages.put("dimensionOverworld", "Overworld");
            messages.put("dimensionNether", "Nether");
            messages.put("dimensionEnd", "End");
            messages.put("dimensionOther", "other dimension");
        }
    }

    public void readConfigFile() {
        if (!configFile.exists()) {
            LOGGER.warn("Config file not found, it might not have been created yet.");
            isModEnabled = true;
            isCompassEnabled = true;
            AddRemovePlayer.isModEnabled = true;
            ClearList.isModEnabled = true;
        } else {
            if (configFile.exists()) {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader("mods/FriendGuard/FriendGuardConfig.json"));
                    JsonObject configObject = JsonParser.parseReader(reader).getAsJsonObject();

                    for (JsonElement element : configObject.getAsJsonArray("player_names")) {
                        String playerName = element.getAsString();
                        ProtectedPlayers.add(playerName);
                        LOGGER.info("Player added by config: " + playerName);
                    }

                    isModEnabled = configObject.get("is_mod_enabled").getAsBoolean();
                    AddRemovePlayer.isModEnabled = configObject.get("is_mod_enabled").getAsBoolean();
                    ClearList.isModEnabled = configObject.get("is_mod_enabled").getAsBoolean();
                    if (configObject.has("is_compass_enabled")) {
                        isCompassEnabled = configObject.get("is_compass_enabled").getAsBoolean();
                    } else {
                        isCompassEnabled = true;
                    }

                    reader.close();
                } catch (Exception var7) {
                    var7.printStackTrace();
                }
            }
        }
    }

    public static void saveConfig() {
        JsonObject configObject = new JsonObject();
        JsonArray playerNamesArray = new JsonArray();

        for (String playerName : ProtectedPlayers) {
            playerNamesArray.add(playerName);
        }

        configObject.add("player_names", playerNamesArray);
        configObject.addProperty("is_mod_enabled", isModEnabled);
        configObject.addProperty("is_compass_enabled", isCompassEnabled);

        try {
            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
            }

            FileWriter writer = new FileWriter(configFile);
            writer.write(configObject.toString());
            writer.close();
        } catch (IOException var4) {
            var4.printStackTrace();
        }
    }

    private void playDidgeridooSound(PlayerEntity player) {
        player.playSound(DIDGERIDOO_SOUND_EVENT, 1.0F, 1.0F);
    }

    private String getCompassSymbol(PlayerEntity player, PlayerEntity closestPlayer) {
        float playerYaw = player.getYaw(1.0F);
        float closestPlayerYaw = this.getClosestPlayerYaw(player, closestPlayer);
        float yawDifference = MathHelper.wrapDegrees(closestPlayerYaw - playerYaw);
        if (yawDifference >= -22.5 && yawDifference <= 22.5) {
            return COMPASS_DIRECTIONS[0];
        } else if (yawDifference > 22.5 && yawDifference <= 67.5) {
            return COMPASS_DIRECTIONS[1];
        } else if (yawDifference > 67.5 && yawDifference <= 112.5) {
            return COMPASS_DIRECTIONS[2];
        } else if (yawDifference > 112.5 && yawDifference <= 157.5) {
            return COMPASS_DIRECTIONS[3];
        } else if (yawDifference > 157.5 || yawDifference <= -157.5) {
            return COMPASS_DIRECTIONS[4];
        } else if (yawDifference > -157.5 && yawDifference <= -112.5) {
            return COMPASS_DIRECTIONS[5];
        } else {
            return yawDifference > -112.5 && yawDifference <= -67.5 ? COMPASS_DIRECTIONS[6] : COMPASS_DIRECTIONS[7];
        }
    }

    private float getClosestPlayerYaw(PlayerEntity player, PlayerEntity closestPlayer) {
        BlockPos playerPos = player.getBlockPos();
        BlockPos closestPlayerPos = closestPlayer.getBlockPos();
        return (float) MathHelper.wrapDegrees(
            MathHelper.atan2(closestPlayerPos.getZ() - playerPos.getZ(), closestPlayerPos.getX() - playerPos.getX()) * (180.0 / Math.PI) - 90.0
        );
    }

    private static class PlayerTrackerInfo {
        final String name;
        final PlayerEntity player;
        final boolean inOtherDimension;
        final String dimensionLabel;

        PlayerTrackerInfo(String name, PlayerEntity player, boolean inOtherDimension, String dimensionLabel) {
            this.name = name;
            this.player = player;
            this.inOtherDimension = inOtherDimension;
            this.dimensionLabel = dimensionLabel;
        }
    }
}
