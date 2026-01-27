import React, {useEffect, useState} from 'react';
import '../Styles/Agendamentos.css';
import Sidebar from "../Components/Sidebar.jsx";
import axios from "axios";

export default function Agendamentos() {
    const [quadras, setQuadras] = useState([]);
    const [agendamentos,setAgendamentos] = useState([]);
    const [loading, setLoading] = useState(true);

    const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
    const [selectedQuadra, setSelectedQuadra] = useState("");


    useEffect(() => {
        findCourts();
    },[]);

    useEffect(() => {
        findAllAgendamentos();
    }, [selectedDate, selectedQuadra]);

    const findCourts = async () => {
        try {
            const response = await axios.get('http://localhost:8080/api/quadra', {
                withCredentials: true
            });
            setQuadras(response.data);
        } catch (error) {
            console.error("Erro ao buscar quadras:", error);
        } finally {
            setLoading(false);
        }
    }

    const findAllAgendamentos = async () => {
        try {
            const response = await axios.get('http://localhost:8080/api/agendamentos/allAgendamentos', {
                params: {
                    idQuadra: selectedQuadra || null,
                    data: selectedDate || null
                },
                withCredentials: true,
            });
            setAgendamentos(response.data);
        } catch (error) {
            console.error("Erro ao buscar Agendamentos:", error);
        } finally {
            setLoading(false);
        }
    }

    return (
            <div className="admin-body">
                <Sidebar/>

                <div className="liquid-bg">
                    <div className="blob-admin b1"></div>
                    <div className="blob-admin b2"></div>
                    <div className="blob-admin b3"></div>
                </div>

                <div className="admin-glass-container">
                    <div className="admin-grid-layout">

                        <main className="bookings-main-panel glass-effect">
                            <header className="admin-header">
                                <div>
                                    <h2 className="glass-title">Painel de Agendamentos</h2>
                                    <p className="glass-subtitle">Controle total das reservas e quadras</p>
                                </div>

                                <div className="admin-filters glass-inner">
                                    <div className="filter-box">
                                        <label>Calendário</label>
                                        <input type="date" className="input-glass"/>
                                    </div>
                                    <div className="filter-box">
                                        <label>Selecionar Quadra</label>
                                        <select className="input-glass">
                                            <option value="">Todas as Unidades</option>
                                            {quadras.map((quadra) => (
                                                <option key={quadra.id} value={quadra.id}>
                                                    {quadra.nome}
                                                </option>
                                            ))}
                                        </select>
                                    </div>
                                </div>
                            </header>

                            {/* card for bookings */}
                            <div className="bookings-scroll-area">
                                {agendamentos.length > 0 ?(
                                    agendamentos.map((agendamento)=>(
                                        <div key={agendamento.id} className={"booking-card-mini"}>
                                            <div className={"card-time-column"}>
                                                <span className={"mini-time"}>
                                                    {new Date(agendamento.data_inicio).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                                </span>
                                            </div>
                                            <div className={"card-info-column"}>
                                                <span className={"mini-client"}>
                                                   {agendamento.nomeCliente || "Cliente"}
                                                </span>
                                                <span className="mini-details">
                                                    {agendamento.quadraNome}
                                                </span>
                                            </div>
                                            <div className="card-status-column">
                                                <span className={`mini-status ${agendamento.status.toLowerCase()}`}>
                                                    {agendamento.status}
                                                </span>
                                            </div>
                                            <div className="card-actions-column">
                                                {/* Botões de ação... */}
                                            </div>
                                        </div>
                                    ))
                                ) : (
                                    <p className="empty-msg">Nenhum agendamento encontrado.</p>
                                )}
                            </div>
                        </main>

                        <aside className="config-sidebar-panel glass-effect">
                            <div className="sidebar-section">
                                <h3 className="sidebar-title">Configurar Arena ️</h3>

                                <div className="config-card">
                                    <h5>Horários de Hoje</h5>
                                    <div className="time-inputs-row">
                                        <div className="input-unit">
                                            <span>Abertura</span>
                                            <input type="time" className="input-glass" defaultValue="07:30"/>
                                        </div>
                                        <div className="input-unit">
                                            <span>Fechamento</span>
                                            <input type="time" className="input-glass" defaultValue="23:30"/>
                                        </div>
                                    </div>
                                </div>

                                <div className="config-card">
                                    <h5>Dias de Operação</h5>
                                    <div className="week-grid">
                                        {['D', 'S', 'T', 'Q', 'Q', 'S', 'S'].map((dia, i) => (
                                            <label key={i} className="day-toggle">
                                                <input type="checkbox" defaultChecked/>
                                                <div className="day-box">{dia}</div>
                                            </label>
                                        ))}
                                    </div>
                                </div>

                                <button className="btn-save-glass">
                                    Atualizar Configurações
                                </button>
                            </div>

                            <div className="arena-info-footer">
                                <div className="indicator-row">
                                    <span className="dot pulse-green"></span>
                                    <span>Arena Online</span>
                                </div>
                            </div>
                        </aside>

                    </div>
                </div>
            </div>
        );
}