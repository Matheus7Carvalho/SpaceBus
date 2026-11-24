package com.spacebus.ops.domain.service.localizacao;

import com.spacebus.ops.domain.model.localizacao.ViagemMotoristaResponse;

import java.util.List;

/** "Minhas viagens" do motorista: as de hoje atribuídas a ele, com iniciar e encerrar. */
public interface IViagensMotoristaService {

    /** Viagens de hoje (fuso da operação) atribuídas ao motorista, mais a que estiver em andamento. */
    List<ViagemMotoristaResponse> deHoje(String motoristaId);

    ViagemMotoristaResponse iniciar(String viagemId, String motoristaId);

    ViagemMotoristaResponse encerrar(String viagemId, String motoristaId);
}
