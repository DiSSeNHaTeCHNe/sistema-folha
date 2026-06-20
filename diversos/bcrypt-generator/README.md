# Gerador de Hash BCrypt

Este é um programa simples para gerar hashes BCrypt para senhas.

## Requisitos

- Java 17 ou superior
- Maven

## Como compilar

```bash
mvn clean package
```

## Como usar

Após compilar, você pode executar o programa de duas formas:

1. Usando o JAR com dependências:
```bash
java -jar target/bcrypt-generator-1.0-SNAPSHOT-jar-with-dependencies.jar sua_senha
```

2. Usando o Maven:
```bash
mvn exec:java -Dexec.mainClass="br.com.techne.bcrypt.BcryptGenerator" -Dexec.args="sua_senha"
```

## Exemplo

```bash
java -jar target/bcrypt-generator-1.0-SNAPSHOT-jar-with-dependencies.jar admin
```

Saída:
```
Senha original: admin
Hash BCrypt: $2a$10$XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
Verificação do hash: CORRETO
```

## Observações

- O BCrypt gera um hash diferente cada vez que é executado, mesmo para a mesma senha
- Isso é normal e esperado, pois o BCrypt usa um "salt" aleatório
- O método `matches()` do BCrypt é capaz de verificar se a senha está correta, mesmo com hashes diferentes 