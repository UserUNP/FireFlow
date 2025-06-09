package de.blazemcworld.fireflow.code.node.impl.number;

import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.type.ConditionType;
import de.blazemcworld.fireflow.code.type.NumberType;
import net.minecraft.item.Items;

public class IsNaNNode extends Node {
    public IsNaNNode() {
        super("is_nan", "Is NaN", "Returns whether a number is NaN", Items.DEAD_BUBBLE_CORAL_BLOCK);
        Input<Double> number = new Input<>("input", "Number", NumberType.INSTANCE);
        Output<Boolean> output = new Output<>("result", "Result", ConditionType.INSTANCE);

        output.valueFrom(ctx -> number.getValue(ctx).isNaN());
    }

    @Override
    public Node copy() {
        return new IsNaNNode();
    }
}
