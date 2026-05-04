import {useState, useEffect, useRef, useMemo} from "react";
import '../Styles/HomeClient.css'
import axios from "axios";
import ModalBooking from "../Components/ModalBooking.jsx";
import ClientHeader from "../Components/clientHeader.jsx";
import ClientNav from "../Components/clientNav.jsx"
import {ArenaBST} from "../utils/ArenaBST.js";

export default function HomeClient() {
    const [loading, setLoading] = useState(true);
    const [arenas,setArenas] = useState([]);
    const [searchItem,setSearchItem] = useState("");
    const [selectedArena, setSelectedArena] = useState(null);
    const [userLocation, setUserLocation] = useState(null);

    const lastUpdateRef = useRef(0);
    const UPDATE_INTERVAL = 5000000;

    // 🌳 BST para busca local instantânea
    const arenaBSTRef = useRef(ArenaBST.buildBalanced([]));

    // 🌳 Filtragem local instantânea via BST (sem esperar debounce da API)
    const instantResults = useMemo(() => {
        if (!searchItem || searchItem.trim().length < 2) return null; // null = usar arenas da API
        return arenaBSTRef.current.searchByName(searchItem, 12);
    }, [searchItem]);

    useEffect(() => {
        let watchId = null;

        if ("geolocation" in navigator) {
            watchId = navigator.geolocation.watchPosition(
                (position) => {
                    const now = Date.now();
                    if (now - lastUpdateRef.current >= UPDATE_INTERVAL) {
                        const newLocation = {
                            lat: position.coords.latitude,
                            lng: position.coords.longitude
                        };
                        setUserLocation(newLocation);
                        lastUpdateRef.current = now;
                    }
                },
                (error) => {
                    console.warn("GPS indisponível ou negado:", error.message);
                    fetchArenasFromBackend();
                },
                { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
            );
        } else {
            fetchArenasFromBackend();
        }

        return () => { if (watchId !== null) navigator.geolocation.clearWatch(watchId); }
    }, []);


    useEffect(() => {
        if (searchItem) {
            const delayDebounceFn = setTimeout(() => {
                fetchArenasFromBackend();
            }, 500);
            return () => clearTimeout(delayDebounceFn);
        }
        if (userLocation) {
            fetchArenasFromBackend();
        }
    }, [searchItem, userLocation]);

    const fetchArenasFromBackend = async () => {
        setLoading(true);
        try {
            const params = {};
            if (userLocation) {
                params.lat = userLocation.lat;
                params.lon = userLocation.lng;
            }
            if (searchItem) {
                params.search = searchItem;
            }
            const response = await axios.get('http://localhost:8080/api/arena', {
                params: params,
                withCredentials: true
            });

            setArenas(response.data);

            // 🌳 Reconstrói a BST com os novos dados da API
            arenaBSTRef.current = ArenaBST.buildBalanced(response.data);
        } catch (error) {
            if (error.response?.status !== 403) {
                console.error("Erro ao buscar arenas:", error);
            }
        } finally {
            setLoading(false);
        }
    }

    // 🌳 Decide qual lista renderizar: BST instantânea ou resultados da API
    const displayArenas = (instantResults && instantResults.length > 0) ? instantResults : arenas;

    const processedArenas = displayArenas.map(arena => {
        let formattedDist = "Nova";

        if (arena.distanceKm !== null && arena.distanceKm !== undefined) {
            const dist = arena.distanceKm;

            formattedDist =
                dist < 1
                    ? `${Math.round(dist * 1000)}m`
                    : `${dist.toFixed(1)}km`;
        }

        return { ...arena, formattedDistance: formattedDist };
    });

    return (
        <div className="client-body">
            <div className="client-background-fixed">
                <div className="client-blob client-blob-1"></div>
                <div className="client-blob client-blob-2"></div>
            </div>

            <ClientHeader />

            <main className="client-content">
                <div className="search-section">
                    <div className="search-bar glass-panel">
                        <svg className="search-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <circle cx="11" cy="11" r="8"></circle>
                            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                        </svg>
                        <input
                            type="text"
                            value={searchItem}
                            onChange={(e) => setSearchItem(e.target.value)}
                            placeholder="Buscar arenas, quadras..."
                        />
                    </div>
                </div>

                <h3 className="section-title">
                    {userLocation ? "Arenas Próximas " : "Últimas Adicionadas"}
                </h3>

                <div className="home-arenas-list">
                    {loading ? (
                        <p style={{color: '#aaa', textAlign: 'center', marginTop: '20px'}}>
                            Carregando arenas...
                        </p>
                    ) : (
                        processedArenas.length > 0 ? (
                            processedArenas.map((arena) => (
                                <div key={arena.id} className="arena-card-compact glass-panel">

                                    {/* Lado Esquerdo: Ícone SVG com fundo arredondado */}
                                    <div className="arena-icon-wrapper">
                                        <img
                                            src="/Assets/stadium-svgrepo-com.svg"
                                            alt="arena logo"
                                            className="arena-icon-svg"
                                        />
                                    </div>

                                    {/* Meio: Informações e Distância */}
                                    <div className="arena-info-compact">
                                        <div className="arena-header-compact">
                                            <h4 title={arena.name || arena.nome}>{arena.name || arena.nome}</h4>
                                            <span className="distance-badge-compact">
                                                {arena.formattedDistance}
                                            </span>
                                        </div>
                                        <p className="arena-address-compact">
                                            {arena.endereco || 'Endereço não informado'} • {arena.cidade || ''}
                                        </p>
                                    </div>

                                    {/* Lado Direito: Botão Agendar */}
                                    <div className="arena-action-compact">
                                        <button className="btn-book-mini" onClick={() => setSelectedArena(arena)}>
                                            Agendar
                                        </button>
                                    </div>

                                </div>
                            ))
                        ): (
                            <div style={{textAlign: 'center', padding: '20px', color: '#aaa'}}>
                                <p>Nenhuma arena encontrada para "{searchItem}"</p>
                            </div>
                        )
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