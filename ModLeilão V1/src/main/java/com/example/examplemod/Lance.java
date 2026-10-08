package com.example.examplemod;

import java.util.UUID;

//Representa um lance realizado em um leilão.

public class Lance {

   //UID DO PLAYER 

    private final UUID jogador;

    //Valor Inicial pelo jogador.

    private final long valor;

    //momento em que o lance foi realizado. E Guardar o horário
    private final long momento;

    public Lance(UUID jogador, long valor) {
        this.jogador = jogador;
        this.valor = valor;
        this.momento = System.currentTimeMillis();
    }

    //aVISA JOGADOR QUE FOi concluido + Valor e Horario

    public UUID getJogador() {
        return jogador;
    }

    
    public long getValor() {
        return valor;
    }

    
    public long getMomento() {
        return momento;
    }
}