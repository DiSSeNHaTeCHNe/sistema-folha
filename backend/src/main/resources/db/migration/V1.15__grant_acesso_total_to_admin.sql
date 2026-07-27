INSERT INTO usuario_permissoes (usuario_id, permissao)
SELECT u.id, 'ACESSO_TOTAL'
FROM usuarios u
WHERE u.login = 'admin'
ON CONFLICT DO NOTHING;
