package com.example.examplemod;


import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

//Gerencia o leilão do server
public class LeilaoManager {


    //Guarda todos os Leiloes (VIA ID)
    private final Map<Long, Leilao> leiloes = new HashMap<>();

    // ID disponivel
    private long proximoId = 1;


    public Leilao criarLeilao(
            UUID vendedor,
            String vendedorNome,
            ItemStack item,
            long valorInicial,
            long duracaoMilissegundos
    ) {

        // Verifica se o valor do leilão e valido

        if (valorInicial <= 0) {
            throw new IllegalArgumentException(
                    "O valor inicial deve ser maior que zero."
            );
        }

        // Verifica se o iten existe

        if (item == null || item.isEmpty()) {
            throw new IllegalArgumentException(
                    "O item do leilão não pode estar vazio."
            );
        }

        //Proximo id
        long id = proximoId;

        //Incrementa para proximo ID
        proximoId++;

        //OBJ LEILÃO

        Leilao leilao = new Leilao(
                id,
                vendedor,
                vendedorNome,
                item,
                valorInicial,
                duracaoMilissegundos
        );

        // ADICIONA LEILÃO NA LISTA
        leiloes.put(id, leilao);

        return leilao;
    }

    // bUSCA LEILÃO VIA ID

    public Leilao getLeilao(long id) {

        return leiloes.get(id);
    }

    //retorna os leiloes

    public Collection<Leilao> getLeiloes() {

        return leiloes.values();
    }

    // tenta fazer o lance

    public boolean darLance(
            long id,
            UUID jogador,
            long valor
    ) {

        //Procura o Leilão

        Leilao leilao = leiloes.get(id);

        // Se não existe lance e invalido

        if (leilao == null) {
            return false;
        }

        //verifica regras

        return leilao.darLance(jogador, valor);
    }

    //finaliza

    public void encerrarLeilao(long id) {

        Leilao leilao = leiloes.get(id);


        if (leilao == null) {
            return;
        }


        leilao.encerrar();
    }


    public void removerLeilao(long id) {

        leiloes.remove(id);
    }


    public void verificarLeiloesExpirados() {

        for (Leilao leilao : leiloes.values()) {

            //se o tempo acaba fim do leilão

            if (!leilao.isEncerrado() && leilao.tempoTerminou()) {

                leilao.encerrar();
            }
        }
    }

    // Encerra os leilões expirados e entrega os itens a quem está online
    public void processarLeiloes(MinecraftServer server) {

        verificarLeiloesExpirados();

        Iterator<Leilao> it = leiloes.values().iterator();

        while (it.hasNext()) {

            Leilao leilao = it.next();

            if (!leilao.isEncerrado() || leilao.isEntregue()) {
                continue;
            }

            ServerPlayer destinatario =
                    server.getPlayerList().getPlayer(leilao.getDestinatario());

            // Offline: fica pendente até ele entrar no servidor
            if (destinatario == null) {
                continue;
            }

            entregar(leilao, destinatario);
            it.remove();
        }
    }

    // Entrega os itens pendentes de um jogador que acabou de entrar
    public void entregarPendentes(ServerPlayer jogador) {

        Iterator<Leilao> it = leiloes.values().iterator();

        while (it.hasNext()) {

            Leilao leilao = it.next();

            if (leilao.isEncerrado()
                    && !leilao.isEntregue()
                    && leilao.getDestinatario().equals(jogador.getUUID())) {

                entregar(leilao, jogador);
                it.remove();
            }
        }
    }

    // Coloca o item no inventário (se estiver cheio, cai no chão)
    private void entregar(Leilao leilao, ServerPlayer jogador) {

        ItemStack item = leilao.getItem();

        jogador.getInventory().add(item);

        if (!item.isEmpty()) {
            jogador.drop(item, false);
        }

        leilao.marcarEntregue();

        if (leilao.isCancelado()) {
            jogador.sendSystemMessage(Component.literal(
                    "O leilão #" + leilao.getId()
                            + " foi cancelado. Seu item foi devolvido."));
        } else if (leilao.getLanceAtual() != null) {
            jogador.sendSystemMessage(Component.literal(
                    "Você venceu o leilão #" + leilao.getId()
                            + " por " + leilao.getValorAtual()
                            + "! O item foi entregue."));
        } else {
            jogador.sendSystemMessage(Component.literal(
                    "O leilão #" + leilao.getId()
                            + " terminou sem lances. Seu item foi devolvido."));
        }
    }

    //Retorna a quantidade de leilões existentes.

    public int quantidadeLeiloes() {

        return leiloes.size();
    }
}