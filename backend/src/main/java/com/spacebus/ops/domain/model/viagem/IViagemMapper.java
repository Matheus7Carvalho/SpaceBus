package com.spacebus.ops.domain.model.viagem;

import com.spacebus.ops.domain.core.mapper.IBaseMapper;
import com.spacebus.ops.domain.core.mapper.Referencias;
import com.spacebus.ops.domain.entity.Rota;
import com.spacebus.ops.domain.entity.Usuario;
import com.spacebus.ops.domain.entity.Veiculo;
import com.spacebus.ops.domain.entity.Viagem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface IViagemMapper extends IBaseMapper<Viagem, ViagemRequest, ViagemResponse> {

    @Override
    @Mapping(target = "rota", source = "rotaId", qualifiedByName = "rota")
    @Mapping(target = "veiculo", source = "veiculoId", qualifiedByName = "veiculo")
    @Mapping(target = "motorista", source = "motoristaId", qualifiedByName = "motorista")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "rotina", ignore = true)
    @Mapping(target = "iniciadaEm", ignore = true)
    @Mapping(target = "iniciadaPor", ignore = true)
    @Mapping(target = "encerradaEm", ignore = true)
    @Mapping(target = "encerradaPor", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCriacao", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    Viagem fromRequest(ViagemRequest request);

    @Override
    @Mapping(target = "rotaId", source = "rota.id")
    @Mapping(target = "rotaNome", source = "rota.nome")
    @Mapping(target = "origem", source = "rota.origem")
    @Mapping(target = "destino", source = "rota.destino")
    @Mapping(target = "veiculoId", source = "veiculo.id")
    @Mapping(target = "veiculoPlaca", source = "veiculo.placa")
    @Mapping(target = "veiculoDescricao", source = "veiculo.descricao")
    @Mapping(target = "tipoVeiculo", source = "veiculo.tipo")
    @Mapping(target = "motoristaId", source = "motorista.id")
    @Mapping(target = "motoristaNome", source = "motorista.nome")
    @Mapping(target = "rotinaId", source = "rotina.id")
    @Mapping(target = "semSinal", ignore = true)
    ViagemResponse toResponse(Viagem viagem);

    @Named("rota")
    default Rota rota(String id) {
        return Referencias.porId(id, Rota::new);
    }

    @Named("veiculo")
    default Veiculo veiculo(String id) {
        return Referencias.porId(id, Veiculo::new);
    }

    @Named("motorista")
    default Usuario motorista(String id) {
        return Referencias.porId(id, Usuario::new);
    }
}
