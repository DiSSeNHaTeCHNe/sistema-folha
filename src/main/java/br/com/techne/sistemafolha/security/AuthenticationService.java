package br.com.techne.sistemafolha.security;

import br.com.techne.sistemafolha.dto.LoginDTO;
import br.com.techne.sistemafolha.dto.TokenDTO;
import br.com.techne.sistemafolha.model.Usuario;
import br.com.techne.sistemafolha.repository.UsuarioRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthenticationService implements UserDetailsService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UsuarioRepository usuarioRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    public TokenDTO authenticate(LoginDTO loginDTO) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDTO.login(),
                        loginDTO.senha()
                )
        );

        var user = usuarioRepository.findByLogin(loginDTO.login())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        var jwtToken = jwtService.generateToken(loadUserByUsername(user.getLogin()));
        return new TokenDTO(user.getLogin(), jwtToken);
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        List<SimpleGrantedAuthority> authorities = usuario.getPermissoes().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new User(
                usuario.getLogin(),
                usuario.getSenha(),
                authorities
        );
    }
} 