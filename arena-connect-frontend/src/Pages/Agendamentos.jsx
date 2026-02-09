import React, {useEffect, useState} from 'react';
import '../Styles/Agendamentos.css';
import Sidebar from "../Components/Sidebar.jsx";
import axios from "axios";
import ModalBooking from "../Components/ModalBooking.jsx";

const DAYS_MAP = [
    { code: 'DOM', label: 'D' },
    { code: 'SEG', label: 'S' },
    { code: 'TER', label: 'T' },
    { code: 'QUA', label: 'Q' },
    { code: 'QUI', label: 'Q' },
    { code: 'SEX', label: 'S' },
    { code: 'SAB', label: 'S' }
];

export default function Agendamentos() {
    const [quadras, setQuadras] = useState([]);
    const [agendamentos,setAgendamentos] = useState([]);
    const [loading, setLoading] = useState(true);

    const [editingBooking, setEditingBooking] = useState(null);

    const [selectedDate, setSelectedDate] = useState("");
    const [selectedQuadra, setSelectedQuadra] = useState("");

    const [config, setConfig] = useState({
        abertura: "07:30",
        fechamento: "23:30",
        diasOperacao: []
    });


    useEffect(() => {
        findAllAgendamentos();
        findCourts();
        findArenaConfig();
    }, []);

    useEffect(() => {
        findAllAgendamentos();
    }, [selectedDate, selectedQuadra]);

    const findArenaConfig = async () =>{
        try{
            const response = await axios.get('http://localhost:8080/api/arena/config',{
                withCredentials: true
            });

            setConfig({
                abertura: response.data.abertura ? response.data.abertura.slice(0,5) : "07:00",
                fechamento: response.data.fechamento ? response.data.fechamento.slice(0,5) : "23:00",
                diasOperacao: response.data.diasOperacao || []
            });
        }catch (error) {
            console.error("Erro ao carregar configurações da arena:", error);
        }
    };

    const toggleDay = (dayCode) => {
        setConfig(prev => {
            const isActive = prev.diasOperacao.includes(dayCode);
            let newDays;

            if(isActive){
                newDays = prev.diasOperacao.filter(d => d !== dayCode)
            } else {

                newDays = [...prev.diasOperacao, dayCode];
            }
            return { ...prev, diasOperacao: newDays };
        });
    };

    const handleUpdateConfig = async () => {
        try {
            await axios.put('http://localhost:8080/api/arena/config', config, {
                withCredentials: true
            });
            alert("Status atualizado com sucesso!");
        } catch (error) {
            console.error("Erro ao atualizar:", error);
            alert("Erro ao salvar configurações.");
        }
    };

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

    const handleStatusUpdate = async (idAgendamento,newStatus) => {
        const actionVerb = newStatus === 'CANCELADO' ? 'cancelar' : 'finalizar';

        if (!confirm(`Tem certeza que deseja ${actionVerb} este agendamento?`)) return;

        try {
            await axios.put(`http://localhost:8080/api/agendamentos/${idAgendamento}/status`, {
                status: newStatus
            }, {
                withCredentials: true
            });
            alert(`Agendamento ${newStatus === 'CANCELADO' ? 'cancelado' : 'finalizado'} com sucesso!`);
            findAllAgendamentos();

        } catch (error) {
            console.error("Erro ao cancelar:", error);
            const msg = error.response?.data?.error || error.response?.data || "Erro ao cancelar.";
            alert(typeof msg === 'object' ? JSON.stringify(msg) : msg);
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
                                    <p className="glass-subtitle">Controle total das reservas</p>
                                </div>

                                <div className="admin-filters glass-inner">
                                    <div className="filter-box">
                                        <label>Calendário</label>
                                        <input type="date" className="input-glass" value={selectedDate} onChange={(e)=>setSelectedDate(e.target.value)}/>
                                    </div>
                                    <div className="filter-box">
                                        <label>Selecionar Quadra</label>
                                        <select className="input-glass" value={selectedQuadra} onChange={(e)=>setSelectedQuadra(e.target.value)}>
                                            <option value="">Todas as Quadras</option>
                                            {quadras.map((quadra) => (
                                                <option key={quadra.id} value={quadra.id}>
                                                    {quadra.nome}
                                                </option>
                                            ))}
                                        </select>
                                    </div>
                                    {(selectedDate || selectedQuadra) && (
                                        <button className="clear-filter-btn" onClick={() => { setSelectedDate(""); setSelectedQuadra(""); }} title="Limpar filtros">
                                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                <line x1="18" y1="6" x2="6" y2="18"></line>
                                                <line x1="6" y1="6" x2="18" y2="18"></line>
                                            </svg>
                                        </button>
                                    )}
                                </div>
                            </header>

                            {/* card for bookings */}
                            <div className="bookings-scroll-area">
                                {agendamentos.length > 0 ? (
                                    agendamentos.map((agendamento) => (
                                        <div key={agendamento.id_agendamento} className="booking-card-mini">

                                            {/* 1. Coluna de Tempo + Data */}
                                            <div className="card-time-column">
                                                <span className="mini-time">
                                                {new Date(agendamento.data_inicio).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                                </span>
                                                {/* Adicionando a DATA aqui */}
                                                <span className="mini-date">
                                                {new Date(agendamento.data_inicio).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })}
                                                </span>
                                            </div>

                                            <div className="card-info-column">
                                                <span className="mini-client">
                                                   {agendamento.nomeCliente || "Cliente"}
                                                </span>
                                                <span className="mini-details">
                                                    {agendamento.quadraNome}
                                                </span>
                                            </div>

                                            <div className="card-status-column">
                                                <span className={`mini-status ${agendamento.status ? agendamento.status.toLowerCase() : ''}`}>
                                                        {agendamento.status}
                                                </span>
                                            </div>


                                            <div className="card-actions-column">
                                                {(agendamento.status !== 'CANCELADO' && agendamento.status !== 'FINALIZADO') && (
                                                    <button className="mini-action-btn edit" title="Editar Agendamento" onClick={() => setEditingBooking(agendamento)}>
                                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                                            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                                                            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4L18.5 2.5z"></path>
                                                        </svg>
                                                    </button>
                                                )}

                                                {(agendamento.status !== 'CANCELADO' && agendamento.status !== 'FINALIZADO' && agendamento.status !== 'PENDENTE') && (
                                                    <button
                                                        className="mini-action-btn check"
                                                        title="Finalizar Agendamento"
                                                        onClick={() => handleStatusUpdate(agendamento.id_agendamento, 'FINALIZADO')}
                                                    >
                                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                                            <polyline points="20 6 9 17 4 12"></polyline>
                                                        </svg>
                                                    </button>
                                                )}

                                                {(agendamento.status !== 'CANCELADO' && agendamento.status !== 'FINALIZADO' && agendamento.status !== 'CONFIRMADO') && (
                                                    <button
                                                        className="mini-action-btn cancel"
                                                        title="Cancelar Agendamento"
                                                        onClick={() => handleStatusUpdate(agendamento.id_agendamento, 'CANCELADO')}
                                                    >
                                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                                            <line x1="18" y1="6" x2="6" y2="18"></line>
                                                            <line x1="6" y1="6" x2="18" y2="18"></line>
                                                        </svg>
                                                    </button>
                                                )}
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
                                <h3 className="sidebar-title">Configurar Arena ⚙️</h3>

                                <div className="config-card">
                                    <h5>Horários de Hoje</h5>
                                    <div className="time-inputs-row">
                                        <div className="input-unit">
                                            <span>Abertura</span>
                                            <input type="time" className="input-glass" value={config.abertura}
                                                   onChange={(e) => setConfig({...config, abertura: e.target.value})}
                                            />
                                        </div>
                                        <div className="input-unit">
                                            <span>Fechamento</span>
                                            <input type="time" className="input-glass" value={config.fechamento}
                                                   onChange={(e) => setConfig({...config, fechamento: e.target.value})}
                                            />
                                        </div>
                                    </div>
                                </div>

                                <div className="config-card">
                                    <h5>Dias de Operação</h5>
                                    <div className="week-grid">
                                        {DAYS_MAP.map((day) => {
                                            const isActive = config.diasOperacao.includes(day.code);
                                            return (
                                                <button
                                                    key={day.code}
                                                    className={`day-btn ${isActive ? 'active' : ''}`}
                                                    onClick={() => toggleDay(day.code)}
                                                >
                                                    {day.label}
                                                </button>
                                            );
                                        })}
                                    </div>
                                </div>

                                <button className="btn-save-glass" onClick={handleUpdateConfig}>
                                    Atualizar Configurações
                                </button>
                            </div>

                            <div className="sidebar-section" style={{ marginTop: '2rem' }}>
                                <h3 className="sidebar-title">Regras Automáticas </h3>

                                <div className="config-card" style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>

                                    <div style={{ display: 'flex', gap: '12px', alignItems: 'flex-start' }}>
                                        <div style={{
                                            minWidth: '24px', height: '24px', borderRadius: '50%',
                                            background: 'rgba(74, 222, 128, 0.15)', color: '#4ade80',
                                            display: 'flex', alignItems: 'center', justifyContent: 'center'
                                        }}>
                                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
                                        </div>
                                        <div>
                    <span style={{ color: '#fff', fontSize: '0.85rem', fontWeight: 'bold', display: 'block', marginBottom: '4px' }}>
                        Finalização Automática
                    </span>
                                            <p style={{ color: 'rgba(255,255,255,0.6)', fontSize: '0.75rem', margin: 0, lineHeight: '1.4' }}>
                                                Jogos <b>Confirmados</b> mudam para Finalizado assim que o horário termina.
                                            </p>
                                        </div>
                                    </div>

                                    <div style={{ width: '100%', height: '1px', background: 'rgba(255,255,255,0.1)' }}></div>

                                    {/* Regra 2: Cancelamento */}
                                    <div style={{ display: 'flex', gap: '12px', alignItems: 'flex-start' }}>
                                        <div style={{
                                            minWidth: '24px', height: '24px', borderRadius: '50%',
                                            background: 'rgba(250,21,21,0.15)', color: '#fa1515',
                                            display: 'flex', alignItems: 'center', justifyContent: 'center'
                                        }}>
                                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
                                        </div>
                                        <div>
                    <span style={{ color: '#fff', fontSize: '0.85rem', fontWeight: 'bold', display: 'block', marginBottom: '4px' }}>
                        Cancelamento automatico
                    </span>
                                            <p style={{ color: 'rgba(255,255,255,0.6)', fontSize: '0.75rem', margin: 0, lineHeight: '1.4' }}>
                                                Reservas <b>Pendentes</b> são canceladas se não pagas até 30min antes do jogo.
                                            </p>
                                        </div>
                                    </div>

                                </div>
                            </div>

                        </aside>
                    </div>
                </div>
                {editingBooking && (
                    <ModalBooking
                        bookingToEdit={editingBooking}
                        onClose={() => setEditingBooking(null)}
                        onSuccess={findAllAgendamentos}
                    />
                )}
            </div>
    );
}