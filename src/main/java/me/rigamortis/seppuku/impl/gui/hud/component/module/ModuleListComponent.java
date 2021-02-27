package me.rigamortis.seppuku.impl.gui.hud.component.module;

import com.mojang.realmsclient.gui.ChatFormatting;
import me.rigamortis.seppuku.Seppuku;
import me.rigamortis.seppuku.api.event.gui.hud.EventUIValueChanged;
import me.rigamortis.seppuku.api.gui.hud.component.TextComponent;
import me.rigamortis.seppuku.api.gui.hud.component.*;
import me.rigamortis.seppuku.api.module.Module;
import me.rigamortis.seppuku.api.texture.Texture;
import me.rigamortis.seppuku.api.util.RenderUtil;
import me.rigamortis.seppuku.api.value.Value;
import me.rigamortis.seppuku.impl.config.ModuleConfig;
import me.rigamortis.seppuku.impl.gui.hud.GuiHudEditor;
import me.rigamortis.seppuku.impl.module.ui.HudEditorModule;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.Item;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * created by noil on 11/4/19 at 12:02 PM
 */
public final class ModuleListComponent extends ResizableHudComponent {

    private final Module.ModuleType type;

    private int scroll = 0;
    private int oldScroll = 0;
    private int totalHeight;

    private final int SCROLL_WIDTH = 5;
    private final int BORDER = 2;
    private final int TEXT_GAP = 1;
    private final int TEXTURE_SIZE = 8;
    private final int TITLE_BAR_HEIGHT = mc.fontRenderer.FONT_HEIGHT + 1;

    private String originalName = "";
    private String title = "";

    private final HudEditorModule hudEditorModule;
    private final Texture texture;
    private final Texture gearTexture;

    private ToolTipComponent currentToolTip;
    public ModuleSettingsComponent currentSettings;

    public ModuleListComponent(Module.ModuleType type) {
        super(StringUtils.capitalize(type.name().toLowerCase()), 100, 100, 150, 400);
        this.type = type;
        this.originalName = StringUtils.capitalize(type.name().toLowerCase());
        this.hudEditorModule = (HudEditorModule) Seppuku.INSTANCE.getModuleManager().find(HudEditorModule.class);
        this.texture = new Texture("module-" + type.name().toLowerCase() + ".png");
        this.gearTexture = new Texture("gear_wheel_modulelist.png");

        this.setSnappable(false);
        this.setLocked(true);
        this.setX(20);
        this.setY(20);
        this.setW(100);
        this.setH(100);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        super.render(mouseX, mouseY, partialTicks);

        if (!(mc.currentScreen instanceof GuiHudEditor))
            return;

        final ScaledResolution sr = new ScaledResolution(mc);

        // Render Y pos offset (make this all modular eventually...)
        int offsetY = 0;

        // Scrolling
        this.handleScrolling(mouseX, mouseY);

        // No dragging inside box
        final boolean insideTitlebar = mouseY <= this.getY() + TITLE_BAR_HEIGHT + BORDER;
        if (!insideTitlebar) {
            this.setDragging(false);
        }

        // clamp max width & height
        if (this.isResizeDragging()) {
            if (this.getH() > this.getTotalHeight()) {
                this.setH(this.getTotalHeight());
                this.setResizeDragging(false);
            }
        } else if (!this.isLocked() && this.currentSettings == null && this.getH() > this.getTotalHeight()) {
            this.setH(this.getTotalHeight());
        } else if (this.currentSettings == null && this.getH() > this.getTotalHeight() && this.getTotalHeight() > this.getInitialHeight()) {
            this.setH(this.getTotalHeight());
        }

        // Background & title
        //RenderUtil.begin2D();
        RenderUtil.drawRect(this.getX() - 1, this.getY() - 1, this.getX() + this.getW() + 1, this.getY() + this.getH() + 1, 0x99101010);
        RenderUtil.drawRect(this.getX(), this.getY(), this.getX() + this.getW(), this.getY() + this.getH(), 0xFF202020);
        GlStateManager.enableBlend();
        texture.bind();
        texture.render(this.getX() + BORDER, this.getY() + BORDER, TEXTURE_SIZE, TEXTURE_SIZE);
        GlStateManager.disableBlend();
        mc.fontRenderer.drawStringWithShadow(this.title, this.getX() + BORDER + /* texture width */ TEXTURE_SIZE + BORDER, this.getY() + BORDER, 0xFFFFFFFF);
        offsetY += mc.fontRenderer.FONT_HEIGHT + 1;

        // Behind list
        final float listTop = this.getY() + offsetY + BORDER;
        RenderUtil.drawRect(this.getX() + BORDER, listTop, this.getX() + this.getW() - SCROLL_WIDTH - BORDER, this.getY() + this.getH() - BORDER, 0xFF101010);

        // Scrollbar bg
        RenderUtil.drawRect(this.getX() + this.getW() - SCROLL_WIDTH, this.getY() + offsetY + BORDER, this.getX() + this.getW() - BORDER, this.getY() + this.getH() - BORDER, 0xFF101010);
        // Scrollbar highlights
        if (this.isMouseInside(mouseX, mouseY)) {
            if (mouseX >= (this.getX() + this.getW() - SCROLL_WIDTH) && mouseX <= (this.getX() + this.getW() - BORDER)) { // mouse is inside scroll area on x-axis
                RenderUtil.drawGradientRect(this.getX() + this.getW() - SCROLL_WIDTH, this.getY() + offsetY + BORDER, this.getX() + this.getW() - BORDER, this.getY() + offsetY + 8 + BORDER, 0xFF909090, 0x00101010);
                RenderUtil.drawGradientRect(this.getX() + this.getW() - SCROLL_WIDTH, this.getY() + this.getH() - 8 - BORDER, this.getX() + this.getW() - BORDER, this.getY() + this.getH() - BORDER, 0x00101010, 0xFF909090);
                float diffY = this.getY() + TITLE_BAR_HEIGHT + ((this.getH() - TITLE_BAR_HEIGHT) / 2);
                if (mouseY > diffY) {
                    RenderUtil.drawGradientRect(this.getX() + this.getW() - SCROLL_WIDTH, this.getY() + (this.getH() / 2) + BORDER + BORDER, this.getX() + this.getW() - BORDER, this.getY() + this.getH() - BORDER, 0x00101010, 0x90909090);
                } else {
                    RenderUtil.drawGradientRect(this.getX() + this.getW() - SCROLL_WIDTH, this.getY() + offsetY + BORDER, this.getX() + this.getW() - BORDER, this.getY() + (this.getH() / 2) + BORDER + BORDER, 0x90909090, 0x00101010);
                }
            }
        }
        // Scrollbar
        RenderUtil.drawRect(this.getX() + this.getW() - SCROLL_WIDTH, MathHelper.clamp((this.getY() + offsetY + BORDER) + ((this.getH() * this.scroll) / this.totalHeight), (this.getY() + offsetY + BORDER), (this.getY() + this.getH() - BORDER)), this.getX() + this.getW() - BORDER, MathHelper.clamp((this.getY() + this.getH() - BORDER) - (this.getH() * (this.totalHeight - this.getH() - this.scroll) / this.totalHeight), (this.getY() + offsetY + BORDER), (this.getY() + this.getH() - BORDER)), 0xFF909090);

        // Begin scissoring and render the module "buttons"
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        RenderUtil.glScissor(this.getX() + BORDER, this.getY() + offsetY + BORDER, this.getX() + this.getW() - BORDER - SCROLL_WIDTH, this.getY() + this.getH() - BORDER, sr);
        if (this.currentSettings != null) {
            this.title = this.currentSettings.module.getDisplayName();
            this.currentSettings.setX(this.getX() + BORDER);
            this.currentSettings.setY(this.getY() + offsetY + BORDER - this.scroll);
            this.currentSettings.setW(this.getW() - BORDER - SCROLL_WIDTH - BORDER - 2);
            this.currentSettings.setH(this.getH() - BORDER);
            this.currentSettings.render(mouseX, mouseY, partialTicks);
            offsetY += this.currentSettings.getH();
            for (HudComponent settingComponent : this.currentSettings.components) {
                //if (settingComponent.getY() > this.getY() + this.currentSettings.getH())
                offsetY += settingComponent.getH();
            }
        } else {
            this.title = this.originalName;
            for (Module module : Seppuku.INSTANCE.getModuleManager().getModuleList(this.type)) {
                // draw module button bg
                RenderUtil.drawRect(this.getX() + BORDER + TEXT_GAP, this.getY() + offsetY + BORDER + TEXT_GAP - this.scroll, this.getX() + BORDER + TEXT_GAP + this.getW() - BORDER - SCROLL_WIDTH - BORDER - 2, this.getY() + offsetY + BORDER + TEXT_GAP + mc.fontRenderer.FONT_HEIGHT - this.scroll, module.isEnabled() ? 0x453B005F : 0x451F1C22);

                final boolean insideModule = mouseX >= (this.getX() + BORDER) && mouseX <= (this.getX() + this.getW() - BORDER - SCROLL_WIDTH - 1) && mouseY >= (this.getY() + BORDER + mc.fontRenderer.FONT_HEIGHT + 1 + offsetY - this.scroll - mc.fontRenderer.FONT_HEIGHT + 1) && mouseY <= (this.getY() + BORDER + (mc.fontRenderer.FONT_HEIGHT) + 1 + offsetY - this.scroll);
                if (insideModule) { // draw options line
                    final boolean isHoveringOptions = mouseX >= (this.getX() + this.getW() - BORDER - SCROLL_WIDTH - 12) && mouseX <= (this.getX() + this.getW() - BORDER - SCROLL_WIDTH - 2) && mouseY >= (this.getY() + BORDER + mc.fontRenderer.FONT_HEIGHT + 1 + offsetY - this.scroll - mc.fontRenderer.FONT_HEIGHT + 1) && mouseY <= (this.getY() + BORDER + (mc.fontRenderer.FONT_HEIGHT) + 1 + offsetY - this.scroll);

                    // draw bg behind gear
                    RenderUtil.drawRect(this.getX() + BORDER + TEXT_GAP + this.getW() - BORDER - SCROLL_WIDTH - BORDER - 12, this.getY() + offsetY + BORDER + TEXT_GAP - this.scroll, this.getX() + BORDER + TEXT_GAP + this.getW() - BORDER - SCROLL_WIDTH - BORDER - 2, this.getY() + offsetY + BORDER + TEXT_GAP + mc.fontRenderer.FONT_HEIGHT - this.scroll, 0x45202020);
                    // draw gear
                    this.gearTexture.bind();
                    this.gearTexture.render(this.getX() + BORDER + TEXT_GAP + this.getW() - BORDER - SCROLL_WIDTH - BORDER - 11, this.getY() + offsetY + BORDER + TEXT_GAP - this.scroll + 0.5f, 8, 8);
                    if (isHoveringOptions) { // draw options line hover gradient
                        RenderUtil.drawGradientRect(this.getX() + BORDER + TEXT_GAP + this.getW() - BORDER - SCROLL_WIDTH - BORDER - 12, this.getY() + offsetY + BORDER + TEXT_GAP - this.scroll, this.getX() + BORDER + TEXT_GAP + this.getW() - BORDER - SCROLL_WIDTH - BORDER - 2, this.getY() + offsetY + BORDER + TEXT_GAP + mc.fontRenderer.FONT_HEIGHT - this.scroll, 0x50909090, 0x00101010);
                    }

                    // draw hover gradient
                    RenderUtil.drawGradientRect(this.getX() + BORDER + TEXT_GAP, this.getY() + offsetY + BORDER + TEXT_GAP - this.scroll, this.getX() + BORDER + TEXT_GAP + this.getW() - BORDER - SCROLL_WIDTH - BORDER - 2, this.getY() + offsetY + BORDER + TEXT_GAP + mc.fontRenderer.FONT_HEIGHT - this.scroll, 0x30909090, 0x00101010);
                }

                // draw module name
                mc.fontRenderer.drawStringWithShadow(module.getDisplayName(), this.getX() + BORDER + TEXT_GAP + 1, this.getY() + offsetY + BORDER + TEXT_GAP - this.scroll, module.isEnabled() ? 0xFFC255FF : 0xFF7A6E80);

                offsetY += mc.fontRenderer.FONT_HEIGHT + TEXT_GAP;
            }
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // Over list
        if (this.scroll > 6) {
            RenderUtil.drawGradientRect(this.getX() + BORDER, listTop, this.getX() + this.getW() - SCROLL_WIDTH - BORDER, listTop + 8, 0xFF101010, 0x00000000);
        }
        if (this.getH() != this.getTotalHeight() && this.scroll != (this.totalHeight - this.getH())) {
            RenderUtil.drawGradientRect(this.getX() + BORDER, this.getY() + this.getH() - BORDER - 8, this.getX() + this.getW() - SCROLL_WIDTH - BORDER, this.getY() + this.getH() - BORDER, 0x00000000, 0xFF101010);
        }

        // Handle tooltips
        if (this.hudEditorModule != null && this.hudEditorModule.tooltips.getValue() && !insideTitlebar) {
            if (this.isMouseInside(mouseX, mouseY)) {
                String tooltipText = "";
                int height = BORDER;

                if (this.currentSettings != null) {
                    for (HudComponent valueComponent : this.currentSettings.components) {
                        if (valueComponent.isMouseInside(mouseX, mouseY)) {
                            tooltipText = valueComponent.getTooltipText();
                        } else {
                            if (this.currentToolTip != null) {
                                if (this.currentToolTip.text.equals(valueComponent.getTooltipText())) {
                                    this.currentToolTip = null;
                                }
                            }
                        }
                        height += mc.fontRenderer.FONT_HEIGHT + TEXT_GAP;
                    }
                } else {
                    for (Module module : Seppuku.INSTANCE.getModuleManager().getModuleList(this.type)) {
                        final boolean insideComponent = mouseX >= (this.getX() + BORDER) && mouseX <= (this.getX() + this.getW() - BORDER - SCROLL_WIDTH) && mouseY >= (this.getY() + BORDER + mc.fontRenderer.FONT_HEIGHT + 1 + height - this.scroll) && mouseY <= (this.getY() + BORDER + (mc.fontRenderer.FONT_HEIGHT * 2) + 1 + height - this.scroll);
                        if (insideComponent) {
                            tooltipText = module.getDesc();
                        } else {
                            if (this.currentToolTip != null) {
                                if (this.currentToolTip.text.equals(module.getDesc())) {
                                    this.currentToolTip = null;
                                }
                            }
                        }
                        height += mc.fontRenderer.FONT_HEIGHT + TEXT_GAP;
                    }
                }

                if (!tooltipText.equals("")) {
                    if (this.currentToolTip == null) {
                        this.currentToolTip = new ToolTipComponent(tooltipText);
                    } else {
                        this.currentToolTip.render(mouseX, mouseY, partialTicks);
                    }
                } else {
                    this.removeTooltip();
                }
            } else {
                this.removeTooltip();
            }
        }
        //RenderUtil.end2D();

        // figures up a "total height (pixels)" of the inside of the list area (for calculating scroll height)
        this.totalHeight = BORDER + TEXT_GAP + offsetY + BORDER;
    }

    @Override
    public void mouseRelease(int mouseX, int mouseY, int button) {
        super.mouseRelease(mouseX, mouseY, button);

        final boolean inside = this.isMouseInside(mouseX, mouseY);
        final int titleBarHeight = mc.fontRenderer.FONT_HEIGHT + 1;
        final boolean insideTitlebar = mouseY <= this.getY() + BORDER + titleBarHeight;

        if (inside && !insideTitlebar && !isResizeDragging()) {
            if (this.currentSettings != null) {
                this.currentSettings.mouseRelease(mouseX, mouseY, button);
            } else {
                int offsetY = BORDER;
                for (Module module : Seppuku.INSTANCE.getModuleManager().getModuleList(this.type)) {
                    final boolean insideComponent = mouseX >= (this.getX() + BORDER) && mouseX <= (this.getX() + this.getW() - BORDER - SCROLL_WIDTH - 1) && mouseY >= (this.getY() + BORDER + mc.fontRenderer.FONT_HEIGHT + 1 + offsetY - this.scroll) && mouseY <= (this.getY() + BORDER + (mc.fontRenderer.FONT_HEIGHT * 2) + 1 + offsetY - this.scroll);
                    if (insideComponent) {
                        switch (button) {
                            case 0:
                                if (mouseX >= (this.getX() + this.getW() - BORDER - SCROLL_WIDTH - 12) && mouseX <= (this.getX() + this.getW() - BORDER - SCROLL_WIDTH - 1)) {
                                    this.removeTooltip();
                                    this.currentSettings = new ModuleSettingsComponent(module, this);
                                    this.setOldScroll(this.getScroll());
                                    this.setScroll(0);
                                } else {
                                    module.toggle();
                                }
                                this.setDragging(false);
                                break;
                            case 1:
                                this.removeTooltip();
                                this.currentSettings = new ModuleSettingsComponent(module, this);
                                this.setOldScroll(this.getScroll());
                                this.setScroll(0);
                                break;
                        }
                    }
                    offsetY += mc.fontRenderer.FONT_HEIGHT + TEXT_GAP;
                }
            }

            if (button == 0) {
                if (mouseX >= (this.getX() + this.getW() - SCROLL_WIDTH) && mouseX <= (this.getX() + this.getW() - BORDER)) { // mouse is inside scroll area on x-axis
                    float diffY = this.getY() + TITLE_BAR_HEIGHT + ((this.getH() - TITLE_BAR_HEIGHT) / 2);
                    if (mouseY > diffY) {
                        scroll += 10;
                    } else {
                        scroll -= 10;
                    }
                } else { // not inside scroll bar zone
                    //Seppuku.INSTANCE.getConfigManager().saveAll();
                }
            }
        }
    }

    @Override
    public void mouseClick(int mouseX, int mouseY, int button) {
        final boolean insideDragZone = mouseY <= this.getY() + TITLE_BAR_HEIGHT + BORDER || mouseY >= ((this.getY() + this.getH()) - CLICK_ZONE);
        if (insideDragZone) {
            super.mouseClick(mouseX, mouseY, button);
        } else {
            if (this.currentSettings != null) {
                this.currentSettings.mouseClick(mouseX, mouseY, button);
            }
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        super.keyTyped(typedChar, keyCode);

        if (this.currentSettings != null) {
            this.currentSettings.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void onClosed() {
        super.onClosed();

        if (this.currentToolTip != null) {
            this.currentToolTip = null;
        }
    }

    private void handleScrolling(int mouseX, int mouseY) {
        if (this.isMouseInside(mouseX, mouseY) && Mouse.hasWheel()) {
            this.scroll += -(Mouse.getDWheel() / 5);

            if (this.scroll < 0) {
                this.scroll = 0;
            }

            if (this.scroll > this.totalHeight - this.getH()) {
                this.scroll = this.totalHeight - (int) this.getH();
            }

            if (this.getOldScroll() != 0) {
                if (this.currentSettings == null) {
                    this.setScroll(this.getOldScroll());
                    this.setOldScroll(0);
                }
            }
        }
    }

    public void removeTooltip() {
        if (this.currentToolTip != null)
            this.currentToolTip = null;
    }

    public Module.ModuleType getType() {
        return type;
    }

    public int getScroll() {
        return scroll;
    }

    public void setScroll(int scroll) {
        this.scroll = scroll;
    }

    public int getOldScroll() {
        return oldScroll;
    }

    public void setOldScroll(int oldScroll) {
        this.oldScroll = oldScroll;
    }

    public int getTotalHeight() {
        return totalHeight;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getTitle() {
        return title;
    }

    public Texture getTexture() {
        return texture;
    }

    public ToolTipComponent getCurrentToolTip() {
        return currentToolTip;
    }

    public ModuleSettingsComponent getCurrentSettings() {
        return currentSettings;
    }

    public static class BackButtonComponent extends HudComponent {
        public final ModuleListComponent parentModuleList;

        public BackButtonComponent(ModuleListComponent parentModuleList) {
            super("Back", "Go back.");
            this.parentModuleList = parentModuleList;
        }

        @Override
        public void render(int mouseX, int mouseY, float partialTicks) {
            super.render(mouseX, mouseY, partialTicks);

            if (isMouseInside(mouseX, mouseY))
                RenderUtil.drawGradientRect(this.getX(), this.getY(), this.getX() + this.getW(), this.getY() + this.getH(), 0x30909090, 0x00101010);

            RenderUtil.drawRect(this.getX(), this.getY(), this.getX() + this.getW(), this.getY() + this.getH(), 0x45303030);
            Minecraft.getMinecraft().fontRenderer.drawString(this.getName(), (int) this.getX() + 1, (int) this.getY() + 1, -1);
        }

        @Override
        public void mouseRelease(int mouseX, int mouseY, int button) {
            super.mouseRelease(mouseX, mouseY, button);

            if (!this.isMouseInside(mouseX, mouseY) || button != 0)
                return;

            for (HudComponent component : Seppuku.INSTANCE.getHudManager().getComponentList()) {
                if (component instanceof ModuleListComponent) {
                    ModuleListComponent moduleList = (ModuleListComponent) component;
                    if (moduleList.getName().equals(parentModuleList.getName())) {
                        moduleList.currentSettings = null;
                        moduleList.removeTooltip();
                    }
                }
            }
        }
    }

    public static class ModuleSettingsComponent extends HudComponent {
        public final Module module;
        public final List<HudComponent> components;
        private final ModuleListComponent parentModuleList;

        public ModuleSettingsComponent(Module module, ModuleListComponent parentModuleList) {
            super(module.getDisplayName());

            this.module = module;
            this.components = new ArrayList<>();
            this.parentModuleList = parentModuleList;

            //components.add(new ButtonComponent(this.getName()));
            components.add(new BackButtonComponent(parentModuleList));

            TextComponent keybindText = new TextComponent("Keybind", module.getKey().toLowerCase(), false);
            keybindText.setTooltipText("The current key for toggling this module.");
            keybindText.textListener = new TextComponent.TextComponentListener() {
                @Override
                public void onKeyTyped(int keyCode) {
                    if (keyCode == Keyboard.KEY_ESCAPE) {
                        module.setKey("NONE");
                        keybindText.displayValue = "none";
                        keybindText.focused = false;
                        // re-open the hud editor
                        final HudEditorModule hudEditorModule = (HudEditorModule) Seppuku.INSTANCE.getModuleManager().find(HudEditorModule.class);
                        if (hudEditorModule != null) {
                            hudEditorModule.displayHudEditor();
                        }
                    } else {
                        String newKey = Keyboard.getKeyName(keyCode);
                        module.setKey(newKey);
                        keybindText.displayValue = newKey.length() == 1 /* is letter */ ? newKey.substring(1) : newKey.toLowerCase();
                        keybindText.focused = false;
                    }
                }
            };
            components.add(keybindText);

            ButtonComponent enabledButton = new ButtonComponent("Enabled");
            enabledButton.setTooltipText("Enables this module.");
            enabledButton.enabled = module.isEnabled();
            enabledButton.mouseClickListener = new ComponentListener() {
                @Override
                public void onComponentEvent() {
                    module.toggle();
                }
            };
            components.add(enabledButton);

            ButtonComponent hiddenButton = new ButtonComponent("Hidden");
            hiddenButton.setTooltipText("Hides this module from the enabled mods list.");
            hiddenButton.enabled = module.isHidden();
            hiddenButton.mouseClickListener = new ComponentListener() {
                @Override
                public void onComponentEvent() {
                    module.setHidden(hiddenButton.enabled);
                }
            };
            components.add(hiddenButton);

            ColorComponent colorComponent = new ColorComponent("List Color", module.getColor());
            colorComponent.setTooltipText("The color for this module in the enabled mods list.");
            colorComponent.returnListener = new ComponentListener() {
                @Override
                public void onComponentEvent() {
                    module.setColor(colorComponent.getCurrentColor().getRGB());
                    Seppuku.INSTANCE.getConfigManager().save(ModuleConfig.class);
                }
            };
            components.add(colorComponent);

            for (Value value : module.getValueList()) {
                if (value.getValue() instanceof Boolean) {
                    ButtonComponent valueButton = new ButtonComponent(value.getName());
                    valueButton.setTooltipText(value.getDesc());
                    valueButton.enabled = (Boolean) value.getValue();
                    valueButton.mouseClickListener = new ComponentListener() {
                        @Override
                        public void onComponentEvent() {
                            value.setValue(valueButton.enabled);
                            Seppuku.INSTANCE.getEventManager().dispatchEvent(new EventUIValueChanged(value));
                        }
                    };
                    components.add(valueButton);
                } else if (value.getValue() instanceof Number) {
                    /*TextComponent valueNumberText = new TextComponent(value.getName(), value.getValue().toString(), true);
                    valueNumberText.setTooltipText(value.getDesc() + " " + ChatFormatting.GRAY + "(" + value.getMin() + " - " + value.getMax() + ")");
                    valueNumberText.returnListener = new ComponentListener() {
                        @Override
                        public void onComponentEvent() {
                            try {
                                if (value.getValue() instanceof Integer) {
                                    value.setValue(Integer.parseInt(valueNumberText.displayValue));
                                } else if (value.getValue() instanceof Double) {
                                    value.setValue(Double.parseDouble(valueNumberText.displayValue));
                                } else if (value.getValue() instanceof Float) {
                                    value.setValue(Float.parseFloat(valueNumberText.displayValue));
                                } else if (value.getValue() instanceof Long) {
                                    value.setValue(Long.parseLong(valueNumberText.displayValue));
                                } else if (value.getValue() instanceof Byte) {
                                    value.setValue(Byte.parseByte(valueNumberText.displayValue));
                                }
                                Seppuku.INSTANCE.getConfigManager().save(ModuleConfig.class); // save module configs
                            } catch (NumberFormatException e) {
                                Seppuku.INSTANCE.logfChat("%s - %s: Invalid number format.", module.getDisplayName(), value.getName());
                            }
                        }
                    };
                    components.add(valueNumberText);
                    this.addComponentToButtons(valueNumberText);*/
                    //TODO: after v3.1
                    SliderComponent sliderComponent = new SliderComponent(value.getName(), value);
                    sliderComponent.setTooltipText(value.getDesc() + " " + ChatFormatting.GRAY + "(" + value.getMin() + " - " + value.getMax() + ")");
                    components.add(sliderComponent);
                    this.addComponentToButtons(sliderComponent);
                } else if (value.getValue() instanceof Enum) {
                    final Enum val = (Enum) value.getValue();
                    final int size = val.getClass().getEnumConstants().length;
                    final StringBuilder options = new StringBuilder();

                    for (int i = 0; i < size; i++) {
                        final Enum option = val.getClass().getEnumConstants()[i];
                        options.append(option.name().toLowerCase()).append((i == size - 1) ? "" : ", ");
                    }

                    /*TextComponent valueText = new TextComponent(value.getName(), value.getValue().toString().toLowerCase(), false);
                    valueText.setTooltipText(value.getDesc() + " " + ChatFormatting.GRAY + "(" + options.toString() + ")");
                    valueText.returnListener = new ComponentListener() {
                        @Override
                        public void onComponentEvent() {
                            if (value.getEnum(valueText.displayValue) != -1) {
                                value.setEnumValue(valueText.displayValue);
                                Seppuku.INSTANCE.getConfigManager().save(ModuleConfig.class); // save configs
                                Seppuku.INSTANCE.getEventManager().dispatchEvent(new EventUIValueChanged(value));
                            } else {
                                Seppuku.INSTANCE.logfChat("%s - %s: Invalid entry.", module.getDisplayName(), value.getName());
                            }
                        }
                    };
                    components.add(valueText);
                    this.addComponentToButtons(valueText);*/

                    CarouselComponent carouselComponent = new CarouselComponent(value.getName(), value);
                    carouselComponent.setTooltipText(value.getDesc() + " " + ChatFormatting.GRAY + "(" + options.toString() + ")");
                    components.add(carouselComponent);
                    this.addComponentToButtons(carouselComponent);
                } else if (value.getValue() instanceof String) {
                    TextComponent valueText = new TextComponent(value.getName(), value.getValue().toString().toLowerCase(), false);
                    valueText.setTooltipText(value.getDesc());
                    valueText.returnListener = new ComponentListener() {
                        @Override
                        public void onComponentEvent() {
                            if (valueText.displayValue.length() > 0) {
                                value.setValue(valueText.displayValue);
                                Seppuku.INSTANCE.getConfigManager().save(ModuleConfig.class); // save configs
                                Seppuku.INSTANCE.getEventManager().dispatchEvent(new EventUIValueChanged(value));
                            } else {
                                Seppuku.INSTANCE.logfChat("%s - %s: Not enough input.", module.getDisplayName(), value.getName());
                            }
                        }
                    };
                    components.add(valueText);
                    this.addComponentToButtons(valueText);
                } else if (value.getValue() instanceof Color) {
                    ColorComponent valueColor = new ColorComponent(value.getName(), ((Color) value.getValue()).getRGB());
                    valueColor.setTooltipText("Edit the color of: " + value.getName());
                    valueColor.returnListener = new ComponentListener() {
                        @Override
                        public void onComponentEvent() {
                            value.setValue(valueColor.getCurrentColor());
                            Seppuku.INSTANCE.getConfigManager().save(ModuleConfig.class);
                            Seppuku.INSTANCE.getEventManager().dispatchEvent(new EventUIValueChanged(value));
                        }
                    };
                    components.add(valueColor);
                    this.addComponentToButtons(valueColor);
                } else if (value.getValue() instanceof List) {
                    final List<?> valueList = ((List<?>) value.getValue());
                    if (!valueList.isEmpty()) {
                        if (valueList.get(0) instanceof Item) { // Items
                            ItemsComponent itemsComponent = new ItemsComponent(value);
                            components.add(itemsComponent);
                            this.addComponentToButtons(itemsComponent);
                        } else if (valueList.get(0) instanceof Block) { // Blocks
                            BlocksComponent blocksComponent = new BlocksComponent(value);
                            components.add(blocksComponent);
                            this.addComponentToButtons(blocksComponent);
                        }
                    }
                }
            }
        }

        @Override
        public void render(int mouseX, int mouseY, float partialTicks) {
            super.render(mouseX, mouseY, partialTicks);

            int offsetY = 1;
            for (HudComponent component : this.components) {
                int offsetX = 0;

                boolean skipRendering = false;
                for (HudComponent otherComponent : this.components) {
                    if (otherComponent == component || otherComponent.getName().equals(component.getName()))
                        continue;

                        boolean isChildComponent = component.getName().toLowerCase().startsWith(otherComponent.getName().toLowerCase());
                        if (isChildComponent) {
                            if (!otherComponent.rightClickEnabled) {
                                skipRendering = true;
                            }

                            offsetX += 4;
                        }
                }

                if (skipRendering)
                    continue;

                component.setX(this.getX() + 1 + offsetX);
                component.setY(this.getY() + offsetY);
                component.setW(this.getW() - offsetX);
                component.setH(Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT);
                component.render(mouseX, mouseY, partialTicks);

                if (offsetX > 0) {
                    RenderUtil.drawLine(component.getX() - offsetX + 1, component.getY(), component.getX() - offsetX + 1, component.getY() + component.getH(), 2.0f, 0x90707070);
                    RenderUtil.drawLine(component.getX() - offsetX + 1.5f, component.getY() + component.getH() / 2, component.getX() - 0.5f, component.getY() + component.getH() / 2, 2.0f, 0x90707070);
                }

                offsetY += component.getH() + 1;
            }
        }

        @Override
        public void mouseClick(int mouseX, int mouseY, int button) {
            super.mouseClick(mouseX, mouseY, button);
            for (HudComponent component : this.components) {
                component.mouseClick(mouseX, mouseY, button);
            }
        }

        @Override
        public void mouseClickMove(int mouseX, int mouseY, int button) {
            super.mouseClickMove(mouseX, mouseY, button);
            for (HudComponent component : this.components) {
                component.mouseClickMove(mouseX, mouseY, button);
            }
        }

        @Override
        public void mouseRelease(int mouseX, int mouseY, int button) {
            super.mouseRelease(mouseX, mouseY, button);
            for (HudComponent component : this.components) {
                component.mouseRelease(mouseX, mouseY, button);
            }
        }

        @Override
        public void keyTyped(char typedChar, int keyCode) {
            super.keyTyped(typedChar, keyCode);
            for (HudComponent component : this.components) {
                component.keyTyped(typedChar, keyCode);
            }
        }

        private void addComponentToButtons(HudComponent hudComponent) {
            for (HudComponent component : this.components) {
                if (component == hudComponent)
                    continue;

                boolean similarName = hudComponent.getName().toLowerCase().startsWith(component.getName().toLowerCase());
                if (similarName) {
                    component.subComponents++;
                    hudComponent.setDisplayName(hudComponent.getName().substring(component.getName().length()));
                }
            }
        }
    }
}
