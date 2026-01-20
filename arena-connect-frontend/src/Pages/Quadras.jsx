import {useState} from 'react'

import '../Styles/quadras.css'

import ModalCourts from '../Components/ModalCourts.jsx'
import Sidebar from "../Components/Sidebar.jsx";

export default function Quadras () {
    const [showModalCourts,setModalCourts] = useState(false);

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
                    {showModalCourts && (
                        <ModalCourts onClose={() => setModalCourts(false)} />
                    )}

                <div className="courts-grid">
                    <p>Carregando Quadras...</p>
                </div>
            </main>
        </div>

    );

}