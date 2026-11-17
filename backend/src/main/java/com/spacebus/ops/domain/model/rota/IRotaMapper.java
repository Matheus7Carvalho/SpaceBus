package com.spacebus.ops.domain.model.rota;

import com.spacebus.ops.domain.core.mapper.IBaseMapper;
import com.spacebus.ops.domain.entity.PontoParada;
import com.spacebus.ops.domain.entity.Rota;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface IRotaMapper extends IBaseMapper<Rota, RotaRequest, RotaResponse> {

    @Override
    @Mapping(target = "pontos", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCriacao", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    Rota fromRequest(RotaRequest request);

    @Mapping(target = "rota", ignore = true)
    @Mapping(target = "ordem", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCriacao", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    PontoParada fromPontoRequest(PontoParadaRequest request);

    List<PontoParada> fromPontosRequest(List<PontoParadaRequest> pontos);

    PontoParadaResponse toPontoResponse(PontoParada ponto);

    /** Liga cada ponto à rota (lado dono da relação). A ordem é definida no serviço. */
    @AfterMapping
    default void vincularPontos(RotaRequest request, @MappingTarget Rota rota) {
        rota.substituirPontos(fromPontosRequest(request.getPontos()));
    }
}
