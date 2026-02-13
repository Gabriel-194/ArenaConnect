import React, { useState } from 'react';
import '../Styles/SuperAdmin.css';

export default function SuperAdmin() {
    const [activeTab, setActiveTab] = useState('dashboard');

    return (
        <div className="superadmin-body">
            <div className="liquid-background-fixed">
                <div className="neon-blob blob-1"></div>
                <div className="neon-blob blob-2"></div>
                <div className="neon-blob blob-3"></div>
            </div>

            <div className="superadmin-container">

                {/* HEADER */}
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

                    {/* ABA DASHBOARD */}
                    {activeTab === 'dashboard' && (
                        <div className="admin-panels-grid">

                            {/* Arenas */}
                            <section className="glass-panel">
                                <div className="panel-header">
                                    <h2>Arenas Registradas</h2>
                                    <span className="counter-badge">1</span>
                                </div>
                                <div className="custom-scroll-area">
                                    <div className="list-item">
                                        <div className="col-status"><span className="status-indicator online"></span></div>
                                        <div className="col-info-main">
                                            <h3>Arena Exemplo</h3>
                                            <span className="sub-text">CNPJ: 00.000.000/0001-00</span>
                                        </div>
                                        <div className="col-info-secondary"><span className="info-text">📍 Curitiba - PR</span></div>
                                        <div className="col-actions">
                                            <button className="mini-action-btn edit">✏️</button>
                                            <button className="mini-action-btn delete">🗑️</button>
                                        </div>
                                    </div>
                                </div>
                            </section>

                            {/* Usuários */}
                            <section className="glass-panel">
                                <div className="panel-header">
                                    <h2>Usuários</h2>
                                    <span className="counter-badge">1</span>
                                </div>
                                <div className="custom-scroll-area">
                                    <div className="list-item">
                                        <div className="col-avatar"><div className="user-avatar-mini">U</div></div>
                                        <div className="col-info-main">
                                            <h3>Usuário Teste</h3>
                                            <span className="role-tag client">CLIENTE</span>
                                        </div>
                                        <div className="col-info-secondary"><span className="sub-text">user@email.com</span></div>
                                        <div className="col-actions">
                                            <button className="mini-action-btn edit">✏️</button>
                                            <button className="mini-action-btn delete">🗑️</button>
                                        </div>
                                    </div>
                                </div>
                            </section>
                        </div>
                    )}

                    {/* ABA FINANCEIRO */}
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

                            <section className="glass-panel full-width">
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