import React from 'react';
import styled from 'styled-components';

const ForgotPasswordModal = ({onClose}) => {
    // Troque esse número (1, 2, 3 ou 4) para visualizar cada etapa do layout!
    const step = 1;

    return (
        <StyledWrapper>
            <div className="modal-overlay" onClick={onClose}>
                <div className="form" onClick={(e) => e.stopPropagation()}>

                    {/* O SVG de fundo animado (Liquid Blur) fica fixo para todos os steps */}
                    <svg className="svg" viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
                        <path fill="#4073ff" d="M56.8,-23.9C61.7,-3.2,45.7,18.8,26.5,31.7C7.2,44.6,-15.2,48.2,-35.5,36.5C-55.8,24.7,-73.9,-2.6,-67.6,-25.2C-61.3,-47.7,-30.6,-65.6,-2.4,-64.8C25.9,-64.1,51.8,-44.7,56.8,-23.9Z" transform="translate(100 100)" className="path" />
                    </svg>

                    {/* ==========================================
              STEP 1: INSERIR E-MAIL
              ========================================== */}
                    {step === 1 && (
                        <div className="content">
                            <p align="center">Recuperar Senha</p>
                            <span className="subtitle" align="center">Digite seu e-mail para receber o código.</span>

                            <div className="input-group">
                                <input placeholder="exemplo@email.com" type="email" className="full-input" />
                            </div>

                            <button>Enviar Código</button>
                        </div>
                    )}

                    {/* ==========================================
              STEP 2: INSERIR TOKEN (Seu código original ajustado)
              ========================================== */}
                    {step === 2 && (
                        <div className="content">
                            <p align="center">Código de Verificação</p>
                            <span className="subtitle" align="center">Enviamos um código para o seu e-mail.</span>

                            <div className="inp">
                                <input placeholder="" type="text" className="input" maxLength={1} />
                                <input placeholder="" type="text" className="input" maxLength={1} />
                                <input placeholder="" type="text" className="input" maxLength={1} />
                                <input placeholder="" type="text" className="input" maxLength={1} />
                            </div>

                            <button>Verificar</button>
                        </div>
                    )}

                    {/* ==========================================
              STEP 3: NOVA SENHA
              ========================================== */}
                    {step === 3 && (
                        <div className="content">
                            <p align="center">Nova Senha</p>
                            <span className="subtitle" align="center">Crie uma nova senha segura.</span>

                            <div className="input-group">
                                <input placeholder="Nova senha" type="password" className="full-input" />
                                <input placeholder="Confirme a senha" type="password" className="full-input" />
                            </div>

                            <button>Redefinir Senha</button>
                        </div>
                    )}

                    {/* ==========================================
              STEP 4: SUCESSO
              ========================================== */}
                    {step === 4 && (
                        <div className="content success-content">
                            <div className="success-icon">
                                <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#00ff7f" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                                    <polyline points="22 4 12 14.01 9 11.01"></polyline>
                                </svg>
                            </div>
                            <p align="center">Senha Redefinida!</p>
                            <span className="subtitle" align="center">Sua senha foi alterada com sucesso.</span>
                        </div>
                    )}

                </div>
            </div>
        </StyledWrapper>
    );
}

const StyledWrapper = styled.div`
    .modal-overlay {
        position: fixed;
        top: 0;
        left: 0;
        width: 100vw;
        height: 100vh;
        display: flex;
        justify-content: center;
        align-items: center;
        background: rgba(0, 0, 0, 0.6);
        z-index: 9999;
    }

    .form {
        display: flex;
        flex-direction: column;
        gap: 10px;
        background: rgba(5, 5, 5, 0.4);
        border-radius: 16px;
        box-shadow: 0 4px 30px rgba(0, 255, 127, 0.15);
        backdrop-filter: blur(12px);
        -webkit-backdrop-filter: blur(12px);
        border: 1px solid rgba(0, 255, 127, 0.4);

        /* Tamanho ajustado para caber os formulários de senha */
        width: 26em;
        min-height: 28em;
        padding: 30px 20px;
        position: relative;
        justify-content: center;
    }

    .content {
        display: flex;
        flex-direction: column;
        gap: 20px;
        margin-top: auto;
        margin-bottom: auto;
        z-index: 2;
    }

    .form p {
        color: #fff;
        font-weight: bolder;
        font-size: 1.3rem;
        margin: 0;
    }

    .subtitle {
        color: #ccc;
        font-size: 0.85rem;
        margin-top: -10px;
        text-align: center;
    }


    .path {
        fill: rgba(29, 221, 125, 0.26);
    }

    .svg {
        filter: blur(20px);
        z-index: -1;
        position: absolute;
        opacity: 50%;
        animation: anim 4s ease-in-out infinite;
        left: 50%;
        transform: translateX(-50%);
    }

    @keyframes anim {
        0% {
            transform: translate(-50%, -110px);
        }
        50% {
            transform: translate(-50%, 110px);
        }
        100% {
            transform: translate(-50%, -110px);
        }
    }

    .inp {
        margin-left: auto;
        margin-right: auto;
        white-space: 4px;
        display: flex;
        gap: 0.5em;
    }

    .input {
        color: #fff;
        height: 3.5em;
        width: 3.5em;
        font-size: 1.2rem;
        text-align: center;
        background: transparent;
        outline: none;
        border: 1px solid #00ff7f;
        border-radius: 10px;
        transition: all 0.6s ease;
    }

    .input:focus {
        outline: none;
        border: 1px solid #fff;
        box-shadow: 0 0 10px rgba(0, 255, 127, 0.5);
    }

    /* NOVOS INPUTS LONGOS */

    .input-group {
        display: flex;
        flex-direction: column;
        gap: 15px;
        width: 100%;
    }

    .full-input {
        width: 100%;
        height: 3.5em;
        background: rgba(0, 0, 0, 0.2);
        border: 1px solid #00ff7f;
        border-radius: 10px;
        color: white;
        padding: 0 15px;
        outline: none;
        font-size: 0.95rem;
        transition: all 0.3s ease;
        box-sizing: border-box;
    }

    .full-input:focus {
        border-color: #fff;
        background: rgba(0, 0, 0, 0.4);
        box-shadow: 0 0 10px rgba(0, 255, 127, 0.3);
    }

    .full-input::placeholder {
        color: rgba(255, 255, 255, 0.4);
    }

    /* BOTÃO GERAL */

    .form button {
        margin-left: auto;
        margin-right: auto;
        background-color: transparent;
        color: #fff;
        font-weight: bold;
        width: 100%;
        height: 3.5em;
        border: 0.2em solid #00ff7f;
        border-radius: 11px;
        cursor: pointer;
        transition: all 0.5s ease;
    }

    .form button:hover {
        background-color: #00ff7f;
        color: #000;
        box-shadow: 0 0 15px rgba(0, 255, 127, 0.6);
    }

    /* TELA DE SUCESSO */

    .success-content {
        align-items: center;
        justify-content: center;
    }

    .success-icon {
        width: 70px;
        height: 70px;
        border-radius: 50%;
        background: rgba(0, 255, 127, 0.1);
        border: 2px solid #00ff7f;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 10px;
        box-shadow: 0 0 20px rgba(0, 255, 127, 0.4);
    }
`;

export default ForgotPasswordModal;