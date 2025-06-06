package de.blazemcworld.fireflow.code.node.impl.command;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
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
import de.blazemcworld.fireflow.code.value.PlayerValue;
import de.blazemcworld.fireflow.command.CommandHelper;
import net.minecraft.command.argument.NumberRangeArgumentType;
import net.minecraft.command.argument.NumberRangeArgumentType.IntRangeArgumentType;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class SpaceCommandDefinition {

    private static final String IO_PREFIX = "cmd_";
    private static final String CMD_PREFIX = "/";

    public final String name;
    public final Set<SyntaxNode> syntaxes = new HashSet<>();

    public SpaceCommandDefinition(String name) {
        this.name = name;
    }

    public SyntaxNode addSyntax() {
        return new SyntaxNode();
    }

    public class SyntaxNode extends Node {
        private final List<RequiredArgumentBuilder<ServerCommandSource, ?>> args;
        private final Output<Void> signal;
        private final Output<PlayerValue> player;
        private SyntaxNode() {
            super("cmd_syntax_node", SpaceCommandDefinition.this.name + " Command", "", Items.CHAIN_COMMAND_BLOCK);
            this.args = new ArrayList<>();
            signal = new Output<>("signal", "Signal", SignalType.INSTANCE);
            player = new Output<>("player", "Player", PlayerType.INSTANCE);
            player.valueFromScope();
            addArg(CommandManager.argument("test1", StringArgumentType.string()));
            addArg(CommandManager.argument("test2", BoolArgumentType.bool()));
            addArg(CommandManager.argument("test3", IntegerArgumentType.integer()));
            addArg(CommandManager.argument("test4", NumberRangeArgumentType.intRange()));
        }

        public void addArg(RequiredArgumentBuilder<ServerCommandSource, ?> arg) {
            args.add(arg);
            new Output<>(IO_PREFIX + arg.getName(), arg.getName(), (WireType<?>) switch (arg.getType()) {
                case StringArgumentType t -> StringType.INSTANCE;
                case BoolArgumentType t -> ConditionType.INSTANCE;
                case IntegerArgumentType t -> NumberType.INSTANCE;
                case DoubleArgumentType t -> NumberType.INSTANCE;
                case IntRangeArgumentType t -> ListType.of(NumberType.INSTANCE);
                default -> throw new RuntimeException("unsupported argument type");
            }).valueFromScope();
        }

        private void onCommand(CodeEvaluator evaluator, CommandContext<ServerCommandSource> ctx) {
            CodeThread thread = evaluator.newCodeThread();
            thread.setScopeValue(player, new PlayerValue(CommandHelper.getPlayer(ctx.getSource())));
            for (int i = 0; i < args.size(); i++) {
                var arg = args.get(i);
                @SuppressWarnings("unchecked")
                Output<Object> out = (Output<Object>) outputs.get(i + 2);
                if (!(IO_PREFIX + arg.getName()).equals(out.id)) continue;
                var a = out.type.convert(out.type, ctx.getArgument(arg.getName(), Object.class));
                if (a == null) throw new IllegalArgumentException("Incompatible argument type: " + arg.getName() + " type of " + arg.getType().getClass().getSimpleName());
                thread.setScopeValue(out, a);
            }
            thread.sendSignal(signal);
            thread.clearQueue();
        }

        private ArgumentBuilder<ServerCommandSource, ?> makeCommand(int index, ArgumentBuilder<ServerCommandSource, ?> arg, Command<ServerCommandSource> executor) {
            if (index >= args.size()) return arg.executes(executor);
            return arg.then(makeCommand(index + 1, args.get(index), executor));
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
            n.args.addAll(this.args);
            return n;
        }
    }
}
