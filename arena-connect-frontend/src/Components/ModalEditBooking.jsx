import React, { useEffect, useState } from "react";
import axios from 'axios';
import styled, { keyframes } from 'styled-components';
import '../Styles/components.css';

export default function ModalEditBooking({ bookingToEdit, onClose, onSuccess }) {
    const [quadras, setQuadras] = useState([]);
    const [loading, setLoading] = useState(true);

    const [selectedQuadra, setSelectedQuadra] = useState(null);
    const [availableHours, setAvailableHours] = useState([]);
    const [loadingHours, setLoadingHours] = useState(false);
    const [selectedHour, setSelectedHour] = useState(null);

    const [selectedDate, setSelectedDate] = useState("");

    const getTodayLocal = () => {
        const date = new Date();
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    };
    const today = getTodayLocal();

    useEffect(() => {
        const init = async () => {
            try {
                const response = await axios.get(`http://localhost:8080/api/quadra`, {
                    withCredentials: true
                });
                setQuadras(response.data);

                if (bookingToEdit) {
                    const court = response.data.find(q => q.id === bookingToEdit.id_quadra);
                    setSelectedQuadra(court);

                    const dateObj = new Date(bookingToEdit.data_inicio);
                    const isoDate = dateObj.toISOString().split('T')[0];
                    const hourStr = dateObj.toTimeString().split(' ')[0].substring(0, 5);

                    setSelectedDate(isoDate);
                    setSelectedHour(hourStr);
                }
            } catch (err) {
                console.error("Erro ao inicializar:", err);
            } finally {
                setLoading(false);
            }
        };
        init();
    }, [bookingToEdit]);

    const handleBack = () => {
        setSelectedQuadra(null);
        setAvailableHours([]);
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
                        withCredentials: true
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

                    if (bookingToEdit) {
                        const originalDate = new Date(bookingToEdit.data_inicio).toISOString().split('T')[0];
                        const originalTime = new Date(bookingToEdit.data_inicio).toTimeString().substring(0,5);

                        if (selectedDate === originalDate && selectedQuadra.id === bookingToEdit.id_quadra) {
                            if (!filteredHours.includes(originalTime)) {
                                filteredHours.push(originalTime);
                                filteredHours.sort();
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
    }, [selectedQuadra, selectedDate, today, bookingToEdit]);

    const handleConfirmUpdate = async () => {
        if(!selectedHour) return;

        const hourString = String(selectedHour);
        const timeFormatted = hourString.length === 5 ? hourString + ':00' : hourString;
        const dataInicioISO = `${selectedDate}T${timeFormatted}`;

        if(!window.confirm(`Atualizar reserva para ${selectedDate} às ${hourString}?`)) return;

        try {
            const agendamentoData = {
                id_agendamento: bookingToEdit.id_agendamento,
                id_quadra: selectedQuadra.id,
                data_inicio: dataInicioISO,
                valor: selectedQuadra.valor_hora,
                status: bookingToEdit.status
            };

            await axios.post('http://localhost:8080/api/agendamentos/reservar', agendamentoData, {
                withCredentials: true
            });

            alert(`✅ Agendamento atualizado com sucesso!`);
            if (onSuccess) onSuccess();
            onClose();
        } catch (error) {
            console.error("Erro ao atualizar:", error);
            const msg = error.response?.data?.message || "Erro ao atualizar agendamento.";
            alert(msg);
        }
    }

    return (
        <div className="modal" onClick={onClose}>
            <StyledWrapper onClick={e => e.stopPropagation()}>
                <div className="form-container">

                    <div className="form-header">
                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                            {selectedQuadra && (
                                <button className="back-btn" onClick={handleBack} title="Trocar Quadra">
                                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
                                </button>
                            )}
                            <h3>{selectedQuadra ? `Editar Agendamento` : `Escolha a Quadra`}</h3>
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
                                                <span className="court-price">R$ {quadra.valor_hora}</span>
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
                                            <button className="change-court-btn" onClick={handleBack}>
                                                Trocar
                                            </button>
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

                                                    <div className="confirm-container">
                                                        <button
                                                            className="confirm-btn-glass"
                                                            disabled={!selectedHour}
                                                            onClick={handleConfirmUpdate}
                                                        >
                                                            {selectedHour
                                                                ? `Salvar Mudança (${selectedHour.toString().substring(0, 5)})`
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

    .form-header h3 { font-size: 1.2rem; font-weight: bold; color: #fff; margin: 0; }

    .back-btn { background: none; border: none; color: #4ade80; cursor: pointer; padding: 0; display: flex; align-items: center; transition: transform 0.2s; }
    .back-btn:hover { transform: translateX(-3px); }

    .close-btn { background: transparent; border: none; color: #717171; font-size: 1.8rem; cursor: pointer; transition: color 0.3s; line-height: 0.8; }
    .close-btn:hover { color: #4ade80; }

    .form-content { overflow-y: auto; padding-right: 5px; display: flex; flex-direction: column; gap: 15px; }
    .form-content::-webkit-scrollbar { width: 5px; }
    .form-content::-webkit-scrollbar-track { background: transparent }
    .form-content::-webkit-scrollbar-thumb { background: none }


    .current-court-info {
        background: rgba(255, 255, 255, 0.03);
        border: 1px solid #333;
        border-radius: 10px;
        padding: 12px;
        margin-bottom: 10px;
    }
    .court-display-label { font-size: 0.8rem; color: #999; margin-bottom: 4px; }
    .court-display-row { display: flex; justify-content: space-between; align-items: center; }
    .court-display-name { font-size: 1rem; font-weight: bold; color: #4ade80; }
    .change-court-btn {
        background: transparent;
        border: 1px solid #4ade80;
        color: #4ade80;
        border-radius: 6px;
        padding: 4px 10px;
        font-size: 0.75rem;
        cursor: pointer;
        transition: all 0.2s;
    }
    .change-court-btn:hover { background: #4ade80; color: #000; }

    .loading-text, .no-hours-text { text-align: center; color: #9e9e9e; margin-top: 10px; font-style: italic; }

    .courts-list { display: flex; flex-direction: column; gap: 12px; }

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
    .court-item:hover { border-color: #4ade80; background: rgba(74, 222, 128, 0.05); }

    .court-info { display: flex; flex-direction: column; }
    .court-name { font-weight: 600; font-size: 15px; color: #fff; }
    .court-type { font-size: 0.8rem; color: #9e9e9e; }

    .court-action { display: flex; flex-direction: column; align-items: flex-end; gap: 5px; }
    .court-price { color: #4ade80; font-weight: bold; font-size: 0.9rem; }

    .book-btn { background: #313131; border: 1px solid #414141; color: #ccc; padding: 6px 12px; border-radius: 6px; font-size: 0.8rem; cursor: pointer; transition: all 0.2s; }
    .book-btn:hover { background: #4ade80; color: #121212; border-color: #4ade80; }

    .booking-section { display: flex; flex-direction: column; gap: 20px; animation: fadeIn 0.3s ease; }

    .form-group { display: flex; flex-direction: column; gap: 8px; }
    .form-group label, .section-label { color: #9e9e9e; font-weight: 600; font-size: 0.9rem; }

    .date-input { width: 100%; padding: 12px; border-radius: 8px; background-color: transparent; border: 1px solid #414141; color: #fff; font-family: inherit; outline: none; cursor: pointer; }
    .date-input:focus { border-color: #4ade80; }
    .date-input::-webkit-calendar-picker-indicator { filter: invert(1); cursor: pointer; }

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
    .hour-card:hover { background: rgba(74, 222, 128, 0.1); border-color: #4ade80; }
    .hour-card.selected { background: #4ade80; color: #121212; border-color: #4ade80; box-shadow: 0 0 10px rgba(74, 222, 128, 0.4); transform: scale(1.05); }

    .confirm-container { display: flex; justify-content: center; }
    .confirm-btn-glass {
        background: linear-gradient(135deg, #4ade80 0%, #22c55e 100%);
        border: none;
        color: #000;
        padding: 12px 24px;
        border-radius: 12px;
        font-weight: 700;
        cursor: pointer;
        transition: transform 0.2s, box-shadow 0.2s;
        width: 100%;
    }
    .confirm-btn-glass:disabled { background: #333; color: #666; cursor: not-allowed; }
    .confirm-btn-glass:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 0 20px rgba(74, 222, 128, 0.3); }

    @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
`;