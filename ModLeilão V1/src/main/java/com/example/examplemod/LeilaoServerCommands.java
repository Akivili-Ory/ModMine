package com.example.examplemod;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class LeilaoServerCommands {

    /*
     * Gerenciador dos leilões.
     *
     * Ele guarda todos os leilões criados.
     */
    private static final LeilaoManager MANAGER = new LeilaoManager();

    /*
     * Permite acessar o gerenciador dos leilões.
     */
    public static LeilaoManager getManager() {
        return MANAGER;
    }

    /*
     * Registra os comandos do servidor.
     *
     * /leilao
     * /leilao criar <valorInicial> <tempo> <unidade>
     * /leilao lance <id> <valor>
     * /leilao cancelar <id>
     * /leilao procurar <item>
     */
    public static void registrar(RegisterCommandsEvent event) {

        event.getDispatcher().register(
                Commands.literal("leilao")

                        .executes(LeilaoServerCommands::abrir)

                        .then(Commands.literal("criar")
                                .then(Commands.argument("valorInicial", LongArgumentType.longArg(1))
                                        .then(Commands.argument("tempo", IntegerArgumentType.integer(1))
                                                .then(Commands.argument("unidade", StringArgumentType.word())
                                                        .suggests((ctx, builder) ->
                                                                SharedSuggestionProvider.suggest(
                                                                        List.of("seg", "min", "horas", "dias", "mes"),
                                                                        builder))
                                                        .executes(LeilaoServerCommands::criar)))))

                        .then(Commands.literal("lance")
                                .then(Commands.argument("id", LongArgumentType.longArg(1))
                                        .then(Commands.argument("valor", LongArgumentType.longArg(1))
                                                .executes(LeilaoServerCommands::lance))))

                        .then(Commands.literal("cancelar")
                                .then(Commands.argument("id", LongArgumentType.longArg(1))
                                        .executes(LeilaoServerCommands::cancelar)))

                        .then(Commands.literal("procurar")
                                .then(Commands.argument("busca", StringArgumentType.greedyString())
                                        .suggests((ctx, builder) ->
                                                SharedSuggestionProvider.suggest(sugestoesBusca(), builder))
                                        .executes(LeilaoServerCommands::procurar)))
        );
    }

    /*
     * Abre o baú duplo. Se "filtro" não for null,
     * mostra somente leilões daquele item.
     */
    private static void abrirMenu(ServerPlayer jogador, LeilaoFiltro filtro) {

        Component titulo = filtro == null
                ? Component.literal("Leilões")
                : Component.literal("Leilões: " + filtro.getDescricao());

        jogador.openMenu(new SimpleMenuProvider(
                (id, inv, pl) -> new LeilaoMenu(id, inv, new SimpleContainer(54), filtro),
                titulo
        ));
    }

    /*
     * /leilao
     */
    private static int abrir(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {

        ServerPlayer jogador = ctx.getSource().getPlayerOrException();

        abrirMenu(jogador, null);

        return 1;
    }

    /*
     * Sugestões que aparecem ao digitar /leilao procurar:
     * as categorias e os itens que estão em leilão agora.
     */
    private static List<String> sugestoesBusca() {

        List<String> lista = new ArrayList<>(LeilaoFiltro.categorias());

        for (Leilao leilao : MANAGER.getLeiloes()) {

            if (leilao.isEncerrado()) {
                continue;
            }

            String id = BuiltInRegistries.ITEM
                    .getKey(leilao.getItem().getItem())
                    .getPath();

            if (!lista.contains(id)) {
                lista.add(id);
            }
        }

        return lista;
    }

    /*
     * /leilao procurar <busca>
     *
     * Aceita categoria (madeira) ou parte do nome/ID (log, oak_log).
     */
    private static int procurar(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {

        ServerPlayer jogador = ctx.getSource().getPlayerOrException();

        String busca = StringArgumentType.getString(ctx, "busca");

        LeilaoFiltro filtro = LeilaoFiltro.de(busca);

        // Verifica se existe pelo menos um leilão ativo que combine com a busca
        boolean existe = false;

        for (Leilao leilao : MANAGER.getLeiloes()) {

            if (!leilao.isEncerrado() && filtro.aceita(leilao.getItem())) {
                existe = true;
                break;
            }
        }

        if (!existe) {
            jogador.sendSystemMessage(
                    Component.literal("Nenhum leilão ativo encontrado para: " + busca)
            );
            return 0;
        }

        abrirMenu(jogador, filtro);

        return 1;
    }

    /*
     * /leilao criar <valorInicial> <tempo> <unidade>
     *
     * Unidades: seg, min, horas, dias, mes
     * Exemplos: /leilao criar 50 3 min
     *           /leilao criar 50 1 horas
     */
    private static int criar(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {

        ServerPlayer jogador = ctx.getSource().getPlayerOrException();

        long valorInicial = LongArgumentType.getLong(ctx, "valorInicial");
        int tempo = IntegerArgumentType.getInteger(ctx, "tempo");
        String unidade = StringArgumentType.getString(ctx, "unidade");

        // Item que o jogador está segurando na mão principal
        ItemStack item = jogador.getMainHandItem();

        if (item.isEmpty()) {
            jogador.sendSystemMessage(
                    Component.literal("Você precisa estar segurando um item.")
            );
            return 0;
        }

        // Converte (tempo + unidade) para milissegundos
        long msPorUnidade = unidadeEmMilissegundos(unidade);

        if (msPorUnidade < 0) {
            jogador.sendSystemMessage(
                    Component.literal("Unidade inválida. Use: seg, min, horas, dias ou mes.")
            );
            return 0;
        }

        long duracaoMilissegundos = tempo * msPorUnidade;

        // Duração máxima: 365 dias
        long duracaoMaxima = 365L * 24L * 60L * 60L * 1000L;

        if (duracaoMilissegundos > duracaoMaxima) {
            jogador.sendSystemMessage(
                    Component.literal("A duração máxima de um leilão é 365 dias.")
            );
            return 0;
        }

        Leilao leilao = MANAGER.criarLeilao(
                jogador.getUUID(),
                jogador.getGameProfile().getName(),
                item.copy(),
                valorInicial,
                duracaoMilissegundos
        );

        // Remove o item da mão do jogador
        jogador.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        jogador.sendSystemMessage(
                Component.literal(
                        "Leilão criado! ID: #" + leilao.getId()
                                + " | Valor inicial: " + valorInicial
                                + " | Duração: " + tempo + " " + unidade
                )
        );

        return 1;
    }

    /*
     * Converte a unidade digitada para milissegundos.
     * Retorna -1 se a unidade não existir.
     */
    private static long unidadeEmMilissegundos(String unidade) {

        switch (unidade.toLowerCase(Locale.ROOT)) {

            case "seg":
            case "segundo":
            case "segundos":
            case "s":
                return 1000L;

            case "min":
            case "minuto":
            case "minutos":
                return 60L * 1000L;

            case "hora":
            case "horas":
            case "h":
                return 60L * 60L * 1000L;

            case "dia":
            case "dias":
            case "d":
                return 24L * 60L * 60L * 1000L;

            // 1 mês = 30 dias
            case "mes":
            case "meses":
                return 30L * 24L * 60L * 60L * 1000L;

            default:
                return -1L;
        }
    }

    /*
     * /leilao lance <id> <valor>
     */
    private static int lance(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {

        ServerPlayer jogador = ctx.getSource().getPlayerOrException();

        long id = LongArgumentType.getLong(ctx, "id");
        long valor = LongArgumentType.getLong(ctx, "valor");

        Leilao leilao = MANAGER.getLeilao(id);

        if (leilao == null || leilao.isEncerrado()) {
            jogador.sendSystemMessage(
                    Component.literal("Leilão não encontrado.")
            );
            return 0;
        }

        if (leilao.getVendedor().equals(jogador.getUUID())) {
            jogador.sendSystemMessage(
                    Component.literal("Você não pode dar lance no seu próprio leilão.")
            );
            return 0;
        }

        if (!MANAGER.darLance(id, jogador.getUUID(), valor)) {
            jogador.sendSystemMessage(
                    Component.literal(
                            "Lance inválido. O valor precisa ser maior que "
                                    + leilao.getValorAtual()
                    )
            );
            return 0;
        }

        jogador.sendSystemMessage(
                Component.literal("Lance de " + valor + " feito no leilão #" + id)
        );

        return 1;
    }

    /*
     * /leilao cancelar <id>
     *
     * Regras:
     * - o dono pode cancelar, mas só se ainda não houver lances;
     * - operadores (permissão 2) podem cancelar qualquer leilão.
     *
     * O item sempre volta para o vendedor.
     */
    private static int cancelar(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {

        ServerPlayer jogador = ctx.getSource().getPlayerOrException();

        long id = LongArgumentType.getLong(ctx, "id");

        Leilao leilao = MANAGER.getLeilao(id);

        if (leilao == null || leilao.isEncerrado()) {
            jogador.sendSystemMessage(
                    Component.literal("Leilão não encontrado.")
            );
            return 0;
        }

        boolean admin = ctx.getSource().hasPermission(2);
        boolean dono = leilao.getVendedor().equals(jogador.getUUID());

        if (!dono && !admin) {
            jogador.sendSystemMessage(
                    Component.literal("Você só pode cancelar os seus próprios leilões.")
            );
            return 0;
        }

        if (leilao.getLanceAtual() != null && !admin) {
            jogador.sendSystemMessage(
                    Component.literal("Não é possível cancelar um leilão que já recebeu lances.")
            );
            return 0;
        }

        leilao.cancelar();

        // Devolve o item na hora
        MANAGER.processarLeiloes(ctx.getSource().getServer());

        jogador.sendSystemMessage(
                Component.literal("Leilão #" + id + " cancelado.")
        );

        return 1;
    }
}