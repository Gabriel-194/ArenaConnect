import { useState, useMemo, useRef, useEffect } from 'react';
import axios from 'axios';
import { MapContainer, TileLayer, Marker, useMapEvents } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';

import markerIcon from 'leaflet/dist/images/marker-icon.png';
import markerShadow from 'leaflet/dist/images/marker-shadow.png';

let DefaultIcon = L.icon({
    iconUrl: markerIcon,
    shadowUrl: markerShadow,
    iconSize: [25, 41],
    iconAnchor: [12, 41]
});
L.Marker.prototype.options.icon = DefaultIcon;

function ChangeView({ center }) {
    const map = useMapEvents({});
    map.setView(center, 16);
    return null;
}

const maskCNPJ = (value) => {
    return value
        .replace(/\D/g, "")
        .replace(/^(\d{2})(\d)/, "$1.$2")
        .replace(/^(\d{2})\.(\d{3})(\d)/, "$1.$2.$3")
        .replace(/\.(\d{3})(\d)/, ".$1/$2")
        .replace(/(\d{4})(\d)/, "$1-$2")
        .replace(/(-\d{2})\d+?$/, "$1");
};

const maskCEP = (value) => {
    return value
        .replace(/\D/g, "")
        .replace(/^(\d{5})(\d)/, "$1-$2")
        .replace(/(-\d{3})\d+?$/, "$1");
};

export default function ModalArena({ onClose, arenaToEdit, onSuccess }) {
    const [nameArena, setNameArena] = useState('');
    const [cnpjArena, setCnpjArena] = useState('');
    const [cepArena, setCepArena] = useState('');
    const [enderecoArena, setEnderecoArena] = useState('');
    const [cidadeArena, setCidadeArena] = useState('');
    const [estadoArena, setEstadoArena] = useState('');

    const [latitude, setLatitude] = useState(-25.4290);
    const [longitude, setLongitude] = useState(-49.2671);
    const markerRef = useRef(null);
    const [error, setErro] = useState('');

    useEffect(() => {
        if(arenaToEdit){
            setNameArena(arenaToEdit.nome || arenaToEdit.name || '');
            setCnpjArena(maskCNPJ(arenaToEdit.cnpj || ''));
            setCepArena(maskCEP(arenaToEdit.cep || ''));
            setEnderecoArena(arenaToEdit.endereco || '');
            setCidadeArena(arenaToEdit.cidade || '');
            setEstadoArena(arenaToEdit.estado || '');

            if (arenaToEdit.latitude && arenaToEdit.longitude) {
                setLatitude(arenaToEdit.latitude);
                setLongitude(arenaToEdit.longitude);
            }
        }

    }, [arenaToEdit]);

    const eventHandlers = useMemo(
        () => ({
            dragend() {
                const marker = markerRef.current;
                if (marker != null) {
                    const { lat, lng } = marker.getLatLng();
                    setLatitude(lat);
                    setLongitude(lng);
                }
            },
        }),
        [],
    );

    const fetchCoordinates = async (endereco, cidade, estado) => {
        if (!endereco || !cidade || !estado) return false;

        try {
            const query = `${endereco}, ${cidade} - ${estado}, Brasil`;
            const response = await axios.get(
                "https://nominatim.openstreetmap.org/search",
                {
                    params: { q: query, format: "json", limit: 1 },
                }
            );

            if (response.data?.length > 0) {
                const { lat, lon } = response.data[0];
                setLatitude(parseFloat(lat));
                setLongitude(parseFloat(lon));
                return true;
            }
            return false;
        } catch (error) {
            console.error("Erro ao buscar coordenadas:", error);
            return false;
        }
    };

    const checkCEP = async (e) => {
        const cep = e.target.value.replace(/\D/g, '');
        if (cep.length !== 8) return;

        try {
            document.getElementById('cep-status').style.display = 'block';
            const res = await axios.get(`https://viacep.com.br/ws/${cep}/json/`);

            if (res.data.erro) {
                alert('CEP não encontrado!');
                return;
            }

            const { logradouro, localidade, uf, bairro } = res.data;
            setEnderecoArena(`${logradouro}, Número, ${bairro}`); // Sugere o formato
            setCidadeArena(localidade);
            setEstadoArena(uf);

            await fetchCoordinates(logradouro, localidade, uf);

        } catch (err) {
            console.error("Erro ao buscar CEP", err);
        } finally {
            document.getElementById('cep-status').style.display = 'none';
        }
    };

    const handleSaveArena = async (e) => {
        e.preventDefault();
        setErro('');

        try {
            const payload = {
                name: nameArena,
                cnpj: cnpjArena.replace(/\D/g, ""),
                cep: cepArena.replace(/\D/g, ""),
                endereco: enderecoArena,
                cidade: cidadeArena,
                estado: estadoArena,
                latitude: latitude,
                longitude: longitude
            };

            const response = await axios.put(`http://localhost:8080/api/arena/${arenaToEdit.idArena || arenaToEdit.id}`, payload, {
                withCredentials: true
            });

            if (response.status === 200) {
                alert("Arena atualizada com sucesso!");
                if (onSuccess) onSuccess();
                onClose();
            }

        } catch (err) {
            if (err.response && err.response.data?.message) {
                setErro(err.response.data.message);
            } else {
                setErro("Erro inesperado ao atualizar a arena.");
            }
        }
    }

    return (
        <div className="modal active">
            <div className="modal-content modal-wide" style={{ maxWidth: '850px' }}>
                <div className="modal-header">
                    <h2>Editar Arena</h2>
                    <button className="modal-close" type="button" onClick={onClose}>&times;</button>
                </div>

                <form onSubmit={handleSaveArena} className="form form-grid">

                    <h3 className="col-span-2 section-title">Dados do Complexo e Localização</h3>

                    <div className="form-group col-span-2">
                        <label>Nome da Arena *</label>
                        <input type="text" required value={nameArena} onChange={(e)=>setNameArena(e.target.value)}/>
                    </div>

                    <div className="form-group">
                        <label>CNPJ *</label>
                        <input type="text" required maxLength="18" value={cnpjArena} onChange={(e) =>setCnpjArena(maskCNPJ(e.target.value))} />
                    </div>

                    <div className="form-group">
                        <label>CEP *</label>
                        <input type="text" required maxLength="9" value={cepArena} onChange={(e)=>setCepArena(maskCEP(e.target.value))} onBlur={checkCEP}/>
                        <small id="cep-status" style={{color: "green", display: "none"}}>Buscando...</small>
                    </div>

                    <div className="form-group col-span-2">
                        <label>Endereço Completo</label>
                        <input type="text" required placeholder="Rua, Número, Bairro" value={enderecoArena} onChange={(e) => setEnderecoArena(e.target.value)}/>
                    </div>

                    <div className="form-group">
                        <label>Cidade</label>
                        <input type="text" required value={cidadeArena} onChange={(e) => setCidadeArena(e.target.value)}/>
                    </div>

                    <div className="form-group">
                        <label>UF</label>
                        <input type="text" required maxLength="2" value={estadoArena} onChange={(e) => setEstadoArena(e.target.value.toUpperCase())}/>
                    </div>

                    <h3 className="col-span-2 section-title">Ajuste a localização no mapa se necessário</h3>
                    <div className="col-span-2" style={{ height: '250px', margin: '10px 0', borderRadius: '8px', overflow: 'hidden', border: '1px solid #ddd' }}>
                        <MapContainer center={[latitude, longitude]} zoom={13} style={{ height: '100%', width: '100%' }}>
                            <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
                            <ChangeView center={[latitude, longitude]} />
                            <Marker
                                draggable={true}
                                eventHandlers={eventHandlers}
                                position={[latitude, longitude]}
                                ref={markerRef}
                            />
                        </MapContainer>
                        <p style={{ fontSize: '11px', color: '#aaa', marginTop: '4px' }}>* Ajuste a posição exata arrastando o marcador azul.</p>
                    </div>

                    <div className="modal-actions col-span-2">
                        {error && <p style={{ color: "red", fontSize: '14px' }}>{error}</p>}
                        <button type="button" className="btn-secondary" onClick={onClose}>Cancelar</button>
                        <button type="submit" className="btn-primary">Salvar Alterações</button>
                    </div>
                </form>
            </div>
        </div>
    );
}