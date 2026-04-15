import ClientHeader from "../Components/clientHeader.jsx";
import ClientNav from "../Components/clientNav.jsx"
import {useEffect, useState, useCallback} from "react";
import axios from "axios";
import "../Styles/ClientsAgendamentos.css"
import ModalBooking from "../Components/ModalBooking.jsx";

export default function ClientAgendamentos(){
    const [filterType, setFilterType] = useState('upcoming');
    const [bookings, setBookings] = useState([]);
    const [loading, setLoading] = useState(true);
    const [editingBooking, setEditingBooking] = useState(null);
    // Estados para a Tooltip flutuante de Mensalidades
    const [hoveredMensalidade, setHoveredMensalidade] = useState(null);
    const [mousePos, setMousePos] = useState({ x: 0, y: 0 });

    // Estados adicionados para mensalidade
    const [mensalidades, setMensalidades] = useState([]);

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

    // Função para buscar mensalidades
    const fetchMensalidades = async () => {
        try {
            const response = await axios.get('http://localhost:8080/api/contratos-mensalistas/meus-contratos', {
                withCredentials: true,
            });
            setMensalidades(response.data);
        } catch (error) {
            console.error("Erro ao buscar mensalidades:", error);
        }
    }

    // Os dados são carregados no onMount via refreshData().
    // A mudança de aba (filterType) apenas alterna a visualização, sem refetch.

    const handleCancelBooking = async (idAgendamento, idArena) => {
        if (!confirm("Tem certeza que deseja cancelar este agendamento?")) return;
        try{
            const response = await axios.put(`http://localhost:8080/api/agendamentos/${idAgendamento}/status`,{
                status: "CANCELADO"
            },{
                withCredentials: true,
                headers: {
                    'Content-Type': 'application/json',
                    'X-Tenant-ID': idArena
                }
            });

            alert("Agendamento cancelado com sucesso");
            await fetchBookings();

        } catch (error) {
            console.error("Erro ao cancelar:", error);
            alert(error.response?.data?.error || "Erro ao cancelar agendamento.");
        }
    }
    const handleCancelMensalidade = async (idContrato, status) => {
        if (status !== 'PENDENTE') {
            alert("Apenas contratos pendentes podem ser cancelados.");
            return;
        }

        if (!confirm("Tem certeza que deseja cancelar esta mensalidade? Todos os jogos vinculados no mês serão cancelados irreversivelmente.")) return;

        try {
            await axios.put(`http://localhost:8080/api/contratos-mensalistas/cancelar/${idContrato}`, {}, {
                withCredentials: true
            });

            alert("Mensalidade cancelada com sucesso!");
            refreshData(); // Recarrega a tela para sumir o botão

        } catch (error) {
            console.error("Erro ao cancelar mensalidade:", error);
            alert(error.response?.data?.error || "Erro ao cancelar a mensalidade.");
        }
    };

    const formatarDiaSemana = (numero) => {
        const dias = ["Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"];
        return dias[numero - 1] || "Dia não definido";
    };

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

    const refreshData = useCallback(async () => {
        setLoading(true);
        // O Promise.all dispara as duas buscas em paralelo no frontend
        await Promise.all([
            fetchBookings(),
            fetchMensalidades()
        ]);
        setLoading(false);
    }, []);

    // Carrega tudo apenas UMA VEZ quando entra na tela (onMount)
    useEffect(() => {
        refreshData();
    }, [refreshData]);

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
                            <button
                                className={`category-pill ${filterType === 'mensalidades' ? 'active' : ''}`}
                                onClick={() => setFilterType('mensalidades')}>
                                Minhas Mensalidades
                            </button>
                        </div>

                        <div className="arenas-list">
                            {loading ? (
                                <p style={{textAlign: 'center', color: '#888'}}>Carregando...</p>
                            ) : filterType === 'mensalidades' ? (
                                // --- RENDERIZAÇÃO DAS MENSALIDADES ---
                                // --- RENDERIZAÇÃO DAS MENSALIDADES ---
                                mensalidades.length > 0 ? (
                                    mensalidades.map((mensal, mIndex) => (
                                        <div
                                            key={`mensal-${mensal.id}-${mIndex}`}
                                            className="mensalidade-card glass-panel"
                                            // Eventos para o mini-modal (tooltip)
                                            onMouseEnter={(e) => {
                                                setHoveredMensalidade(mensal);
                                                setMousePos({ x: e.clientX, y: e.clientY });
                                            }}
                                            onMouseMove={(e) => setMousePos({ x: e.clientX, y: e.clientY })}
                                            onMouseLeave={() => setHoveredMensalidade(null)}
                                            style={{ position: 'relative', zIndex: 1 }} // Garante que a z-index não atrapalha
                                        >
                                            <div className="card-content-base">
                                                <div className="card-main-info" style={{ display: 'flex', flexDirection: 'column', gap: '5px' }}>
                                                    <span className="arena-tag" style={{ alignSelf: 'flex-start' }}>Mensalista</span>
                                                    <h3 style={{ color: '#fff', margin: '0' }}>Contrato #{mensal.id}</h3>

                                                    {/* 🟢 NOVA LINHA: Dia da Semana em Destaque */}
                                                    <div style={{
                                                        background: 'rgba(0, 255, 127, 0.1)',
                                                        borderLeft: '3px solid #00ff7f',
                                                        padding: '4px 8px',
                                                        marginTop: '5px',
                                                        borderRadius: '0 4px 4px 0',
                                                        display: 'inline-block',
                                                        alignSelf: 'flex-start'
                                                    }}>
                                                        <span style={{ color: '#00ff7f', fontWeight: 'bold', fontSize: '0.85rem' }}>
                                                            Toda {formatarDiaSemana(mensal.diaSemana)}
                                                        </span>
                                                    </div>

                                                    <p className="booking-time" style={{ color: '#ccc', margin: '5px 0 0 0', fontSize: '0.9rem' }}>
                                                        Horário: {mensal.horaInicio} às {mensal.horaFim}
                                                    </p>
                                                    <p className="price-tag" style={{ color: '#00ff7f', margin: '5px 0 0 0', fontWeight: 'bold' }}>
                                                        R$ {mensal.valorPactuado ? mensal.valorPactuado.toFixed(2) : '0.00'} <span style={{fontSize: '0.7rem', color: '#aaa', fontWeight: 'normal'}}>/ mês</span>
                                                    </p>
                                                </div>
                                                <div className="card-status-info" style={{ marginTop: '10px' }}>
                                                    <span className={`status-badge ${getStatusClass(mensal.status)}`}>
                                                        {mensal.status}
                                                    </span>
                                                </div>

                                                {/* Botões protegidos: stopPropagation impede que o clique dispare eventos não desejados */}
                                                {mensal.status === 'PENDENTE' && (
                                                    <div
                                                        style={{ display: 'flex', gap: '10px', marginTop: '15px', position: 'relative', zIndex: 10 }}
                                                        onMouseEnter={() => setHoveredMensalidade(null)} // Esconde tooltip quando passa nos botões
                                                    >
                                                        {mensal.asaasInvoiceUrl && (
                                                            <button
                                                                className="btn-pay-neon"
                                                                style={{ flex: 1, margin: 0, padding: '10px' }}
                                                                onClick={(e) => { e.stopPropagation(); window.open(mensal.asaasInvoiceUrl, '_blank'); }}
                                                            >
                                                                Pagar
                                                            </button>
                                                        )}
                                                        <button
                                                            className="btn-cancel"
                                                            style={{
                                                                flex: 1, padding: '10px', background: 'transparent',
                                                                border: '1px solid #fa1515', color: '#fa1515',
                                                                borderRadius: '8px', cursor: 'pointer', fontWeight: 'bold',
                                                                transition: 'all 0.3s'
                                                            }}
                                                            onMouseOver={(e) => e.target.style.background = 'rgba(250, 21, 21, 0.1)'}
                                                            onMouseOut={(e) => e.target.style.background = 'transparent'}
                                                            onClick={(e) => { e.stopPropagation(); handleCancelMensalidade(mensal.id, mensal.status); }}
                                                        >
                                                            Cancelar
                                                        </button>
                                                    </div>
                                                )}
                                            </div>
                                            {/* (Removemos a div "mensalidade-hover-overlay" daqui. Ela vai ser renderizada fora) */}
                                        </div>
                                    ))
                                ) : (
                                    <div style={{textAlign: 'center', color: '#888', padding: '20px', gridColumn: '1 / -1'}}>
                                        <p>Você ainda não possui contratos de mensalidade.</p>
                                    </div>
                                )
                            ) : (
                                // --- RENDERIZAÇÃO DOS AGENDAMENTOS ORIGINAIS ---
                                getAgendamentosFiltrados().length > 0 ? (
                                    // 🟢 CORREÇÃO DA KEY AQUI
                                    getAgendamentosFiltrados().map((booking, bIndex) => (
                                        <div key={`booking-${booking.id_arena}-${booking.id_agendamento}-${bIndex}`} className="arena-card glass-panel booking-card">
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
                                                        <button className="btn-cancel" onClick={() => handleCancelBooking(booking.id_agendamento, booking.id_arena)}
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

            {/* O Modal Normal (já estava cá) */}
            {editingBooking && (
                <ModalBooking
                    bookingToEdit={editingBooking}
                    onClose={() => setEditingBooking(null)}
                    onSuccess={refreshData}
                />
            )}

            {/* 🟢 NOVO: O Mini Modal Flutuante (Tooltip) */}
            {hoveredMensalidade && (
                <div
                    style={{
                        position: 'fixed',
                        top: mousePos.y + 15, // 15px abaixo do cursor
                        left: mousePos.x + 15, // 15px à direita do cursor
                        background: 'rgba(20, 20, 20, 0.95)',
                        border: '1px solid #4ade80',
                        borderRadius: '8px',
                        padding: '12px',
                        boxShadow: '0 4px 15px rgba(0,0,0,0.5)',
                        pointerEvents: 'none', // IMPORTANTE: O rato ignora a tooltip para não bloquear os cliques
                        zIndex: 9999, // Fica por cima de tudo
                        minWidth: '220px'
                    }}
                >
                    <h4 style={{ color: '#4ade80', margin: '0 0 10px 0', fontSize: '0.85rem' }}>Jogos Vinculados (Este Mês)</h4>
                    <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '6px' }}>
                        {bookings
                            .filter(b => b.id_quadra === hoveredMensalidade.idQuadra && b.status?.includes('MENSALISTA'))
                            .slice(0, 4)
                            .map((jogo, jIndex) => {
                                const dataJogo = new Date(jogo.data_inicio);
                                return (
                                    <li key={`tooltip-jogo-${jIndex}`} style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid rgba(255,255,255,0.1)', paddingBottom: '4px' }}>
                                        <span style={{ color: '#fff', fontSize: '0.8rem' }}>
                                            {dataJogo.toLocaleDateString('pt-BR')}
                                            <span style={{ color: '#aaa', fontSize: '0.7rem', marginLeft: '5px' }}>({formatarDiaSemana(dataJogo.getDay() || 7)})</span>
                                        </span>
                                        <span style={{ color: jogo.status.includes('CONFIRMADO') ? '#00ff7f' : 'orange', fontSize: '0.75rem', fontWeight: 'bold' }}>
                                            {jogo.status.replace('MENSALISTA_', '')}
                                        </span>
                                    </li>
                                );
                            })}
                        {bookings.filter(b => b.id_quadra === hoveredMensalidade.idQuadra && b.status?.includes('MENSALISTA')).length === 0 && (
                            <p style={{ fontSize: '0.75rem', color: '#888', margin: 0, fontStyle: 'italic' }}>Nenhum jogo confirmado ainda.</p>
                        )}
                    </ul>
                </div>
            )}
        </div>
    );
}