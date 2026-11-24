package com.spacebus.ops.domain.service.auditoria;

import com.spacebus.ops.domain.entity.AuditoriaAcesso;
import com.spacebus.ops.domain.entity.enums.AcaoAuditada;
import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.repository.auditoria.AuditoriaAcessoRepository;
import com.spacebus.ops.domain.repository.usuario.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService implements IAuditoriaService {

    private static final Logger LOG = LoggerFactory.getLogger("auditoria");

    private final AuditoriaAcessoRepository repository;
    private final UsuarioRepository usuarioRepository;

    public AuditoriaService(AuditoriaAcessoRepository repository, UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional
    public void registrar(String usuarioId, Perfil perfil, AcaoAuditada acao, String recursoId) {
        repository.save(new AuditoriaAcesso(usuarioRepository.getReferenceById(usuarioId), perfil, acao, recursoId));
        LOG.info("usuario={} perfil={} acao={} recurso={}", usuarioId, perfil, acao, recursoId);
    }
}
