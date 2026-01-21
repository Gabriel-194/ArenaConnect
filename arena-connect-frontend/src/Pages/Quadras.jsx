import {useState,useEffect} from 'react'
import axios from "axios";

import '../Styles/quadras.css'

import ModalCourts from '../Components/ModalCourts.jsx'
import Sidebar from "../Components/Sidebar.jsx";

export default function Quadras () {
    const [showModalCourts,setModalCourts] = useState(false);
    const [quadras, setQuadras] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        findCourts();
    },[]);

    const findCourts = async () => {
        try{
            const response = await axios.get('http://localhost:8080/quadra', {
                withCredentials:true
            })
            setQuadras(response.data)
        } catch (error) {
            console.error("Erro ao buscar quadras:", error);
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className={'dashboard-container'}>
            <Sidebar />

            <main className="main-content">
                <header className="page-header">
                    <div>
                        <h2>Quadras</h2>
                        <p className="page-subtitle">
                            Gerencie suas quadras esportivas
                        </p>
                    </div>

                    <div className="header-actions">
                        <button className="btn-primary" onClick={() => setModalCourts(true)}>
                            + Nova Quadra
                        </button>
                    </div>
                </header>

                <div className="courts-grid">
                    {loading ? (<p>Carregando Quadras...</p>) : quadras.length === 0 ? (<p>Nenhuma quadra cadastrada.</p>) : (
                        quadras.map((quadra) => (
                            <div key={quadra.id} className="court-card">
                                <div className={`court-status ${quadra.ativo ? 'success' : 'danger'}`}></div>

                                <h3 className="court-name">{quadra.nome}</h3>

                                <div className="court-info">
                                    <div className="info-row">
                                        <span className="info-label">Tipo</span>
                                        <span className="info-value">{quadra.tipo_quadra}</span>
                                    </div>
                                    <div className="info-row">
                                        <span className="info-label">Valor/Hora</span>
                                        <span className="info-value">
                                            {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(quadra.valor_hora)}
                                        </span>
                                    </div>
                                    <div className="info-row">
                                        <span className="info-label">Status</span>
                                        <span className="info-value">
                                            {quadra.ativo ? 'Disponível' : 'Manutenção'}
                                        </span>
                                    </div>
                                </div>

                                <div className="court-actions">
                                    <button className="btn-secondary">
                                        Editar
                                    </button>

                                    <button
                                        className={quadra.ativo ? "btn-danger" : "btn-success"}
                                    >
                                        {quadra.ativo ? 'Desativar' : 'Ativar'}
                                    </button>
                                </div>
                            </div>
                        ))
                    )}
                </div>
            </main>

            {showModalCourts && (
                <ModalCourts onClose={() => {setModalCourts(false)}}
                             onSuccess={findCourts}
                />
            )}
        </div>
    );
}