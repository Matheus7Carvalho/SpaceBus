package com.spacebus.ops.domain.model.rota;

import com.spacebus.ops.domain.entity.enums.FrequenciaRotina;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RotinaResponse {
    private FrequenciaRotina frequencia;
    /** Segunda a domingo, em ordem. */
    private List<DayOfWeek> dias;
    /** Em ordem crescente. */
    private List<LocalTime> horarios;
    private String veiculoId;
    private String veiculoPlaca;
    private String motoristaId;
    private String motoristaNome;
    private BigDecimal preco;
    private Integer capacidade;
    private LocalDate inicio;
    private LocalDate fim;
    private Boolean publicar;
    private LocalDateTime geradaEm;
    private Integer geradas;
    private Integer conflitos;
}
