package br.com.navalha.domain;

import java.math.BigDecimal;

public enum Servico {
    CORTE("Corte de cabelo", 30, "40.00"),
    BARBA("Barba", 30, "30.00"),
    COMBO("Corte + barba", 60, "65.00");

    private final String descricao;
    private final int minutos;
    private final BigDecimal preco;

    Servico(String descricao, int minutos, String preco) {
        this.descricao = descricao;
        this.minutos = minutos;
        this.preco = new BigDecimal(preco);
    }
    public String getDescricao() { return descricao; }
    public int getMinutos() { return minutos; }
    public BigDecimal getPreco() { return preco; }
}
