package com.spacebus.ops.domain.model.veiculo;

import com.spacebus.ops.domain.core.mapper.IBaseMapper;
import com.spacebus.ops.domain.entity.Veiculo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IVeiculoMapper extends IBaseMapper<Veiculo, VeiculoRequest, VeiculoResponse> {

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCriacao", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    Veiculo fromRequest(VeiculoRequest request);
}
