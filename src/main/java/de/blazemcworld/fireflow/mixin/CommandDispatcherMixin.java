package de.blazemcworld.fireflow.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;

import de.blazemcworld.fireflow.command.CommandHelper;
import de.blazemcworld.fireflow.space.PlayWorld;
import de.blazemcworld.fireflow.space.Space;
import de.blazemcworld.fireflow.space.SpaceManager;
import net.minecraft.server.command.ServerCommandSource;

@Mixin(value = CommandDispatcher.class, remap = false)
public class CommandDispatcherMixin {

    @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/ParseResults;", at = @At("HEAD"), cancellable = true)
    public void spaceParse(final StringReader command, final Object source, CallbackInfoReturnable<ParseResults<ServerCommandSource>> ci) {
        if (!(source instanceof ServerCommandSource plSource)) return;
        if (!(plSource.getWorld() instanceof PlayWorld)) return;
        Space space = SpaceManager.getSpaceForPlayer(CommandHelper.getPlayer(plSource));
        if (space == null) throw new RuntimeException("how");
        var cd = (CommandDispatcher<ServerCommandSource>) (Object) this;
        final CommandContextBuilder<ServerCommandSource> context = new CommandContextBuilder<>(cd, plSource, space.evaluator.rootCommandNode, command.getCursor());
        ci.setReturnValue(((CommandDispatcherInvoker<ServerCommandSource>) cd).invokeParseNodes(space.evaluator.rootCommandNode, command, context));
    }

}
