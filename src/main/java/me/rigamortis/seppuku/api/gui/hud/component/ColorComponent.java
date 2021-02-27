package me.rigamortis.seppuku.api.gui.hud.component;

import me.rigamortis.seppuku.Seppuku;
import me.rigamortis.seppuku.api.texture.Texture;
import me.rigamortis.seppuku.api.util.ColorUtil;
import me.rigamortis.seppuku.api.util.RenderUtil;
import net.minecraft.client.Minecraft;

import java.awt.*;

public class ColorComponent extends TextComponent {

    private Color currentColor;

    private static final int BORDER = 1;
    private static final int TEXT_BLOCK_PADDING = 1;
    private static final int COLOR_SIZE = 7;

    private String customDisplayValue;

    private final Texture gearTexture;
    private final Texture gearTextureEnabled;

    public ColorComponent(String name, int defaultColor) {
        super(name, String.valueOf(defaultColor), false);
        this.currentColor = new Color(defaultColor);
        this.displayValue = "#" + Integer.toHexString(this.currentColor.getRGB()).toLowerCase().substring(2);
        this.gearTexture = new Texture("gear_wheel.png");
        this.gearTextureEnabled = new Texture("gear_wheel-enabled.png");

        this.setH(9);
    }

    public ColorComponent(String name, int defaultColor, String customDisplayValue) {
        this(name, defaultColor);
        this.customDisplayValue = customDisplayValue;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        //super.render(mouseX, mouseY, partialTicks);
        /*if (this.focused) {
            this.setH(50);
        } else {
            this.setH(9);
        }*/

        if (isMouseInside(mouseX, mouseY))
            RenderUtil.drawGradientRect(this.getX(), this.getY(), this.getX() + this.getW(), this.getY() + this.getH(), 0x30909090, 0x00101010);

        // draw bg rect
        RenderUtil.drawRect(this.getX(), this.getY(), this.getX() + this.getW() - (this.focused ? 20 : 10), this.getY() + this.getH(), 0x45303030);

        // draw color rect
        RenderUtil.drawRect(this.getX() + BORDER, this.getY() + BORDER, this.getX() + BORDER + COLOR_SIZE, this.getY() + BORDER + COLOR_SIZE, ColorUtil.changeAlpha(this.currentColor.getRGB(), 0xFF));

        // draw name / display value
        String displayedName = this.getName();
        if (this.focused) {
            displayedName = this.displayValue;
        } else if (customDisplayValue != null) {
            displayedName = customDisplayValue;
        } else if (this.getDisplayName() != null) {
            displayedName = this.getDisplayName();
        }
        Minecraft.getMinecraft().fontRenderer.drawString(displayedName, (int) this.getX() + BORDER + COLOR_SIZE + BORDER, (int) this.getY() + BORDER, this.focused ? 0xFFFFFFFF : 0xFFAAAAAA);

        // draw bg rect behind right button
        RenderUtil.drawRect(this.getX() + this.getW() - (this.focused ? 20 : 10), this.getY(), this.getX() + this.getW(), this.getY() + this.getH(), 0x45202020);

        if (this.focused) {
            if (!this.selectedText.equals("")) {
                RenderUtil.drawRect(this.getX() + BORDER + COLOR_SIZE + BORDER, this.getY(), this.getX() + BORDER + COLOR_SIZE + BORDER + Minecraft.getMinecraft().fontRenderer.getStringWidth(this.displayValue), this.getY() + this.getH(), 0x45FFFFFF);
            }

            float blockX = this.getX() + BORDER + Minecraft.getMinecraft().fontRenderer.getStringWidth(this.displayValue) + COLOR_SIZE + BORDER + TEXT_BLOCK_PADDING;
            float blockY = this.getY() + TEXT_BLOCK_PADDING;
            int blockWidth = 2;
            int blockHeight = Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT - 2;
            RenderUtil.drawRect(blockX, blockY, blockX + blockWidth, blockY + blockHeight, 0xFFFFFFFF);

            // draw gear
            RenderUtil.drawRect(this.getX() + this.getW() - 10, this.getY(), this.getX() + this.getW(), this.getY() + this.getH(), 0xFF101010);
            this.gearTextureEnabled.bind();
            this.gearTextureEnabled.render(this.getX() + this.getW() - 9, this.getY() + 0.5f, 8, 8);

            // check
            RenderUtil.drawRect(this.getX() + this.getW() - 20, this.getY(), this.getX() + this.getW() - 10, this.getY() + this.getH(), 0xFF101010);
            this.checkTexture.bind();
            this.checkTexture.render(this.getX() + this.getW() - 19, this.getY() + 0.5f, 8, 8);

            // handle holding backspace
            this.handleBackspacing();
        } else {
            // draw gear
            this.gearTexture.bind();
            this.gearTexture.render(this.getX() + this.getW() - 9, this.getY() + 0.5f, 8, 8);
        }
    }

    @Override
    public void mouseRelease(int mouseX, int mouseY, int button) {
        super.mouseRelease(mouseX, mouseY, button);

        if (!this.focused) // must be focused
            return;

        if (button == 0) {
            // check for clicking check
            if (mouseX >= this.getX() + this.getW() - 20 && mouseX <= this.getX() + this.getW() - 10 && mouseY >= this.getY() && mouseY <= this.getY() + this.getH()) {
                this.enterPressed();
            }
        }
    }

    @Override
    protected void enterPressed() {
        try {
            int newColor = (int) Long.parseLong(this.displayValue.replaceAll("#", ""), 16);
            this.currentColor = new Color(newColor);
        } catch (NumberFormatException e) {
            Seppuku.INSTANCE.logChat(this.getName() + ": Invalid color format. Correct format example: \"ff0000\" for red.");
        } catch (Exception e) {
            Seppuku.INSTANCE.logChat(this.getName() + ": Something went terribly wrong while setting the color. Please try again.");
        }

        super.enterPressed();
    }

    public Color getCurrentColor() {
        return currentColor;
    }

    public void setCurrentColor(Color currentColor) {
        this.currentColor = currentColor;
    }

    public String getCustomDisplayValue() {
        return customDisplayValue;
    }

    public void setCustomDisplayValue(String customDisplayValue) {
        this.customDisplayValue = customDisplayValue;
    }
}
