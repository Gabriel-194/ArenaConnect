import React, { useEffect, useState } from "react";
import axios from 'axios';
import styled, { keyframes } from 'styled-components';
import '../Styles/components.css';

export default function ModalBooking({ arena, onClose }) {
    const [quadras, setQuadras] = useState([]);
    const [loading, setLoading] = useState(true);

    const[selectedQuadra,setSelectedQuadra] = useState();
    const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]); // Hoje
    const [availableHours, setAvailableHours] = useState([]);
    const [loadingHours, setLoadingHours] = useState(false);
    const [selectedHour, setSelectedHour] = useState(null);


    useEffect(() => {
        const fetchQuadras = async () => {
            try {
                const response = await axios.get(`http://localhost:8080/api/quadra/courtAtivas`, {
                    withCredentials: true,
                    headers: { 'X-Tenant-ID': arena.schemaName }
                });
                setQuadras(response.data);
            } catch (err) {
                console.error("Erro ao buscar quadras:", err);
            } finally {
                setLoading(false);
            }
        };
        fetchQuadras();
    }, [arena.schemaName]);

    const handleModalClick = (e) => {
        e.stopPropagation();
    }

    const handleBack = () => {
        setSelectedQuadra(null);
        setAvailableHours([]);
    }

    useEffect(() => {
        if (selectedQuadra && selectedDate) {
            const fetchHours = async () => {
                setLoadingHours(true);
                try {
                    const response = await axios.get(`http://localhost:8080/api/agendamentos/disponibilidade`, {
                        params: {
                            idQuadra: selectedQuadra.id,
                            data: selectedDate
                        },
                        withCredentials: true,
                        headers: { 'X-Tenant-ID': arena.schemaName }
                    });
                    setAvailableHours(response.data);
                } catch (err) {
                    console.error("Erro ao buscar horários:", err);
                    setAvailableHours([]);
                } finally {
                    setLoadingHours(false);
                }
            };
            fetchHours();
        }
    }, [selectedQuadra, selectedDate, arena.schemaName]);
    const handleConfirmBooking = async (hour) => {
        const hourString = String(selectedHour);

        const dataInicioISO = `${selectedDate}T${hourString.length === 5 ? hourString + ':00' : hourString}`;

        if(!window.confirm(`Confirmar reserva para ${selectedDate} às ${hour}?`)) return;

        try{
            const agendamentoData = {
                id_quadra: selectedQuadra.id,
                id_user: 1, // TODO: Substituir pelo ID do usuário logado (localStorage)
                data_inicio: dataInicioISO,
                valor: selectedQuadra.valor_hora,
                cliente_avulso: "Não",
                status: "CONFIRMADO"
            };

            await axios.post('http://localhost:8080/api/agendamentos/reservar', agendamentoData, {
                withCredentials: true,
                headers: { 'X-Tenant-ID': arena.schemaName }
            });
        } catch (error) {
            console.error("Erro ao agendar:", error);
            alert("Erro ao realizar o agendamento. Tente novamente.");
        }
    }

    const handleSelectHour = (hour) => {
        setSelectedHour(hour);
    }

    return (
        <div className="modal" onClick={onClose}>
            <StyledWrapper onClick={e => e.stopPropagation()}>
                <div className="form-container">

                    {/* HEADER */}
                    <div className="form-header">
                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                            {selectedQuadra && (
                                <button className="back-btn" onClick={handleBack}>
                                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
                                </button>
                            )}
                            <h3>{selectedQuadra ? `Reservar ${selectedQuadra.nome}` : `Quadras em ${arena.name || arena.nome}`}</h3>
                        </div>
                        <button type="button" className="close-btn" onClick={onClose}>&times;</button>
                    </div>

                    {/* CONTENT */}
                    <div className="form-content">
                        {loading ? (
                            <p className="loading-text">Carregando...</p>
                        ) : (
                            !selectedQuadra ? (
                                // LISTA DE QUADRAS
                                <div className="courts-list">
                                    {quadras.length > 0 ? quadras.map(quadra => (
                                        <div key={quadra.id} className="court-item">
                                            <div className="court-info">
                                                <span className="court-name">{quadra.nome}</span>
                                                <span className="court-type">{quadra.tipo_quadra}</span>
                                            </div>
                                            <div className="court-action">
                                                <span className="court-price">R$ {quadra.valor_hora}</span>
                                                <button className="book-btn" onClick={() => setSelectedQuadra(quadra)}>Ver Horários</button>
                                            </div>
                                        </div>
                                    )) : <p className="loading-text">Nenhuma quadra disponível.</p>}
                                </div>
                            ) : (
                                // SEÇÃO DE AGENDAMENTO
                                <div className="booking-section">
                                    <div className="form-group">
                                        <label>Escolha a data:</label>
                                        <input type="date" value={selectedDate} onChange={(e) => setSelectedDate(e.target.value)} className="date-input"/>
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
                                                                onClick={() => handleSelectHour(hour)}
                                                            >
                                                                {hour.toString().substring(0, 5)}
                                                            </div>
                                                        ))}
                                                    </div>

                                                    <div className="confirm-container">
                                                        <button className="btn-icon-glass" style={{width: "100px"}} disabled={!selectedHour} onClick={handleConfirmBooking}>
                                                            {selectedHour ? `Confirmar às ${selectedHour.toString().substring(0, 5)}`
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
        max-width: 450px;
        max-height: 85vh; 
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

    .hour-btn {
        background: rgba(255, 255, 255, 0.05);
        border: 1px solid #414141;
        color: #4ade80;
        padding: 10px;
        border-radius: 8px;
        cursor: pointer;
        font-weight: 600;
        transition: all 0.2s;
    }

    .hour-btn:hover {
        background: #4ade80;
        color: #121212;
        box-shadow: 0 0 10px rgba(74, 222, 128, 0.4);
        transform: translateY(-2px);
    }

    .hours-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin-bottom: 20px; }

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

    /* ESTILO QUANDO SELECIONADO */
    .hour-card.selected {
        background: #4ade80;
        color: #121212;
        border-color: #4ade80;
        box-shadow: 0 0 10px rgba(74, 222, 128, 0.4);
        transform: scale(1.05);
    }

    @keyframes fadeIn {
        from { opacity: 0; transform: translateY(10px); }
        to { opacity: 1; transform: translateY(0); }
    }
`;

