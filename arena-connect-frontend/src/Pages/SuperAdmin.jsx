import React, { useState,useEffect } from 'react';
import '../Styles/SuperAdmin.css';
import axios from 'axios';

const formatTelefone = (telefone) => {
    if (!telefone) return "---";
    const limpo = telefone.replace(/\D/g, '');
    if (limpo.length === 11) return `(${limpo.slice(0, 2)}) ${limpo.slice(2, 7)}-${limpo.slice(7)}`;
    if (limpo.length === 10) return `(${limpo.slice(0, 2)}) ${limpo.slice(2, 6)}-${limpo.slice(6)}`;
    return telefone;
};

const formatCep = (cep) => {
    if (!cep) return "---";
    const limpo = cep.replace(/\D/g, '');
    if (limpo.length !== 8) return cep;
    return limpo.replace(/(\d{5})(\d{3})/, "$1-$2");
};

const formatCpf = (cpf) => {
    if (!cpf) return "---";
    const limpo = cpf.replace(/\D/g, '');
    if (limpo.length !== 11) return cpf;
    return limpo.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, "$1.$2.$3-$4");
};

const formatCnpj = (cnpj) => {
    if (!cnpj) return "---";
    const limpo = cnpj.replace(/\D/g, '');
    if (limpo.length !== 14) return cnpj;
    return limpo.replace(/(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})/, "$1.$2.$3/$4-$5");
};

export default function SuperAdmin() {
    const [activeTab, setActiveTab] = useState('dashboard');
    const [users, setUsers] = useState([]);
    const [arenas, setArenas] = useState([]);
    const [loading, setLoading] = useState(false);


    const fetchDatas = async () => {
        setLoading(true);
        try {
            const [usersResponse, arenasResponse] = await Promise.all([
                axios.get('http://localhost:8080/api/users/all', { withCredentials: true }),
                axios.get('http://localhost:8080/api/arena/all', { withCredentials: true })
            ]);

            setUsers(usersResponse.data);
            setArenas(arenasResponse.data);
        } catch (error) {
            console.error("Erro ao buscar dados:", error);
        } finally {
            setLoading(false);
        }
    };

    const handleDeleteUser = async (id) => {
        if (!window.confirm("Tem certeza que deseja excluir (desativar) este usuário?")) {
            return;
        }

        try {
            await axios.delete(`http://localhost:8080/api/users/${id}`, {
                withCredentials: true
            });

            setUsers(prevUsers => prevUsers.filter(user => (user.idUser || user.id) !== id));

        } catch (error) {
            console.error("Erro ao excluir usuário:", error);
            alert("Não foi possível excluir o usuário.");
        }
    };

    useEffect(() => {
        if (activeTab === 'dashboard') {
            fetchDatas();
        }
    }, [activeTab]);

    return (
        <div className="superadmin-body">
            <div className="liquid-background-fixed">
                <div className="neon-blob blob-1"></div>
                <div className="neon-blob blob-2"></div>
                <div className="neon-blob blob-3"></div>
            </div>

            <div className="superadmin-container">


                <header className="glass-header-panel">
                    <div className="header-left">
                        <a href="#" className="logo-compact">
                            <img src="/Assets/3-removebg-preview.png" alt="Logo" />
                        </a>
                        <span className="brand-text">Arena Connect</span>
                    </div>

                    <nav className="header-nav">
                        <button
                            className={`nav-btn ${activeTab === 'dashboard' ? 'active' : ''}`}
                            onClick={() => setActiveTab('dashboard')}
                        >
                            Visão Geral
                        </button>
                        <button
                            className={`nav-btn ${activeTab === 'finance' ? 'active' : ''}`}
                            onClick={() => setActiveTab('finance')}
                        >
                            Financeiro
                        </button>
                    </nav>

                    <button className="btn-neon-outlined">Sair</button>
                </header>

                <div className="admin-content-area">

                    {activeTab === 'dashboard' && (
                        <div className="admin-panels-grid">

                            <section className="admin-glass-panel">
                                <div className="panel-header">
                                    <h2>Arenas Registradas</h2>
                                    <span className="counter-badge">{arenas.length}</span>
                                </div>

                                <div className="list-header-row arena-header">
                                    <span>St</span>
                                    <span>Arena</span>
                                    <span>CEP / CNPJ</span>
                                    <span>Responsável</span>
                                    <span style={{textAlign: 'right'}}>Ações</span>
                                </div>

                                <div className="custom-scroll-area">
                                    {loading ? (
                                        <p style={{textAlign: 'center', color: '#888', marginTop: '20px'}}>Carregando arenas...</p>
                                    ) : arenas.length > 0 ? (
                                        arenas.map((arena) => (
                                            <div className="list-item arena-item" key={arena.idArena || arena.id || Math.random()}>

                                                <div className="col-status">
                                                    <span className={`status-indicator ${arena.ativo ? 'online' : 'offline'}`} title={arena.ativo ? 'Ativa' : 'Inativa'}></span>
                                                </div>

                                                <div className="col-info-main">
                                                    <h3 title={arena.nome}>{arena.nome}</h3>
                                                </div>

                                                <div className="col-documents">
                                                    <span className="mini-data">{formatCep(arena.cep)}</span>
                                                    <span className="mini-data">{formatCnpj(arena.cnpj)}</span>
                                                </div>

                                                <div className="col-documents">
                                                    <span className="mini-data" style={{fontWeight: 'bold', color: '#fff'}}> {arena.adminNome || 'Sem Admin'}</span>
                                                    <span className="mini-data">{arena.adminEmail || 'N/A'}</span>
                                                </div>

                                                <div className="col-actions">
                                                    <button className="mini-action-btn edit" title="Editar Arena">
                                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                                            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                                                            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4L18.5 2.5z"></path>
                                                        </svg>
                                                    </button>
                                                    <button className="mini-action-btn cancel" title="Excluir Arena">
                                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                                            <line x1="18" y1="6" x2="6" y2="18"></line>
                                                            <line x1="6" y1="6" x2="18" y2="18"></line>
                                                        </svg>
                                                    </button>
                                                </div>
                                            </div>
                                        ))
                                    ) : (
                                        <p style={{textAlign: 'center', color: '#888', marginTop: '20px'}}>Nenhuma arena encontrada.</p>
                                    )}
                                </div>
                            </section>

                            <section className="admin-glass-panel">
                                <div className="panel-header">
                                    <h2>Usuários</h2>
                                    <span className="counter-badge">{users.length}</span>
                                </div>

                                <div className="list-header-row">
                                    <span></span>
                                    <span>Nome</span>
                                    <span>E-mail</span>
                                    <span>CPF</span>
                                    <span>Telefone</span>
                                    <span style={{textAlign: 'right'}}>Ações</span>
                                </div>

                                <div className="custom-scroll-area">
                                    {loading ? (
                                        <p style={{textAlign: 'center', color: '#888', marginTop: '20px'}}>Carregando usuários...</p>
                                    ) : users.length > 0 ? (
                                        users.map((user) => (
                                            <div className="list-item" key={user.idUser || user.id || Math.random()}>

                                                <div className="col-avatar">
                                                    <div className="user-avatar-mini">
                                                        {user.nome ? user.nome.charAt(0).toUpperCase() : 'U'}
                                                    </div>
                                                </div>

                                                <div className="col-info-main">
                                                    <h3 title={user.nome}>{user.nome || "Sem Nome"}</h3>
                                                    <span className={`role-tag ${user.role === 'ADMIN' || user.role === 'SUPERADMIN' ? 'admin-tag' : 'client-tag'}`}>
                                                        {user.role || 'CLIENTE'}
                                                    </span>
                                                </div>

                                                <span className="sub-text email-col" title={user.email}>
                                                    {user.email}
                                                </span>

                                                <span className="mini-data" title="CPF">
                                                    {formatCpf(user.cpf)}
                                                </span>

                                                <span className="mini-data" title="Telefone">
                                                    {formatTelefone(user.telefone)}
                                                </span>

                                                <div className="col-actions">
                                                    <button className="mini-action-btn edit" title="Editar usuario" >
                                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                                            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                                                            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4L18.5 2.5z"></path>
                                                        </svg>
                                                    </button>
                                                    <button className="mini-action-btn cancel" title="deletar usuario" onClick={() => handleDeleteUser(user.idUser || user.id)}>
                                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                                            <line x1="18" y1="6" x2="6" y2="18"></line>
                                                            <line x1="6" y1="6" x2="18" y2="18"></line>
                                                        </svg>
                                                    </button>
                                                </div>

                                            </div>
                                        ))
                                    ) : (
                                        <p style={{textAlign: 'center', color: '#888', marginTop: '20px'}}>Nenhum usuário encontrado.</p>
                                    )}
                                </div>
                            </section>
                        </div>
                    )}

                    {activeTab === 'finance' && (
                        <div className="finance-layout">
                            <div className="finance-summary">
                                <div className="summary-card">
                                    <span className="summary-label">Faturamento Total</span>
                                    <h3 className="summary-value">R$ 15.250,00</h3>
                                </div>
                                <div className="summary-card highlight">
                                    <span className="summary-label">A Receber (Asaas)</span>
                                    <h3 className="summary-value">R$ 2.400,00</h3>
                                </div>
                                <div className="summary-card">
                                    <span className="summary-label">Lucro Plataforma</span>
                                    <h3 className="summary-value text-green">+ R$ 1.250,00</h3>
                                </div>
                            </div>

                            <section className="admin-glass-panel full-width">
                                <div className="panel-header">
                                    <h2>Transações Recentes</h2>
                                    <button className="btn-neon-sm">Exportar</button>
                                </div>
                                <div className="custom-scroll-area">
                                    <div className="list-header">
                                        <span>Data</span>
                                        <span>Cliente</span>
                                        <span>Descrição</span>
                                        <span>Valor</span>
                                        <span>Status</span>
                                    </div>
                                    <div className="list-item transaction-row">
                                        <span>12/02/2026</span>
                                        <span>João Silva</span>
                                        <span>Reserva Quadra 1</span>
                                        <span className="text-green">R$ 200,00</span>
                                        <span className="status-badge paid">Pago</span>
                                    </div>
                                </div>
                            </section>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}