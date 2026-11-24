package com.spacebus.ops.domain.model.reserva;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Retorno (webhook) do provedor de pagamento. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetornoPagamentoRequest {

    public enum Resultado { APROVADO, RECUSADO }

    @NotBlank(message = "Informe a cobrança.")
    @Size(max = 64, message = "Cobrança inválida.")
    private String provedorId;

    @NotNull(message = "Informe o resultado.")
    private Resultado resultado;
}
