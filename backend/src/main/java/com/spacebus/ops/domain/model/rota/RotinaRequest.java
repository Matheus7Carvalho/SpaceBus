package com.spacebus.ops.domain.model.rota;

import com.spacebus.ops.domain.entity.enums.FrequenciaRotina;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Rotina da Rota Fixa. Mesmos limites da viagem (preço, capacidade). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RotinaRequest {

    public static final int MAX_HORARIOS = 12;

    @NotNull(message = "Selecione a frequência.")
    private FrequenciaRotina frequencia;

    /** Obrigatório (ao menos um) na semanal; ignorado na diária. */
    @Size(max = 7, message = "Escolha até 7 dias.")
    @Builder.Default
    private List<DayOfWeek> dias = new ArrayList<>();

    @NotEmpty(message = "Adicione ao menos um horário.")
    @Size(max = MAX_HORARIOS, message = "A rotina pode ter até 12 horários.")
    @Builder.Default
    private List<@NotNull(message = "Informe o horário.") LocalTime> horarios = new ArrayList<>();

    @NotBlank(message = "Selecione o veículo.")
    @Size(max = 36, message = "Veículo inválido.")
    private String veiculoId;

    @Size(max = 36, message = "Motorista inválido.")
    private String motoristaId;

    @NotNull(message = "Informe o preço.")
    @DecimalMin(value = "0.01", message = "O preço deve ser entre R$ 0,01 e R$ 9.999,99.")
    @DecimalMax(value = "9999.99", message = "O preço deve ser entre R$ 0,01 e R$ 9.999,99.")
    @Digits(integer = 4, fraction = 2, message = "Use no máximo 2 casas decimais no preço.")
    private BigDecimal preco;

    @NotNull(message = "Informe a capacidade.")
    @Min(value = 1, message = "A capacidade deve ser entre 1 e 60 lugares.")
    @Max(value = 60, message = "A capacidade deve ser entre 1 e 60 lugares.")
    private Integer capacidade;

    @NotNull(message = "Informe o início da vigência.")
    private LocalDate inicio;

    private LocalDate fim;

    @Builder.Default
    private Boolean publicar = Boolean.FALSE;
}
