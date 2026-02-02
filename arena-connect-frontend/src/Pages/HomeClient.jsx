import {useState,useEffect} from "react";
import '../Styles/HomeClient.css'
import axios from "axios";
import ModalBooking from "../Components/ModalBooking.jsx";
import ClientHeader from "../Components/clientHeader.jsx";
import ClientNav from "../Components/clientNav.jsx"

export default function HomeClient() {
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
            <div className="client-background-fixed">
                <div className="client-blob client-blob-1"></div>
                <div className="client-blob client-blob-2"></div>
            </div>

            <ClientHeader />

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
            <ClientNav active="home" />
        </div>
    );
}
