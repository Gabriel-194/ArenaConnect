import React, {useEffect,useState} from 'react';
import Sidebar from '../Components/Sidebar';
import '../Styles/Dashboard.css';
import {useRef} from "react";
import axios from "axios";

import {
    BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell
} from 'recharts';

export default function Dashboard() {
    const carouselRef = useRef(null);

    const [stats, setStats] = useState({
        confirmados: 0,
        pendentes: 0,
        finalizados: 0,
        cancelados: 0
    });

    const dadosFaturamento = [
        { mes: 'Jan', valor: 3200 },
        { mes: 'Fev', valor: 4100 },
        { mes: 'Mar', valor: 2800 },
        { mes: 'Abr', valor: 5600 },
        { mes: 'Mai', valor: 4900 },
        { mes: 'Jun', valor: 6200 },
        { mes: 'Jul', valor: 7100 },
        { mes: 'Ago', valor: 6800 },
        { mes: 'Set', valor: 5200 },
        { mes: 'Out', valor: 4300 },
        { mes: 'Nov', valor: 3900 },
        { mes: 'Dez', valor: 8500 },
    ];

    const CustomTooltip = ({ active, payload, label }) => {
        if (active && payload && payload.length) {
            return (
                <div className="dashboard-glass-panel" style={{ padding: '10px 15px', border: '1px solid rgba(0, 255, 127, 0.4)' }}>
                    <p style={{ margin: 0, color: '#aaa', fontSize: '0.85rem' }}>{label} / 2026</p>
                    <p style={{ margin: '5px 0 0 0', color: '#00ff7f', fontWeight: 'bold', fontSize: '1.2rem' }}>
                        R$ {payload[0].value.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
                    </p>
                </div>
            );
        }
        return null;
    };

    useEffect(() => {
        const fecthStatusAgendamentos = async () => {
            try {
                const response = await axios.get('http://localhost:8080/api/agendamentos/stats', {
                    withCredentials: true
                });
                const data = response.data;

                if (Array.isArray(data)) {
                    const counts = data.reduce((acc, curr) => {
                        const s = curr.status ? curr.status.toUpperCase() : '';
                        if (s === 'CONFIRMADO') acc.confirmados++;
                        else if (s === 'PENDENTE') acc.pendentes++;
                        else if (s === 'FINALIZADO') acc.finalizados++;
                        else if (s === 'CANCELADO') acc.cancelados++;
                        return acc;
                    }, { confirmados: 0, pendentes: 0, finalizados: 0, cancelados: 0 });

                    setStats(counts);
                }
            } catch (error) {
                console.error("Erro ao carregar estatísticas:", error);
            }
        };
        fecthStatusAgendamentos()
    }, []);

    const scrollLeft = () => {
        if (carouselRef.current) {
            carouselRef.current.scrollBy({ left: -250, behavior: 'smooth' });
        }
    };

    const scrollRight = () => {
        if (carouselRef.current) {
            carouselRef.current.scrollBy({ left: 250, behavior: 'smooth' });
        }
    };

    const quadrasMock = [
        { id: 1, nome: "Quadra 1 (Sintética)", jogos: 25 },
        { id: 2, nome: "Quadra 2 (Futsal)", jogos: 18 },
        { id: 3, nome: "Quadra 3 (Areia)", jogos: 9 },
        { id: 4, nome: "Quadra 4 (Society)", jogos: 12 },
    ];

    return (
        <div className="dashboard-layout">

            <Sidebar />

            <div className="liquid-background-fixed">
                <div className="neon-blob blob-1"></div>
                <div className="neon-blob blob-2"></div>
                <div className="neon-blob blob-3"></div>
            </div>

            <main className="dashboard-main-content">

                <header className="dashboard-header dashboard-glass-panel">
                    <div>
                        <h2>Visão Geral da Arena</h2>
                        <p>Acompanhe seus agendamentos, finanças e desempenho das quadras.</p>
                    </div>
                    <button className="btn-neon-outlined">Gerar Relatório</button>
                </header>

                <h3>Agendamentos:</h3>
                <section className="status-cards-row">
                    <div className="status-card dashboard-glass-panel">
                        <div className="card-icon confirmed">✓</div>
                        <div className="card-info">
                            <span className="card-label">Confirmados</span>
                            <h3 className="card-value text-green">{stats.confirmados}</h3>
                        </div>
                    </div>

                    <div className="status-card dashboard-glass-panel">
                        <div className="card-icon pending">⌛</div>
                        <div className="card-info">
                            <span className="card-label">Pendentes</span>
                            <h3 className="card-value text-yellow">{stats.pendentes}</h3>
                        </div>
                    </div>

                    <div className="status-card dashboard-glass-panel">
                        <div className="card-icon finalized">🏁</div>
                        <div className="card-info">
                            <span className="card-label">Finalizados</span>
                            <h3 className="card-value text-blue">{stats.finalizados}</h3>
                        </div>
                    </div>

                    <div className="status-card dashboard-glass-panel">
                        <div className="card-icon canceled">✕</div>
                        <div className="card-info">
                            <span className="card-label">Cancelados</span>
                            <h3 className="card-value text-red">{stats.cancelados}</h3>
                        </div>
                    </div>
                </section>

                <section className="dashboard-grid">

                    <div className="grid-left">

                        <div className="courts-carousel-container dashboard-glass-panel">
                            <h3>Desempenho das Quadras</h3>

                            <div className="carousel-wrapper">

                                <button className="carousel-btn left" onClick={scrollLeft}>
                                    &#10094;
                                </button>

                                <div className="courts-carousel" ref={carouselRef}>
                                    {quadrasMock.map((quadra) => (
                                        <div className="court-card-dashboard" key={quadra.id}>
                                            <span className="court-name">{quadra.nome}</span>
                                            <span className="court-games text-green">{quadra.jogos} jogos</span>
                                        </div>
                                    ))}
                                </div>

                                <button className="carousel-btn right" onClick={scrollRight}>
                                    &#10095;
                                </button>
                            </div>
                        </div>

                        <div className="chart-container dashboard-glass-panel">
                            <div className="chart-header">
                                <h3>Faturamento Anual</h3>
                                <select className="chart-filter">
                                    <option>2026</option>
                                </select>
                            </div>

                            <div style={{ width: '100%', height: 300 }}>
                                <ResponsiveContainer width="100%" height="100%">
                                    <BarChart data={dadosFaturamento} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>

                                        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" vertical={false} />

                                        <XAxis dataKey="mes" stroke="#666" tick={{ fill: '#aaa', fontSize: 12 }} axisLine={false} tickLine={false} />
                                        <YAxis stroke="#666" tick={{ fill: '#aaa', fontSize: 12 }} axisLine={false} tickLine={false} tickFormatter={(val) => `R$${val/1000}k`} />

                                        <Tooltip content={<CustomTooltip />} cursor={{ fill: 'rgba(255,255,255,0.02)' }} />

                                        <Bar dataKey="valor" radius={[6, 6, 0, 0]}>
                                            {
                                                dadosFaturamento.map((entry, index) => (
                                                    <Cell
                                                        key={`cell-${index}`}
                                                        fill="#008f4c"
                                                        style={{
                                                            fill: '#00ff7f',
                                                            filter: 'drop-shadow(0px 0px 8px rgba(0, 255, 127, 0.4))'
                                                        }}
                                                    />
                                                ))
                                            }
                                        </Bar>
                                    </BarChart>
                                </ResponsiveContainer>
                            </div>
                        </div>

                    </div>

                    <div className="grid-right">

                        <div className="recent-activity dashboard-glass-panel">
                            <h3>Últimas Movimentações</h3>
                            <ul className="activity-list">
                                <li>
                                    <span className="activity-time">Há 5 min</span>
                                    <span className="activity-desc">João pagou a reserva da <strong>Quadra 1</strong>.</span>
                                </li>
                                <li>
                                    <span className="activity-time">Há 20 min</span>
                                    <span className="activity-desc">Novo agendamento: <strong>Quadra 3</strong> (19:00).</span>
                                </li>
                                <li>
                                    <span className="activity-time">Há 1 hora</span>
                                    <span className="activity-desc text-red">Reserva cancelada por Marcos (Quadra 2).</span>
                                </li>
                            </ul>
                        </div>

                    </div>

                </section>
            </main>
        </div>
    );
}