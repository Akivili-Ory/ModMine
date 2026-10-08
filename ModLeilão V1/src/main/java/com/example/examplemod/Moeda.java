package com.example.examplemod;


// parou de fruncionar\
// Definindo o valor dos Nuggets/Pepitas

public enum Moeda {

    COBRE(1),
    FERRO(10),
    OURO(100);

    /**
     * Valor da moeda dentro do sistema.
     */
    private final long valor;

    Moeda(long valor) {
        this.valor = valor;
    }

    //Retorna o valor da moeda.
    
    public long getValor() {
        return valor;
    }
}