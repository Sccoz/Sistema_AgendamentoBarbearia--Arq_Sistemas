package br.com.navalha.domain;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Regra de funcionamento da barbearia. Única fonte do expediente. */
public final class Expediente {
    public static final LocalTime ABERTURA = LocalTime.of(9, 0);
    public static final LocalTime FECHAMENTO = LocalTime.of(19, 0);
    public static final int PASSO_MINUTOS = 30;

    private Expediente() { }

    /** Verdadeiro se o atendimento começa e termina dentro do expediente (seg a sáb). */
    public static boolean atende(LocalDateTime inicio, Servico servico) {
        if (inicio.getDayOfWeek() == DayOfWeek.SUNDAY) return false;
        LocalTime hora = inicio.toLocalTime();
        LocalDateTime fim = inicio.plusMinutes(servico.getMinutos());
        return hora.getMinute() % PASSO_MINUTOS == 0
                && !hora.isBefore(ABERTURA)
                && !fim.isAfter(inicio.toLocalDate().atTime(FECHAMENTO));
    }
}
