package de.blazemcworld.fireflow.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.tree.CommandNode;

@Mixin(value = CommandDispatcher.class, remap = false)
public interface CommandDispatcherInvoker<S> {
    @Invoker("parseNodes")
    public ParseResults<S> invokeParseNodes(final CommandNode<S> node, final StringReader originalReader, final CommandContextBuilder<S> contextSoFar);
}
