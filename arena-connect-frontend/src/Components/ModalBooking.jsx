import React, { useState, useEffect } from 'react';
import axios from 'axios';
import styled, { keyframes } from 'styled-components';
import '../Styles/components.css';

export default function ModalBooking({ arena, bookingToEdit, onClose, onSuccess }) {
    const [quadras, setQuadras] = useState([]);
    const [loading, setLoading] = useState(true);

    const [selectedQuadra, setSelectedQuadra] = useState();
    const [availableHours, setAvailableHours] = useState([]);
    const [loadingHours, setLoadingHours] = useState(false);
    const [selectedHour, setSelectedHour] = useState(null);

    // 🟢 ESTADO PARA CONTROLAR O TIPO DE RESERVA
    const [tipoReserva, setTipoReserva] = useState('AVULSO'); // 'AVULSO' ou 'MENSAL'

    const isEditing = !!bookingToEdit;

    const getTodayLocal = () => {
        const date = new Date();
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    };

    const today = getTodayLocal();
    const [selectedDate, setSelectedDate] = useState(today);

    const getHeaders = () => {
        const headers = { 'Content-Type': 'application/json' };

        // Verifica de onde tirar o ID da Arena (seja editando ou criando)
        if (bookingToEdit) {
            headers['X-Tenant-ID'] = bookingToEdit.id_arena || bookingToEdit.idArena || bookingToEdit.tenantId;
        } else if (arena) {
            headers['X-Tenant-ID'] = arena.id || arena.idArena || arena.id_arena;
        }

        // Console log para te ajudar a debugar se o ID está indo certo
        console.log("Tenant ID enviado:", headers['X-Tenant-ID']);

        return headers;
    };

    useEffect(() => {
        const init = async () => {
            try {
                const response = await axios.get(`http://localhost:8080/api/quadra/courtAtivas`, {
                    withCredentials: true,
                    headers: getHeaders()
                });
                setQuadras(response.data);

                if (isEditing) {
                    const courtId = bookingToEdit.id_quadra;
                    const dateRaw = bookingToEdit.data_inicio;

                    const court = response.data.find(q => q.id === courtId);
                    if (court) setSelectedQuadra(court);

                    if (dateRaw) {
                        const [datePart, timePart] = dateRaw.split('T');
                        const isoDate = datePart;
                        const hourStr = timePart.substring(0, 5);

                        setSelectedDate(isoDate);
                        setSelectedHour(hourStr);
                    }
                } else {
                    setSelectedDate(today);
                }
            } catch (err) {
                console.error("Erro ao inicializar:", err);
            } finally {
                setLoading(false);
            }
        };
        init();
    }, [bookingToEdit, arena]);

    const handleBack = () => {
        setSelectedQuadra(null);
        setAvailableHours([]);
        setSelectedHour(null);
    }

    useEffect(() => {
        if (selectedQuadra && selectedDate) {
            const fetchAndFilterHours = async () => {
                setLoadingHours(true);
                setAvailableHours([]);

                try {
                    const response = await axios.get(`http://localhost:8080/api/agendamentos/disponibilidade`, {
                        params: {
                            idQuadra: selectedQuadra.id,
                            data: selectedDate
                        },
                        withCredentials: true,
                        headers: getHeaders()
                    });

                    const rawHours = response.data;
                    let filteredHours = rawHours;

                    if (selectedDate === today) {
                        const now = new Date();
                        const currentHour = now.getHours();
                        const currentMinute = now.getMinutes();

                        filteredHours = rawHours.filter(hour => {
                            const [h, m] = hour.toString().split(':').map(Number);
                            if (h > currentHour) return true;
                            if (h === currentHour && m > currentMinute) return true;
                            return false;
                        });
                    }

                    let normalizedHours = filteredHours.map(h => h.toString().substring(0, 5));

                    if (isEditing) {
                        const [origDate, origTimeRaw] = bookingToEdit.data_inicio.split('T');
                        const origTime = origTimeRaw.substring(0, 5);

                        if (selectedDate === origDate && selectedQuadra.id === bookingToEdit.id_quadra) {
                            if (!normalizedHours.includes(origTime)) {
                                normalizedHours.push(origTime);
                                normalizedHours.sort();
                            }
                        }
                    }

                    setAvailableHours(filteredHours);
                } catch (err) {
                    console.error("Erro ao buscar horários:", err);
                } finally {
                    setLoadingHours(false);
                }
            };
            fetchAndFilterHours();
        }
    }, [selectedQuadra, selectedDate, isEditing, bookingToEdit]);

    // Helper para o card do mensalista
    const formatarDiaDaSemana = (jsDay) => {
        const dias = ["Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado"];
        return dias[jsDay] || "";
    };

    const handleConfirm = async () => {
        if(!selectedHour) {
            alert("Por favor, selecione um horário.");
            return;
        }

        const hourString = String(selectedHour);
        const timeFormatted = hourString.length === 5 ? hourString + ':00' : hourString;
        const dataInicioISO = `${selectedDate}T${timeFormatted}`;

        const [year, month, day] = selectedDate.split('-');
        const dataFormatada = `${day}/${month}/${year}`;

        const actionText = isEditing ? "Confirmar novo horário" : "Confirmar reserva";
        if(!window.confirm(`${actionText} para ${dataFormatada} às ${hourString}?`)) return;

        try {
            if (isEditing) {
                await axios.put(
                    `http://localhost:8080/api/agendamentos/${bookingToEdit.id_agendamento}/reagendar`,
                    { data_inicio: dataInicioISO },
                    { withCredentials: true, headers: getHeaders() }
                );
                alert("Horário atualizado com sucesso!");
            } else {

                // 🟢 LÓGICA DIVIDIDA ENTRE AVULSO E MENSALISTA
                if (tipoReserva === 'AVULSO') {
                    const payload = {
                        id_quadra: selectedQuadra.id,
                        data_inicio: dataInicioISO,
                        valor: selectedQuadra.valor_hora,
                        status: "PENDENTE"
                    };

                    await axios.post(
                        'http://localhost:8080/api/agendamentos/reservar',
                        payload,
                        { withCredentials: true, headers: getHeaders() }
                    );
                    alert("Reserva avulsa criada! Acesse 'Meus Agendamentos' para pagar.");

                }else if (tipoReserva === 'MENSAL') {
                    // Calcula horaFim (+1 hora)
                    let [h, m] = hourString.split(':').map(Number);
                    let hFim = (h + 1).toString().padStart(2, '0');
                    let mFim = m.toString().padStart(2, '0');
                    let horaFimStr = `${hFim}:${mFim}:00`;

                    // Calcula diaSemanaBackend (1=Segunda, 7=Domingo)
                    const jsDay = new Date(selectedDate + "T00:00:00").getDay();
                    const diaSemanaBackend = jsDay === 0 ? 7 : jsDay;

                    const idDaArena = arena?.id || arena?.idArena || arena?.id_arena || bookingToEdit?.id_arena || 1;

                    // 🟢 EM VEZ DE URL PARAMS, ENVIAMOS UM JSON PAYLOAD
                    const payloadMensal = {
                        idArena: idDaArena,
                        idQuadra: selectedQuadra.id,
                        diaSemana: diaSemanaBackend,
                        horaInicio: timeFormatted,
                        horaFim: horaFimStr
                    };

                    await axios.post(
                        'http://localhost:8080/api/contratos-mensalistas/assinar',
                        payloadMensal,
                        { withCredentials: true, headers: getHeaders() }
                    );
                    alert("Contrato mensalista criado com sucesso! Acesse 'Minhas Mensalidades' para realizar o pagamento.");
                }
                if (onSuccess) onSuccess();
                onClose();
            }
        } catch (error) {
            console.error("Erro ao salvar:", error);
            const msg = error.response?.data?.message || error.response?.data?.error || "Erro ao processar solicitação.";
            alert(msg);
        }
    };

    return (
        <div className="modal" onClick={onClose}>
            <StyledWrapper onClick={e => e.stopPropagation()}>
                <div className="form-container">

                    <div className="form-header">
                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                            {selectedQuadra && !isEditing && (
                                <button className="back-btn" onClick={handleBack} title="Trocar Quadra">
                                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
                                </button>
                            )}
                            <h3>
                                {selectedQuadra
                                    ? (isEditing ? `Editar: ${selectedQuadra.nome}` : `Reservar: ${selectedQuadra.nome}`)
                                    : (isEditing ? "Editar Agendamento" : `Quadras: ${arena?.nome || arena?.name || 'Disponíveis'}`)
                                }
                            </h3>
                        </div>
                        <button type="button" className="close-btn" onClick={onClose}>&times;</button>
                    </div>

                    <div className="form-content">
                        {loading ? (
                            <p className="loading-text">Carregando...</p>
                        ) : (
                            !selectedQuadra ? (
                                <div className="courts-list">
                                    {quadras.map(quadra => (
                                        <div key={quadra.id} className="court-item">
                                            <div className="court-info">
                                                <span className="court-name">{quadra.nome}</span>
                                                <span className="court-type">{quadra.tipo_quadra}</span>
                                            </div>
                                            <div className="court-action">
                                                <span className="court-price">R$ {quadra.valor_hora?.toFixed(2)}</span>
                                                <button className="book-btn" onClick={() => setSelectedQuadra(quadra)}>Selecionar</button>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            ) : (

                                <div className="booking-section">

                                    <div className="current-court-info">
                                        <div className="court-display-label">Quadra Selecionada:</div>
                                        <div className="court-display-row">
                                            <span className="court-display-name">{selectedQuadra.nome}</span>
                                            {selectedQuadra && !isEditing && (
                                                <button className="change-court-btn" onClick={handleBack}>
                                                    Trocar
                                                </button>
                                            )}
                                        </div>
                                    </div>

                                    <div className="form-group">
                                        <label>Data:</label>
                                        <input
                                            type="date"
                                            min={today}
                                            value={selectedDate}
                                            onChange={(e) => {
                                                setSelectedDate(e.target.value);
                                                setSelectedHour(null);
                                            }}
                                            className="date-input"
                                        />
                                    </div>

                                    <div className="hours-section">
                                        <label className="section-label">Horários Disponíveis:</label>
                                        {loadingHours ? (
                                            <div className="loading-text">Verificando agenda...</div>
                                        ) : (
                                            availableHours.length > 0 ? (
                                                <>
                                                    <div className="hours-grid">
                                                        {availableHours.map((hour, index) => (
                                                            <div
                                                                key={index}
                                                                className={`hour-card ${selectedHour === hour ? 'selected' : ''}`}
                                                                onClick={() => setSelectedHour(hour)}
                                                            >
                                                                {hour.toString().substring(0, 5)}
                                                            </div>
                                                        ))}
                                                    </div>

                                                    {/* 🟢 CARDS DE ESCOLHA APARECEM APÓS SELECIONAR A HORA */}
                                                    {selectedHour && !isEditing && (
                                                        <div className="checkout-options-container" style={{ marginTop: '20px' }}>
                                                            <h4 style={{ textAlign: 'center', marginBottom: '15px', color: '#fff', fontSize: '1.1rem' }}>Escolha o tipo de reserva:</h4>

                                                            <div className="cards-reserva-grid" style={{ display: 'flex', gap: '15px', justifyContent: 'center' }}>

                                                                {/* CARD 1: JOGO AVULSO */}
                                                                <div
                                                                    className={`reserva-card glass-panel ${tipoReserva === 'AVULSO' ? 'active-neon' : ''}`}
                                                                    style={{
                                                                        flex: 1, padding: '20px', cursor: 'pointer', borderRadius: '12px',
                                                                        border: tipoReserva === 'AVULSO' ? '2px solid #00ff7f' : '1px solid rgba(255,255,255,0.1)',
                                                                        transition: 'all 0.3s'
                                                                    }}
                                                                    onClick={() => setTipoReserva('AVULSO')}
                                                                >
                                                                    <h3 style={{ margin: '0 0 10px 0', color: '#fff', fontSize: '1.1rem' }}>Jogo Avulso</h3>
                                                                    <p style={{ fontSize: '0.85rem', color: '#aaa', minHeight: '40px', margin: 0 }}>
                                                                        Reserva única para o dia <b>{new Date(selectedDate + "T00:00:00").toLocaleDateString('pt-BR')}</b>.
                                                                    </p>
                                                                    <div style={{ marginTop: '15px', paddingTop: '15px', borderTop: '1px solid rgba(255,255,255,0.1)' }}>
                                                                        <span style={{ fontSize: '0.85rem', color: '#ccc' }}>Valor:</span><br/>
                                                                        <strong style={{ fontSize: '1.4rem', color: '#fff' }}>R$ {selectedQuadra.valor_hora?.toFixed(2)}</strong>
                                                                    </div>
                                                                </div>

                                                                {/* CARD 2: MENSALISTA */}
                                                                <div
                                                                    className={`reserva-card glass-panel ${tipoReserva === 'MENSAL' ? 'active-neon' : ''}`}
                                                                    style={{
                                                                        flex: 1, padding: '20px', cursor: 'pointer', borderRadius: '12px',
                                                                        border: tipoReserva === 'MENSAL' ? '2px solid #00ff7f' : '1px solid rgba(255,255,255,0.1)',
                                                                        background: tipoReserva === 'MENSAL' ? 'rgba(0, 255, 127, 0.05)' : '',
                                                                        position: 'relative', overflow: 'hidden', transition: 'all 0.3s'
                                                                    }}
                                                                    onClick={() => setTipoReserva('MENSAL')}
                                                                >
                                                                    {/* Etiqueta de Promoção */}
                                                                    <div style={{
                                                                        position: 'absolute', top: '15px', right: '-35px', background: '#00ff7f', color: '#000',
                                                                        padding: '4px 35px', fontSize: '0.7rem', fontWeight: 'bold', transform: 'rotate(45deg)'
                                                                    }}>
                                                                        MELHOR OPÇÃO
                                                                    </div>

                                                                    <h3 style={{ margin: '0 0 10px 0', color: '#00ff7f', fontSize: '1.1rem' }}>Mensalista</h3>
                                                                    <p style={{ fontSize: '0.85rem', color: '#aaa', minHeight: '40px', margin: 0 }}>
                                                                        Garanta toda <b>{formatarDiaDaSemana(new Date(selectedDate + "T00:00:00").getDay())}</b> neste horário!
                                                                    </p>
                                                                    <div style={{ marginTop: '15px', paddingTop: '15px', borderTop: '1px solid rgba(255,255,255,0.1)' }}>
                                                                        <span style={{ fontSize: '0.85rem', color: '#ccc' }}>Valor por jogo:</span><br/>
                                                                        <strong style={{ fontSize: '1.4rem', color: '#00ff7f' }}>
                                                                            R$ {(selectedQuadra.valor_hora * (1 - (arena?.descontoMensalista || 10) / 100)).toFixed(2)}
                                                                        </strong>
                                                                    </div>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    )}

                                                    <div className="confirm-container">
                                                        <button
                                                            className="confirm-btn-glass"
                                                            disabled={!selectedHour}
                                                            onClick={handleConfirm}
                                                        >
                                                            {selectedHour
                                                                ? (isEditing ? "Salvar Alterações" : `Confirmar como ${tipoReserva === 'AVULSO' ? 'Jogo Único' : 'Mensalista'}`)
                                                                : 'Selecione um horário'}
                                                        </button>
                                                    </div>
                                                </>
                                            ) : (
                                                <div className="no-hours-container">
                                                    <p className="no-hours-text">Sem horários livres para esta data.</p>
                                                </div>
                                            )
                                        )}
                                    </div>
                                </div>
                            )
                        )}
                    </div>
                </div>
            </StyledWrapper>
        </div>
    );
}

const gradient = keyframes`
    0% { background-position: 0% 50%; }
    50% { background-position: 100% 50%; }
    100% { background-position: 0% 50%; }
`;

const StyledWrapper = styled.div`
    .form-container {
        width: 100%;
        max-width: 480px;
        max-height: calc(100vh - 155px);
        margin-top: 20px;
        margin-bottom: 20px;
        background: linear-gradient(#212121, #212121) padding-box,
        linear-gradient(145deg, transparent 35%, #4ade80, #062904) border-box;
        border: 2px solid transparent;
        padding: 32px 24px;
        font-size: 14px;
        color: white;
        display: flex;
        flex-direction: column;
        gap: 20px;
        box-sizing: border-box;
        border-radius: 16px;
        background-size: 200% 100%;
        animation: ${gradient} 5s ease infinite;
        position: relative;
    }

    .form-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        flex-shrink: 0;
        margin-bottom: 10px;
        border-bottom: 1px solid #333;
        padding-bottom: 15px;
    }

    .form-header h3 {
        font-size: 1.2rem;
        font-weight: bold;
        color: #fff;
        margin: 0;
    }

    .back-btn {
        background: none;
        border: none;
        color: #4ade80;
        cursor: pointer;
        padding: 0;
        display: flex;
        align-items: center;
        transition: transform 0.2s;
    }
    .back-btn:hover { transform: translateX(-3px); }

    .close-btn {
        background: transparent;
        border: none;
        color: #717171;
        font-size: 1.8rem;
        cursor: pointer;
        transition: color 0.3s;
        line-height: 0.8;
    }
    .close-btn:hover { color: #4ade80; }

    .change-court-btn {
        background: transparent;
        border: 1px solid #4ade80;
        color: #4ade80;
        border-radius: 6px;
        padding: 4px 10px;
        font-size: 0.75rem;
        cursor: pointer;
        transition: all 0.2s;
        margin-left: 10px;
    }
    .change-court-btn:hover {
        background: #4ade80;
        color: #000;
    }

    .confirm-btn-glass {
        background: linear-gradient(135deg, #4ade80 0%, #22c55e 100%);
        border: none;
        color: #000;
        padding: 10px 18px;
        border-radius: 12px;
        font-weight: 700;
        cursor: pointer;
        transition: transform 0.2s, box-shadow 0.2s;
        width: 100%;
        margin-top: 10px;
    }
    .confirm-btn-glass:disabled {
        background: #333;
        color: #666;
        cursor: not-allowed;
    }
    .confirm-btn-glass:hover:not(:disabled) {
        transform: translateY(-2px);
        box-shadow: 0 0 20px rgba(74, 222, 128, 0.3);
    }

    .form-content {
        overflow-y: auto;
        padding-right: 5px;
        display: flex;
        flex-direction: column;
        gap: 15px;
    }

    .form-content::-webkit-scrollbar { width: 5px; }
    .form-content::-webkit-scrollbar-track { background: rgba(255, 255, 255, 0.05); border-radius: 4px; }
    .form-content::-webkit-scrollbar-thumb { background: #4ade80; border-radius: 4px; }

    .loading-text, .no-hours-text {
        text-align: center;
        color: #9e9e9e;
        margin-top: 10px;
        font-style: italic;
    }

    .courts-list {
        display: flex;
        flex-direction: column;
        gap: 12px;
    }

    .court-item {
        background: rgba(255, 255, 255, 0.03);
        border: 1px solid #414141;
        border-radius: 8px;
        padding: 12px 16px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        transition: all 0.3s;
    }

    .court-item:hover {
        border-color: #4ade80;
        background: rgba(74, 222, 128, 0.05);
    }

    .court-info { display: flex; flex-direction: column; }
    .court-name { font-weight: 600; font-size: 15px; color: #fff; }
    .court-type { font-size: 0.8rem; color: #9e9e9e; }

    .court-action { display: flex; flex-direction: column; align-items: flex-end; gap: 5px; }
    .court-price { color: #4ade80; font-weight: bold; font-size: 0.9rem; }

    .book-btn {
        background: #313131;
        border: 1px solid #414141;
        color: #ccc;
        padding: 6px 12px;
        border-radius: 6px;
        font-size: 0.8rem;
        cursor: pointer;
        transition: all 0.2s;
    }
    .book-btn:hover { background: #4ade80; color: #121212; border-color: #4ade80; }

    .booking-section {
        display: flex;
        flex-direction: column;
        gap: 20px;
        animation: fadeIn 0.3s ease;
    }

    .form-group {
        display: flex;
        flex-direction: column;
        gap: 8px;
    }

    .form-group label, .section-label {
        color: #9e9e9e;
        font-weight: 600;
        font-size: 0.9rem;
    }

    .date-input {
        width: 100%;
        padding: 12px;
        border-radius: 8px;
        background-color: transparent;
        border: 1px solid #414141;
        color: #fff;
        font-family: inherit;
        outline: none;
        cursor: pointer;
    }
    .date-input:focus { border-color: #4ade80; }

    .date-input::-webkit-calendar-picker-indicator {
        filter: invert(1);
        cursor: pointer;
    }

    .hours-grid {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 10px;
    }

    .hour-card {
        background: rgba(255, 255, 255, 0.05);
        border: 1px solid #414141;
        color: #fff;
        padding: 10px;
        border-radius: 8px;
        cursor: pointer;
        font-weight: 600;
        text-align: center;
        transition: all 0.2s;
    }

    .hour-card:hover {
        background: rgba(74, 222, 128, 0.1);
        border-color: #4ade80;
    }

    .hour-card.selected {
        background: #4ade80;
        color: #121212;
        border-color: #4ade80;
        box-shadow: 0 0 10px rgba(74, 222, 128, 0.4);
        transform: scale(1.05);
    }

    /* Animação extra para os cards de escolha */
    .reserva-card {
        animation: slideUp 0.3s ease-out forwards;
    }

    @keyframes slideUp {
        from { opacity: 0; transform: translateY(15px); }
        to { opacity: 1; transform: translateY(0); }
    }

    @keyframes fadeIn {
        from { opacity: 0; transform: translateY(10px); }
        to { opacity: 1; transform: translateY(0); }
    }
`;