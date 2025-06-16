#!/bin/bash

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# URL base da API
BASE_URL="http://localhost:8080"

# Função para fazer requisições
make_request() {
  local method=$1
  local endpoint=$2
  local data=$3
  local token=$4

  if [ -z "$token" ]; then
    response=$(curl -s -X "$method" \
      -H "Content-Type: application/json" \
      -d "$data" \
      "$BASE_URL$endpoint")
  else
    response=$(curl -s -X "$method" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $token" \
      -d "$data" \
      "$BASE_URL$endpoint")
  fi

  echo "$response"
}

# Função para validar resposta
validate_response() {
  local response=$1
  local expected_status=$2

  if [[ $response == *"$expected_status"* ]]; then
    echo -e "${GREEN}✓ Sucesso${NC}"
    return 0
  else
    echo -e "${RED}✗ Falha${NC}"
    echo "Resposta: $response"
    return 1
  fi
}

echo -e "${YELLOW}Iniciando testes da API...${NC}\n"

# Teste de login
echo "Testando login..."
login_data='{"login":"admin","senha":"admin123"}'
login_response=$(make_request "POST" "/auth/login" "$login_data")
token=$(echo $login_response | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$token" ]; then
  echo -e "${RED}Erro ao obter token de autenticação${NC}"
  exit 1
fi

echo -e "${GREEN}Token obtido com sucesso${NC}\n"

# Teste de usuários
echo "Testando CRUD de usuários..."

# Criar usuário
echo "Criando usuário..."
create_user_data='{"login":"test.user","senha":"test123","nome":"Usuário Teste","centroCusto":"CC001","permissoes":["USER"]}'
create_user_response=$(make_request "POST" "/usuarios" "$create_user_data" "$token")
validate_response "$create_user_response" "id"

# Listar usuários
echo "Listando usuários..."
list_users_response=$(make_request "GET" "/usuarios" "" "$token")
validate_response "$list_users_response" "id"

# Atualizar usuário
echo "Atualizando usuário..."
update_user_data='{"id":1,"login":"test.user","nome":"Usuário Teste Atualizado","centroCusto":"CC002","permissoes":["USER"]}'
update_user_response=$(make_request "PUT" "/usuarios/1" "$update_user_data" "$token")
validate_response "$update_user_response" "id"

# Remover usuário
echo "Removendo usuário..."
delete_user_response=$(make_request "DELETE" "/usuarios/1" "" "$token")
validate_response "$delete_user_response" "id"

echo -e "\n${YELLOW}Testes de usuários concluídos${NC}\n"

# Teste de funcionários
echo "Testando CRUD de funcionários..."

# Criar funcionário
echo "Criando funcionário..."
create_employee_data='{"nome":"João Silva","cargo":"Desenvolvedor","centroCusto":"CC001","linhaNegocio":"TI","idExterno":"F001","dataAdmissao":"2024-01-01","sexo":"M","tipoSalario":"MENSAL","funcao":"DESENVOLVEDOR","depIrrf":0,"depSalFamilia":0,"vinculo":"CLT"}'
create_employee_response=$(make_request "POST" "/funcionarios" "$create_employee_data" "$token")
validate_response "$create_employee_response" "id"

# Listar funcionários
echo "Listando funcionários..."
list_employees_response=$(make_request "GET" "/funcionarios" "" "$token")
validate_response "$list_employees_response" "id"

# Atualizar funcionário
echo "Atualizando funcionário..."
update_employee_data='{"id":1,"nome":"João Silva Atualizado","cargo":"Desenvolvedor Senior","centroCusto":"CC002","linhaNegocio":"TI","idExterno":"F001","dataAdmissao":"2024-01-01","sexo":"M","tipoSalario":"MENSAL","funcao":"DESENVOLVEDOR","depIrrf":1,"depSalFamilia":1,"vinculo":"CLT"}'
update_employee_response=$(make_request "PUT" "/funcionarios/1" "$update_employee_data" "$token")
validate_response "$update_employee_response" "id"

# Remover funcionário
echo "Removendo funcionário..."
delete_employee_response=$(make_request "DELETE" "/funcionarios/1" "" "$token")
validate_response "$delete_employee_response" "id"

echo -e "\n${YELLOW}Testes de funcionários concluídos${NC}\n"

# Teste de folha de pagamento
echo "Testando CRUD de folha de pagamento..."

# Criar folha de pagamento
echo "Criando folha de pagamento..."
create_payroll_data='{"funcionarioId":1,"rubricaId":1,"dataInicio":"2024-01-01","dataFim":"2024-01-31","valor":5000.00,"quantidade":1,"baseCalculo":5000.00}'
create_payroll_response=$(make_request "POST" "/folha-pagamento" "$create_payroll_data" "$token")
validate_response "$create_payroll_response" "id"

# Listar folha de pagamento
echo "Listando folha de pagamento..."
list_payroll_response=$(make_request "GET" "/folha-pagamento" "" "$token")
validate_response "$list_payroll_response" "id"

# Atualizar folha de pagamento
echo "Atualizando folha de pagamento..."
update_payroll_data='{"id":1,"funcionarioId":1,"rubricaId":1,"dataInicio":"2024-01-01","dataFim":"2024-01-31","valor":5500.00,"quantidade":1,"baseCalculo":5500.00}'
update_payroll_response=$(make_request "PUT" "/folha-pagamento/1" "$update_payroll_data" "$token")
validate_response "$update_payroll_response" "id"

# Remover folha de pagamento
echo "Removendo folha de pagamento..."
delete_payroll_response=$(make_request "DELETE" "/folha-pagamento/1" "" "$token")
validate_response "$delete_payroll_response" "id"

echo -e "\n${YELLOW}Testes de folha de pagamento concluídos${NC}\n"

# Teste de benefícios
echo "Testando CRUD de benefícios..."

# Criar benefício
echo "Criando benefício..."
create_benefit_data='{"funcionarioId":1,"descricao":"Vale Refeição","valor":500.00,"dataInicio":"2024-01-01","dataFim":"2024-12-31","observacao":"Benefício mensal"}'
create_benefit_response=$(make_request "POST" "/beneficios" "$create_benefit_data" "$token")
validate_response "$create_benefit_response" "id"

# Listar benefícios
echo "Listando benefícios..."
list_benefits_response=$(make_request "GET" "/beneficios" "" "$token")
validate_response "$list_benefits_response" "id"

# Atualizar benefício
echo "Atualizando benefício..."
update_benefit_data='{"id":1,"funcionarioId":1,"descricao":"Vale Refeição Atualizado","valor":600.00,"dataInicio":"2024-01-01","dataFim":"2024-12-31","observacao":"Benefício mensal atualizado"}'
update_benefit_response=$(make_request "PUT" "/beneficios/1" "$update_benefit_data" "$token")
validate_response "$update_benefit_response" "id"

# Remover benefício
echo "Removendo benefício..."
delete_benefit_response=$(make_request "DELETE" "/beneficios/1" "" "$token")
validate_response "$delete_benefit_response" "id"

echo -e "\n${YELLOW}Testes de benefícios concluídos${NC}\n"

# Teste de relatórios
echo "Testando relatórios..."

# Gerar relatório de folha
echo "Gerando relatório de folha..."
generate_payroll_report_data='{"mes":1,"ano":2024}'
generate_payroll_report_response=$(make_request "POST" "/relatorios/folha" "$generate_payroll_report_data" "$token")
validate_response "$generate_payroll_report_response" "id"

# Listar relatórios de folha
echo "Listando relatórios de folha..."
list_payroll_reports_response=$(make_request "GET" "/relatorios/folha" "" "$token")
validate_response "$list_payroll_reports_response" "id"

# Download relatório de folha
echo "Fazendo download de relatório de folha..."
download_payroll_report_response=$(make_request "GET" "/relatorios/folha/1/download" "" "$token")
validate_response "$download_payroll_report_response" "id"

# Gerar relatório de benefícios
echo "Gerando relatório de benefícios..."
generate_benefit_report_data='{"mes":1,"ano":2024}'
generate_benefit_report_response=$(make_request "POST" "/relatorios/beneficio" "$generate_benefit_report_data" "$token")
validate_response "$generate_benefit_report_response" "id"

# Listar relatórios de benefícios
echo "Listando relatórios de benefícios..."
list_benefit_reports_response=$(make_request "GET" "/relatorios/beneficio" "" "$token")
validate_response "$list_benefit_reports_response" "id"

# Download relatório de benefícios
echo "Fazendo download de relatório de benefícios..."
download_benefit_report_response=$(make_request "GET" "/relatorios/beneficio/1/download" "" "$token")
validate_response "$download_benefit_report_response" "id"

echo -e "\n${YELLOW}Testes de relatórios concluídos${NC}\n"

echo -e "${GREEN}Todos os testes foram concluídos!${NC}" 