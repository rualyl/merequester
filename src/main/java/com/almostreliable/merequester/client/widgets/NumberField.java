package com.almostreliable.merequester.client.widgets;

import appeng.client.gui.MathExpressionParser;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ConfirmableTextField;
import appeng.client.gui.widgets.NumberEntryWidget;
import appeng.core.localization.GuiText;
import com.almostreliable.merequester.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;

/**
 * yoinked from {@link NumberEntryWidget}
 */
public class NumberField extends ConfirmableTextField {

    private static final int WIDTH = 52;
    private static final int HEIGHT = 12;

    private static final int TEXT_COLOR = 0xFF_FFFF;
    private static final int ERROR_COLOR = 0xFF_0000;

    private static final NumberEntryType TYPE = NumberEntryType.UNITLESS;
    private static final int MIN_VALUE = 0;

    private final String name;
    private final DecimalFormat decimalFormat;

    NumberField(int x, int y, String name, ScreenStyle style, Consumer<Long> onConfirm) {
        super(style, Minecraft.getInstance().font, x, y, WIDTH, HEIGHT);
        this.name = name;

        decimalFormat = new DecimalFormat("#.######", new DecimalFormatSymbols());
        decimalFormat.setParseBigDecimal(true);
        decimalFormat.setNegativePrefix("-");

        setBordered(false);
        setVisible(true);
        setMaxLength(7);
        setLongValue(0);
        setResponder(text -> validate());
        setOnConfirm(() -> {
            if (getLongValue().isPresent()) {
                onConfirm.accept(getLongValue().getAsLong());
            }
        });
        validate();
    }

    @Override
    public void setTooltipMessage(List<Component> tooltipMessage) {
        tooltipMessage.add(0, Utils.translate("tooltip", name));
        super.setTooltipMessage(tooltipMessage);
    }

    @Override
    public boolean mouseClicked(double mX, double mY, int button) {
        if (button == 1) {
            //isMouseOver(mX, mY)
            setValue("");
        }
        return super.mouseClicked(mX, mY, button);
    }

    private void validate() {
        List<Component> validationErrors = new ArrayList<>();
        List<Component> infoMessages = new ArrayList<>();

        var possibleValue = getValueInternal();
        if (possibleValue.isPresent()) {
            if (possibleValue.get().scale() > 0) {
                validationErrors.add(Component.literal("Must be whole number!"));
            } else {
                var value = convertToExternalValue(possibleValue.get());
                if (value < MIN_VALUE) {
                    var formatted = decimalFormat.format(convertToInternalValue(MIN_VALUE));
                    validationErrors.add(GuiText.NumberLessThanMinValue.text(formatted));
                } else if (!isNumber()) {
                    infoMessages.add(Component.literal("= " + decimalFormat.format(possibleValue.get())));
                }
            }
        } else {
            validationErrors.add(GuiText.InvalidNumber.text());
        }

        boolean valid = validationErrors.isEmpty();
        var tooltip = valid ? infoMessages : validationErrors;
        setTextColor(valid ? TEXT_COLOR : ERROR_COLOR);
        setTooltipMessage(tooltip);
    }

    OptionalLong getLongValue() {
        var internalValue = getValueInternal();
        if (internalValue.isEmpty()) {
            return OptionalLong.empty();
        }

        var externalValue = convertToExternalValue(internalValue.get());
        if (externalValue < MIN_VALUE) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(externalValue);
    }

    void setLongValue(long value) {
        var internalValue = convertToInternalValue(Math.max(value, MIN_VALUE));
        setValue(decimalFormat.format(internalValue));
        moveCursorToEnd();
        validate();
    }

    private boolean isNumber() {
        var position = new ParsePosition(0);
        var textValue = getValue().trim();
        decimalFormat.parse(textValue, position);
        return position.getErrorIndex() == -1 && position.getIndex() == textValue.length();
    }

    private long convertToExternalValue(BigDecimal internalValue) {
        var multiplicand = BigDecimal.valueOf(TYPE.amountPerUnit());
        var value = internalValue.multiply(multiplicand, MathContext.DECIMAL128);
        value = value.setScale(0, RoundingMode.UP);
        return value.longValue();
    }

    private BigDecimal convertToInternalValue(long externalValue) {
        var divisor = BigDecimal.valueOf(TYPE.amountPerUnit());
        return BigDecimal.valueOf(externalValue).divide(divisor, MathContext.DECIMAL128);
    }

    private Optional<BigDecimal> getValueInternal() {
        return MathExpressionParser.parse(getValue(), decimalFormat);
    }
}
