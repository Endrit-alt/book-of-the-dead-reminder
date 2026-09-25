package com.bookofthedeadnotifier;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.BackgroundComponent;
import net.runelite.client.ui.overlay.components.TextComponent;
import net.runelite.client.util.Text;

@Singleton
public class BookOfTheDeadNotifierOverlay extends Overlay
{
    private static final int PADDING = 6;
    private static final int BUTTON_GAP = 12;
    private static final String CONFIRM_TEXT = "Confirm";

    private final Client client;
    private final BookOfTheDeadNotifierPlugin plugin;
    private final BookOfTheDeadNotifierConfig config;

    // Publish bounds and warning identity together, from the renderer to the AWT mouse listener.
    private volatile ConfirmTarget confirmTarget;

    @Inject
    BookOfTheDeadNotifierOverlay(Client client, BookOfTheDeadNotifierPlugin plugin, BookOfTheDeadNotifierConfig config)
    {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (client.getGameState() != GameState.LOGGED_IN || !plugin.shouldShowWarning())
        {
            clearConfirmTarget();
            return null;
        }

        String displayText = getDisplayText();
        FontMetrics metrics = graphics.getFontMetrics();
        int textWidth = metrics.stringWidth(Text.removeTags(displayText));
        int buttonWidth = metrics.stringWidth(CONFIRM_TEXT) + PADDING * 2;
        int height = metrics.getHeight() + PADDING * 2;
        int buttonX = PADDING + textWidth + BUTTON_GAP;
        int width = buttonX + buttonWidth + PADDING;

        Color reminderColor = config.reminderColor();
        Color warningColor = config.flashReminderBox() && client.getGameCycle() % 40 >= 20
            ? config.flashColor() : reminderColor;
        BackgroundComponent background = new BackgroundComponent();
        // Leave a gap before Confirm so no flashing pixels are drawn beneath its translucent fill.
        background.setRectangle(new Rectangle(0, 0, buttonX - BUTTON_GAP / 2, height));
        background.setBackgroundColor(warningColor);
        background.render(graphics);

        Rectangle button = new Rectangle(buttonX, 2, buttonWidth, height - 4);
        Rectangle canvasButton = new Rectangle(button);
        canvasButton.translate(getBounds().x, getBounds().y);
        net.runelite.api.Point mouse = client.getMouseCanvasPosition();
        boolean hovered = mouse != null && !client.isMenuOpen() && canvasButton.contains(mouse.getX(), mouse.getY());
        Color buttonColor = new Color(reminderColor.getRed() / 4, reminderColor.getGreen() / 4,
            reminderColor.getBlue() / 4, 235);
        graphics.setColor(hovered ? new Color(40, 150, 60, 235) : buttonColor);
        graphics.fillRoundRect(button.x, button.y, button.width, button.height, 5, 5);
        graphics.setColor(Color.WHITE);
        graphics.drawRoundRect(button.x, button.y, button.width - 1, button.height - 1, 5, 5);

        int baseline = PADDING + metrics.getAscent();
        drawText(graphics, displayText, PADDING, baseline, Color.WHITE);
        drawText(graphics, CONFIRM_TEXT, buttonX + PADDING, baseline, Color.WHITE);
        confirmTarget = new ConfirmTarget(canvasButton, plugin.getWarningVersion());
        return new Dimension(width, height);
    }

    private void drawText(Graphics2D graphics, String text, int x, int y, Color color)
    {
        TextComponent component = new TextComponent();
        component.setText(text);
        component.setPosition(new Point(x, y));
        component.setColor(color);
        component.render(graphics);
    }

    boolean confirmAt(Point point)
    {
        ConfirmTarget target = confirmTarget;
        if (target == null || !plugin.shouldShowWarning() || client.getGameState() != GameState.LOGGED_IN
            || client.isMenuOpen() || !target.bounds.contains(point))
        {
            return false;
        }

        clearConfirmTarget();
        plugin.confirmWarning(target.warningVersion);
        return true;
    }

    void clearConfirmTarget()
    {
        confirmTarget = null;
    }

    private String getDisplayText()
    {
        switch (config.reminderStyle())
        {
            case CUSTOM_TEXT:
                return config.customText() == null ? "" : config.customText();
            case SHORT_TEXT:
                return plugin.getReminderShortText();
            default:
                return plugin.getReminderLongText();
        }
    }

    private static final class ConfirmTarget
    {
        private final Rectangle bounds;
        private final long warningVersion;

        private ConfirmTarget(Rectangle bounds, long warningVersion)
        {
            this.bounds = bounds;
            this.warningVersion = warningVersion;
        }
    }
}
