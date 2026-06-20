package br.com.techne.bcrypt;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptGenerator {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Por favor, forneça a senha como argumento.");
            System.out.println("Exemplo: java -jar bcrypt-generator.jar admin");
            return;
        }

        String senha = args[0];
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(senha);

        System.out.println("Senha original: " + senha);
        System.out.println("Hash BCrypt: " + hash);
        
        // Verifica se o hash está correto
        boolean matches = encoder.matches(senha, hash);
        System.out.println("Verificação do hash: " + (matches ? "CORRETO" : "INCORRETO"));
    }
} 