import {useState,useEffect} from "react";

import Sidebar from "../Components/Sidebar.jsx";
import '../Styles/times.css'
import ModalTeams from "../Components/modalTeams.jsx";

export default function times() {
    const [modalTeams, showModalTeams] = useState(false);

    return (
        <div className="dashboard-container">
            <Sidebar />

            <main className="main-content">
                <header className="page-header">
                    <div>
                        <h2>Gerenciar Times</h2>
                        <p className="page-subtitle">Visualize e administre as equipes registradas.</p>
                    </div>

                    <button
                        className="btn-primary" onClick={(e)=>showModalTeams(true)}>
                        + Nova Equipe
                    </button>
                </header>

                {/* Grid de Cards */}
            {/*    <div className="teams-grid">*/}
            {/*        {teams.map((team) => (*/}
            {/*            <div key={team.id} className={`team-card ${!team.active ? 'inactive' : ''}`}>*/}
            {/*                <div className="card-glass-glow" />*/}

            {/*                <div className="team-header">*/}
            {/*                    <div className="team-icon-placeholder">*/}
            {/*                        {team.name.charAt(0)}*/}
            {/*                    </div>*/}
            {/*                    <div className={`status-badge ${team.active ? 'active' : 'inactive'}`}>*/}
            {/*                        {team.active ? 'Ativo' : 'Inativo'}*/}
            {/*                    </div>*/}
            {/*                </div>*/}

            {/*                <h3 className="team-name">{team.name}</h3>*/}

            {/*                <div className="team-stats">*/}
            {/*                    <div className="stat-item">*/}
            {/*                        <span className="label">Capitão</span>*/}
            {/*                        <span className="value">{team.captain}</span>*/}
            {/*                    </div>*/}
            {/*                    <div className="stat-item">*/}
            {/*                        <span className="label">Jogadores</span>*/}
            {/*                        <span className="value">{team.players}</span>*/}
            {/*                    </div>*/}
            {/*                </div>*/}

            {/*                <div className="team-actions">*/}
            {/*                    <button*/}
            {/*                        className="btn-action edit"*/}
            {/*                        onClick={() => handleEdit(team.id)}*/}
            {/*                    >*/}
            {/*                        Editar*/}
            {/*                    </button>*/}
            {/*                    <button*/}
            {/*                        className="btn-action toggle"*/}
            {/*                        onClick={() => handleDeactivate(team.id)}*/}
            {/*                    >*/}
            {/*                        {team.active ? 'Desativar' : 'Ativar'}*/}
            {/*                    </button>*/}
            {/*                </div>*/}
            {/*            </div>*/}
            {/*        ))}*/}
            {/*    </div>*/}
                {showModalTeams &&(
                    <ModalTeams onClose={()=>{showModalTeams(false)}}/>
                )}
            </main>
        </div>
    );
}