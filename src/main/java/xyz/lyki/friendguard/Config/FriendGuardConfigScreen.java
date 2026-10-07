package xyz.lyki.friendguard.Config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import xyz.lyki.friendguard.FriendGuardClient;
import xyz.lyki.friendguard.KeyUtils.AddRemovePlayer;
import xyz.lyki.friendguard.KeyUtils.ClearList;
import xyz.lyki.friendguard.Config.TrackerPanel;

public class FriendGuardConfigScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget textFieldWidget;
    private List<String> protectedPlayers;
    private String errorMessage;
    private int scrollOffset;
    private static final int MAX_DISPLAY = 10;
    private static final int SCROLL_STEP = 10;
    private ButtonWidget onButton;
    private ButtonWidget offButton;
    private ButtonWidget compassOnButton;
    private ButtonWidget compassOffButton;
    private boolean isModEnabled;
    private boolean isCompassEnabled;
    private Map<String, String> messages = new HashMap<>();

    protected FriendGuardConfigScreen(Screen parent) {
        super(Text.literal("FriendGuard Config"));
        this.parent = parent;
        this.protectedPlayers = FriendGuardClient.ProtectedPlayers;
        this.scrollOffset = 0;
        this.isModEnabled = FriendGuardClient.isModEnabled;
        this.isCompassEnabled = FriendGuardClient.isCompassEnabled;
        this.loadMessages();
        this.errorMessage = this.messages.get("descriptionLabel");
    }

    private void loadMessages() {
        String clientLanguage = MinecraftClient.getInstance().getLanguageManager().getLanguage();
        if ("tr_tr".equals(clientLanguage)) {
            this.messages.put("friendguardlabel", "FriendGuard: Arkadaşınıza Zarar Vermeyin");
            this.messages.put("trackerButton", "Tracked Players (J)");
            this.messages.put("playerNameLabel", "Oyuncu Adı:");
            this.messages.put("modSettingsLabel", "Mod Ayarları");
            this.messages.put("addPlayerButton", "Oyuncu Ekle");
            this.messages.put("removePlayerButton", "Oyuncu Kaldır");
            this.messages.put("clearListButton", "Listeyi Temizle");
            this.messages.put("websiteButton", "Web Sitesi");
            this.messages.put("doneButton", "Tamam");
            this.messages.put("protectedPlayersLabel", "Korunan Oyuncular:");
            this.messages.put("playerNameError", "Oyuncu adı 3 ile 16 karakter arasında olmalıdır.");
            this.messages.put("enableButton", "Aktif Et");
            this.messages.put("disableButton", "Devre Dışı Bırak");
            this.messages.put("scrollUpButton", "▲");
            this.messages.put("scrollDownButton", "▼");
            this.messages.put("errorPrefix", "Hata: ");
            this.messages.put("descriptionLabel", "Takım Arkadaşınıza Kazara Zarar Vermeyi Önleyin.");
            this.messages.put("compassSettingsLabel", "Pusula Ayarları");
            this.messages.put("enableCompassButton", "Göster");
            this.messages.put("disableCompassButton", "Gizle");
        } else if ("fr_fr".equals(clientLanguage)) {
            this.messages.put("friendguardlabel", "FriendGuard: Ne Faites Pas de Mal à Votre Ami");
            this.messages.put("playerNameLabel", "Nom du joueur :");
            this.messages.put("modSettingsLabel", "Paramètres du Mod");
            this.messages.put("addPlayerButton", "Ajouter Joueur");
            this.messages.put("removePlayerButton", "Supprimer Joueur");
            this.messages.put("clearListButton", "Effacer Liste");
            this.messages.put("websiteButton", "Site Web");
            this.messages.put("doneButton", "Terminé");
            this.messages.put("protectedPlayersLabel", "Joueurs Protégés :");
            this.messages.put("playerNameError", "Le nom du joueur doit contenir entre 3 et 16 caractères.");
            this.messages.put("enableButton", "Activer");
            this.messages.put("disableButton", "Désactiver");
            this.messages.put("scrollUpButton", "▲");
            this.messages.put("scrollDownButton", "▼");
            this.messages.put("errorPrefix", "Erreur : ");
            this.messages.put("descriptionLabel", "Évitez d'endommager accidentellement votre ami.");
            this.messages.put("compassSettingsLabel", "Paramètres de la Boussole");
            this.messages.put("enableCompassButton", "Afficher");
            this.messages.put("disableCompassButton", "Masquer");
        } else if ("es_es".equals(clientLanguage)) {
            this.messages.put("friendguardlabel", "FriendGuard: No Dañes a Tu Amigo");
            this.messages.put("playerNameLabel", "Nombre del jugador:");
            this.messages.put("modSettingsLabel", "Ajustes del Mod");
            this.messages.put("addPlayerButton", "Añadir Jugador");
            this.messages.put("removePlayerButton", "Eliminar Jugador");
            this.messages.put("clearListButton", "Limpiar Lista");
            this.messages.put("websiteButton", "Sitio Web");
            this.messages.put("doneButton", "Hecho");
            this.messages.put("protectedPlayersLabel", "Jugadores Protegidos:");
            this.messages.put("playerNameError", "El nombre del jugador debe tener entre 3 y 16 caracteres.");
            this.messages.put("enableButton", "Activar");
            this.messages.put("disableButton", "Desactivar");
            this.messages.put("scrollUpButton", "▲");
            this.messages.put("scrollDownButton", "▼");
            this.messages.put("errorPrefix", "Error: ");
            this.messages.put("descriptionLabel", "Evite dañar accidentalmente a su amigo.");
            this.messages.put("compassSettingsLabel", "Ajustes de la Brújula");
            this.messages.put("enableCompassButton", "Mostrar");
            this.messages.put("disableCompassButton", "Ocultar");
        } else if ("de_de".equals(clientLanguage)) {
            this.messages.put("friendguardlabel", "FriendGuard: Schäden Sie Ihrem Freund Nicht");
            this.messages.put("playerNameLabel", "Spielername:");
            this.messages.put("modSettingsLabel", "Mod Einstellungen");
            this.messages.put("addPlayerButton", "Spieler Hinzufügen");
            this.messages.put("removePlayerButton", "Spieler Entfernen");
            this.messages.put("clearListButton", "Liste Leeren");
            this.messages.put("websiteButton", "Webseite");
            this.messages.put("doneButton", "Fertig");
            this.messages.put("protectedPlayersLabel", "Geschützte Spieler:");
            this.messages.put("playerNameError", "Der Spielername muss zwischen 3 und 16 Zeichen lang sein.");
            this.messages.put("enableButton", "Aktivieren");
            this.messages.put("disableButton", "Deaktivieren");
            this.messages.put("scrollUpButton", "▲");
            this.messages.put("scrollDownButton", "▼");
            this.messages.put("errorPrefix", "Fehler: ");
            this.messages.put("descriptionLabel", "Verhindern Sie versehentlich Schaden an Ihrem Freund.");
            this.messages.put("compassSettingsLabel", "Kompass-Einstellungen");
            this.messages.put("enableCompassButton", "Anzeigen");
            this.messages.put("disableCompassButton", "Verstecken");
        } else if ("pt_br".equals(clientLanguage)) {
            this.messages.put("friendguardlabel", "FriendGuard: Não Dano ao Seu Amigo");
            this.messages.put("playerNameLabel", "Nome do jogador:");
            this.messages.put("modSettingsLabel", "Configurações do Mod");
            this.messages.put("addPlayerButton", "Adicionar Jogador");
            this.messages.put("removePlayerButton", "Remover Jogador");
            this.messages.put("clearListButton", "Limpar Lista");
            this.messages.put("websiteButton", "Website");
            this.messages.put("doneButton", "Concluído");
            this.messages.put("protectedPlayersLabel", "Jogadores Protegidos:");
            this.messages.put("playerNameError", "O nome do jogador deve ter entre 3 e 16 caracteres.");
            this.messages.put("enableButton", "Ativar");
            this.messages.put("disableButton", "Desativar");
            this.messages.put("scrollUpButton", "▲");
            this.messages.put("scrollDownButton", "▼");
            this.messages.put("errorPrefix", "Erro: ");
            this.messages.put("descriptionLabel", "Evite danificar acidentalmente seu amigo.");
            this.messages.put("compassSettingsLabel", "Configurações da Bússola");
            this.messages.put("enableCompassButton", "Mostrar");
            this.messages.put("disableCompassButton", "Ocultar");
        } else if ("ru_ru".equals(clientLanguage)) {
            this.messages.put("friendguardlabel", "FriendGuard: Не Навредите Своему Другу");
            this.messages.put("playerNameLabel", "Имя игрока:");
            this.messages.put("modSettingsLabel", "Настройки мода");
            this.messages.put("addPlayerButton", "Добавить игрока");
            this.messages.put("removePlayerButton", "Удалить игрока");
            this.messages.put("clearListButton", "Очистить список");
            this.messages.put("websiteButton", "Веб-сайт");
            this.messages.put("doneButton", "Готово");
            this.messages.put("protectedPlayersLabel", "Защищенные игроки:");
            this.messages.put("playerNameError", "Имя игрока должно содержать от 3 до 16 символов.");
            this.messages.put("enableButton", "Включить");
            this.messages.put("disableButton", "Выключить");
            this.messages.put("scrollUpButton", "▲");
            this.messages.put("scrollDownButton", "▼");
            this.messages.put("errorPrefix", "Ошибка: ");
            this.messages.put("descriptionLabel", "Предотвратите случайное повреждение вашего друга.");
            this.messages.put("compassSettingsLabel", "Настройки Компаса");
            this.messages.put("enableCompassButton", "Показать");
            this.messages.put("disableCompassButton", "Скрыть");
        } else if ("zh_cn".equals(clientLanguage)) {
            this.messages.put("friendguardlabel", "FriendGuard: 不要伤害你的朋友");
            this.messages.put("playerNameLabel", "玩家名称:");
            this.messages.put("modSettingsLabel", "模组设置");
            this.messages.put("addPlayerButton", "添加玩家");
            this.messages.put("removePlayerButton", "移除玩家");
            this.messages.put("clearListButton", "清空列表");
            this.messages.put("websiteButton", "网站");
            this.messages.put("doneButton", "完成");
            this.messages.put("protectedPlayersLabel", "受保护的玩家:");
            this.messages.put("playerNameError", "玩家名称必须在3到16个字符之间.");
            this.messages.put("enableButton", "启用");
            this.messages.put("disableButton", "禁用");
            this.messages.put("scrollUpButton", "▲");
            this.messages.put("scrollDownButton", "▼");
            this.messages.put("errorPrefix", "错误: ");
            this.messages.put("descriptionLabel", "避免意外損害您的朋友。");
            this.messages.put("compassSettingsLabel", "指南针设置");
            this.messages.put("enableCompassButton", "显示");
            this.messages.put("disableCompassButton", "隐藏");
        } else {
            this.messages.put("friendguardlabel", "FriendGuard: Don't Damage to Your Friend");
            this.messages.put("playerNameLabel", "Player Name:");
            this.messages.put("modSettingsLabel", "Mod Settings");
            this.messages.put("addPlayerButton", "Add Player");
            this.messages.put("removePlayerButton", "Remove Player");
            this.messages.put("clearListButton", "Clear List");
            this.messages.put("websiteButton", "Website");
            this.messages.put("doneButton", "Done");
            this.messages.put("protectedPlayersLabel", "Protected Players:");
            this.messages.put("playerNameError", "Player name must be between 3 and 16 characters.");
            this.messages.put("enableButton", "Enable");
            this.messages.put("disableButton", "Disable");
            this.messages.put("scrollUpButton", "▲");
            this.messages.put("scrollDownButton", "▼");
            this.messages.put("errorPrefix", "Error: ");
            this.messages.put("descriptionLabel", "Prevent Accidentally Damaging Your Friend.");
            this.messages.put("compassSettingsLabel", "Compass Settings");
            this.messages.put("enableCompassButton", "Show");
            this.messages.put("disableCompassButton", "Hide");
        }
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 4 - 20;
        int listX = 30;
        int listY = startY + 30;

        this.textFieldWidget = new TextFieldWidget(this.textRenderer, centerX + 50, startY + 20, 200, 20, Text.literal(""));
        this.addDrawableChild(this.textFieldWidget);

        this.addDrawableChild(ButtonWidget.builder(Text.literal(this.messages.get("addPlayerButton")), button -> {
            String playerName = this.textFieldWidget.getText();
            if (this.isValidPlayerName(playerName)) {
                if (!this.protectedPlayers.contains(playerName)) {
                    this.protectedPlayers.add(playerName);
                    this.textFieldWidget.setText("");
                    this.errorMessage = this.messages.get("descriptionLabel");
                }
            } else {
                this.errorMessage = this.messages.get("playerNameError");
            }
        }).position(centerX + 50, startY + 50).size(200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal(this.messages.get("removePlayerButton")), button -> {
            String playerName = this.textFieldWidget.getText();
            if (this.protectedPlayers.contains(playerName)) {
                this.protectedPlayers.remove(playerName);
                this.textFieldWidget.setText("");
                this.errorMessage = this.messages.get("descriptionLabel");
            }
        }).position(centerX + 50, startY + 80).size(200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal(this.messages.get("clearListButton")), button -> {
            this.protectedPlayers.clear();
            this.errorMessage = this.messages.get("descriptionLabel");
        }).position(centerX + 50, startY + 110).size(200, 20).build());

        this.addDrawableChild(
            ButtonWidget.builder(
                Text.literal(this.messages.get("trackerButton")),
                button -> TrackerPanel.openTrackerPanel(this)
            )
                .position(centerX + 50, startY + 140)
                .size(200, 20)
                .build()
        );

        this.addDrawableChild(
            ButtonWidget.builder(Text.literal(this.messages.get("websiteButton")), button -> Util.getOperatingSystem().open("https://lyki.dev"))
                .position(centerX + 50, startY + 140)
                .size(95, 20)
                .build()
        );

        this.addDrawableChild(
            ButtonWidget.builder(Text.literal(this.messages.get("doneButton")), button -> this.client.setScreen(this.parent))
                .position(centerX + 155, startY + 140)
                .size(95, 20)
                .build()
        );

        this.addDrawableChild(
            ButtonWidget.builder(Text.literal("▲"), button -> this.scrollOffset = Math.max(0, this.scrollOffset - 10))
                .position(listX - 2, listY - 32)
                .size(20, 20)
                .build()
        );

        this.addDrawableChild(
            ButtonWidget.builder(Text.literal("▼"), button -> this.scrollOffset = Math.min(this.protectedPlayers.size() * 10, this.scrollOffset + 10))
                .position(listX - 2, listY + 162)
                .size(20, 20)
                .build()
        );

        this.onButton = (ButtonWidget) this.addDrawableChild(
            ButtonWidget.builder(Text.literal(this.messages.get("enableButton")), button -> this.toggleButtons(true))
                .position(centerX + 50, startY + 195)
                .size(95, 20)
                .build()
        );

        this.offButton = (ButtonWidget) this.addDrawableChild(
            ButtonWidget.builder(Text.literal(this.messages.get("disableButton")), button -> this.toggleButtons(false))
                .position(centerX + 155, startY + 195)
                .size(95, 20)
                .build()
        );

        this.compassOnButton = (ButtonWidget) this.addDrawableChild(
            ButtonWidget.builder(Text.literal(this.messages.get("enableCompassButton")), button -> this.toggleCompassButtons(true))
                .position(centerX + 50, startY + 232)
                .size(95, 20)
                .build()
        );

        this.compassOffButton = (ButtonWidget) this.addDrawableChild(
            ButtonWidget.builder(Text.literal(this.messages.get("disableCompassButton")), button -> this.toggleCompassButtons(false))
                .position(centerX + 155, startY + 232)
                .size(95, 20)
                .build()
        );

        this.toggleButtons(this.isModEnabled);
        this.toggleCompassButtons(this.isCompassEnabled);
    }

    private void toggleButtons(boolean durum) {
        FriendGuardClient.isModEnabled = durum;
        AddRemovePlayer.isModEnabled = durum;
        ClearList.isModEnabled = durum;
        this.onButton.active = !durum;
        this.offButton.active = durum;
    }

    private void toggleCompassButtons(boolean durum) {
        FriendGuardClient.isCompassEnabled = durum;
        this.compassOnButton.active = !durum;
        this.compassOffButton.active = durum;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount < 0.0) {
            this.scrollOffset = Math.max(0, this.scrollOffset - 10);
        } else if (verticalAmount > 0.0) {
            this.scrollOffset = Math.min(this.protectedPlayers.size() * 10, this.scrollOffset + 10);
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        int centerX = this.width / 2;
        int listX = 30;
        int listY = this.height / 4 + 10;
        int startY = this.height / 4 - 20;
        int white = Formatting.WHITE.getColorValue() | 0xFF000000;
        int green = Formatting.GREEN.getColorValue() | 0xFF000000;
        String friendGuardLabelText = this.messages.get("friendguardlabel");
        int friendGuardLabelWidth = this.textRenderer.getWidth(friendGuardLabelText);
        context.drawTextWithShadow(this.textRenderer, Text.literal(friendGuardLabelText), centerX - friendGuardLabelWidth / 2, 20, white);
        context.drawTextWithShadow(this.textRenderer, Text.literal(this.messages.get("playerNameLabel")), centerX + 50, this.height / 4 - 15, white);
        context.drawTextWithShadow(this.textRenderer, Text.literal(this.messages.get("modSettingsLabel")), centerX + 52, startY + 180, white);
        context.drawTextWithShadow(this.textRenderer, Text.literal(this.messages.get("compassSettingsLabel")), centerX + 52, startY + 217, white);
        context.drawTextWithShadow(this.textRenderer, Text.literal(this.messages.get("protectedPlayersLabel")), listX + 70, listY - 30, green);
        List<String> sortedPlayers = this.protectedPlayers.stream().sorted().collect(Collectors.toList());

        for (int i = 0; i < 10 && i + this.scrollOffset / 10 < sortedPlayers.size(); i++) {
            context.drawTextWithShadow(this.textRenderer, Text.literal(sortedPlayers.get(i + this.scrollOffset / 10)), listX + 70, listY - 10 + i * 20, white);
        }

        if (this.errorMessage != null) {
            String descriptionLabelText = this.errorMessage.equals(this.messages.get("descriptionLabel"))
                ? this.messages.get("descriptionLabel")
                : this.errorMessage;
            int descriptionLabelWidth = this.textRenderer.getWidth(descriptionLabelText);
            Formatting descriptionColor = this.errorMessage.equals(this.messages.get("descriptionLabel")) ? Formatting.WHITE : Formatting.RED;
            context.drawTextWithShadow(
                this.textRenderer,
                Text.literal(descriptionLabelText).formatted(descriptionColor),
                centerX - descriptionLabelWidth / 2,
                this.height / 4 - 45,
                descriptionColor.getColorValue() | 0xFF000000
            );
        } else {
            String descriptionLabelText = this.messages.get("descriptionLabel");
            int descriptionLabelWidth = this.textRenderer.getWidth(descriptionLabelText);
            context.drawTextWithShadow(
                this.textRenderer, Text.literal(descriptionLabelText).formatted(Formatting.WHITE), centerX - descriptionLabelWidth / 2, this.height / 4 - 45, white
            );
        }
    }

    private boolean isValidPlayerName(String playerName) {
        return playerName.length() >= 3 && playerName.length() <= 16;
    }
}
