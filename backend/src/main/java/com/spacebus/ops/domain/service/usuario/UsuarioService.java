package com.spacebus.ops.domain.service.usuario;

import com.spacebus.ops.domain.core.exception.DomainException;
import com.spacebus.ops.domain.core.service.BaseService;
import com.spacebus.ops.domain.entity.Usuario;
import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.repository.usuario.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService extends BaseService<Usuario> implements IUsuarioService {

    private final PasswordEncoder passwordEncoder;

    protected UsuarioService(final UsuarioRepository repository, final PasswordEncoder passwordEncoder) {
        super(repository);
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public Usuario create(Usuario usuario) throws DomainException {
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        return super.create(usuario);
    }

    @Override
    public void validate(Usuario usuario) throws DomainException {
        if (getRepository().existeEmail(usuario.getEmail(), usuario.getId())) {
            throw new DomainException("Já existe um usuário com o e-mail %s.", usuario.getEmail());
        }
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return getRepository().findByEmailIgnoreCase(email);
    }

    @Override
    public List<Usuario> motoristasAtivos() {
        return getRepository().findByPerfilAndAtivoTrueOrderByNome(Perfil.MOTORISTA);
    }

    @Override
    public UsuarioRepository getRepository() {
        return (UsuarioRepository) super.getRepository();
    }
}
