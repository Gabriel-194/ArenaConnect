INSERT INTO public.users (
    nome,
    email,
    cpf,
    telefone,
    senha_hash,
    role,
    asaas_customer_id,
    id_arena,
    ativo
)
SELECT
    'ArenaConnect',
    'ac.arenaconnect@gmail.com',
    '11352002973',
    '41(984890734)',
    '$2a$10$rhKB1nf9yqG2TduP9qz25.yc/BoM93dUkzmDNs0SWsE/gPgXZIUmi',
    'SUPERADMIN',
    NULL,
    NULL,
    true
    WHERE NOT EXISTS (
    SELECT 1 FROM public.users WHERE email = 'ac.arenaconnect@gmail.com' OR cpf = '11352002973'
);