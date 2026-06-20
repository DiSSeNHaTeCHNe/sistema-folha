#!/bin/bash

# Script para testar a importação de folha de pagamento
# Requer: curl, jq

echo "=== Teste de Importação de Folha de Pagamento ==="

# Configurações
API_URL="http://localhost:8080"
FILE_PATH="../exemplo-folha-pagamento.txt"

# Verifica se o arquivo existe
if [ ! -f "$FILE_PATH" ]; then
    echo "Erro: Arquivo $FILE_PATH não encontrado"
    exit 1
fi

echo "Arquivo de teste: $FILE_PATH"
echo "API URL: $API_URL"

# Testa se a API está rodando
echo "Verificando se a API está rodando..."
if ! curl -s "$API_URL/actuator/health" > /dev/null; then
    echo "Erro: API não está rodando em $API_URL"
    echo "Certifique-se de que o backend está iniciado"
    exit 1
fi

echo "API está rodando ✓"

# Testa a importação
echo "Iniciando teste de importação..."

# Faz o upload do arquivo
RESPONSE=$(curl -s -X POST \
    -F "arquivo=@$FILE_PATH" \
    "$API_URL/importacao/folha")

echo "Resposta da API:"
echo "$RESPONSE" | jq '.'

# Verifica se a importação foi bem-sucedida
SUCCESS=$(echo "$RESPONSE" | jq -r '.success // false')

if [ "$SUCCESS" = "true" ]; then
    echo "✓ Importação realizada com sucesso!"
    
    # Exibe informações adicionais
    ARQUIVO=$(echo "$RESPONSE" | jq -r '.arquivo // "N/A"')
    TAMANHO=$(echo "$RESPONSE" | jq -r '.tamanho // 0')
    
    echo "Arquivo: $ARQUIVO"
    echo "Tamanho: $TAMANHO bytes"
    
else
    echo "✗ Erro na importação"
    MESSAGE=$(echo "$RESPONSE" | jq -r '.message // "Erro desconhecido"')
    echo "Mensagem de erro: $MESSAGE"
    exit 1
fi

echo ""
echo "=== Teste concluído ===" 