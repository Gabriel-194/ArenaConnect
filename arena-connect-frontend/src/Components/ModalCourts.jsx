import React, {useEffect, useState} from "react";
import axios from 'axios';
import styled, { keyframes } from 'styled-components';
import '../Styles/components.css';

export default function ModalCourts({ onClose,onSuccess,quadraToEdit }) {
    const [nome, setNome] = useState('');
    const [tipo, setTipo] = useState('');
    const [valorHora, setValorHora] = useState('');
    const [error, setErro] = useState('');

    useEffect(() => {
        if(quadraToEdit) {
            setNome(quadraToEdit.nome || '');
            setTipo(quadraToEdit.tipo_quadra || '');
            setValorHora(quadraToEdit.valor_hora || '');
        }else {
            setNome('');
            setTipo('');
            setValorHora('');
        }
    }, [quadraToEdit]);

    const handleSave = async (e) => {
        e.preventDefault();
        setErro('');

        try {
            let response;
            const config = {
                withCredentials: true,
                headers: { 'Content-Type': 'application/json' }
            };

            const datas = {
                nome: nome,
                tipo_quadra: tipo,
                valor_hora: parseFloat(valorHora)
            };

            if (quadraToEdit) {
                response = await axios.put(
                    "http://localhost:8080/quadra",
                    { ...datas, id: quadraToEdit.id },
                    config
                );
            } else {
                response = await axios.post("http://localhost:8080/quadra/createQuadra", datas,config);
            }

            if (response.status === 200 || response.status === 201) {
                alert(quadraToEdit ? "Quadra atualizada!" : "Quadra cadastrada!");
                if (onSuccess) onSuccess();
                onClose();
            }
        } catch (err) {
            console.error("Erro:", err);

            if (err.response && err.response.data) {

                setErro(err.response.data.message || "Erro ao processar requisição.");
            } else {
                setErro("Erro de conexão com o servidor.");
            }
        }
    }


    const handleModalClick = (e) => {
        e.stopPropagation();
    }

    return (
        <div className="modal" onClick={onClose}>
            <StyledWrapper onClick={handleModalClick}>
                <div className="form-container">

                    <div className="form-header">
                        <h3>{quadraToEdit ? 'Editar Quadra' : 'Nova Quadra'}</h3>
                        <button type="button" className="close-btn" onClick={onClose}>&times;</button>
                    </div>

                    <form className="form" onSubmit={handleSave}>
                        <div className="form-group">
                            <label htmlFor="nome">Nome da Quadra</label>
                            <input
                                required
                                name="nome"
                                id="nome"
                                type="text"
                                placeholder="Ex: Quadra 1"
                                value={nome}
                                onChange={(e) => setNome(e.target.value)}
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="tipo">Tipo da Quadra</label>
                            <input
                                required
                                name="tipo"
                                id="tipo"
                                type="text"
                                placeholder="Ex: Futsal"
                                value={tipo}
                                onChange={(e) => setTipo(e.target.value)}
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="valor">Valor por Hora (R$)</label>
                            <input
                                required
                                name="valor"
                                id="valor"
                                type="number"
                                step="0.01"
                                placeholder="150.00"
                                value={valorHora}
                                onChange={(e) => setValorHora(e.target.value)}
                            />
                        </div>

                        {error && <p className="error-text">{error}</p>}

                        <button type="submit" className="form-submit-btn">
                            {quadraToEdit ? 'Salvar Alterações' : 'Cadastrar'}
                        </button>
                    </form>
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
        width: 400px;
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
    }

    .form-title {
        font-size: 1.2rem;
        font-weight: bold;
        color: #fff;
    }

    .close-btn {
        background: transparent;
        border: none;
        color: #717171;
        font-size: 1.5rem;
        cursor: pointer;
        transition: color 0.3s;
    }

    .close-btn:hover {
        color: #4ade80;
    }

    .form-container button:active {
        scale: 0.95;
    }

    .form-container .form {
        display: flex;
        flex-direction: column;
        gap: 20px;
    }

    .form-container .form-group {
        display: flex;
        flex-direction: column;
        gap: 2px;
    }

    .form-container .form-group label {
        display: block;
        margin-bottom: 5px;
        color: #9e9e9e;
        font-weight: 600;
        font-size: 15px;
    }

    .form-container .form-group input {
        width: 100%;
        padding: 12px 16px;
        border-radius: 8px;
        color: #fff;
        font-family: inherit;
        background-color: transparent;
        border: 1px solid #414141;
    }

    .form-container .form-group input::placeholder {
        opacity: 0.5;
    }

    .form-container .form-group input:focus {
        outline: none;
        border-color: #4ade80;
    }

    .form-submit-btn {
        display: flex;
        align-items: center;
        justify-content: center;
        align-self: flex-start;
        font-family: inherit;
        color: #9e9e9e;
        font-weight: 600;
        width: 100%;
        background: #313131;
        border: 1px solid #414141;
        padding: 12px 16px;
        font-size: inherit;
        gap: 8px;
        margin-top: 8px;
        cursor: pointer;
        border-radius: 6px;
        transition: all 0.3s;
    }

    .form-submit-btn:hover {
        background-color: #fff;
        border-color: #fff;
        color: #212121;
    }

    .error-text {
        color: #ff4d4d;
        font-size: 0.9rem;
        text-align: center;
        margin: 0;
    }
`;