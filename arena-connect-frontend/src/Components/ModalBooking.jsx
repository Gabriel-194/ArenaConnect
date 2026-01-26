import React, { useEffect, useState } from "react";
import axios from 'axios';
import styled, { keyframes } from 'styled-components';
import '../Styles/components.css'; // Para garantir que o overlay .modal funcione

export default function ModalBooking({ arena, onClose }) {
    const [quadras, setQuadras] = useState([]);
    const [loading, setLoading] = useState(true);

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

    return (
        <div className="modal" onClick={onClose}>
            <StyledWrapper onClick={handleModalClick}>
                <div className="form-container">

                    <div className="form-header">
                        <h3>Quadras em {arena.name || arena.nome}</h3>
                        <button type="button" className="close-btn" onClick={onClose}>&times;</button>
                    </div>

                    {/* Área de Conteúdo (Scrollável) */}
                    <div className="form-content">
                        {loading ? (
                            <p className="loading-text">Carregando quadras...</p>
                        ) : (
                            <div className="courts-list">
                                {quadras.length > 0 ? quadras.map(quadra => (
                                    <div key={quadra.id} className="court-item">
                                        <div className="court-info">
                                            <span className="court-name">{quadra.nome}</span>
                                            <span className="court-type">{quadra.tipo_quadra}</span>
                                        </div>
                                        <div className="court-action">
                                            <span className="court-price">R$ {quadra.valor_hora}</span>
                                            <button className="btn-secondary">Reservar</button>
                                        </div>
                                    </div>
                                )) : (
                                    <p className="loading-text">Nenhuma quadra disponível.</p>
                                )}
                            </div>
                        )}
                    </div>

                </div>
            </StyledWrapper>
        </div>
    );
}

// --- ESTILOS IDÊNTICOS AO MODAL COURTS ---

const gradient = keyframes`
    0% { background-position: 0% 50%; }
    50% { background-position: 100% 50%; }
    100% { background-position: 0% 50%; }
`;

const StyledWrapper = styled.div`
    .form-container {
        width: 400px;
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
        margin-bottom: 10px;
        flex-shrink: 0; /* Impede o header de encolher */
    }

    .form-header h3 {
        font-size: 1.2rem;
        font-weight: bold;
        color: #fff;
        margin: 0;
    }

    .close-btn {
        background: transparent;
        border: none;
        color: #717171;
        font-size: 1.5rem;
        cursor: pointer;
        transition: color 0.3s;
        line-height: 1;
    }

    .close-btn:hover {
        color: #4ade80;
    }

    /* Área rolável para as quadras */
    .form-content {
        overflow-y: auto;
        padding-right: 5px;
        display: flex;
        flex-direction: column;
        gap: 15px;
    }

    /* Scrollbar estilizada para combinar */
    .form-content::-webkit-scrollbar {
        width: 5px;
    }
    .form-content::-webkit-scrollbar-track {
        background: rgba(255, 255, 255, 0.05);
        border-radius: 4px;
    }
    .form-content::-webkit-scrollbar-thumb {
        background: #4ade80;
        border-radius: 4px;
    }

    .loading-text {
        text-align: center;
        color: #9e9e9e;
        margin-top: 20px;
    }

    .courts-list {
        display: flex;
        flex-direction: column;
        gap: 12px;
    }

    /* Estilo de cada item (Quadra) imitando os inputs do outro modal */
    .court-item {
        width: 100%;
        padding: 12px 16px;
        border-radius: 8px;
        color: #fff;
        background-color: transparent;
        border: 1px solid #414141;
        display: flex;
        justify-content: space-between;
        align-items: center;
        transition: border-color 0.3s;
        box-sizing: border-box;
    }

    .court-item:hover {
        border-color: #4ade80;
    }

    .court-info {
        display: flex;
        flex-direction: column;
    }

    .court-name {
        font-weight: 600;
        font-size: 15px;
        color: #fff;
    }

    .court-type {
        font-size: 0.8rem;
        color: #9e9e9e;
    }

    .court-action {
        display: flex;
        flex-direction: column;
        align-items: flex-end;
        gap: 5px;
    }

    .court-price {
        color: #4ade80;
        font-weight: bold;
        font-size: 0.9rem;
    }

    .book-btn {
        background: #313131;
        border: 1px solid #414141;
        color: #9e9e9e;
        padding: 4px 10px;
        border-radius: 4px;
        font-size: 0.8rem;
        cursor: pointer;
        transition: all 0.3s;
    }

    .book-btn:hover {
        background: #fff;
        color: #212121;
        border-color: #fff;
    }
`;