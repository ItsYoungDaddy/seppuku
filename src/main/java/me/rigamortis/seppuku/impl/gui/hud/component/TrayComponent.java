package me.rigamortis.seppuku.impl.gui.hud.component;

import me.rigamortis.seppuku.Seppuku;
import me.rigamortis.seppuku.api.event.gui.hud.EventHubComponentClick;
import me.rigamortis.seppuku.api.event.minecraft.EventDisplayGui;
import me.rigamortis.seppuku.api.gui.hud.component.DraggableHudComponent;
import me.rigamortis.seppuku.api.gui.hud.component.HudComponent;
import me.rigamortis.seppuku.api.gui.hud.component.ToolTipComponent;
import me.rigamortis.seppuku.api.texture.Texture;
import me.rigamortis.seppuku.api.util.RenderUtil;
import me.rigamortis.seppuku.impl.gui.hud.GuiHudEditor;
import me.rigamortis.seppuku.impl.module.ui.HudEditorModule;
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener;

import java.util.ArrayList;
import java.util.List;

public class TrayComponent extends DraggableHudComponent {

    private static final int TEXTURE_WIDTH = 16;
    private static final int TEXTURE_HEIGHT = 16;

    private ToolTipComponent currentToolTip;

    private final List<TrayButtonComponent> buttons = new ArrayList<>();

    public TrayComponent() {
        super("Tray");

        buttons.add(new TrayButtonComponent("hub"));
        buttons.add(new TrayButtonComponent("colors"));
        buttons.add(new TrayButtonComponent("combat"));
        buttons.add(new TrayButtonComponent("movement"));
        buttons.add(new TrayButtonComponent("render"));
        buttons.add(new TrayButtonComponent("player"));
        buttons.add(new TrayButtonComponent("world"));
        buttons.add(new TrayButtonComponent("misc"));

        this.setVisible(true);
    }

    @Override
    public void mouseRelease(int mouseX, int mouseY, int button) {
        super.mouseRelease(mouseX, mouseY, button);

        if (this.isMouseInside(mouseX, mouseY) && button == 1) { // inside the tray, and is a right click
            for (TrayButtonComponent trayButton : buttons) {
                trayButton.mouseRelease(mouseX, mouseY, button); // handle mouse logic inside of tray button
            }
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        super.render(mouseX, mouseY, partialTicks);

        // ensure we are in the hud editor
        boolean isInHudEditor = mc.currentScreen instanceof GuiHudEditor;
        if (!isInHudEditor) {
            this.setW(0);
            this.setH(0);
            this.setEmptyH(TEXTURE_HEIGHT);
            return;
        }

        for (int i = 0; i < buttons.size(); i++) {
            TrayButtonComponent trayButton = buttons.get(i);
            if (trayButton != null) {
                trayButton.setX(this.getX() + (i * TEXTURE_WIDTH)); // divide them up by (position in array * texture width)
                trayButton.setY(this.getY()); // keep the same y pos
                trayButton.render(mouseX, mouseY, partialTicks); // render the tray button
            }
        }

        if (this.isMouseInside(mouseX, mouseY)) {
            final HudEditorModule hudEditorModule = (HudEditorModule) Seppuku.INSTANCE.getModuleManager().find(HudEditorModule.class);
            if (hudEditorModule != null) {
                if (hudEditorModule.tooltips.getValue()) {
                    if (this.currentToolTip != null) {
                        this.currentToolTip.render(mouseX, mouseY, partialTicks);
                    } else {
                        final String tooltipText = "Tray (right click to open a panel)";
                        this.currentToolTip = new ToolTipComponent(tooltipText);
                    }
                }
            }
        } else {
            if (this.currentToolTip != null) {
                this.currentToolTip = null;
            }
        }

        this.setW(buttons.size() * TEXTURE_WIDTH);
        this.setH(TEXTURE_HEIGHT);
    }

    public static class TrayButtonComponent extends HudComponent {

        private boolean pressed;
        private final Texture textureOff;
        private final Texture textureOn;

        public TrayButtonComponent(String name) {
            super(name);
            this.pressed = false;
            this.textureOff = new Texture("module-" + name + ".png");
            this.textureOn = new Texture("module-" + name + "-enabled.png");

            this.setW(TEXTURE_WIDTH);
            this.setH(TEXTURE_HEIGHT);

            Seppuku.INSTANCE.getEventManager().addEventListener(this);
        }

        @Listener
        public void onDisplayGui(EventDisplayGui event) {
            if (event.getScreen() instanceof GuiHudEditor) {
                HudComponent component = Seppuku.INSTANCE.getHudManager().findComponent(this.getName()); // find our component from the name
                if (component != null) {
                    this.setPressed(component.isVisible());
                }
            }
        }

        @Listener
        public void onHubComponentClick(EventHubComponentClick event) {
            if (this.getName().equalsIgnoreCase(event.hubComponentName)) {
                this.setPressed(event.hubComponentVisible);
            }
        }

        @Override
        public void render(int mouseX, int mouseY, float partialTicks) {
            super.render(mouseX, mouseY, partialTicks);

            // background
            RenderUtil.drawRect(this.getX(), this.getY(), this.getX() + this.getW(), this.getY() + this.getH(), 0x75101010);

            // overlay
            if (isPressed()) {
                textureOn.bind();
                textureOn.render(this.getX(), this.getY(), TEXTURE_WIDTH, TEXTURE_HEIGHT);
            } else {
                textureOff.bind();
                textureOff.render(this.getX(), this.getY(), TEXTURE_WIDTH, TEXTURE_HEIGHT);
            }
        }

        @Override
        public void mouseRelease(int mouseX, int mouseY, int button) {
            super.mouseRelease(mouseX, mouseY, button);

            if (this.isMouseInside(mouseX, mouseY)) {
                HudComponent component = Seppuku.INSTANCE.getHudManager().findComponent(this.getName()); // find our clicked component
                if (component != null) {
                    component.setVisible(!component.isVisible());
                    this.setPressed(component.isVisible());
                }
            }
        }

        public boolean isPressed() {
            return pressed;
        }

        public void setPressed(boolean pressed) {
            this.pressed = pressed;
        }
    }
}
