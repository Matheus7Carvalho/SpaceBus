package com.spacebus.ops.domain.service.limpeza;

import com.spacebus.ops.domain.core.exception.DomainException;
import com.spacebus.ops.domain.entity.Usuario;
import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.repository.usuario.UsuarioRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

/**
 * Motorista só de um teste E2E (perfil dev): um motorista tem no máximo uma viagem em andamento, e os testes correm em
 * paralelo. O nome leva a marca, para a limpeza ({@link LimpezaDadosTesteService}) apagá-lo depois.
 */
@Service
@Profile("dev")
public class MotoristaTesteService {

    /** Mesma senha das contas de demonstração (seed de dev). */
    public static final String SENHA = "demo123";

    public record MotoristaTeste(String id, String nome, String email) {
    }

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;

    public MotoristaTesteService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public MotoristaTeste criar(String marca) {
        if (!LimpezaDadosTesteService.marcaValida(marca)) throw new DomainException("Informe uma marca de teste (ex.: E2E).");
        var sufixo = UUID.randomUUID().toString().substring(0, 8);
        var nome = marca + " Motorista " + sufixo.toUpperCase(Locale.ROOT);
        var email = marca.toLowerCase(Locale.ROOT) + "-motorista-" + sufixo + "@teste.local";
        var salvo = usuarios.save(new Usuario(nome, email, passwordEncoder.encode(SENHA), Perfil.MOTORISTA, true));
        return new MotoristaTeste(salvo.getId(), nome, email);
    }
}
