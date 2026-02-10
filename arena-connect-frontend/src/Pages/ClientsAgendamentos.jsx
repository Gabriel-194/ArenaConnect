import ClientHeader from "../Components/clientHeader.jsx";
import ClientNav from "../Components/clientNav.jsx"
import {useEffect, useState} from "react";
import axios from "axios";
import "../Styles/ClientsAgendamentos.css"
import ModalBooking from "../Components/ModalBooking.jsx";

export default function ClientAgendamentos(){
    const [filterType, setFilterType] = useState('upcoming');
    const [bookings, setBookings] = useState([]);
    const [loading, setLoading] = useState(true);
    const [editingBooking, setEditingBooking] = useState(null);

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

    const handleCancelBooking = async (idAgendamento, schemaName) => {
        if (!confirm("Tem certeza que deseja cancelar este agendamento?")) return;
        try{
            const response = await axios.put(`http://localhost:8080/api/agendamentos/${idAgendamento}/status`,{
                status: "CANCELADO"
            },{
                withCredentials: true,
                headers: {
                    'X-TENANT-ID': schemaName
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

        return bookings.filter(booking => {
            if (!booking.data_inicio) return false;

            const bookingDate = new Date(booking.data_inicio);
            const bookingDateOnly = new Date(bookingDate);
            bookingDateOnly.setHours(0, 0, 0, 0);

            if(filterType === 'upcoming'){
                return bookingDateOnly >= now && booking.status !== 'CANCELADO' && booking.status !== 'FINALIZADO';
            } else {
                return bookingDateOnly < now || booking.status === 'CANCELADO' || booking.status === 'FINALIZADO';
            }
        }).sort((a,b) => {
            const dateA = new Date(a.data_inicio);
            const dateB = new Date(b.data_inicio);
            return filterType === 'upcoming' ? dateA - dateB : dateB - dateA;
        });
    };

    const getStatusClass = (status) => {
        switch (status?.toUpperCase()) {
            case 'CONFIRMADO': return 'status-confirmed';
            case 'PENDENTE': return 'status-pending';
            case 'CANCELADO': return 'status-canceled';
            case 'FINALIZADO': return 'status-completed';
            default: return 'status-completed';
        }
    };

    const formatDate = (dateString) => {
        if(!dateString) return '--/--';
        return new Date(dateString).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' });
    };

    const formatTime = (dateString) => {
        if(!dateString) return '--:--';
        return new Date(dateString).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
    };

    return (
        <div className="client-body">
            <div className="client-background-fixed">
                <div className="client-blob client-blob-1"></div>
                <div className="client-blob client-blob-2"></div>
            </div>

            <ClientHeader />

            <main className="client-content">

                <div className="layout-grid">

                    <aside className="rules-sidebar glass-panel">
                        <h3 style={{ fontSize: '1rem', color: '#fff', margin: '0 0 15px 0', borderBottom: '1px solid rgba(255,255,255,0.1)', paddingBottom: '10px' }}>
                            Regras Automáticas
                        </h3>

                        <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                            <div style={{ display: 'flex', gap: '10px', alignItems: 'flex-start' }}>
                                <div style={{
                                    minWidth: '22px', height: '22px', borderRadius: '50%',
                                    background: 'rgba(74, 222, 128, 0.15)', color: '#4ade80',
                                    display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0
                                }}>
                                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
                                </div>
                                <div>
                                    <span style={{ color: '#fff', fontSize: '0.8rem', fontWeight: 'bold', display: 'block', marginBottom: '2px' }}>
                                        Finalização
                                    </span>
                                    <p style={{ color: 'rgba(255,255,255,0.6)', fontSize: '0.7rem', margin: 0, lineHeight: '1.3' }}>
                                        Jogos confirmados encerram ao fim do horário.
                                    </p>
                                </div>
                            </div>

                            <div style={{ width: '100%', height: '1px', background: 'rgba(255,255,255,0.1)' }}></div>

                            <div style={{ display: 'flex', gap: '10px', alignItems: 'flex-start' }}>
                                <div style={{
                                    minWidth: '22px', height: '22px', borderRadius: '50%',
                                    background: 'rgba(250,21,21,0.15)', color: '#fa1515',
                                    display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0
                                }}>
                                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
                                </div>
                                <div>
                                    <span style={{ color: '#fff', fontSize: '0.8rem', fontWeight: 'bold', display: 'block', marginBottom: '2px' }}>
                                        Cancelamento
                                    </span>
                                    <p style={{ color: 'rgba(255,255,255,0.6)', fontSize: '0.7rem', margin: 0, lineHeight: '1.3' }}>
                                        Pendentes cancelados 30min antes se não pagos.
                                    </p>
                                </div>
                            </div>
                        </div>
                    </aside>

                    <section className="main-feed">
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
                                        <div key={`${booking.schemaName}-${booking.id_agendamento}`} className="arena-card glass-panel booking-card">
                                            <div className="liquid-glow"></div>

                                            <div className="booking-header-row">
                                                <div className="booking-time-group">
                                                    <div className="date-box">
                                                        <span className="date-label">Data</span>
                                                        <span className="date-value">{formatDate(booking.data_inicio)}</span>
                                                    </div>
                                                    <div className="time-info">
                                                        <h4>{formatTime(booking.data_inicio)}</h4>
                                                        <span>até {formatTime(booking.data_fim)}</span>
                                                    </div>
                                                </div>

                                                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '5px' }}>
                                                    <span className={`status-badge ${getStatusClass(booking.status)}`}>
                                                        {booking.status || 'Agendado'}
                                                    </span>
                                                    {(booking.status === 'PENDENTE') && booking.asaasInvoiceUrl && (
                                                        <a
                                                            href={booking.asaasInvoiceUrl}
                                                            target="_blank"
                                                            rel="noopener noreferrer"
                                                            className="btn-pay-liquid"
                                                        >
                                                            <span>Pagar </span>
                                                        </a>
                                                    )}
                                                </div>
                                            </div>

                                            <div className="booking-body">
                                                <h4 style={{ marginBottom: '4px', color: '#fff' }}>
                                                    {booking.arenaName || 'Arena Desconhecida'}
                                                </h4>
                                                <p style={{ margin: 0, fontSize: '0.9rem', color: '#ccc' }}>
                                                    {booking.quadraNome || `Quadra ${booking.id_quadra}`}
                                                </p>
                                                <p className="booking-address" style={{ fontSize: '0.8rem', color: 'rgba(255,255,255,0.5)', marginTop: '4px' }}>
                                                    {booking.enderecoArena || 'Endereço não disponível'}
                                                </p>
                                            </div>

                                            <div className="booking-footer">
                                                <span className="price-tag">
                                                    Valor: <strong>R$ {booking.valor ? booking.valor.toFixed(2) : '0.00'}</strong>
                                                </span>

                                                <div className="card-actions">
                                                    {(booking.status !== 'CANCELADO' && booking.status !== 'FINALIZADO' && filterType === 'upcoming') && (
                                                        <button
                                                            className="mini-action-btn edit"
                                                            title="Editar Agendamento"
                                                            onClick={() => setEditingBooking(booking)}
                                                        >
                                                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4L18.5 2.5z"></path></svg>
                                                        </button>
                                                    )}

                                                    {(booking.status !== 'CANCELADO' && booking.status !== 'CONFIRMADO'&& booking.status !== 'FINALIZADO' && filterType === 'upcoming') && (
                                                        <button className="btn-cancel" onClick={() => handleCancelBooking(booking.id_agendamento, booking.schemaName)}
                                                        >Cancelar</button>
                                                    )}
                                                </div>
                                            </div>
                                        </div>
                                    ))
                                ) : (
                                    <div style={{textAlign: 'center', color: '#888', padding: '20px', gridColumn: '1 / -1'}}>
                                        <p>Nenhum agendamento encontrado.</p>
                                    </div>
                                )
                            )}
                        </div>
                    </section>

                </div>

                <div style={{ height: '100px' }}></div>
            </main>

            <ClientNav active="agendamentos" />

            {editingBooking && (
                <ModalBooking
                    bookingToEdit={editingBooking}
                    onClose={() => setEditingBooking(null)}
                    onSuccess={fetchBookings}
                />
            )}
        </div>
    );
}