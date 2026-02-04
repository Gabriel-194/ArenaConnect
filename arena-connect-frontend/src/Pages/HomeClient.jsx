import {useState, useEffect, useRef} from "react";
import '../Styles/HomeClient.css'
import axios from "axios";
import ModalBooking from "../Components/ModalBooking.jsx";
import ClientHeader from "../Components/clientHeader.jsx";
import ClientNav from "../Components/clientNav.jsx"
/*
function getDistanceFromLatLonInKm(lat1, lon1, lat2, lon2) {
    const R = 6371;
    const dLat = deg2rad(lat2 - lat1);
    const dLon = deg2rad(lon2 - lon1);
    const a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));


    const rawDist = R * c;
    let factor = 1;

    if (rawDist <= 2.0) {
        factor = 1.7;
    }
    return rawDist * factor;
}
function deg2rad(deg) {
    return deg * (Math.PI / 180);
}
*/
export default function HomeClient() {
    const [loading, setLoading] = useState(true);
    const [arenas,setArenas] = useState([]);
    const [searchItem,setSearchItem] = useState("");
    const [selectedArena, setSelectedArena] = useState(null);
    const [userLocation, setUserLocation] = useState(null);

    const lastUpdateRef = useRef(0);
    const UPDATE_INTERVAL = 500000;

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
                        console.log("📍 Localização atualizada:", newLocation);
                    }
                },
                (error) => console.error("Erro GPS:", error),
                { enableHighAccuracy: true, timeout: 20000, maximumAge: 0, distanceFilter: 200 }
            );
        }
        return () => { if (watchId !== null) navigator.geolocation.clearWatch(watchId); }
    }, []);


    useEffect(() => {
        const delayDebounceFn = setTimeout(() => {
            fetchArenasFromBackend();
        }, 500);

        return () => clearTimeout(delayDebounceFn);
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
        } catch (error) {
            console.error("Erro ao buscar arenas:", error);
        } finally {
            setLoading(false);
        }
    }

    const processedArenas = arenas.map(arena => {
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

                <div className="arenas-list">
                    {loading ? (
                        <p style={{color: '#aaa', textAlign: 'center', marginTop: '20px'}}>
                            Carregando arenas...
                        </p>
                    ) : (
                        processedArenas.length > 0 ? (
                            processedArenas.map((arena) => (
                                <div key={arena.id} className="arena-card glass-panel">
                                    <div className="liquid-glow"></div>

                                    <div className="arena-image-container">
                                        <div className="img-placeholder-gradient" />
                                        <div className="arena-badge">
                                            {arena.formattedDistance}
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