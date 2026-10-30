package com.shinecreator.storyquest.client.gui.theme;

import com.shinecreator.storyquest.StoryQuest;
import com.shinecreator.storyquest.client.StoryQuestClientConfig;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.TranslatableEnum;

/**
 * A visual theme for every StoryQuest screen.
 *
 * <p>Textures live in {@code assets/storyquest/textures/gui/<theme>/}. Each slice
 * stores its native size and 9-slice border so screens can stretch it to any
 * rectangle without blurring the pixel art.</p>
 */
public enum QuestTheme implements TranslatableEnum {
    PAPER(
            "paper",
            false,
            new Palette(
                    0xFF8C3B32,
                    0xFF5A4530,
                    0xFF6E563E,
                    0xFF7E6547,
                    0xFFF0E3C4,
                    0xFF3F7A4A,
                    0xFF8C3B32,
                    0xFFA8702A,
                    0x66B49A70,
                    0xFF5A4530,
                    10
            )
    ),
    BOOK(
            "book",
            true,
            new Palette(
                    0xFF9E2B25,
                    0xFF5A4530,
                    0xFF6E563E,
                    0xFF7E6547,
                    0xFFF3E7C8,
                    0xFF3F7A4A,
                    0xFF9E2B25,
                    0xFFA8702A,
                    0x66B49A70,
                    0xFFF3E7C8,
                    12
            )
    );

    /** One stretchable texture: native size plus the width of its 9-slice border. */
    public record Slice(ResourceLocation location, int width, int height, int border) {}

    private record Palette(
            int accent,
            int text,
            int textDim,
            int textMuted,
            int onAccent,
            int positive,
            int negative,
            int warning,
            int selection,
            int hudText,
            int padding
    ) {}

    private final String id;
    private final boolean bookStyle;
    private final Palette palette;

    private final Slice panel;
    private final Slice page;
    private final Slice inset;
    private final Slice button;
    private final Slice buttonHover;
    private final Slice buttonDisabled;
    private final Slice buttonPrimary;
    private final Slice buttonPrimaryHover;
    private final Slice slot;
    private final Slice divider;
    private final Slice scrollTrack;
    private final Slice scrollThumb;
    private final Slice decoration;
    private final Slice spine;

    QuestTheme(String id, boolean bookStyle, Palette palette) {
        this.id = id;
        this.bookStyle = bookStyle;
        this.palette = palette;
        if (bookStyle) {
            this.panel = slice("panel_cover", 24, 24, 4);
            this.page = slice("panel_page", 24, 24, 3);
            this.decoration = slice("bookmark", 12, 28, 0);
            this.spine = slice("spine", 12, 32, 0);
        } else {
            this.panel = slice("panel", 24, 24, 3);
            this.page = null;
            this.decoration = slice("seal", 16, 16, 0);
            this.spine = null;
        }
        this.inset = slice("panel_inset", 24, 24, 2);
        this.button = slice("button", 24, 16, 2);
        this.buttonHover = slice("button_hover", 24, 16, 2);
        this.buttonDisabled = slice("button_disabled", 24, 16, 2);
        this.buttonPrimary = slice("button_primary", 24, 16, 2);
        this.buttonPrimaryHover = slice("button_primary_hover", 24, 16, 2);
        this.slot = slice("slot", 18, 18, 2);
        this.divider = slice("divider", 24, 2, 0);
        this.scrollTrack = slice("scrollbar_track", 8, 16, 0);
        this.scrollThumb = slice("scrollbar_thumb", 8, 16, 0);
    }

    public static QuestTheme current() {
        QuestTheme selected = StoryQuestClientConfig.theme();
        return selected == null ? PAPER : selected;
    }

    private Slice slice(String name, int width, int height, int border) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                StoryQuest.MODID, "textures/gui/" + id + "/" + name + ".png");
        return new Slice(location, width, height, border);
    }

    public ResourceLocation icon(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                StoryQuest.MODID, "textures/gui/" + id + "/icon_" + name + ".png");
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "storyquest.theme." + id;
    }

    @Override
    public Component getTranslatedName() {
        return Component.translatable(translationKey());
    }

    /** True when the panel is leather with an inner paper page. */
    public boolean bookStyle() {
        return bookStyle;
    }

    public Slice panel() {
        return panel;
    }

    /** Inner page for book-style themes, {@code null} for single-layer panels. */
    public Slice page() {
        return page;
    }

    /**
     * Distance from the panel edge to the usable inner page. Book themes draw a
     * leather cover with an inner page, so their content starts a little lower.
     */
    public int pageInset() {
        return bookStyle ? 6 : 0;
    }

    public Slice inset() {
        return inset;
    }

    public Slice button() {
        return button;
    }

    public Slice buttonHover() {
        return buttonHover;
    }

    public Slice buttonDisabled() {
        return buttonDisabled;
    }

    public Slice buttonPrimary() {
        return buttonPrimary;
    }

    public Slice buttonPrimaryHover() {
        return buttonPrimaryHover;
    }

    public Slice slot() {
        return slot;
    }

    public Slice divider() {
        return divider;
    }

    public Slice scrollTrack() {
        return scrollTrack;
    }

    public Slice scrollThumb() {
        return scrollThumb;
    }

    public Slice decoration() {
        return decoration;
    }

    /** Leather spine strip, only for book-style themes. */
    public Slice spine() {
        return spine;
    }

    public int accent() {
        return palette.accent();
    }

    public int text() {
        return palette.text();
    }

    public int textDim() {
        return palette.textDim();
    }

    public int textMuted() {
        return palette.textMuted();
    }

    public int onAccent() {
        return palette.onAccent();
    }

    public int positive() {
        return palette.positive();
    }

    public int negative() {
        return palette.negative();
    }

    public int warning() {
        return palette.warning();
    }

    public int selection() {
        return palette.selection();
    }

    /** Text colour for the in-game HUD: dark ink on paper, light page on leather. */
    public int hudText() {
        return palette.hudText();
    }

    /** Text colour for normal buttons. */
    public int buttonText() {
        return bookStyle ? palette.onAccent() : palette.text();
    }

    /** Text colour for completed objectives on the HUD. */
    public int hudPositive() {
        return bookStyle ? 0xFF8FD9A0 : palette.positive();
    }

    /** Muted text on the HUD background. */
    public int hudMuted() {
        return bookStyle ? 0xFFC9B78F : palette.textMuted();
    }

    public int padding() {
        return palette.padding();
    }
}
