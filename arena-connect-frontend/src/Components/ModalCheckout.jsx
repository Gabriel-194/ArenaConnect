import React from 'react';
import '../Styles/ModalCheckout.css';

const ModalCheckout = ({ paymentUrl }) => {
    if (!paymentUrl) return null;

    return (
        <div className="modal-overlay-glass">
            <div className="checkout-blob checkout-blob-1"></div>
            <div className="checkout-blob checkout-blob-2"></div>

            <div className="modal-content-glass">
                <div className="border-glow"></div>

                <div className="glass-header">
                    <h2>Ative sua Arena!</h2>
                    <p>Para liberar o acesso ao sistema, finalize a assinatura.</p>
                </div>

                <div className="payment-container">
                    <div className="plan-card">
                        <h3>Plano Pro</h3>
                        <div className="price">R$ 100,00<span>/mês</span></div>

                        <ul className="benefits">
                            <li>✅ Gestão de Quadras Ilimitada</li>
                            <li>✅ Recebimento de reservas</li>
                            <li>✅ Dashboard Financeiro</li>
                        </ul>

                        <a href={paymentUrl} target="_blank" rel="noopener noreferrer" className="btn-checkout-standard">
                            Pagar Agora 💳
                        </a>
                    </div>

                    <p className="note">Você será redirecionado para o ambiente seguro do Asaas.</p>
                    <p className="refresh-note">
                        Já pagou? <a href="/home" onClick={() => window.location.reload()}>Atualizar página</a>
                    </p>
                </div>
            </div>
        </div>
    );
};

export default ModalCheckout;