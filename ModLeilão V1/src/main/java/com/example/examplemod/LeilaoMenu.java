package com.example.examplemod;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

/*
 * Menu de leilões (baú duplo vanilla, 6 linhas).
 *
 * Roda no SERVIDOR. O cliente desenha o baú sozinho.
 *
 * Slots 0 a 44   -> leilões (5 linhas)
 * Slots 45 a 53  -> barra inferior
 *      45 -> página anterior
 *      49 -> indicador de página
 *      53 -> próxima página
 *      demais -> decoração
 */
public class LeilaoMenu extends ChestMenu {

    private static final int LINHAS = 6;
    private static final int TAMANHO = LINHAS * 9; // 54 slots

    private static final int POR_PAGINA = 45;
    private static final int SLOT_ANTERIOR = 45;
    private static final int SLOT_INFO = 49;
    private static final int SLOT_PROXIMA = 53;

    /*
     * Item usado na decoração da barra inferior.
     * Para usar planta, troque por exemplo para Items.FERN.
     */
    private static final Item DECORACAO = Items.RED_STAINED_GLASS_PANE;

    private final SimpleContainer container;

    /*
     * Se não for null, mostra só leilões que combinam com o filtro
     * (usado pelo /leilao procurar).
     */
    private final LeilaoFiltro filtro;

    private int pagina = 0;

    public LeilaoMenu(int id, Inventory inv, SimpleContainer container, LeilaoFiltro filtro) {
        super(MenuType.GENERIC_9x6, id, inv, container, LINHAS);
        this.container = container;
        this.filtro = filtro;
        atualizar();
    }

    /*
     * Lista somente os leilões ativos (e do filtro, se houver).
     */
    private List<Leilao> ativos() {
        List<Leilao> lista = new ArrayList<>();

        for (Leilao l : LeilaoServerCommands.getManager().getLeiloes()) {

            if (l.isEncerrado()) {
                continue;
            }

            if (filtro != null && !filtro.aceita(l.getItem())) {
                continue;
            }

            lista.add(l);
        }

        return lista;
    }

    private int totalPaginas() {
        return Math.max(1, (int) Math.ceil(ativos().size() / (double) POR_PAGINA));
    }

    /*
     * Painel de decoração sem nome visível.
     */
    private ItemStack decoracao() {
        ItemStack stack = DECORACAO.getDefaultInstance();
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        return stack;
    }

    /*
     * Preenche o baú com a página atual.
     */
    private void atualizar() {

        container.clearContent();

        List<Leilao> lista = ativos();
        int inicio = pagina * POR_PAGINA;

        // Itens dos leilões
        for (int i = 0; i < POR_PAGINA && inicio + i < lista.size(); i++) {

            Leilao l = lista.get(inicio + i);

            ItemStack stack = l.getItem();

            stack.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal("Valor: " + l.getValorAtual()),
                    Component.literal("Vendedor: " + l.getVendedorNome()),
                    Component.literal("ID: #" + l.getId()),
                    Component.literal("Tempo: " + formatar(l.getTempoRestante()))
            )));

            container.setItem(i, stack);
        }

        // Barra inferior toda decorada
        for (int slot = POR_PAGINA; slot < TAMANHO; slot++) {
            container.setItem(slot, decoracao());
        }

        // Seta anterior
        if (pagina > 0) {
            ItemStack seta = Items.ARROW.getDefaultInstance();
            seta.set(DataComponents.CUSTOM_NAME, Component.literal("Página anterior"));
            container.setItem(SLOT_ANTERIOR, seta);
        }

        // Indicador de página
        ItemStack papel = Items.PAPER.getDefaultInstance();
        papel.set(
                DataComponents.CUSTOM_NAME,
                Component.literal("Página " + (pagina + 1) + "/" + totalPaginas())
        );

        if (filtro != null) {
            papel.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal("Filtro: " + filtro.getDescricao())
            )));
        }

        container.setItem(SLOT_INFO, papel);

        // Seta próxima
        if (pagina < totalPaginas() - 1) {
            ItemStack seta = Items.ARROW.getDefaultInstance();
            seta.set(DataComponents.CUSTOM_NAME, Component.literal("Próxima página"));
            container.setItem(SLOT_PROXIMA, seta);
        }

        broadcastChanges();
    }

    /*
     * Controla os cliques.
     *
     * Nunca chamamos super.clicked(), assim ninguém
     * consegue pegar ou mover os itens do menu.
     */
    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {

        if (slotId < 0 || slotId >= TAMANHO) {
            return;
        }

        if (slotId == SLOT_ANTERIOR && pagina > 0) {
            pagina--;
            atualizar();
            return;
        }

        if (slotId == SLOT_PROXIMA && pagina < totalPaginas() - 1) {
            pagina++;
            atualizar();
            return;
        }

        // Clique em um leilão
        if (slotId < POR_PAGINA) {

            List<Leilao> lista = ativos();
            int indice = pagina * POR_PAGINA + slotId;

            if (indice < lista.size() && player instanceof ServerPlayer sp) {

                long id = lista.get(indice).getId();

                sp.closeContainer();

                // Mensagem clicável: preenche "/leilao lance <id> " no chat
                sp.sendSystemMessage(
                        Component.literal("[Clique aqui para dar lance no leilão #" + id + "]")
                                .withStyle(estilo -> estilo.withClickEvent(
                                        new ClickEvent(
                                                ClickEvent.Action.SUGGEST_COMMAND,
                                                "/leilao lance " + id + " "
                                        )
                                ))
                );
            }
        }
    }

    /*
     * Bloqueia shift + clique.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    /*
     * Converte milissegundos para HH:MM:SS (ou "Xd HH:MM:SS").
     */
    private static String formatar(long milissegundos) {

        long segundos = milissegundos / 1000L;

        long dias = segundos / 86400L;
        long resto = segundos % 86400L;

        String horario = String.format(
                "%02d:%02d:%02d",
                resto / 3600L,
                (resto % 3600L) / 60L,
                resto % 60L
        );

        // Ex.: "2d 05:30:00" quando faltar mais de 1 dia
        return dias > 0 ? dias + "d " + horario : horario;
    }
}