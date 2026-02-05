import React, { useState } from 'react';
import styled, { keyframes } from 'styled-components';

export default function ModalPix({ pixData, onClose }) {
    const [copied, setCopied] = useState(false);

    const handleCopy = () => {
        navigator.clipboard.writeText(pixData.copyPaste);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    return (
        <div className="modal" onClick={onClose}>
            <StyledWrapper onClick={(e) => e.stopPropagation()}>
                <div className="pix-container">

                    {/* Header */}
                    <div className="pix-header">
                        <h3>💳 Pagamento PIX</h3>
                        <button className="close-btn" onClick={onClose}>&times;</button>
                    </div>

                    {/* QR Code */}
                    <div className="qr-section">
                        <p className="instruction">Escaneie o QR Code com seu banco:</p>
                        <div className="qr-code-wrapper">
                            <img
                                src={`data:image/png;base64,${pixData.qrCode}`}
                                alt="QR Code PIX"
                                className="qr-code-image"
                            />
                        </div>
                    </div>

                    {/* Código Pix Copia e Cola */}
                    <div className="copy-section">
                        <p className="instruction">Ou copie o código PIX:</p>
                        <div className="code-box">
                            <input
                                type="text"
                                readOnly
                                value={pixData.copyPaste}
                                className="pix-code"
                            />
                            <button
                                className={`copy-btn ${copied ? 'copied' : ''}`}
                                onClick={handleCopy}
                            >
                                {copied ? '✓ Copiado!' : '📋 Copiar'}
                            </button>
                        </div>
                    </div>

                    {/* Valor */}
                    <div className="value-section">
                        <span className="label">Valor a pagar:</span>
                        <span className="value">R$ {pixData.value.toFixed(2)}</span>
                    </div>

                    {/* Aviso */}
                    <div className="warning-box">
                        <p>⏱️ Após o pagamento, sua reserva será confirmada automaticamente em até 2 minutos.</p>
                    </div>

                    {/* Link da fatura (opcional) */}
                    {pixData.invoiceUrl && (
                        <a
                            href={pixData.invoiceUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="invoice-link"
                        >
                            Ver fatura completa →
                        </a>
                    )}

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
    .pix-container {
        width: 100%;
        max-width: 450px;
        background: linear-gradient(#212121, #212121) padding-box,
                    linear-gradient(145deg, transparent 35%, #4ade80, #062904) border-box;
        border: 2px solid transparent;
        padding: 32px 24px;
        border-radius: 16px;
        background-size: 200% 100%;
        animation: ${gradient} 5s ease infinite;
        display: flex;
        flex-direction: column;
        gap: 20px;
    }

    .pix-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-bottom: 1px solid #333;
        padding-bottom: 15px;
    }

    .pix-header h3 {
        color: #fff;
        font-size: 1.3rem;
        margin: 0;
    }

    .close-btn {
        background: transparent;
        border: none;
        color: #717171;
        font-size: 1.8rem;
        cursor: pointer;
        transition: color 0.3s;
    }
    .close-btn:hover { color: #4ade80; }

    .instruction {
        color: #9e9e9e;
        font-size: 0.9rem;
        margin-bottom: 10px;
        text-align: center;
    }

    .qr-section {
        display: flex;
        flex-direction: column;
        align-items: center;
    }

    .qr-code-wrapper {
        background: #fff;
        padding: 15px;
        border-radius: 12px;
        box-shadow: 0 0 20px rgba(74, 222, 128, 0.2);
    }

    .qr-code-image {
        width: 200px;
        height: 200px;
        display: block;
    }

    .copy-section {
        display: flex;
        flex-direction: column;
        gap: 10px;
    }

    .code-box {
        display: flex;
        gap: 10px;
    }

    .pix-code {
        flex: 1;
        padding: 10px;
        background: rgba(255, 255, 255, 0.05);
        border: 1px solid #414141;
        border-radius: 8px;
        color: #fff;
        font-family: 'Courier New', monospace;
        font-size: 0.85rem;
    }

    .copy-btn {
        padding: 10px 20px;
        background: #313131;
        border: 1px solid #414141;
        border-radius: 8px;
        color: #ccc;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.3s;
        white-space: nowrap;
    }

    .copy-btn:hover {
        background: #4ade80;
        color: #000;
        border-color: #4ade80;
    }

    .copy-btn.copied {
        background: #4ade80;
        color: #000;
        border-color: #4ade80;
    }

    .value-section {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 15px;
        background: rgba(74, 222, 128, 0.1);
        border-radius: 8px;
        border: 1px solid rgba(74, 222, 128, 0.3);
    }

    .value-section .label {
        color: #9e9e9e;
        font-size: 0.9rem;
    }

    .value-section .value {
        color: #4ade80;
        font-size: 1.5rem;
        font-weight: bold;
    }

    .warning-box {
        background: rgba(255, 193, 7, 0.1);
        border: 1px solid rgba(255, 193, 7, 0.3);
        border-radius: 8px;
        padding: 12px;
    }

    .warning-box p {
        color: #ffc107;
        font-size: 0.85rem;
        margin: 0;
        text-align: center;
    }

    .invoice-link {
        color: #4ade80;
        text-decoration: none;
        text-align: center;
        font-size: 0.9rem;
        transition: opacity 0.3s;
    }
    .invoice-link:hover { opacity: 0.8; }
`;