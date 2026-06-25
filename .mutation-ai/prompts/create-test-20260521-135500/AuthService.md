Você é um gerador de testes unitários Java.

Tarefa:
Gere um único arquivo de teste unitário completo para a classe informada abaixo.

Regras obrigatórias de saída:
- responda com apenas código Java puro
- não inclua texto fora do código
- não inclua markdown
- não inclua blocos de código
- não inclua ```java, ``` ou qualquer outro delimitador de código
- não inclua explicações
- não inclua comentários explicando o código gerado
- retorne um único arquivo Java completo e compilável
- o arquivo deve incluir package, imports, declaração da classe de teste e métodos de teste completos
- inclua todos os imports necessários para o arquivo compilar no contexto do projeto
- inclua todos os static imports necessários para assertions do JUnit e utilitários do Mockito quando usados
- não use classes sem import correspondente, exceto classes do mesmo package ou de java.lang

Restrições de implementação:
- use JUnit 5
- use Mockito
- se usar assertEquals, assertThrows, assertNotNull ou assertNull, inclua os static imports apropriados
- se usar verify, when, doThrow, any, eq ou outros utilitários do Mockito, inclua os static imports apropriados
- use explicitamente @Mock e @InjectMocks quando fizer sentido
- use preferencialmente @ExtendWith(MockitoExtension.class) quando fizer sentido
- prefira testes unitários puros
- não suba contexto Spring sem necessidade estrita
- o nome da classe de teste deve ser AuthServiceTest
- mantenha no teste o mesmo package da classe alvo
- gere código compatível com src/test/java
- se houver dependências colaboradoras, use @Mock nelas quando fizer sentido
- use @InjectMocks na classe sob teste quando fizer sentido

Objetivo dos testes:
- cobrir comportamento observável
- cobrir caminho feliz
- cobrir falhas relevantes
- cobrir bordas importantes
- use assertNull apenas quando o cenário de null estiver explicitamente refletido no fluxo da implementação real
- nunca use assertNull em fluxos com Optional.get(), orElseThrow() ou acesso posterior a objeto ausente
- cubra cenários onde dependências retornam null somente quando isso estiver explicitamente refletido no fluxo real
- cubra Optional vazio quando aplicável
- se o fluxo real indicar exceção em cenário not found, use assertThrows
- cubra exceções relevantes
- para métodos void, não capture retorno; valide comportamento com verify(...) e, quando fizer sentido, assertThrows(...)
- não use assertEquals, assertNull ou assertNotNull para validar retorno de métodos void
- verifique interações com mocks quando isso fizer parte do comportamento observável
- quando o método delegar para repository, gateway, client ou outros serviços, valide as interações relevantes com verify(...)
- evite assertNotNull isolado quando houver interações relevantes ou resultado determinístico que possa ser validado de forma mais forte
- quando houver valor determinístico no resultado, prefira assertEquals com o valor esperado
- usar nomes de testes descritivos e legíveis
- evitar testes frágeis
- evitar mocks desnecessários
- não testar detalhes internos irrelevantes

Contexto do alvo:
- fullyQualifiedName: com.newsportal.service.AuthService
- sourceFile: src/main/java/com/newsportal/service/AuthService.java

Dependências identificadas (prioridade para dependências de construtor):
- UserRepository userRepository
- PasswordEncoder passwordEncoder
- JwtService jwtService
- LoginAttemptService loginAttemptService
- boolean debugResetToken

Código fonte da classe alvo:
package com.newsportal.service;

import com.newsportal.dto.AuthenticatedUserResponse;
import com.newsportal.dto.LoginRequest;
import com.newsportal.dto.LoginResponse;
import com.newsportal.dto.RegisterRequest;
import com.newsportal.exception.ResourceNotFoundException;
import com.newsportal.exception.BadRequestException;
import com.newsportal.model.User;
import com.newsportal.repository.UserRepository;
import com.newsportal.security.JwtService;
import com.newsportal.security.LoginAttemptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final Pattern STRONG_PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Value("${app.auth.debug-reset-token:false}")
    private boolean debugResetToken;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String emailKey = request.getEmail().trim().toLowerCase();
        if (loginAttemptService.isBlocked(emailKey)) {
            long waitSeconds = loginAttemptService.getRemainingBlockSeconds(emailKey);
            throw new BadRequestException("Too many login attempts. Try again in " + Math.max(waitSeconds, 1) + " seconds");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    loginAttemptService.onFailure(emailKey);
                    return new BadRequestException("Invalid email or password");
                });

        if (!user.getIsActive()) {
            throw new BadRequestException("Account is inactive");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.onFailure(emailKey);
            throw new BadRequestException("Invalid email or password");
        }
        loginAttemptService.onSuccess(emailKey);

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user);

        return new LoginResponse(user.getId(), user.getName(),
                user.getEmail(), user.getRole(), user.getAvatarUrl(),
                token, "Bearer", jwtService.extractExpiration(token).toInstant().getEpochSecond());
    }

    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already in use");
        }
        validatePasswordStrength(request.getPassword());

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() == null ? User.Role.EDITOR : request.getRole());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setIsActive(true);

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AuthenticatedUserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return new AuthenticatedUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getAvatarUrl()
        );
    }

    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {

            return;
        }

        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));

        userRepository.save(user);

        if (debugResetToken) {
            System.out.println("Reset token for " + email + ": " + resetToken);
        }
    }

    public void resetPassword(String token, String newPassword) {
        validatePasswordStrength(newPassword);

        User user = userRepository.findByValidResetToken(token, LocalDateTime.now())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);

        userRepository.save(user);
    }

    private void validatePasswordStrength(String password) {
        if (password == null || !STRONG_PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BadRequestException("Password must have at least 8 chars, one uppercase, one lowercase and one number");
        }
    }
}