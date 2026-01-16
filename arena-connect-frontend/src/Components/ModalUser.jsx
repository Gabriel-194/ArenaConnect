import { useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';

export default function ModalUser({ onClose }){
    const navigate = useNavigate();
    const [nome, setNome] = useState('');
    const [email,setEmail] = useState('');
    const [cpf,setCpf] = useState('');
    const [telefone,setTelefone] = useState('');
    const [senha, setSenha] = useState('');
    const [erro, setErro] = useState('');

    const handleRegisterUser = async (e) => {
        e.preventDefault();
        setErro('');

        try{
            const response = await axios.post('http://localhost:8080/api/users/register', {
                nome: nome,
                email: email,
                cpf: cpf,
                telefone: telefone,
                senha: senha
            });

            if(response.ok){
                alert("Cadastro realizado!");
                navigate("/login");
            } else {
                setErro("dados invalidos");
            }
        } catch (error){
            setErro("Falha ao registrar. Verifique se o Java está rodando e o CORS configurado.");
        }
    }

    return(
        <div className="modal active">
                <div className="modal-content modal-wide">
                    <div className="modal-header">
                        <h2>Cadastro de Cliente</h2>
                        <button className="modal-close" type="button" onClick={onClose}>
                            &times;
                        </button>
                    </div>

                    <form onSubmit={handleRegisterUser} className="form form-grid">

                        <div className="form-group col-span-2">
                            <label>Nome Completo *</label>
                            <input
                                type="text"
                                placeholder="Seu nome completo"
                                required
                                value={nome}
                                onChange={(e) => setNome(e.target.value)}
                            />
                        </div>

                        <div className="form-group">
                            <label>CPF *</label>
                            <input
                                type="text"
                                placeholder="000.000.000-00"
                                required
                                maxLength="14"
                                value={cpf}
                                onChange={(e) => setCpf(e.target.value)}
                            />
                        </div>

                        <div className="form-group">
                            <label>Telefone</label>
                            <input type="tel" placeholder="(00) 00000-0000" maxLength="15" value={telefone}
                                   onChange={(e) => setTelefone(e.target.value)}/>
                        </div>

                        <div className="form-group col-span-2">
                            <label>E-mail *</label>
                            <input type="email" placeholder="seu@email.com" required value={email}
                                   onChange={(e) => setEmail(e.target.value)}/>
                        </div>

                        <div className="form-group">
                            <label>Senha *</label>
                            <input type="password" placeholder="••••••••" required minLength="6" value={senha}
                                   onChange={(e) => setSenha(e.target.value)}/>
                        </div>

                        <div className="form-group">
                            <label>Confirmar Senha *</label>
                            <input type="password" name="confirmPassword" placeholder="••••••••" required minLength="6"/>
                        </div>

                        <input type="hidden" name="role" value="CLIENTE"/>

                        <div className="modal-actions col-span-2">
                            <button type="button" className="btn-secondary" onClick={onClose}>Cancelar</button>
                            <button type="submit" className="btn-primary">Criar Conta</button>
                        </div>
                    </form>
                </div>
        </div>
    );

}