package com.spacebus.ops.domain.service.auditoria;

import com.spacebus.ops.domain.entity.AuditoriaAcesso;
import com.spacebus.ops.domain.entity.Usuario;
import com.spacebus.ops.domain.entity.enums.AcaoAuditada;
import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.repository.auditoria.AuditoriaAcessoRepository;
import com.spacebus.ops.domain.repository.usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    @Mock private AuditoriaAcessoRepository repository;
    @Mock private UsuarioRepository usuarioRepository;
    @InjectMocks private AuditoriaService service;

    @Test
    void gravaQuemAcessouComPerfilAcaoERecurso() {
        var ana = new Usuario("Ana", "operador@spacebus.local", "h", Perfil.OPERADOR, true);
        when(usuarioRepository.getReferenceById("op1")).thenReturn(ana);

        service.registrar("op1", Perfil.OPERADOR, AcaoAuditada.CONSULTA_PASSAGEIROS_VIAGEM, "vg1");

        var captor = ArgumentCaptor.forClass(AuditoriaAcesso.class);
        verify(repository).save(captor.capture());
        var registro = captor.getValue();
        assertThat(registro.getUsuario()).isSameAs(ana);
        assertThat(registro.getPerfil()).isEqualTo(Perfil.OPERADOR);
        assertThat(registro.getAcao()).isEqualTo(AcaoAuditada.CONSULTA_PASSAGEIROS_VIAGEM);
        assertThat(registro.getRecursoId()).isEqualTo("vg1");
    }

    /** A unicidade por hora fica no banco (insert ... on conflict do nothing); aqui, só o que é enviado. */
    @Test
    void consultaPorHoraUsaOInsertCondicionalSemSaveDaEntidade() {
        var agora = java.time.LocalDateTime.of(2026, 11, 17, 8, 40);

        service.registrarUmaVezPorHora("adm1", Perfil.ADMINISTRADOR, AcaoAuditada.CONSULTA_LOCALIZACAO_VIAGEM, "vg1", agora);

        verify(repository).inserirSeNovoNaHora(any(), org.mockito.ArgumentMatchers.eq(agora), org.mockito.ArgumentMatchers.eq("adm1"),
                org.mockito.ArgumentMatchers.eq("ADMINISTRADOR"), org.mockito.ArgumentMatchers.eq("CONSULTA_LOCALIZACAO_VIAGEM"),
                org.mockito.ArgumentMatchers.eq("vg1"));
        verify(repository, never()).save(any());
    }
}
