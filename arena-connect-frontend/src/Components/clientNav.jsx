import '../Styles/HomeClient.css';
import {Link} from "react-router-dom";

export default function clientNav ({active}){
    return(
        <nav className="bottom-nav glass-panel">
            <Link to="/homeClient" className={`nav-icon-item ${active === 'home' ? 'active' : ''}`}>
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
                    <polyline points="9 22 9 12 15 12 15 22"></polyline>
                </svg>
                <span>Início</span>
            </Link>

            <Link to="/clientsAgendamentos" className={`nav-icon-item ${active === 'agendamentos' ? 'active' : ''}`}>
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                    <line x1="16" y1="2" x2="16" y2="6"></line>
                    <line x1="8" y1="2" x2="8" y2="6"></line>
                    <line x1="3" y1="10" x2="21" y2="10"></line>
                </svg>
                <span>Agendados</span>
            </Link>

            <a href="#" className={`nav-icon-item ${active === 'Perfil' ? 'active' : ''}`}>
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                    <circle cx="12" cy="7" r="4"></circle>
                </svg>
                <span>Perfil</span>
            </a>
        </nav>
    );
}