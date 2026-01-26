import {useState,useEffect} from "react";
import '../Styles/HomeClient.css'
import axios from "axios";
import ModalBooking from "../Components/ModalBooking.jsx";

export default function HomeClient() {
    const username = localStorage.getItem('userName') || 'Atleta';
    const userInitial = username.charAt(0).toUpperCase();
    const [loading, setLoading] = useState(true);
    const [arenas,setArenas] = useState([]);
    const [searchItem,setSearchItem] = useState("");
    const [selectedArena, setSelectedArena] = useState(null);

    const filteredArenas = arenas.filter(arena => {

        const name = arena.name || arena.nome || "";
        const city = arena.cidade || "";

        return name.toLowerCase().includes(searchItem.toLowerCase()) ||
            city.toLowerCase().includes(searchItem.toLowerCase());
    });
    useEffect(() => {
        findArenas();
    },[]);

    const findArenas = async () =>{
        try{
            const response = await axios.get('http://localhost:8080/api/arena', {
                withCredentials:true
            })
            setArenas(response.data)
        } catch (error) {
            console.error("Erro ao buscar arenas:", error);
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="client-body">
            <div className="liquid-background-fixed">
                <div className="blob blob-1"></div>
                <div className="blob blob-2"></div>
            </div>

            <header className="client-header glass-panel">
                <div className="header-user">
                    <div className="user-avatar">
                        <span>{userInitial}</span>
                    </div>
                    <div className="user-greeting">
                        <p>Olá, {username}</p>
                        <h3 style={{ fontFamily: 'inherit'}}>Bora jogar hoje? </h3>
                    </div>
                </div>
                <button className="btn-icon-glass">
                    {/* Ícone de Notificação */}
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                        <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
                    </svg>
                </button>
            </header>

            <main className="client-content">

                {/* Barra de Busca */}
                <div className="search-section">
                    <div className="search-bar glass-panel">
                        <svg className="search-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <circle cx="11" cy="11" r="8"></circle>
                            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                        </svg>
                        <input type="text"
                            value={searchItem} onChange={(e) => setSearchItem(e.target.value)} placeholder="Buscar arenas, quadras..."/>
                    </div>
                </div>
                <h3 className="section-title">Arenas Próximas</h3>

                <div className="arenas-list">

                    {loading ? (
                        <p style={{color: '#aaa', textAlign: 'center', marginTop: '20px'}}>
                            Carregando arenas...
                        </p>
                    ) : (
                        filteredArenas.length > 0 ? (
                            filteredArenas.map((arena) => (
                                <div key={arena.schemaName} className="arena-card glass-panel">
                                    <div className="liquid-glow"></div>

                                    <div className="arena-image-container">
                                        <div className="img-placeholder-gradient" />
                                        <div className="arena-badge">{arena.distance || '2.5km'}</div>
                                    </div>

                                    <div className="arena-info">
                                        <div className="arena-header-row">
                                            {/* Garante que pega name ou nome */}
                                            <h4>{arena.name || arena.nome}</h4>
                                        </div>
                                        <p className="arena-address">
                                            {arena.endereco || 'Endereço não informado'} - {arena.estado || ''}
                                        </p>

                                        <div className="arena-footer-row">
                                            <span className="price-tag">
                                                A partir de <strong>R$ {arena.price || '80'}</strong>
                                            </span>
                                            <button className="btn-book-mini" onClick={() => setSelectedArena(arena)}>
                                                Agendar
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            ))
                            ): (
                            <div style={{textAlign: 'center', padding: '20px', color: '#aaa'}}>
                                <p>Nenhuma arena encontrada para "{searchItem}"</p>
                            </div>
                        )
                        )}

                        {!loading && arenas.length === 0 && (
                            <p style={{color: '#aaa', textAlign: 'center'}}>Nenhuma arena encontrada.</p>
                        )}
                </div>

                {selectedArena && (
                    <ModalBooking arena={selectedArena} onClose={() => setSelectedArena(null)} />
                )}

                <div style={{ height: '100px' }}></div>
            </main>
            <nav className="bottom-nav glass-panel">
                <a href="#" className="nav-icon-item active">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
                        <polyline points="9 22 9 12 15 12 15 22"></polyline>
                    </svg>
                    <span>Início</span>
                </a>

                <a href="#" className="nav-icon-item">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                        <line x1="16" y1="2" x2="16" y2="6"></line>
                        <line x1="8" y1="2" x2="8" y2="6"></line>
                        <line x1="3" y1="10" x2="21" y2="10"></line>
                    </svg>
                    <span>Agendados</span>
                </a>

                <a href="#" className="nav-icon-item">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                        <circle cx="12" cy="7" r="4"></circle>
                    </svg>
                    <span>Perfil</span>
                </a>
            </nav>
        </div>
    );
}
