package xyz.lyki.friendguard.Config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.lyki.friendguard.FriendGuardClient;
import xyz.lyki.friendguard.KeyUtils.AddRemovePlayer;
import xyz.lyki.friendguard.KeyUtils.ClearList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class TrackerPanel extends Screen {
    private final Screen parent;
    private TextFieldWidget textFieldWidget;
    private List<String> trackedPlayers;
    private boolean isModEnabled;
    private int scrollOffset;
    private static final int MAX_DISPLAY = 20;
    private static final int SCROLL_STEP = 10;
    private ButtonWidget onButton;
    private ButtonWidget offButton;
    private boolean isCompassEnabled;
    private List<ButtonWidget> playerButtons = new ArrayList<>();
    private List<String> allServerPlayers;
    private int buttonRow;
    private int buttonCol;
    private int selectedCount;
    private Map<String, Boolean> playerSelectionState;

    public TrackerPanel(Screen parent) {
        super(Text.literal("No Friendly Fire - Track Players"));
        this.parent = parent;
        this.trackedPlayers = new ArrayList<>(FriendGuardClient.ProtectedPlayers);
        this.scrollOffset = 0;
        this.isModEnabled = FriendGuardClient.isModEnabled;
        this.isCompassEnabled = FriendGuardClient.isCompassEnabled;
        this.allServerPlayers = new ArrayList<>();
        this.buttonRow = 0;
        this.buttonCol = 0;
        this.selectedCount = 0;
        this.playerSelectionState = new HashMap<>();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 4 - 30;

        this.textFieldWidget = new TextFieldWidget(this.textRenderer, centerX + 50, startY + 20, 200, 20, Text.literal(""));
        this.textFieldWidget.setChangedListener(text -> {});
        this.addDrawableChild(this.textFieldWidget);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Refresh List"), button -> this.refreshPlayerList()).position(centerX + 50, startY + 50).size(200, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Clear Selected"), button -> this.clearSelectedPlayers()).position(centerX + 50, startY + 80).size(200, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Select All"), button -> this.selectAllPlayers()).position(centerX + 50, startY + 110).size(200, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> this.client.setScreen(this.parent)).position(centerX + 155, startY + 110).size(95, 20).build());

        this.onButton = (ButtonWidget) this.addDrawableChild(ButtonWidget.builder(Text.literal("Enabled"), button -> this.toggleEnabled(true)).position(centerX + 50, startY + 140).size(95, 20).build());
        this.offButton = (ButtonWidget) this.addDrawableChild(ButtonWidget.builder(Text.literal("Disabled"), button -> this.toggleEnabled(false)).position(centerX + 155, startY + 140).size(95, 20).build());
        this.toggleEnabled(this.isModEnabled);

        this.refreshPlayerList();
    }

    private void refreshPlayerList() {
        this.allServerPlayers.clear();
        this.allServerPlayers.addAll(FriendGuardClient.getAllPlayers());
        if (this.allServerPlayers.isEmpty()) {
            this.allServerPlayers.add("No players found.");
        }
        this.clearPlayerButtons();
        this.selectedCount = 0;
        this.buttonRow = 0;
        this.buttonCol = 0;
        this.playerSelectionState.clear();

        for (String playerName : this.allServerPlayers) {
            if (playerName.equals("No players found.")) {
                continue;
            }
            boolean isSelected = this.trackedPlayers.contains(playerName);
            this.playerSelectionState.put(playerName, isSelected);
            if (isSelected) {
                this.selectedCount++;
            }
            if (this.buttonCol >= 4) {
                this.buttonRow++;
                this.buttonCol = 0;
            }
            String buttonText = isSelected ? "[✓] " + playerName : "[ ] " + playerName;
            ButtonWidget button = ButtonWidget.builder(Text.literal(buttonText), b -> this.togglePlayer(playerName)).position(40, 20 + this.buttonRow * 25).size(200, 20).build();
            this.playerButtons.add(button);
            this.addDrawableChild(button);
            this.buttonCol++;
        }
        this.scrollOffset = 0;
        this.drawStatusBar();
    }

    private void togglePlayer(String playerName) {
        boolean currentlySelected = this.trackedPlayers.contains(playerName);
        if (currentlySelected) {
            this.trackedPlayers.remove(playerName);
            this.selectedCount--;
            this.playerSelectionState.put(playerName, false);
        } else {
            this.trackedPlayers.add(playerName);
            this.selectedCount++;
            this.playerSelectionState.put(playerName, true);
        }
        this.refreshPlayerList();
    }

    private void selectAllPlayers() {
        for (String playerName : this.allServerPlayers) {
            if (!playerName.equals("No players found.")) {
                if (!this.trackedPlayers.contains(playerName)) {
                    this.trackedPlayers.add(playerName);
                    this.selectedCount++;
                    this.playerSelectionState.put(playerName, true);
                }
            }
        }
        this.refreshPlayerList();
    }

    private void clearSelectedPlayers() {
        this.trackedPlayers.clear();
        this.selectedCount = 0;
        this.playerSelectionState.clear();
        this.refreshPlayerList();
    }

    private void toggleEnabled(boolean durum) {
        FriendGuardClient.isModEnabled = durum;
        AddRemovePlayer.isModEnabled = durum;
        ClearList.isModEnabled = durum;
        this.onButton.active = !durum;
        this.offButton.active = durum;
        this.isModEnabled = durum;
    }

    private void drawStatusBar() {
        int centerX = this.width / 2;
        String statusText = "Tracked: " + this.selectedCount + " / " + this.allServerPlayers.size();
        int statusWidth = this.textRenderer.getWidth(statusText);
        int y = this.height - 20;
        // Use system out as a fallback - in 1.21.11 Screen doesn't have direct matrixStack access
        System.out.println(statusText);
    }

    private void clearPlayerButtons() {
        this.playerButtons.clear();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    public static void openTrackerPanel(Screen parent) {
        TrackerPanel panel = new TrackerPanel(parent);
        panel.client.setScreen(panel);
    }
}
