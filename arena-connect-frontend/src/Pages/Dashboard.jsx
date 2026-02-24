import React, {use, useEffect, useState} from 'react';
import Sidebar from '../Components/Sidebar';
import '../Styles/Dashboard.css';
import {useRef} from "react";
import axios from "axios";

import {
    BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell
} from 'recharts';

export default function Dashboard() {
    const carouselRef = useRef(null);

    const [ano,setAno]= useState(new Date().getFullYear());
    const [dadosFaturamento,setDadosFaturamento] = useState([]);

    const [quadras,setQuadras] = useState([]);

    const [movimentacoes, setMovimentacoes] = useState([]);

    const [stats, setStats] = useState({
        confirmados: 0,
        pendentes: 0,
        finalizados: 0,
        cancelados: 0
    });


    useEffect(() => {
        const fetchFaturamento = async () => {
            try {

                const response = await axios.get(`http://localhost:8080/api/agendamentos/faturamento?ano=${ano}`, {
                    withCredentials: true
                });

                setDadosFaturamento(response.data);
            } catch (error) {
                console.error("Erro ao carregar dados do faturamento:", error);
            }
        };
        fetchFaturamento();
    }, [ano]);

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

    useEffect(() => {
        const fetchEstatisticasQuadras = async () => {
            try {
                const response = await axios.get('http://localhost:8080/api/quadra/estatisticas', {
                    withCredentials: true
                });
                setQuadras(response.data);
            } catch (error) {
                console.error("Erro ao carregar estatísticas das quadras:", error);
            }
        };
        fetchEstatisticasQuadras();
    }, []);

    useEffect(() => {
        const fetchMovimentacoes = async () => {
            try {
                const response = await axios.get('http://localhost:8080/api/agendamentos/movimentacoes', {
                    withCredentials: true
                });
                setMovimentacoes(response.data);
            } catch (error) {
                console.error("Erro ao carregar movimentações:", error);
            }
        };
        fetchMovimentacoes();
    }, []);

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

                                {quadras.length > 3 && (
                                    <button className="carousel-btn left" onClick={scrollLeft}>
                                        &#10094;
                                    </button>
                                )}

                                <div className="courts-carousel" ref={carouselRef}>
                                    {quadras.map((quadra) => (
                                        <div className="court-card-dashboard" key={quadra.id}>
                                            <span className="court-name">{quadra.nome}</span>
                                            <span className="court-games text-green">{quadra.jogos} jogos</span>
                                        </div>
                                    ))}
                                    {quadras.length === 0 && (
                                        <p style={{ color: '#aaa', padding: '10px' }}>Nenhuma quadra encontrada.</p>
                                    )}
                                </div>

                                {quadras.length > 3 && (
                                    <button className="carousel-btn right" onClick={scrollRight}>
                                        &#10095;
                                    </button>
                                )}
                            </div>
                        </div>

                        <div className="chart-container dashboard-glass-panel">
                            <div className="chart-header">
                                <h3>Faturamento Anual</h3>
                                <select
                                    className="chart-filter"
                                    value={ano}
                                    onChange={(e) => setAno(e.target.value)}
                                >
                                    <option value="2026">2026</option>
                                    <option value="2027">2027</option>
                                    <option value="2027">2028</option>
                                </select>
                            </div>

                            <div style={{ width: '100%', height: 300, marginTop: '20px' }}>
                                <ResponsiveContainer width="100%" height={300}>
                                    <BarChart data={dadosFaturamento} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>

                                        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" vertical={false} />

                                        <XAxis dataKey="mes" stroke="#666" tick={{ fill: '#aaa', fontSize: 12 }} axisLine={false} tickLine={false} />
                                        <YAxis stroke="#666" width={80} tick={{ fill: '#aaa', fontSize: 12 }} axisLine={false} tickLine={false} tickFormatter={(val) => `R$${val}`} />

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
                                {movimentacoes.map((mov, index) => {
                                    const statusClass = `mov-${mov.tipoStatus.toLowerCase()}`;

                                    return (
                                        <li key={index} className={`activity-item ${statusClass}`}>
                                            <span className="activity-time">{mov.tempo}</span>
                                            <span className="activity-desc">
                                                {mov.descricao}
                                            </span>
                                        </li>
                                    );
                                })}
                                {movimentacoes.length === 0 && (
                                    <p style={{ color: '#aaa', fontSize: '0.9rem', padding: '10px 0' }}>Nenhuma atividade recente.</p>
                                )}
                            </ul>
                        </div>

                    </div>

                </section>
            </main>
        </div>
    );
}