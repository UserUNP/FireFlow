package de.blazemcworld.fireflow.node.impl.event;

import de.blazemcworld.fireflow.compiler.CompiledNode;
import de.blazemcworld.fireflow.evaluation.CodeEvaluator;
import de.blazemcworld.fireflow.node.Node;
import de.blazemcworld.fireflow.node.io.NodeOutput;
import de.blazemcworld.fireflow.node.annotation.FlowContext;
import de.blazemcworld.fireflow.node.annotation.FlowValueOutput;
import de.blazemcworld.fireflow.value.PlayerValue;
import de.blazemcworld.fireflow.value.SignalValue;
import net.minestom.server.event.player.PlayerStartFlyingEvent;

public class PlayerStartFlyingEventNode extends Node {

    private final NodeOutput signal;

    public PlayerStartFlyingEventNode() {
        super("Player Start Flying");

        signal = output("Signal", SignalValue.INSTANCE);
        output("Player", PlayerValue.INSTANCE);

        loadJava(PlayerStartFlyingEventNode.class);
    }

    @FlowValueOutput("Player")
    private static Object player() {
        return ctx().getInternalVar("ID$player");
    }

    @FlowContext
    private static CompiledNode ctx() {
        throw new IllegalStateException();
    }

    @Override
    public void register(CodeEvaluator evaluator) {
        String entrypoint = evaluator.compiler.markRoot(signal);
        evaluator.events.addListener(PlayerStartFlyingEvent.class, event -> {
            CompiledNode context = evaluator.newContext();
            context.setInternalVar(allocateId("player"), new PlayerValue.Reference(evaluator.space, event.getPlayer()));
            context.emit(entrypoint);
        });
    }
}
