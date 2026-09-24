package com.chaveiro_abencoado.back.config;

import com.chaveiro_abencoado.back.model.UserRole;
import com.chaveiro_abencoado.back.model.Usuario;
import com.chaveiro_abencoado.back.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Pattern;

// Provisiona o primeiro acesso do dono a partir de variáveis de ambiente, em vez de depender
// de uma senha fixa no seed (V2/V5). Roda em todo startup, mas só age quando NÃO existe
// nenhum DONO ativo — reiniciar o servidor nunca reseta a senha de um dono já configurado.
//
// Variáveis: DONO_INICIAL_NOME, DONO_INICIAL_EMAIL, DONO_INICIAL_SENHA.
// Depois do primeiro provisionamento bem-sucedido elas podem ser removidas do ambiente.
@Component
@Order(1)
public class DonoInicialInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DonoInicialInitializer.class);
    private static final Pattern SENHA_VALIDA = Pattern.compile("^(?=.*[A-Z])(?=.*\\d).{8,}$");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String nomeEnv;
    private final String emailEnv;
    private final String senhaEnv;

    public DonoInicialInitializer(UsuarioRepository usuarioRepository,
                                  PasswordEncoder passwordEncoder,
                                  org.springframework.core.env.Environment environment) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.nomeEnv = environment.getProperty("DONO_INICIAL_NOME");
        this.emailEnv = environment.getProperty("DONO_INICIAL_EMAIL");
        this.senhaEnv = environment.getProperty("DONO_INICIAL_SENHA");
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.existsByRoleAndAtivoTrue(UserRole.DONO)) {
            return; // já existe acesso de dono válido; nunca sobrescreve
        }

        if (nomeEnv == null || nomeEnv.isBlank() || emailEnv == null || emailEnv.isBlank()
                || senhaEnv == null || senhaEnv.isBlank()) {
            log.warn("Nenhum DONO ativo e as variaveis DONO_INICIAL_NOME/DONO_INICIAL_EMAIL/"
                    + "DONO_INICIAL_SENHA nao estao todas configuradas. O sistema ficara sem "
                    + "acesso administrativo ate que isso seja corrigido.");
            return;
        }

        if (!SENHA_VALIDA.matcher(senhaEnv).matches()) {
            log.error("DONO_INICIAL_SENHA nao atende aos requisitos minimos (8+ caracteres, "
                    + "1 maiuscula, 1 numero). Acesso inicial do dono NAO foi provisionado.");
            return;
        }

        Optional<Usuario> existente = usuarioRepository.findByEmail(emailEnv);
        if (existente.isPresent() && existente.get().getRole() != UserRole.DONO) {
            log.error("DONO_INICIAL_EMAIL ja pertence a um usuario com role {} — acesso "
                    + "inicial do dono NAO foi provisionado para evitar sobrescrever essa conta.",
                    existente.get().getRole());
            return;
        }

        Usuario dono = existente.orElseGet(() -> new Usuario(nomeEnv, emailEnv,
                passwordEncoder.encode(senhaEnv), UserRole.DONO));
        dono.setNome(nomeEnv);
        dono.setSenha(passwordEncoder.encode(senhaEnv));
        dono.setAtivo(true);
        usuarioRepository.save(dono);

        log.info("Acesso inicial do dono provisionado para {} a partir de DONO_INICIAL_*. "
                + "Essas variaveis podem ser removidas do ambiente agora.", emailEnv);
    }
}
