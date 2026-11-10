package com.register.api.config;

import com.register.api.ApiApplication;
import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
public class JwtConfig {


 /*O método jwtSecretKey dentro da classe JwtConfig transforma
   * uma senha de texto comum (String) em uma chave criptográfica
   * segura compatível com o algoritmo HMAC-SHA256, usada para assinar
   * e validar tokens JWT.
  */
 @Bean 
  SecretKey jwtSecretKey(@Value("${app.jwt.secret}") String secret) {
    return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

   /*
    * Computadores e algoritmos de criptografia não entendem letras
    * textuais diretamente; eles precisam de bytes (números).
    * Essa função converte o seu texto secreto em um array de bytes
    * (byte[]) usando a codificação padrão UTF-8, garantindo que caracteres
    * especiais sejam convertidos da mesma forma em qualquer sistema operacional.
   */
  }

  /*
   * O método jwtEncoder cria e disponibiliza
   * o objeto responsável por gerar (criptografar e assinar)
   * os novos tokens JWT quando os usuários fazem login na API.
  */
  @Bean
  JwtEncoder jwtEncoder(SecretKey jwSecretKey) {
    /*
     * Como o metodo é um Bean o objeto retornado por esse método deve ser guardado
     * no contexto do Spring (o container de Inversão de Controle).
     * Isso significa que, sempre que outra classe da API
     * (como o seu AuthService ou TokenService) precisar gerar um JWT,
     * ela poderá simplesmente injetar o JwtEncoder automaticamente. 
    */
    return new NimbusJwtEncoder(new ImmutableSecret<>(jwSecretKey));
    /*
     * Injeção do Parâmetro (SecretKey jwSecretKey).

     * O Spring é inteligente: ele percebe que você precisa de um SecretKey.
     * Ele vai olhar dentro da mesma classe (ou em outras configurações)
     * e encontrar o método jwtSecretKey.
     * O Spring executa o método, pega a chave configurada no app.jwt.secret e a
     * entrega pronta como argumento para este método.
     * 
     * A Criação do Codificador (new NimbusJwtEncoder(...))
     * 
     * O NimbusJwtEncoder é a implementação oficial padrão do Spring Security para criar tokens. 
     * Ele junta a estrutura do JWT (Header, Payload/Claims) com a sua chave secreta,
     * calcula o Hash criptográfico e transforma tudo em uma string de
     * texto compacta (separada por três pontos xxxx.yyyy.zzzz)
     * pronta para ser enviada ao cliente (frontend/mobile).

    */

  }

  /*
   * O método configura a "máquina de fiscalização" da API.
   * Ele cria o objeto responsável por interceptar, ler e validar
   * os tokens JWT que os clientes (frontend, mobile, etc.)
   * enviam no cabeçalho das requisições para acessar rotas protegidas.
   * 
   * O Spring injeta automaticamente a chave criptográfica gerada a partir
   * do app.jwt.secret.
   * Para validar a assinatura de um token gerado com criptografia simétrica,
   * a API precisa usar exatamente a mesma chave que usou para criá-lo.
   * @Value("${app.jwt.issuer}") String issuer: Injeta uma propriedade que define
   * quem é o "emissor" (issuer) oficial do token
   * (geralmente o nome ou a URL da sua própria API, ex: meusistema.com).
   * 
   * Definindo a Chave de Validação
   * NimbusJwtDecoder.withSecretKey(jwSecretKey)
   * Aqui a API diz ao Nimbus (a biblioteca de segurança):
   * "Para abrir e verificar os tokens que chegarem, use esta chave secreta aqui".
   * Se alguém tentar enviar um token gerado com qualquer outra chave, o decoder
   * vai rejeitá-lo imediatamente e barrar a requisição.

  */
  @Bean
  JwtDecoder jwtDecoder(SecretKey jwSecretKey, @Value("${app.jwt.issuer}") String issuer) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder
      .withSecretKey(jwSecretKey)
      .macAlgorithm(MacAlgorithm.HS256)
      .build();
    return decoder;

  }

  /*
   * Forçando o Algoritmo Correto
   * .macAlgorithm(MacAlgorithm.HS256)
   * Esta linha é crítica para a segurança da API.
   * Ela força o decoder a aceitar apenas tokens assinados especificamente
   * com o algoritmo HS256 (HMAC usando SHA-256).
   * Por que isso importa? No passado, APIs que não travavam
   * o algoritmo eram vulneráveis a um ataque chamado JWT Algorithm Confusion.
   * Nele, um hacker alterava o cabeçalho do token para "alg": "none"
   * (nenhum algoritmo) ou tentava usar chaves públicas para enganar o validador.
   * Forçar o HS256 anula completamente esse tipo de vulnerabilidade.
   * 
   * Construindo e Retornando o Bean
   * .build(); e return decoder;
   * Junta todas as regras configuradas e gera a instância do NimbusJwtDecoder.
   * Por estar com a anotação @Bean, o Spring Security vai pegar esse decodificador
   * e embuti-lo automaticamente no filtro de requisições da API (BearerTokenAuthenticationFilter).
   * 
   * O que acontece por baixo dos panos quando uma requisição chega?
   * Quando um usuário tenta acessar, por exemplo, GET /usuarios,
   * o Spring Security captura o token enviado e entrega para este jwtDecoder.
   * O decoder realiza 3 validações automáticas:
   * 
   * 1. Integridade (Assinatura): Recalcula o hash do token usando a jwSecretKey
   * para garantir que ninguém alterou o conteúdo (como mudar o perfil de USER para ADMIN).
   * 
   * 2. Expiração (exp): Verifica se a data de validade do token ainda é futura.
   * Se o token expirou, ele joga um erro de não autorizado (401).
   * 
   * 3. Formato: Garante que a estrutura do token está matematicamente correta.
   * Se passar por tudo isso, o Spring Security extrai o nome do usuário e as
   * permissões de dentro do token e autoriza a requisição.
   * com a variável issuer injetada, para a validação do emissor dentro desse decoder
   * para deixar a API ainda mais segura.
  */

}
