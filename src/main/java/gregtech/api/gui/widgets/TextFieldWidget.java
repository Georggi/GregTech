package gregtech.api.gui.widgets;

import gregtech.api.gui.Widget;
import gregtech.api.util.MCGuiUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class TextFieldWidget extends Widget {

    @SideOnly(Side.CLIENT)
    protected GuiTextField textField;

    protected int maxStringLength = 32;
    protected Predicate<String> textValidator;
    protected final Supplier<String> textSupplier;
    protected final Consumer<String> textResponder;
    protected String currentString;

    public TextFieldWidget(int xPosition, int yPosition, int width, int height, boolean enableBackground, Supplier<String> textSupplier, Consumer<String> textResponder) {
        super();
        if(isClientSide()) {
            FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
            this.textField = new GuiTextField(0, fontRenderer, xPosition, yPosition, width, height);
            this.textField.setCanLoseFocus(true);
            this.textField.setEnableBackgroundDrawing(enableBackground);
            this.textField.setMaxStringLength(maxStringLength);
            this.textField.setGuiResponder(MCGuiUtil.createTextFieldResponder(this::onTextChanged));
        }
        this.textSupplier = textSupplier;
        this.textResponder = textResponder;
    }

    @Override
    public void drawInBackground(int mouseX, int mouseY) {
        super.drawInBackground(mouseX, mouseY);
        this.textField.drawTextBox();
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        this.textField.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void keyTyped(char charTyped, int keyCode) {
        super.keyTyped(charTyped, keyCode);
        this.textField.textboxKeyTyped(charTyped, keyCode);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if(!textSupplier.get().equals(currentString)) {
            this.currentString = textSupplier.get();
            writeUpdateInfo(1, buffer -> buffer.writeString(currentString));
        }
    }

    @Override
    public void readUpdateInfo(int id, PacketBuffer buffer) {
        super.readUpdateInfo(id, buffer);
        if(id == 1) {
            this.currentString = buffer.readString(Short.MAX_VALUE);
            this.textField.setText(currentString);
        }
    }

    protected void onTextChanged(String newTextString) {
        if(textValidator.test(newTextString)) {
            writeClientAction(1, buffer -> buffer.writeString(newTextString));
        }
    }

    @Override
    public void handleClientAction(int id, PacketBuffer buffer) {
        super.handleClientAction(id, buffer);
        if(id == 1) {
            String clientText = buffer.readString(Short.MAX_VALUE);
            clientText = clientText.substring(0, Math.min(clientText.length(), maxStringLength));
            if(textValidator.test(clientText)) {
                this.currentString = clientText;
                this.textResponder.accept(clientText);
            }
        }
    }

    public TextFieldWidget setTextColor(int textColor) {
        if(isClientSide()) {
            this.textField.setTextColor(textColor);
        }
        return this;
    }

    public TextFieldWidget setMaxStringLength(int maxStringLength) {
        this.maxStringLength = maxStringLength;
        if(isClientSide()) {
            this.textField.setMaxStringLength(maxStringLength);
        }
        return this;
    }

    public TextFieldWidget setValidator(Predicate<String> validator) {
        this.textValidator = validator;
        if(isClientSide()) {
            this.textField.setValidator(validator::test);
        }
        return this;
    }

    private static boolean isClientSide() {
        return FMLCommonHandler.instance().getSide().isClient();
    }
}
