package br.com.navalha.domain;

public enum Barbeiro {
    ANDRE("André"), BRUNO("Bruno"), CAIO("Caio");
    private final String nome;
    Barbeiro(String nome) { this.nome = nome; }
    public String getNome() { return nome; }
}
