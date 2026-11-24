package com.spacebus.ops.domain.model.rota;

import com.spacebus.ops.domain.core.mapper.IBaseMapper;
import com.spacebus.ops.domain.core.mapper.Referencias;
import com.spacebus.ops.domain.entity.PontoParada;
import com.spacebus.ops.domain.entity.Rota;
import com.spacebus.ops.domain.entity.Rotina;
import com.spacebus.ops.domain.entity.Usuario;
import com.spacebus.ops.domain.entity.Veiculo;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

@Mapper(componentModel = "spring")
public interface IRotaMapper extends IBaseMapper<Rota, RotaRequest, RotaResponse> {

    @Override
    @Mapping(target = "pontos", ignore = true)
    @Mapping(target = "rotina", ignore = true)
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

    @Mapping(target = "rota", ignore = true)
    @Mapping(target = "veiculo", source = "veiculoId", qualifiedByName = "veiculo")
    @Mapping(target = "motorista", source = "motoristaId", qualifiedByName = "motorista")
    @Mapping(target = "geradaEm", ignore = true)
    @Mapping(target = "geradas", ignore = true)
    @Mapping(target = "conflitos", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCriacao", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    Rotina fromRotinaRequest(RotinaRequest request);

    @Mapping(target = "veiculoId", source = "veiculo.id")
    @Mapping(target = "veiculoPlaca", source = "veiculo.placa")
    @Mapping(target = "motoristaId", source = "motorista.id")
    @Mapping(target = "motoristaNome", source = "motorista.nome")
    @Mapping(target = "dias", source = "dias", qualifiedByName = "diasOrdenados")
    @Mapping(target = "horarios", source = "horarios", qualifiedByName = "horariosOrdenados")
    RotinaResponse toRotinaResponse(Rotina rotina);

    /** Liga pontos e rotina à rota (lado dono das relações). A ordem é definida no serviço. */
    @AfterMapping
    default void vincular(RotaRequest request, @MappingTarget Rota rota) {
        rota.substituirPontos(fromPontosRequest(request.getPontos()));
        rota.definirRotina(request.getRotina() == null ? null : fromRotinaRequest(request.getRotina()));
    }

    @Named("veiculo")
    default Veiculo veiculo(String id) {
        return Referencias.porId(id, Veiculo::new);
    }

    @Named("motorista")
    default Usuario motorista(String id) {
        return Referencias.porId(id, Usuario::new);
    }

    @Named("diasOrdenados")
    default List<DayOfWeek> diasOrdenados(Collection<DayOfWeek> dias) {
        return dias.stream().sorted().toList();
    }

    @Named("horariosOrdenados")
    default List<LocalTime> horariosOrdenados(Collection<LocalTime> horarios) {
        return horarios.stream().sorted().toList();
    }
}
