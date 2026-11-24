package com.spacebus.ops.domain.service.limpeza;

import com.spacebus.ops.domain.entity.Usuario;
import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.repository.usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MotoristaTesteServiceTest {

    @Mock private UsuarioRepository usuarios;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @Test
    void criaMotoristaAtivoComAMarcaNoNomeESenhaComBcrypt() {
        when(usuarios.save(any())).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId("m9");
            return u;
        });

        var criado = new MotoristaTesteService(usuarios, encoder).criar("E2E");

        var captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captor.capture());
        var salvo = captor.getValue();
        assertThat(salvo.getNome()).startsWith("E2E Motorista ");
        assertThat(salvo.getPerfil()).isEqualTo(Perfil.MOTORISTA);
        assertThat(salvo.getAtivo()).isTrue();
        assertThat(encoder.matches(MotoristaTesteService.SENHA, salvo.getSenha())).isTrue();
        assertThat(criado.id()).isEqualTo("m9");
        assertThat(criado.email()).endsWith("@teste.local").isEqualTo(salvo.getEmail());
    }

    @Test
    void recusaMarcaQueNaoEDeTeste() {
        assertThatThrownBy(() -> new MotoristaTesteService(usuarios, encoder).criar("Centro"))
                .hasMessage("Informe uma marca de teste (ex.: E2E).");
        verifyNoInteractions(usuarios);
    }
}
