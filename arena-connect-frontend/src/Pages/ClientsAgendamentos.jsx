import ClientHeader from "../Components/clientHeader.jsx";
import ClientNav from "../Components/clientNav.jsx"
import {useState} from "react";

import "../Styles/ClientsAgendamentos.css"

export default function ClientAgendamentos(){
    const [filterType, setFilterType] = useState('upcoming');



    return (
        <div className="client-body">
            <div className="liquid-background-fixed">
                <div className="blob blob-1"></div>
                <div className="blob blob-2"></div>
            </div>

            <ClientHeader />

            <main className="client-content">

                {/* Filter Tabs */}
                <div className="categories-scroll" style={{ marginBottom: '20px' }}>
                    <button className={`category-pill ${filterType === 'upcoming' ? 'active' : ''}`}
                            onClick={() => setFilterType('upcoming')}>
                        Próximos Jogos
                    </button>
                    <button className={`category-pill ${filterType === 'upcoming' ? 'active' : ''}`}
                            onClick={() => setFilterType('upcoming')}>
                        Histórico
                    </button>
                </div>

                <div className="arenas-list">

                    {/* CARD 1: Exemplo de Confirmado */}
                    <div className="arena-card glass-panel booking-card">
                        <div className="liquid-glow"></div>

                        {/* Linha do Topo: Data e Status */}
                        <div className="booking-header-row">
                            <div className="booking-time-group">
                                <div className="date-box">
                                    <span className="date-label">Data</span>
                                    <span className="date-value">15/11</span>
                                </div>
                                <div className="time-info">
                                    <h4>19:00</h4>
                                    <span>até 20:00</span>
                                </div>
                            </div>

                            <span className="status-badge status-confirmed">
                            Confirmado
                        </span>
                        </div>

                        {/* Corpo: Infos da Arena */}
                        <div className="booking-body">
                            <h4>Arena Central</h4>
                            <p>Quadra 1 (Sintética)</p>
                            <p className="booking-address">Rua das Flores, 123</p>
                        </div>

                        {/* Rodapé: Preço e Botão */}
                        <div className="booking-footer">
                        <span className="price-tag">
                            Valor: <strong>R$ 120,00</strong>
                        </span>

                            <button className="btn-cancel">
                                Cancelar
                            </button>
                        </div>
                    </div>

                    {/* CARD 2: Exemplo de Pendente */}
                    <div className="arena-card glass-panel booking-card">
                        <div className="liquid-glow"></div>

                        <div className="booking-header-row">
                            <div className="booking-time-group">
                                <div className="date-box">
                                    <span className="date-label">Data</span>
                                    <span className="date-value">20/11</span>
                                </div>
                                <div className="time-info">
                                    <h4>18:00</h4>
                                    <span>até 19:30</span>
                                </div>
                            </div>

                            <span className="status-badge status-pending">
                            Pendente
                        </span>
                        </div>

                        <div className="booking-body">
                            <h4>Arena Power</h4>
                            <p>Quadra Areia 2</p>
                            <p className="booking-address">Av. do Estado, 500</p>
                        </div>

                        <div className="booking-footer">
                        <span className="price-tag">
                            Valor: <strong>R$ 90,00</strong>
                        </span>

                            <button className="btn-cancel">
                                Cancelar
                            </button>
                        </div>
                    </div>

                </div>

                <div style={{ height: '100px' }}></div>
            </main>

            <ClientNav active="agendamentos" />
        </div>
    );
}