#!/bin/bash

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# URL base da API
BASE_URL="http://localhost:8083"

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
login_data='{"login":"admin","senha":"admin"}'
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
create_user_data='{"login":"test.user","senha":"test123","nome":"Usuário Teste","centroCustoId":1,"permissoes":["USER"]}'
create_user_response=$(make_request "POST" "/api/usuarios" "$create_user_data" "$token")
validate_response "$create_user_response" "id"

# Listar usuários
echo "Listando usuários..."
list_users_response=$(make_request "GET" "/api/usuarios" "" "$token")
validate_response "$list_users_response" "id"

# Atualizar usuário
echo "Atualizando usuário..."
update_user_data='{"login":"test.user","nome":"Usuário Teste Atualizado","centroCustoId":1,"permissoes":["USER"]}'
update_user_response=$(make_request "PUT" "/api/usuarios/1" "$update_user_data" "$token")
validate_response "$update_user_response" "id"

# Remover usuário
echo "Removendo usuário..."
delete_user_response=$(make_request "DELETE" "/api/usuarios/1" "" "$token")
validate_response "$delete_user_response" ""

echo -e "\n${YELLOW}Testes de usuários concluídos${NC}\n"

# Teste de cargos
echo "Testando CRUD de cargos..."

# Criar cargo
echo "Criando cargo..."
create_cargo_data='{"codigo":"DEV001","descricao":"Desenvolvedor Java","salarioBase":5000.00}'
create_cargo_response=$(make_request "POST" "/cargos" "$create_cargo_data" "$token")
validate_response "$create_cargo_response" "id"

# Listar cargos
echo "Listando cargos..."
list_cargos_response=$(make_request "GET" "/cargos" "" "$token")
validate_response "$list_cargos_response" "id"

# Atualizar cargo
echo "Atualizando cargo..."
update_cargo_data='{"codigo":"DEV001","descricao":"Desenvolvedor Java Senior","salarioBase":6000.00}'
update_cargo_response=$(make_request "PUT" "/cargos/1" "$update_cargo_data" "$token")
validate_response "$update_cargo_response" "id"

# Remover cargo
echo "Removendo cargo..."
delete_cargo_response=$(make_request "DELETE" "/cargos/1" "" "$token")
validate_response "$delete_cargo_response" ""

echo -e "\n${YELLOW}Testes de cargos concluídos${NC}\n"

# Teste de centros de custo
echo "Testando CRUD de centros de custo..."

# Criar centro de custo
echo "Criando centro de custo..."
create_centro_data='{"codigo":"CC001","descricao":"Centro de Custo TI","responsavel":"João Silva"}'
create_centro_response=$(make_request "POST" "/centros-custo" "$create_centro_data" "$token")
validate_response "$create_centro_response" "id"

# Listar centros de custo
echo "Listando centros de custo..."
list_centros_response=$(make_request "GET" "/centros-custo" "" "$token")
validate_response "$list_centros_response" "id"

# Atualizar centro de custo
echo "Atualizando centro de custo..."
update_centro_data='{"codigo":"CC001","descricao":"Centro de Custo TI Atualizado","responsavel":"João Silva"}'
update_centro_response=$(make_request "PUT" "/centros-custo/1" "$update_centro_data" "$token")
validate_response "$update_centro_response" "id"

# Remover centro de custo
echo "Removendo centro de custo..."
delete_centro_response=$(make_request "DELETE" "/centros-custo/1" "" "$token")
validate_response "$delete_centro_response" ""

echo -e "\n${YELLOW}Testes de centros de custo concluídos${NC}\n"

# Teste de linhas de negócio
echo "Testando CRUD de linhas de negócio..."

# Criar linha de negócio
echo "Criando linha de negócio..."
create_linha_data='{"codigo":"LN001","descricao":"Desenvolvimento de Software","responsavel":"Maria Santos"}'
create_linha_response=$(make_request "POST" "/linhas-negocio" "$create_linha_data" "$token")
validate_response "$create_linha_response" "id"

# Listar linhas de negócio
echo "Listando linhas de negócio..."
list_linhas_response=$(make_request "GET" "/linhas-negocio" "" "$token")
validate_response "$list_linhas_response" "id"

# Atualizar linha de negócio
echo "Atualizando linha de negócio..."
update_linha_data='{"codigo":"LN001","descricao":"Desenvolvimento de Software Atualizado","responsavel":"Maria Santos"}'
update_linha_response=$(make_request "PUT" "/linhas-negocio/1" "$update_linha_data" "$token")
validate_response "$update_linha_response" "id"

# Remover linha de negócio
echo "Removendo linha de negócio..."
delete_linha_response=$(make_request "DELETE" "/linhas-negocio/1" "" "$token")
validate_response "$delete_linha_response" ""

echo -e "\n${YELLOW}Testes de linhas de negócio concluídos${NC}\n"

# Teste de rubricas
echo "Testando CRUD de rubricas..."

# Criar rubrica
echo "Criando rubrica..."
create_rubrica_data='{"codigo":"SAL001","descricao":"Salário Base","tipoRubricaId":1}'
create_rubrica_response=$(make_request "POST" "/rubricas" "$create_rubrica_data" "$token")
validate_response "$create_rubrica_response" "id"

# Listar rubricas
echo "Listando rubricas..."
list_rubricas_response=$(make_request "GET" "/rubricas" "" "$token")
validate_response "$list_rubricas_response" "id"

# Atualizar rubrica
echo "Atualizando rubrica..."
update_rubrica_data='{"codigo":"SAL001","descricao":"Salário Base Atualizado","tipoRubricaId":1}'
update_rubrica_response=$(make_request "PUT" "/rubricas/1" "$update_rubrica_data" "$token")
validate_response "$update_rubrica_response" "id"

# Remover rubrica
echo "Removendo rubrica..."
delete_rubrica_response=$(make_request "DELETE" "/rubricas/1" "" "$token")
validate_response "$delete_rubrica_response" ""

echo -e "\n${YELLOW}Testes de rubricas concluídos${NC}\n"

# Teste de funcionários
echo "Testando CRUD de funcionários..."

# Criar funcionário
echo "Criando funcionário..."
create_employee_data='{"nome":"João Silva","cargoId":1,"centroCustoId":1,"linhaNegocioId":1,"idExterno":"FUNC001","dataAdmissao":"2024-01-15","sexo":"M","tipoSalario":"MENSAL","funcao":"Desenvolvedor","dependentesIrrf":2,"dependentesSalarioFamilia":1,"vinculo":"CLT"}'
create_employee_response=$(make_request "POST" "/funcionarios" "$create_employee_data" "$token")
validate_response "$create_employee_response" "id"

# Listar funcionários
echo "Listando funcionários..."
list_employees_response=$(make_request "GET" "/funcionarios" "" "$token")
validate_response "$list_employees_response" "id"

# Atualizar funcionário
echo "Atualizando funcionário..."
update_employee_data='{"nome":"João Silva Atualizado","cargoId":1,"centroCustoId":1,"linhaNegocioId":1,"idExterno":"FUNC001","dataAdmissao":"2024-01-15","sexo":"M","tipoSalario":"MENSAL","funcao":"Desenvolvedor Senior","dependentesIrrf":2,"dependentesSalarioFamilia":1,"vinculo":"CLT"}'
update_employee_response=$(make_request "PUT" "/funcionarios/1" "$update_employee_data" "$token")
validate_response "$update_employee_response" "id"

# Remover funcionário
echo "Removendo funcionário..."
delete_employee_response=$(make_request "DELETE" "/funcionarios/1" "" "$token")
validate_response "$delete_employee_response" ""

echo -e "\n${YELLOW}Testes de funcionários concluídos${NC}\n"

# Teste de folha de pagamento
echo "Testando consultas de folha de pagamento..."

# Listar folha de pagamento
echo "Listando folha de pagamento..."
list_payroll_response=$(make_request "GET" "/folha-pagamento" "" "$token")
validate_response "$list_payroll_response" "id"

# Consultar por funcionário
echo "Consultando folha por funcionário..."
list_payroll_func_response=$(make_request "GET" "/folha-pagamento/funcionario/1?dataInicio=2024-01-01&dataFim=2024-01-31" "" "$token")
validate_response "$list_payroll_func_response" "id"

# Consultar por centro de custo
echo "Consultando folha por centro de custo..."
list_payroll_centro_response=$(make_request "GET" "/folha-pagamento/centro-custo/1?dataInicio=2024-01-01&dataFim=2024-01-31" "" "$token")
validate_response "$list_payroll_centro_response" "id"

echo -e "\n${YELLOW}Testes de folha de pagamento concluídos${NC}\n"

# Teste de benefícios
echo "Testando consultas de benefícios..."

# Consultar benefícios por funcionário
echo "Consultando benefícios por funcionário..."
list_benefits_func_response=$(make_request "GET" "/beneficios/funcionario/1?data=2024-01-15" "" "$token")
validate_response "$list_benefits_func_response" "id"

# Consultar benefícios por centro de custo
echo "Consultando benefícios por centro de custo..."
list_benefits_centro_response=$(make_request "GET" "/beneficios/centro-custo/1?data=2024-01-15" "" "$token")
validate_response "$list_benefits_centro_response" "id"

echo -e "\n${YELLOW}Testes de benefícios concluídos${NC}\n"

# Teste de resumo folha de pagamento
echo "Testando consultas de resumo folha de pagamento..."

# Listar resumos
echo "Listando resumos de folha de pagamento..."
list_resumos_response=$(make_request "GET" "/resumo-folha-pagamento" "" "$token")
validate_response "$list_resumos_response" "id"

# Consultar por período
echo "Consultando resumos por período..."
list_resumos_periodo_response=$(make_request "GET" "/resumo-folha-pagamento/periodo?dataInicio=2024-01-01&dataFim=2024-01-31" "" "$token")
validate_response "$list_resumos_periodo_response" "id"

# Listar mais recentes
echo "Listando resumos mais recentes..."
list_resumos_latest_response=$(make_request "GET" "/resumo-folha-pagamento/latest" "" "$token")
validate_response "$list_resumos_latest_response" "id"

echo -e "\n${YELLOW}Testes de resumo folha de pagamento concluídos${NC}\n"

echo -e "${GREEN}✓ Todos os testes foram concluídos com sucesso!${NC}" 