import { useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';

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

export default function ModalPartners({onClose}) {
    const [nomeUser, setNomeUser] = useState('');
    const [emailAdmin, setEmailAdmin] = useState('');
    const [cpfUser, setCpf] = useState('');
    const [telefoneUser, setTelefone] = useState('');
    const [senhaAdmin, setSenha] = useState('');
    const [confirmarSenha, setConfirmarSenha] = useState('');

    const [nameArena, setNameArena] = useState('');
    const [cnpjArena, setCnpjArena] = useState('');
    const [cepArena, setCepArena] = useState('');
    const [enderecoArena, setEnderecoArena] = useState('');
    const [cidadeArena, setCidadeArena] = useState('');
    const [estadoArena, setEstadoArena] = useState('');
    const [numeroArena, setNumeroArena] = useState(''); // <--- NOVO
    const [error, setErro] = useState('');
    const [latitude, setLatitude] = useState(null);
    const [longitude, setLongitude] = useState(null);

    const navigate = useNavigate();

    const fetchCoordinates = async (logradouro, numero, cidade, estado) => {
        try {
            const query = `${logradouro}, ${numero}, ${cidade} - ${estado}, Brasil`;

            const response = await axios.get(
                "https://nominatim.openstreetmap.org/search",
                {
                    params: {
                        q: query,
                        format: "json",
                        limit: 1
                    },
                    headers: {
                        "User-Agent": "ArenaConnect/1.0 (gabrielkuchma.gk@gmail.com)"
                    }
                }
            );

            if (response.data?.length > 0) {
                const { lat, lon } = response.data[0];
                setLatitude(parseFloat(lat));
                setLongitude(parseFloat(lon));
                console.log("📍 Coordenadas corretas:", lat, lon);
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

            // 1️⃣ ViaCEP
            const res = await axios.get(`https://viacep.com.br/ws/${cep}/json/`);

            if (res.data.erro) {
                alert('CEP não encontrado!');
                return;
            }

            const { logradouro, localidade, uf } = res.data;

            setEnderecoArena(logradouro || '');
            setCidadeArena(localidade);
            setEstadoArena(uf);

            // 2️⃣ Geocodificação PELO CEP (forma mais estável)
            const geoRes = await axios.get(
                'https://nominatim.openstreetmap.org/search',
                {
                    params: {
                        q: `${cep}, ${localidade}, ${uf}, Brasil`,
                        format: 'json',
                        limit: 1
                    }
                }
            );

            if (geoRes.data && geoRes.data.length > 0) {
                const { lat, lon } = geoRes.data[0];
                setLatitude(parseFloat(lat));
                setLongitude(parseFloat(lon));

                console.log("📍 Coordenadas (CEP):", lat, lon);
            } else {
                console.warn("⚠️ Não foi possível geocodificar o CEP");
            }

            document.getElementById('input-numero').focus();

        } catch (err) {
            console.error("Erro ao buscar CEP", err);
        } finally {
            document.getElementById('cep-status').style.display = 'none';
        }
    };


    const handlePartnerRegister = async (e) =>{
        e.preventDefault();
        setErro('');

        const ok = await fetchCoordinates(
            enderecoArena,
            numeroArena,
            cidadeArena,
            estadoArena
        );

        if (!ok) {
            setErro("Não foi possível localizar o endereço no mapa.");
            return;
        }

        const enderecoCompleto = `${enderecoArena}, ${numeroArena}`;
        try{
            const response = await axios.post("http://localhost:8080/api/users/register-partner",{
                nomeUser: nomeUser,
                emailAdmin: emailAdmin,
                cpfUser: cpfUser.replace(/\D/g, ""),
                telefoneUser: telefoneUser.replace(/\D/g, ""),
                senhaAdmin:  senhaAdmin,
                confirmarSenha: confirmarSenha,
                //arenaDatas
                nomeArena: nameArena,
                cnpjArena: cnpjArena.replace(/\D/g, ""),
                cepArena: cepArena.replace(/\D/g, ""),
                enderecoArena: enderecoCompleto,
                cidadeArena: cidadeArena,
                estadoArena:estadoArena,
                latitude: latitude,
                longitude: longitude
            });

            if (response.status === 200 && response.data.success) {
                setErro('');
                alert(response.data.message || 'Admin cadastrado com sucesso!');
                onClose();
                navigate("/login");
            } else {
                setErro("dados invalidos");
            }
        } catch (err) {
            if (err.response && err.response.data?.message) {
                setErro(err.response.data.message);
            } else {
                setErro("Erro inesperado ao registrar usuário");
            }
        }
    }

    return (
        <div className="modal active">
            <div className="modal-content modal-wide">
                <div className="modal-header">
                    <h2>Cadastro de Parceiro</h2>
                    <button className="modal-close" type="button" onClick={onClose}>
                        &times;
                    </button>
                </div>

                <form onSubmit={handlePartnerRegister} className="form form-grid">
                    <h3 className="col-span-2 section-title">Seus Dados Pessoais</h3>

                    <div className="form-group col-span-2">
                        <label>Nome Completo *</label>
                        <input type="text" placeholder="Nome do proprietário" required value={nomeUser} onChange={(e) => setNomeUser(e.target.value)}/>
                    </div>

                    <div className="form-group">
                        <label>CPF *</label>
                        <input type="text" placeholder="000.000.000-00" required maxLength="14" value={cpfUser} onChange={(e)=> setCpf(maskCPF(e.target.value))}/>
                    </div>

                    <div className="form-group">
                        <label>Celular / WhatsApp</label>
                        <input type="tel" placeholder="(00) 00000-0000" maxLength="15" value={telefoneUser} onChange={(e)=> setTelefone(maskPhone(e.target.value))}/>
                    </div>

                    <div className="form-group col-span-2">
                        <label>E-mail de Login *</label>
                        <input type="email" placeholder="seu@email.com" required value={emailAdmin} onChange={(e) => setEmailAdmin(e.target.value)}/>
                    </div>

                    <div className="form-group">
                        <label>Senha *</label>
                        <input type="password" placeholder="••••••••" required minLength="6" value={senhaAdmin} onChange={(e) => setSenha(e.target.value)}/>
                    </div>

                    <div className="form-group">
                        <label>Confirmar Senha *</label>
                        <input type="password" placeholder="••••••••" required minLength="6"
                               value={confirmarSenha}
                               onChange={(e) => setConfirmarSenha(e.target.value)}/>
                    </div>

                    <hr className="col-span-2 separator"/>

                    <h3 className="col-span-2 section-title"> Dados do seu Complexo Esportivo</h3>

                    <div className="form-group col-span-2">
                        <label>Nome da Arena *</label>
                        <input type="text" placeholder="Ex: Arena Gol de Placa" required value={nameArena} onChange={(e)=>setNameArena(e.target.value)}/>
                    </div>

                    <div className="form-group">
                        <label>CNPJ da Empresa *</label>
                        <input type="text"  placeholder="00.000.000/0000-00" required maxLength="18" value={cnpjArena} onChange={(e) =>setCnpjArena(maskCNPJ(e.target.value))} />
                    </div>

                    <div className="form-group">
                        <label>CEP *</label>
                        <input type="text" placeholder="00000-000" required maxLength="9" value={cepArena}
                               onChange={(e)=>setCepArena(maskCEP(e.target.value))}
                               onBlur={checkCEP}/>
                        <small id="cep-status" style={{color: "var(--accent-green)", display: "none"}}>Buscando endereço...</small>
                    </div>

                    <div className="form-group col-span-2">
                        <label>Endereço</label>
                        <input type="text" placeholder="Rua, Número, Bairro" readOnly className="input-readonly" value={enderecoArena} onChange={(e)=>setEnderecoArena(e.target.value)}/>
                    </div>

                    <div className="form-group">
                        <label>Cidade</label>
                        <input type="text"  placeholder="Cidade" readOnly className="input-readonly" value={cidadeArena} onChange={(e)=>setCidadeArena(e.target.value)}/>
                    </div>

                    <div style={{ display: 'flex', gap: '15px' }}>


                        <div className="form-group" style={{ width: '100px' }}>
                            <label>UF</label>
                            <input
                                type="text"
                                placeholder="UF"
                                readOnly
                                className="input-readonly"
                                maxLength="2"
                                value={estadoArena}
                                onChange={(e) => setEstadoArena(e.target.value)}
                                style={{ textAlign: 'center' }}
                            />
                        </div>

                        <div className="form-group" style={{ flex: 1 }}>
                            <label>Número *</label>
                            <input
                                id="input-numero"
                                type="text"
                                placeholder="Nº"
                                required
                                value={numeroArena}
                                onChange={(e) => setNumeroArena(e.target.value)}
                                onBlur={() => {
                                    if (enderecoArena && cidadeArena && numeroArena) {
                                        fetchCoordinates(enderecoArena, numeroArena, cidadeArena, estadoArena);
                                    }
                                }}
                            />
                        </div>
                    </div>

                    <div className="modal-actions col-span-2" style={{marginTop: "1rem"}}>
                        {error && (
                            <p className="error-text" style={{ color: "red" }}>
                                {error}
                            </p>
                        )}
                        <button type="button" className="btn-secondary" onClick={onClose}>Cancelar</button>
                        <button type="submit" className="btn-primary">Finalizar cadastro</button>
                    </div>
                </form>
            </div>
        </div>
    );
}