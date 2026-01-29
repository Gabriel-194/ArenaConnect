import ClientHeader from "../Components/clientHeader.jsx";
import ClientNav from "../Components/clientNav.jsx"
import {useEffect, useState} from "react";
import axios from "axios";

import "../Styles/ClientsAgendamentos.css"

export default function ClientAgendamentos(){
    const [filterType, setFilterType] = useState('upcoming');
    const [bookings, setBookings] = useState([]);
    const [loading, setLoading] = useState(true);

    const fetchBookings = async () =>{
        try{
            const response = await axios.get('http://localhost:8080/api/agendamentos/agendamentosClients',{
                withCredentials: true,
            });
            setBookings(response.data);
        } catch (error) {
            console.error("Erro ao buscar agendamentos:", error);
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        fetchBookings()
    }, []);

    const handleCancelBooking = async (idAgendamento,schemaName) => {
        if (!confirm("Tem certeza que deseja cancelar este agendamento?")) return;
        try{
            const response = await axios.put(`http://localhost:8080/api/agendamentos/${idAgendamento}/status`,{
                status:"CANCELADO"
            },{
                withCredentials:true,
                headers :{
                    'X-TENANT-id' : schemaName
                }
            });
            alert("Agendamento cancelado com sucesso");
            await fetchBookings();
        } catch (error) {
            console.error("Erro ao cancelar:", error);
            alert(error.response?.data?.error || "Erro ao cancelar agendamento.");
        }
    }

    const getAgendamentosFiltrados = () => {
        const now = new Date();
        now.setHours(0, 0, 0, 0);

        return bookings.filter(booking =>{
            const bookingDate = new Date(booking.dataInicio);
            const bookingDateOnly = new Date(bookingDate);
            bookingDateOnly.setHours(0, 0, 0, 0);

            if(filterType === 'upcoming'){
                return bookingDate >= now && booking.status !== 'CANCELADO';
            } else {
                return bookingDate < now || booking.status === 'CANCELADO';
            }
        })
            .sort((a, b) => {
                const dateA = new Date(a.dataInicio);
                const dateB = new Date(b.dataInicio);
                return filterType === 'upcoming' ? dateA - dateB : dateB - dateA;
            });
    };

    const getStatusClass = (status) => {
        switch (status?.toUpperCase()) {
            case 'CONFIRMADO': return 'status-confirmed';
            case 'PENDENTE': return 'status-pending';
            case 'CANCELADO': return 'status-canceled';
            default: return 'status-completed';
        }
    };

    const formatDate = (dateString) => {
        if (!dateString) return "--/--";
        return new Date(dateString).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' });
    };

    const formatTime = (dateString) => {
        if (!dateString) return "--:--";
        return new Date(dateString).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
    };


    return (
        <div className="client-body">
            <div className="liquid-background-fixed">
                <div className="blob blob-1"></div>
                <div className="blob blob-2"></div>
            </div>

            <ClientHeader />

            <main className="client-content">

                {/* Filter Tabs */}
                <div className="categories-scroll" style={{ marginBottom: '20px' }}>
                    <button className={`category-pill ${filterType === 'upcoming' ? 'active' : ''}`}
                            onClick={() => setFilterType('upcoming')}>
                        Próximos Jogos
                    </button>
                    <button
                        className={`category-pill ${filterType === 'history' ? 'active' : ''}`}
                        onClick={() => setFilterType('history')}>
                        Histórico
                    </button>
                </div>

                <div className="arenas-list">
                    {loading ? (
                        <p style={{textAlign: 'center', color: '#888'}}>Carregando...</p>
                    ) : (
                        getAgendamentosFiltrados().length > 0 ? (
                            getAgendamentosFiltrados().map((booking) => (
                                <div key={`${booking.schemaName}-${booking.idAgendamentoArena}`} className="arena-card glass-panel booking-card">
                                    <div className="liquid-glow"></div>

                                    <div className="booking-header-row">
                                        <div className="booking-time-group">
                                            <div className="date-box">
                                                <span className="date-label">Data</span>
                                                <span className="date-value">{formatDate(booking.dataInicio)}</span>
                                            </div>
                                            <div className="time-info">
                                                <h4>{formatTime(booking.dataInicio)}</h4>
                                                <span>até {formatTime(booking.dataFim)}</span>
                                            </div>
                                        </div>

                                        <span className={`status-badge ${getStatusClass(booking.status)}`}>
                                            {booking.status || 'Agendado'}
                                        </span>
                                    </div>

                                    <div className="booking-body">
                                        <h4 style={{ marginBottom: '4px', color: '#fff' }}>
                                            {booking.nomeArena || 'Arena Desconhecida'}
                                        </h4>

                                        <p style={{ margin: 0, fontSize: '0.9rem', color: '#ccc' }}>
                                            {booking.nomeQuadra || `Quadra...`}
                                        </p>

                                        <p className="booking-address" style={{ fontSize: '0.8rem', color: 'rgba(255,255,255,0.5)', marginTop: '4px' }}>
                                            {booking.enderecoResumido || 'Endereço não disponível'}
                                        </p>
                                    </div>

                                    <div className="booking-footer">
                                        <span className="price-tag">
                                            Valor: <strong>R$ {booking.valor ? booking.valor.toFixed(2) : '0.00'}</strong>
                                        </span>

                                        {booking.status !== 'CANCELADO' && filterType === 'upcoming' && (
                                            <button className="btn-cancel" onClick={() => handleCancelBooking(booking.idAgendamentoArena, booking.schemaName)}
                                            >Cancelar</button>
                                        )}
                                    </div>
                                </div>
                            ))
                        ) : (
                            <div style={{textAlign: 'center', color: '#888', padding: '20px'}}>
                                <p>Nenhum agendamento encontrado.</p>
                            </div>
                        )
                    )}
                </div>


                <div style={{ height: '100px' }}></div>
            </main>

            <ClientNav active="agendamentos" />
        </div>
    );
}