import {useState, useEffect, useRef} from "react";
import '../Styles/HomeClient.css'
import axios from "axios";
import ModalBooking from "../Components/ModalBooking.jsx";
import ClientHeader from "../Components/clientHeader.jsx";
import ClientNav from "../Components/clientNav.jsx"

function getDistanceFromLatLonInKm(lat1, lon1, lat2, lon2) {
    const R = 6371;
    const dLat = deg2rad(lat2 - lat1);
    const dLon = deg2rad(lon2 - lon1);
    const a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    const d = R * c;
    return d;
}
function deg2rad(deg) {
    return deg * (Math.PI / 180);
}

export default function HomeClient() {
    const [loading, setLoading] = useState(true);
    const [arenas,setArenas] = useState([]);
    const [searchItem,setSearchItem] = useState("");
    const [selectedArena, setSelectedArena] = useState(null);
    const [userLocation, setUserLocation] = useState(null);

    const lastUpdateRef = useRef(0);
    const UPDATE_INTERVAL = 50000;

    useEffect(()=>{
        let watchId = null;

        if("geolocation" in navigator) {
            watchId = navigator.geolocation.watchPosition(
                (position) => {
                    const now = Date.now();

                    if (now - lastUpdateRef.current >= UPDATE_INTERVAL) {
                        setUserLocation({
                            lat: position.coords.latitude,
                            lng: position.coords.longitude
                        });

                        lastUpdateRef.current = now;
                        console.log("📍 Localização atualizada:", position.coords);
                    }
                },
                (error) => {
                    console.error("Erro ao obter localização:", error);
                },
                {
                    enableHighAccuracy: true,
                    timeout: 20000,
                    maximumAge: 0,
                    distanceFilter: 200
                }
            );
        }
        return () => {
            if(watchId !== null) {
                navigator.geolocation.clearWatch(watchId);
            }
        }
    },[]);

    const findArenas = async () =>{
        try{
            const response = await axios.get('http://localhost:8080/api/arena', {
                withCredentials:true
            })
            console.log("Arenas recebidas:", response.data); // Debug
            setArenas(response.data)
        } catch (error) {
            console.error("Erro ao buscar arenas:", error);
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        findArenas();
    },[]);

    // Função que retorna a lista processada (filtrada e ordenada)
    const getSortedArenas = () => {
        // 1. Filtra pela busca
        let filtered = arenas.filter(arena => {
            const name = arena.name || arena.nome || "";
            const city = arena.cidade || "";
            return name.toLowerCase().includes(searchItem.toLowerCase()) ||
                city.toLowerCase().includes(searchItem.toLowerCase());
        });

        // 2. Se não tem localização, ordena por ID (Fallback)
        if (!userLocation) {
            return filtered
                .sort((a, b) => (b.id || 0) - (a.id || 0))
                .slice(0, 10)
                .map(arena => ({
                    ...arena,
                    formattedDistance: "Nova"
                }));
        }

        // 3. Se tem localização, calcula distância
        const arenasWithDist = filtered.map(arena => {
            if (!arena.latitude || !arena.longitude) {
                return { ...arena, formattedDistance: null, rawDistance: Infinity };
            }
            const dist = getDistanceFromLatLonInKm(
                userLocation.lat, userLocation.lng, arena.latitude, arena.longitude
            );

            let formattedString = dist < 1
                ? `${(dist * 1000).toFixed(0)}m`
                : `${dist.toFixed(1)}km`;

            return { ...arena, formattedDistance: formattedString, rawDistance: dist };
        });

        return arenasWithDist.sort((a, b) => a.rawDistance - b.rawDistance);
    };

    // --- CORREÇÃO: Invoca a função aqui ---
    const sortedList = getSortedArenas();

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
                        <input type="text"
                               value={searchItem} onChange={(e) => setSearchItem(e.target.value)} placeholder="Buscar arenas, quadras..."/>
                    </div>
                </div>

                <h3 className="section-title">
                    {userLocation ? "Arenas Próximas" : "Todas as Arenas"}
                </h3>

                <div className="arenas-list">
                    {loading ? (
                        <p style={{color: '#aaa', textAlign: 'center', marginTop: '20px'}}>
                            Carregando arenas...
                        </p>
                    ) : (
                        // --- CORREÇÃO: Usa a lista sortedList calculada ---
                        sortedList.length > 0 ? (
                            sortedList.map((arena) => (
                                <div key={arena.schemaName || arena.id} className="arena-card glass-panel">
                                    <div className="liquid-glow"></div>

                                    <div className="arena-image-container">
                                        <div className="img-placeholder-gradient" />
                                        <div className="arena-badge">
                                            {arena.formattedDistance || '...'}
                                        </div>
                                    </div>

                                    <div className="arena-info">
                                        <div className="arena-header-row">
                                            <h4>{arena.name || arena.nome}</h4>
                                        </div>
                                        <p className="arena-address">
                                            {arena.endereco || 'Endereço não informado'} - {arena.estado || ''}
                                        </p>

                                        <div className="arena-footer-row">
                                            <span></span>
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