import { useState, useMemo, useRef } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';

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

const maskCPF = (value) => {
    return value
        .replace(/\D/g, "")
        .replace(/(\d{3})(\d)/, "$1.$2")
        .replace(/(\d{3})(\d)/, "$1.$2")
        .replace(/(\d{3})(\d{1,2})/, "$1-$2")
        .replace(/(-\d{2})\d+?$/, "$1");
};

const maskCNPJ = (value) => {
    return value
        .replace(/\D/g, "")
        .replace(/^(\d{2})(\d)/, "$1.$2")
        .replace(/^(\d{2})\.(\d{3})(\d)/, "$1.$2.$3")
        .replace(/\.(\d{3})(\d)/, ".$1/$2")
        .replace(/(\d{4})(\d)/, "$1-$2")
        .replace(/(-\d{2})\d+?$/, "$1");
};

const maskPhone = (value) => {
    return value
        .replace(/\D/g, "")
        .replace(/(\d{2})(\d)/, "($1) $2")
        .replace(/(\d{5})(\d)/, "$1-$2")
        .replace(/(-\d{4})\d+?$/, "$1");
};

const maskCEP = (value) => {
    return value
        .replace(/\D/g, "")
        .replace(/^(\d{5})(\d)/, "$1-$2")
        .replace(/(-\d{3})\d+?$/, "$1");
};

export default function ModalPartners({onClose, googleData}) {
    const [nomeUser, setNomeUser] = useState(googleData ? googleData.name : '');
    const [emailAdmin, setEmailAdmin] = useState(googleData ? googleData.email : '');
    const [cpfUser, setCpf] = useState('');
    const [telefoneUser, setTelefone] = useState('');
    const [senhaAdmin, setSenha] = useState('');
    const [confirmarSenha, setConfirmarSenha] = useState('');

    const [nameArena, setNameArena] = useState('');
    const [cnpjArena, setCnpjArena] = useState('');
    const [cepArena, setCepArena] = useState('');
    const [enderecoArena, setEnderecoArena] = useState('');
    const [bairroArena, setBairroArena] = useState('');
    const [cidadeArena, setCidadeArena] = useState('');
    const [estadoArena, setEstadoArena] = useState('');
    const [numeroArena, setNumeroArena] = useState('');
    const [error, setErro] = useState('');

    const [latitude, setLatitude] = useState(-25.4290);
    const [longitude, setLongitude] = useState(-49.2671);
    const markerRef = useRef(null);

    const navigate = useNavigate();

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

    const fetchCoordinates = async (logradouro, numero,bairro, cidade, estado) => {
        try {
            const query = `${logradouro}, ${numero}, ${bairro}, ${cidade} - ${estado}, Brasil`;
            const response = await axios.get(
                "https://nominatim.openstreetmap.org/search",
                {
                    params: { q: query, format: "json", limit: 1 },
//                     headers: { "User-Agent": "ArenaConnect/1.0" }
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

            const { logradouro, localidade, uf,bairro } = res.data;
            setEnderecoArena(logradouro || '');
            setCidadeArena(localidade);
            setEstadoArena(uf);
            setBairroArena(bairro || '');

            await fetchCoordinates(logradouro, "", localidade, uf);

            document.getElementById('input-numero').focus();
        } catch (err) {
            console.error("Erro ao buscar CEP", err);
        } finally {
            document.getElementById('cep-status').style.display = 'none';
        }
    };

    const handlePartnerRegister = async (e) => {
        e.preventDefault();
        setErro('');

        const enderecoCompleto = `${enderecoArena}, ${numeroArena}, ${bairroArena}`;
        try {
            const response = await axios.post("http://localhost:8080/api/users/register-partner", {
                nomeUser,
                emailAdmin,
                cpfUser: cpfUser.replace(/\D/g, ""),
                telefoneUser: telefoneUser.replace(/\D/g, ""),
                senhaAdmin,
                confirmarSenha,
                nomeArena: nameArena,
                cnpjArena: cnpjArena.replace(/\D/g, ""),
                cepArena: cepArena.replace(/\D/g, ""),
                enderecoArena: enderecoCompleto,
                cidadeArena,
                estadoArena,
                latitude,
                longitude
            });

            if (response.status === 200 && response.data.success) {
                alert(response.data.message || 'Cadastrado com sucesso!');
                onClose();
                navigate("/login");
            } else {
                setErro("Dados inválidos");
            }
        } catch (err) {
            setErro(err.response?.data?.message || "Erro inesperado ao registrar");
        }
    };

    return (
        <div className="modal active">
            <div className="modal-content modal-wide" style={{ maxWidth: '850px' }}>
                <div className="modal-header">
                    <h2>Cadastro de Parceiro</h2>
                    <button className="modal-close" type="button" onClick={onClose}>&times;</button>
                </div>

                <form onSubmit={handlePartnerRegister} className="form form-grid">
                    <h3 className="col-span-2 section-title">Seus Dados Pessoais</h3>

                    <div className="form-group col-span-2">
                        <label>Nome Completo *</label>
                        <input type="text" required value={nomeUser} onChange={(e) => setNomeUser(e.target.value)} disabled={!!googleData}/>
                    </div>

                    <div className="form-group">
                        <label>CPF *</label>
                        <input type="text" required maxLength="14" value={cpfUser} onChange={(e)=> setCpf(maskCPF(e.target.value))}/>
                    </div>

                    <div className="form-group">
                        <label>Celular / WhatsApp</label>
                        <input type="tel" maxLength="15" value={telefoneUser} onChange={(e)=> setTelefone(maskPhone(e.target.value))}/>
                    </div>

                    <div className="form-group col-span-2">
                        <label>E-mail de Login *</label>
                        <input type="email" required value={emailAdmin} onChange={(e) => setEmailAdmin(e.target.value)} disabled={!!googleData}/>
                    </div>

                    {!googleData && (
                        <>
                            <div className="form-group">
                                <label>Senha *</label>
                                <input type="password" placeholder="••••••••" required minLength="6" value={senhaAdmin}
                                       onChange={(e) => setSenha(e.target.value)}/>
                            </div>
                            <div className="form-group">
                                <label>Confirmar Senha *</label>
                                <input type="password" name="confirmPassword" placeholder="••••••••" required minLength="6"
                                       value={confirmarSenha}
                                       onChange={(e) => setConfirmarSenha(e.target.value)}/>
                            </div>
                        </>

                    )}

                    <hr className="col-span-2 separator"/>
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
                        <label>Endereço</label>
                        <input type="text" readOnly className="input-readonly" value={enderecoArena}/>
                    </div>

                    <div style={{ display: 'flex', gap: '15px', gridColumn: 'span 2' }}>
                        <div className="form-group" style={{ width: '80px' }}>
                            <label>UF</label>
                            <input type="text" readOnly className="input-readonly" value={estadoArena}/>
                        </div>
                        <div className="form-group" style={{ flex: 1 }}>
                            <label>Número *</label>
                            <input id="input-numero" type="text" required value={numeroArena} onChange={(e) => setNumeroArena(e.target.value)}
                                   onBlur={() => fetchCoordinates(enderecoArena, numeroArena, cidadeArena, estadoArena,bairroArena)}/>
                        </div>
                    </div>

                    <h3 className="col-span-2 section-title">Confirme a localização no mapa para evitar erros.</h3>
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
                        <p style={{ fontSize: '11px', color: '#666' }}>* Ajuste a posição exata arrastando o marcador azul.</p>
                    </div>

                    <div className="modal-actions col-span-2">
                        {error && <p style={{ color: "red", fontSize: '14px' }}>{error}</p>}
                        <button type="button" className="btn-secondary" onClick={onClose}>Cancelar</button>
                        <button type="submit" className="btn-primary">Finalizar Cadastro</button>
                    </div>
                </form>
            </div>
        </div>
    );
}