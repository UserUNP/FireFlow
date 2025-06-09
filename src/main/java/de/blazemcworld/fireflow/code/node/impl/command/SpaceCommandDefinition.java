package de.blazemcworld.fireflow.code.node.impl.command;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.RootCommandNode;

import de.blazemcworld.fireflow.code.CodeEvaluator;
import de.blazemcworld.fireflow.code.CodeThread;
import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.type.ConditionType;
import de.blazemcworld.fireflow.code.type.ListType;
import de.blazemcworld.fireflow.code.type.NumberType;
import de.blazemcworld.fireflow.code.type.PlayerType;
import de.blazemcworld.fireflow.code.type.SignalType;
import de.blazemcworld.fireflow.code.type.StringType;
import de.blazemcworld.fireflow.code.type.WireType;
import de.blazemcworld.fireflow.code.value.ListValue;
import de.blazemcworld.fireflow.code.value.PlayerValue;
import de.blazemcworld.fireflow.command.CommandHelper;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.NumberRangeArgumentType;
import net.minecraft.command.argument.NumberRangeArgumentType.FloatRangeArgumentType;
import net.minecraft.item.Items;
import net.minecraft.predicate.NumberRange;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class SpaceCommandDefinition {

    private static final String IO_PREFIX = "cmd_";
    private static final String CMD_PREFIX = "/";

    private static final SpaceArgType<?, ?>[] TYPES = {
        new SpaceArgType<>(String.class, StringArgumentType.class, StringType.INSTANCE, Converter.identity()),
        new SpaceArgType<>(Boolean.class, BoolArgumentType.class, ConditionType.INSTANCE, Converter.identity()),
        new SpaceArgType<>(Double.class, DoubleArgumentType.class, NumberType.INSTANCE, Converter.identity()),
        new SpaceArgType<>(Integer.class, IntegerArgumentType.class, NumberType.INSTANCE, ctx -> ctx.argValue.doubleValue()),

        new SpaceArgType<>(NumberRange.DoubleRange.class, FloatRangeArgumentType.class, ListType.of(NumberType.INSTANCE), ctx -> {
            return new ListValue<>(NumberType.INSTANCE, List.of(ctx.argValue.min().orElse(Double.NaN), ctx.argValue.max().orElse(Double.NaN)));
        }),

        new SpaceArgType<ListValue<PlayerValue>, EntitySelector>(EntitySelector.class, EntityArgumentType.class, ListType.of(PlayerType.INSTANCE), ctx -> {
            try {
                return new ListValue<>(PlayerType.INSTANCE, ctx.argValue.getPlayers(ctx.brigCtx.getSource()).stream().map(PlayerValue::new).toList());
            } catch (CommandSyntaxException e) {
                return new ListValue<>(PlayerType.INSTANCE);
            }
        })
    };

    public final String name;
    public final Set<SyntaxNode> syntaxes = new HashSet<>();

    public SpaceCommandDefinition(String name) {
        this.name = name;
    }

    public SyntaxNode addSyntax() {
        SyntaxNode n = new SyntaxNode();
        syntaxes.add(n);
        n.addArg(CommandManager.argument("test1", StringArgumentType.string()));
        n.addArg(CommandManager.argument("test2", BoolArgumentType.bool()));
        n.addArg(CommandManager.argument("test3", IntegerArgumentType.integer()));
        n.addArg(CommandManager.argument("test4", NumberRangeArgumentType.floatRange()));
        return n;
    }

    public SyntaxNode addSyntax(SyntaxNode from) {
        SyntaxNode n = (SyntaxNode) from.copy();
        syntaxes.add(n);
        return n;
    }

    public class SyntaxNode extends Node {
        public final SpaceCommandDefinition command = SpaceCommandDefinition.this;
        private final List<SpaceCmdArg<?, ?>> args;
        private final Output<Void> signal;
        private final Output<PlayerValue> player;
        private SyntaxNode() {
            super("cmd_syntax_node", SpaceCommandDefinition.this.name + " Command", "", Items.CHAIN_COMMAND_BLOCK);
            this.args = new ArrayList<>();
            signal = new Output<>("signal", "Signal", SignalType.INSTANCE);
            player = new Output<>("player", "Player", PlayerType.INSTANCE);
            player.valueFromScope();
        }

        public <B> void addArg(RequiredArgumentBuilder<ServerCommandSource, B> arg) {
            SpaceArgType<Object, B> type = SpaceArgType.of(arg.getType());
            Output<Object> out = new Output<>(IO_PREFIX + arg.getName(), arg.getName(), type.wireType);
            out.valueFromScope();
            args.add(new SpaceCmdArg<>(type, arg, out));
        }

        private void onCommand(CodeEvaluator evaluator, CommandContext<ServerCommandSource> ctx) {
            CodeThread thread = evaluator.newCodeThread();
            thread.setScopeValue(player, new PlayerValue(CommandHelper.getPlayer(ctx.getSource())));
            for (SpaceCmdArg<?, ?> arg : args) arg.setValue(thread, ctx);
            thread.sendSignal(signal);
            thread.clearQueue();
        }

        private ArgumentBuilder<ServerCommandSource, ?> makeCommand(int index, ArgumentBuilder<ServerCommandSource, ?> arg, Command<ServerCommandSource> executor) {
            if (index >= args.size()) return arg.executes(executor);
            return arg.then(makeCommand(index + 1, args.get(index).brigArg, executor));
        }

        public void register(RootCommandNode<ServerCommandSource> root, CodeEvaluator evaluator) {
            root.addChild(makeCommand(0, CommandManager.literal(CMD_PREFIX + SpaceCommandDefinition.this.name), ctx -> {
                onCommand(evaluator, ctx);
                return Command.SINGLE_SUCCESS;
            }).build());
        }

        @Override
        public Node copy() {
            SyntaxNode n = new SyntaxNode();
            for (var arg : this.args) n.addArg(arg.brigArg);
            return n;
        }
    }

    private record SpaceCmdArg<W, B>(SpaceArgType<W, B> type, RequiredArgumentBuilder<ServerCommandSource, B> brigArg, Node.Output<W> out) {
        void setValue(CodeThread thread, CommandContext<ServerCommandSource> ctx) {
            thread.setScopeValue(out, type.getValue(ctx, brigArg));
        }
    }

    private record SpaceArgType<W, B>(Class<B> type, Class<? extends ArgumentType<B>> brigType, WireType<W> wireType, Converter<W, B> convert) {
        @SuppressWarnings("unchecked")
        static <W, B> SpaceArgType<W, B> of(ArgumentType<B> brigType) {
            for (SpaceArgType<?, ?> t : TYPES) if (t.brigType.isAssignableFrom(brigType.getClass())) return (SpaceArgType<W, B>) t;
            throw new RuntimeException("Unsupported argument type!");
        }

        W getValue(CommandContext<ServerCommandSource> ctx, RequiredArgumentBuilder<ServerCommandSource, B> arg) {
            return convert.apply(new SpaceCmdContext<>(ctx, ctx.getArgument(arg.getName(), type)));
        }
    }

    private record SpaceCmdContext<B>(CommandContext<ServerCommandSource> brigCtx, B argValue) {
    }

    private interface Converter<W, B> {
        static <B> Converter<B, B> identity() {
            return ctx -> ctx.argValue;
        }
        W apply(SpaceCmdContext<B> ctx);
    }
}
