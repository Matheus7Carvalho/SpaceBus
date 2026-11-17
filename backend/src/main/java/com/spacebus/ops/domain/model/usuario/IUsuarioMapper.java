package com.spacebus.ops.domain.model.usuario;

import com.spacebus.ops.domain.core.mapper.IBaseMapper;
import com.spacebus.ops.domain.entity.Usuario;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IUsuarioMapper extends IBaseMapper<Usuario, UsuarioRequest, UsuarioResponse> {
}
