package com.spacebus.ops.domain.service.usuario;

import com.spacebus.ops.domain.core.exception.DomainException;
import com.spacebus.ops.domain.entity.Usuario;
import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.repository.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository repository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private UsuarioService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(repository, encoder);
    }

    @Test
    void gravaSenhaComoHash() {
        when(repository.existeEmail("novo@spacebus.local", null)).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var salvo = service.create(new Usuario("Novo", "novo@spacebus.local", "segredo1", Perfil.MOTORISTA, true));

        assertThat(salvo.getSenha()).isNotEqualTo("segredo1");
        assertThat(encoder.matches("segredo1", salvo.getSenha())).isTrue();
    }

    @Test
    void rejeitaEmailDuplicado() {
        when(repository.existeEmail("admin@spacebus.local", null)).thenReturn(true);

        assertThatThrownBy(() -> service.create(new Usuario("X", "admin@spacebus.local", "segredo1", Perfil.ADMINISTRADOR, true)))
                .isInstanceOf(DomainException.class)
                .hasMessage("Já existe um usuário com o e-mail admin@spacebus.local.");

        verify(repository, never()).save(any());
    }

    @Test
    void buscaPorEmailIgnorandoMaiusculas() {
        var usuario = new Usuario("Ana Operadora", "operador@spacebus.local", "hash", Perfil.OPERADOR, true);
        when(repository.findByEmailIgnoreCase("OPERADOR@spacebus.local")).thenReturn(Optional.of(usuario));

        assertThat(service.buscarPorEmail("OPERADOR@spacebus.local")).contains(usuario);
    }

    @Test
    void buscaPorEmailInexistenteRetornaVazio() {
        when(repository.findByEmailIgnoreCase("ninguem@spacebus.local")).thenReturn(Optional.empty());

        assertThat(service.buscarPorEmail("ninguem@spacebus.local")).isEmpty();
    }
}
