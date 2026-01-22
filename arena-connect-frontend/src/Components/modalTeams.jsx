import styled, {keyframes} from "styled-components";
import React, {useState} from "react";

export default function modalTeams({onClose}) {
    const [nome, setNome] = useState('');
    const [escudo, setEscudo] = useState('');
    const [vitorias, setVitorias] = useState('');
    const [derrotas, setDerrotas] = useState('');


    return (
        <div className="modal-overlay" onClick={onClose}>
            <StyledWrapper >
                <div className="form-container">

                    <div className="form-header">
                        <h3>Novo Time</h3>
                        <button type="button" className="close-btn" onClick={onClose}>&times;</button>
                    </div>

                    <form className="form" >

                        {/* ÁREA DE UPLOAD DO ESCUDO */}
                        <div className="upload-section">
                            <div className="image-preview" >

                                    <img  alt="Escudo Preview" />

                                    <div className="placeholder-icon">
                                        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1">
                                            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                                            <polyline points="17 8 12 3 7 8" />
                                            <line x1="12" y1="3" x2="12" y2="15" />
                                        </svg>
                                    </div>
                            </div>
                            <button type="button" className="upload-btn-text" >
                                Selecionar Escudo
                            </button>

                            {/* Input escondido */}

                        </div>

                        <div className="form-group">
                            <label htmlFor="nome">Nome do Time</label>
                            <input
                                required
                                name="nome"
                                id="nome"
                                type="text"
                                placeholder="Ex: Red Dragons FC"
                                value={nome}
                                onChange={(e) => setNome(e.target.value)}
                            />
                        </div>


                        <button type="submit" className="form-submit-btn">
                            Cadastrar Time
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


