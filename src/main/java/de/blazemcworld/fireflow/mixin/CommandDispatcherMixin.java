package de.blazemcworld.fireflow.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;

import de.blazemcworld.fireflow.space.PlayWorld;
import net.minecraft.server.command.ServerCommandSource;

@Mixin(value = CommandDispatcher.class, remap = false)
public class CommandDispatcherMixin {

    @SuppressWarnings("unchecked")
    @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/ParseResults;", at = @At("HEAD"), cancellable = true)
    public void spaceParse(final StringReader command, final Object source, CallbackInfoReturnable<ParseResults<ServerCommandSource>> ci) {
        if (!(source instanceof ServerCommandSource plSource)) return;
        if (!(plSource.getWorld() instanceof PlayWorld world)) return;
        var cd = (CommandDispatcher<ServerCommandSource>) (Object) this;
        final CommandContextBuilder<ServerCommandSource> context = new CommandContextBuilder<>(cd, plSource, world.space.evaluator.rootCommandNode, command.getCursor());
        ci.setReturnValue(((CommandDispatcherInvoker<ServerCommandSource>) cd).invokeParseNodes(world.space.evaluator.rootCommandNode, command, context));
    }

}
