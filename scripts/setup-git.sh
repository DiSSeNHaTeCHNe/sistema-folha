#!/bin/bash

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}Iniciando configuração do Git...${NC}\n"

# Verifica se o Git está instalado
if ! command -v git &> /dev/null; then
    echo -e "${RED}Git não está instalado. Por favor, instale o Git primeiro.${NC}"
    exit 1
fi

# Inicializa o repositório Git
echo "Inicializando repositório Git..."
git init

# Adiciona os arquivos
echo "Adicionando arquivos..."
git add .

# Faz o primeiro commit
echo "Criando primeiro commit..."
git commit -m "Commit inicial: Sistema de Folha de Pagamento"

# Pede a URL do repositório remoto
echo -e "\n${YELLOW}Por favor, crie um repositório no GitHub e forneça a URL:${NC}"
read -p "URL do repositório (ex: https://github.com/usuario/sistema-folha.git): " repo_url

# Adiciona o remote
echo "Configurando repositório remoto..."
git remote add origin "$repo_url"

# Envia o código
echo "Enviando código para o GitHub..."
git push -u origin main

echo -e "\n${GREEN}Configuração do Git concluída com sucesso!${NC}"
echo -e "${YELLOW}Próximos passos:${NC}"
echo "1. Verifique se o código foi enviado corretamente no GitHub"
echo "2. Configure as proteções de branch no GitHub"
echo "3. Adicione os colaboradores do projeto"
echo "4. Configure as actions do GitHub (se necessário)" 