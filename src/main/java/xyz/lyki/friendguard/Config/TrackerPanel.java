package xyz.lyki.friendguard.Config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckBoxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.lyki.friendguard.FriendGuardClient;
import xyz.lyki.friendguard.KeyUtils.AddRemovePlayer;
import xyz.lyki.friendguard.KeyUtils.ClearList;

import java.util.ArrayList;
import java.util.List;

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
    private CheckBoxWidget[][] playerCheckboxes;
    private List<String> allServerPlayers;
    private int checkboxRow;
    private int checkboxCol;
    private int selectedCount;

    public TrackerPanel(Screen parent) {
        super(Text.literal("No Friendly Fire - Track Players"));
        this.parent = parent;
        this.trackedPlayers = new ArrayList<>(FriendGuardClient.ProtectedPlayers);
        this.scrollOffset = 0;
        this.isModEnabled = FriendGuardClient.isModEnabled;
        this.isCompassEnabled = FriendGuardClient.isCompassEnabled;
        this.playerCheckboxes = new CheckBoxWidget[MAX_DISPLAY][4];
        this.allServerPlayers = new ArrayList<>();
        this.checkboxRow = 0;
        this.checkboxCol = 0;
        this.selectedCount = 0;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 4 - 30;
        int listX = 30;
        int listY = startY + 60;

        this.textFieldWidget = new TextFieldWidget(this.textRenderer, centerX + 50, startY + 20, 200, 20, Text.literal(""));
        this.textFieldWidget.setChangedListener(text -> {});
        this.addDrawableChild(this.textFieldWidget);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Open Player List (J)"), button -> this.refreshPlayerList()).position(centerX + 50, startY + 50).size(200, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Clear Selected"), button -> this.clearSelectedPlayers()).position(centerX + 50, startY + 80).size(200, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Toggle All"), button -> this.toggleAllPlayers()).position(centerX + 50, startY + 110).size(200, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> this.client.setScreen(this.parent)).position(centerX + 155, startY + 110).size(95, 20).build());

        this.onButton = (ButtonWidget) this.addDrawableChild(ButtonWidget.builder(Text.literal("Enabled"), button -> this.toggleEnabled(true)).position(centerX + 50, startY + 140).size(95, 20).build());
        this.offButton = (ButtonWidget) this.addDrawableChild(ButtonWidget.builder(Text.literal("Disabled"), button -> this.toggleEnabled(false)).position(centerX + 155, startY + 140).size(95, 20).build());
        this.toggleButtons(this.isModEnabled);

        this.refreshPlayerList();
    }

    private void refreshPlayerList() {
        this.allServerPlayers.clear();
        this.allServerPlayers.addAll(FriendGuardClient.getAllPlayers());
        if (this.allServerPlayers.isEmpty()) {
            this.allServerPlayers.add("No players found.");
        }
        this.clearCheckboxes();
        this.selectedCount = 0;
        this.checkboxRow = 0;
        this.checkboxCol = 0;

        for (String playerName : this.allServerPlayers) {
            if (playerName.equals("No players found.")) {
                continue;
            }
            boolean isSelected = this.trackedPlayers.contains(playerName);
            if (this.checkboxCol >= 4) {
                this.checkboxRow++;
                this.checkboxCol = 0;
            }
            CheckBoxWidget checkbox = new CheckBoxWidget(this.textRenderer, 40, 20 + this.checkboxRow * 20, 50, 20, Text.literal(playerName), isSelected, false);
            checkbox.setChangedListener(selected -> {
                if (selected) {
                    if (!this.trackedPlayers.contains(playerName)) {
                        this.trackedPlayers.add(playerName);
                        this.selectedCount++;
                    }
                } else {
                    this.trackedPlayers.remove(playerName);
                    this.selectedCount--;
                }
            });
            this.playerCheckboxes[this.checkboxRow][this.checkboxCol] = checkbox;
            this.addDrawableChild(checkbox);
            this.checkboxCol++;
        }
        this.scrollOffset = 0;
        this.drawStatusBar();
    }

    private void toggleAllPlayers() {
        boolean anyUnselected = false;
        for (int r = 0; r < this.checkboxRow + 1; r++) {
            for (int c = 0; c < 4; c++) {
                CheckBoxWidget checkbox = this.playerCheckboxes[r][c];
                if (checkbox != null && !checkbox.selected) {
                    anyUnselected = true;
                    break;
                }
            }
        }
        for (int r = 0; r < this.checkboxRow + 1; r++) {
            for (int c = 0; c < 4; c++) {
                CheckBoxWidget checkbox = this.playerCheckboxes[r][c];
                if (checkbox != null) {
                    checkbox.selected = anyUnselected;
                    if (anyUnselected && !this.trackedPlayers.contains(checkbox.widgetText.getString())) {
                        this.trackedPlayers.add(checkbox.widgetText.getString());
                        this.selectedCount++;
                    } else if (!anyUnselected && this.trackedPlayers.contains(checkbox.widgetText.getString())) {
                        this.trackedPlayers.remove(checkbox.widgetText.getString());
                        this.selectedCount--;
                    }
                }
            }
        }
        this.drawStatusBar();
    }

    private void clearSelectedPlayers() {
        this.trackedPlayers.clear();
        this.selectedCount = 0;
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
        int listX = 30;
        String statusText = "Tracked: " + this.selectedCount + " / " + this.allServerPlayers.size();
        int statusWidth = this.textRenderer.getWidth(Text.literal(statusText));
        this.textRenderer.draw(Text.literal(statusText), centerX - statusWidth / 2, this.height - 20, Formatting.WHITE.getColorValue() | 0xFF000000, false);
    }

    private void clearCheckboxes() {
        for (int r = 0; r < this.checkboxRow + 1; r++) {
            for (int c = 0; c < 4; c++) {
                CheckBoxWidget checkbox = this.playerCheckboxes[r][c];
                if (checkbox != null) {
                    this.removeDrawableChild(checkbox);
                    this.playerCheckboxes[r][c] = null;
                }
            }
        }
    }

    public static void openTrackerPanel(Screen parent) {
        TrackerPanel panel = new TrackerPanel(parent);
        panel.client.setScreen(panel);
    }
}
