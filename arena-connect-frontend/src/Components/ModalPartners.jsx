import { useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';

export default function ModalPartners({onClose}) {
    const navigate = useNavigate();
    const [nome, setNome] = useState('');
    const [email,setEmail] = useState('');
    const [cpf,setCpf] = useState('');
    const [telefone,setTelefone] = useState('');
    const [senha, setSenha] = useState('');
    const [erro, setErro] = useState('');
    //arena
    const [nameArena, setNameArena] = useState('');
    const [cnpjArena, setCnpjArena] = useState('');
    const [cepArena, setCepArena] = useState('');
    const [enderecoArena,setEnderecoArena] = useState('');
    const [cidadeArena, setCidadeArena] = useState('');
    const [estadoArena, setEstadoArena] = useState('');

    const handlePartnerRegister = async (e) =>{
        e.preventDefault();
        setErro('');

        try{
            const response = await axios.post("http://localhost/api/users/register-partner",{
                nome: nome,
                email: email,
                cpf: cpf,
                telefone: telefone,
                senha: senha,
                //arenaDatas
                nomeArena: nameArena,
                cnpjArena: cnpjArena,
                cepArena: cepArena,
                enderecoArena: enderecoArena,
                cidadeArena: cidadeArena,
                estadoArena:estadoArena
            });

            if(response.ok){
                <Link to="/login"></Link>
            } else{
                setErro("dados invalidos");
            }
        } catch (error) {
            setErro("Falha ao registrar. Verifique se o Java está rodando e o CORS configurado.");
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
                        <input type="text" placeholder="Nome do proprietário" required value={nome} onChange={(e) => setNome(e.target.value)}/>
                    </div>

                    <div className="form-group">
                        <label>CPF *</label>
                        <input type="text" placeholder="000.000.000-00" required maxLength="14" value={cpf} onChange={(e)=> setCpf(e.target.value)}/>
                    </div>

                    <div className="form-group">
                        <label>Celular / WhatsApp</label>
                        <input type="tel" placeholder="(00) 00000-0000" maxLength="15" value={telefone} onChange={(e)=> setTelefone(e.target.value)}/>
                    </div>

                    <div className="form-group col-span-2">
                        <label>E-mail de Login *</label>
                        <input type="email" placeholder="seu@email.com" required value={email} onChange={(e) => setEmail(e.target.value)}/>
                    </div>

                    <div className="form-group">
                        <label>Senha *</label>
                        <input type="password" placeholder="••••••••" required minLength="6" value={senha} onChange={(e) => setSenha(e.target.value)}/>
                    </div>

                    <div className="form-group">
                        <label>Confirmar Senha *</label>
                        <input type="password" placeholder="••••••••" required minLength="6"/>
                    </div>

                    <hr className="col-span-2 separator"/>

                    <h3 className="col-span-2 section-title"> Dados do seu Complexo Esportivo</h3>

                    <div className="form-group col-span-2">
                        <label>Nome da Arena *</label>
                        <input type="text" placeholder="Ex: Arena Gol de Placa" required value={nameArena} onChange={(e)=>setNameArena(e.target.value)}/>
                    </div>

                    <div className="form-group">
                        <label>CNPJ da Empresa *</label>
                        <input type="text"  placeholder="00.000.000/0000-00" required maxLength="18" value={cnpjArena} onChange={(e) =>setCnpjArena(e.target.value)} />
                    </div>

                    <div className="form-group">
                        <label>CEP *</label>
                        <input type="text" placeholder="00000-000" required maxLength="9" value={cepArena} onChange={(e)=>setCepArena(e.target.value)}/>
                        <small id="cep-status" style={{color: "white", display: "none"}}>Buscando...</small>
                    </div>

                    <div className="form-group col-span-2">
                        <label>Endereço</label>
                        <input type="text" placeholder="Rua, Número, Bairro" readOnly className="input-readonly" value={enderecoArena} onChange={(e)=>setEnderecoArena(e.target.value)}/>
                    </div>

                    <div className="form-group">
                        <label>Cidade</label>
                        <input type="text"  placeholder="Cidade" readOnly className="input-readonly" value={cidadeArena} onChange={(e)=>setCidadeArena(e.target.value)}/>
                    </div>

                    <div className="form-group">
                        <label>Estado</label>
                        <input type="text" placeholder="UF" readOnly className="input-readonly" maxLength="2" value={estadoArena} onChange={(e)=>setEstadoArena(e.target.value)}/>
                    </div>

                    <div className="modal-actions col-span-2" style={{marginTop: "1rem"}}>
                        <button type="button" className="btn-secondary" onClick={onClose}>Cancelar</button>
                        <button type="submit" className="btn-primary">Finalizar cadastro</button>
                    </div>
                </form>
            </div>
        </div>
    );
}