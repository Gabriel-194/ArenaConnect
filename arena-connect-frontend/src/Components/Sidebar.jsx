import {Link, useNavigate} from "react-router-dom";
import axios from "axios";
import {useLocation} from "react-router-dom";


export default function Sidebar(){
    const navigate = useNavigate();
    const location = useLocation();

    const handleLogout = async (e) => {
        e.preventDefault();
        try {
            await axios.post(
                'http://localhost:8080/api/auth/logout',
                {},
                {
                    withCredentials: true
                }
            );
            localStorage.removeItem('userName');
        } catch (error) {
            console.error('Logout error:', error);
        } finally {
            navigate('/login');
        }
    }

    const isActive = (path) => location.pathname === path ? 'active' : '';

    return (
        <aside className="sidebar">
            <div className="sidebar-header">
                <a href="/home" className="logo-compact" style={{ textDecoration: 'none', color: 'inherit' }}>
                    <img src="/Assets/3-removebg-preview.png" alt="Voltar ao Início" width="100" height="100"/>
                    <span style={{fontFamily: "'Racing Sans One', cursive", fontSize: '25px'}}>
                        Arena Connect
                    </span>
                </a>
            </div>

            <nav className="sidebar-nav">
                <Link to="/dashboard" className="nav-item">
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                        <path d="M3 4a1 1 0 011-1h12a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1V4zM3 10a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H4a1 1 0 01-1-1v-6zM14 9a1 1 0 00-1 1v6a1 1 0 001 1h2a1 1 0 001-1v-6a1 1 0 00-1-1h-2z" />
                    </svg>
                    <span>Dashboard</span>
                </Link>

                <Link to="/agendamentos" className="nav-item">
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                        <path d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z" />
                    </svg>
                    <span>Agendamentos</span>
                </Link>

                <Link to="/campeonatos" className="nav-item">
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                        <path d="M10 3.5a1.5 1.5 0 013 0V4a1 1 0 001 1h3a1 1 0 011 1v3a1 1 0 01-1 1h-.5a1.5 1.5 0 000 3h.5a1 1 0 011 1v3a1 1 0 01-1 1h-3a1 1 0 01-1-1v-.5a1.5 1.5 0 00-3 0v.5a1 1 0 01-1 1H6a1 1 0 01-1-1v-3a1 1 0 00-1-1h-.5a1.5 1.5 0 010-3H4a1 1 0 001-1V6a1 1 0 011-1h3a1 1 0 001-1v-.5z" />
                    </svg>
                    <span>Campeonatos</span>
                </Link>

                <Link to="/quadras" className={`nav-item ${isActive('/quadras')}`}>
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <rect x="2" y="3" width="20" height="14" rx="2" ry="2" />
                        <line x1="8" y1="21" x2="16" y2="21" />
                        <line x1="12" y1="17" x2="12" y2="21" />
                    </svg>
                    <span>Quadras</span>
                </Link>

                <Link to="/times" className={`nav-item ${isActive('/times')}`}>
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                        <circle cx="9" cy="7" r="4"></circle>
                        <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                        <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                    </svg>
                    <span>Times</span>
                </Link>
            </nav>

            <div className="sidebar-footer">
                <button className="btn-logout" onClick={handleLogout}>
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                        <path fillRule="evenodd" clipRule="evenodd" d="M3 3a1 1 0 00-1 1v12a1 1 0 001 1h12a1 1 0 001-1V4a1 1 0 00-1-1H3zm11 4.414l-4.293 4.293a1 1 0 01-1.414 0L4 7.414 5.414 6l3.293 3.293L13.586 5 15 6.414z"/>
                    </svg>
                    Sair
                </button>
            </div>
        </aside>
    );
}