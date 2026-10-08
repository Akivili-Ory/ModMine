package com.example.examplemod;

import net.minecraft.world.item.ItemStack;

import java.util.UUID;


//Representa um item que está sendo vendido em um leilão.
public class Leilao {

    //Id do Leilão (Rastrio Futuro)

    private final long id;

    // Mostra quem é o Vendendor | Item Vendido | Valor Inicial | Valor Atual | Lance (Sem lance fica  igual o valor inicial) | Lance Atual |
    //  Tempo Ate o fim do leilaõa | Define se leilão acabou

    private final UUID vendedor;

    // Nome do vendedor (mostrado no menu)
    private final String vendedorNome;

    private final ItemStack item;

    private final long valorInicial;

    private long valorAtual;

    private Lance lanceAtual;

    private final long tempoFim;

    private boolean encerrado;

    // Define se o item já foi entregue
    private boolean entregue;

    // Define se o leilão foi cancelado
    private boolean cancelado;


     //id ID do leilão
     //vendedor jogador que está vendendo
     //vendedorNome nome do jogador que está vendendo
     //item item que será vendido
     //valorInicial preço inicial
     //duracaoMilissegundos duração do leilão

    public Leilao(
            long id,
            UUID vendedor,
            String vendedorNome,
            ItemStack item,
            long valorInicial,
            long duracaoMilissegundos
    ) {

        this.id = id;
        this.vendedor = vendedor;
        this.vendedorNome = vendedorNome;

        //Cria copia do item Para visualizar

        this.item = item.copy();

        this.valorInicial = valorInicial;
// Altera o novos valores

        this.valorAtual = valorInicial;


        this.tempoFim = System.currentTimeMillis() + duracaoMilissegundos;

        // Leilão comeca aberto ate fechar

        this.encerrado = false;
    }

    //Mostra id do leilão
    public long getId() {
        return id;
    }

    //vendendor
    public UUID getVendedor() {
        return vendedor;
    }

    //nome do vendedor
    public String getVendedorNome() {
        return vendedorNome;
    }

    //copia do iten

    public ItemStack getItem() {
        return item.copy();
    }

    //Valores
    public long getValorInicial() {
        return valorInicial;
    }

    public long getValorAtual() {
        return valorAtual;
    }

    public Lance getLanceAtual() {
        return lanceAtual;
    }

    // momento do fim

    public long getTempoFim() {
        return tempoFim;
    }

    //Verifica O fim
    public boolean isEncerrado() {
        return encerrado;
    }

    public boolean tempoTerminou() {

        return System.currentTimeMillis() >= tempoFim;
    }

    // tempo ate o fim

    public long getTempoRestante() {

        long restante = tempoFim - System.currentTimeMillis();

        // evita negativos

        return Math.max(restante, 0);
    }

    // Novo LANCES
    public boolean darLance(UUID jogador, long valor) {

        // Evita em leilão finalizado

        if (encerrado) {
            return false;
        }

        if (tempoTerminou()) {
            return false;
        }

        // Novo lance sempre maior que o anterior

        if (valor <= valorAtual) {
            return false;
        }

        // novo lance

        Lance novoLance = new Lance(jogador, valor);

        //guarda infos

        this.lanceAtual = novoLance;

        // atualiza

        this.valorAtual = valor;

        return true;
    }

    // Quem recebe o item: quem deu o último lance ou, sem lances
    // (ou se foi cancelado), o vendedor
    public UUID getDestinatario() {
        if (cancelado || lanceAtual == null) {
            return vendedor;
        }
        return lanceAtual.getJogador();
    }

    public boolean isCancelado() {
        return cancelado;
    }

    // Cancela o leilão: encerra e o item volta para o vendedor
    public void cancelar() {
        this.cancelado = true;
        this.encerrado = true;
    }

    public boolean isEntregue() {
        return entregue;
    }

    public void marcarEntregue() {
        this.entregue = true;
    }

    //finaliza
    public void encerrar() {

        this.encerrado = true;
    }
}