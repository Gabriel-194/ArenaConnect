import { useState,useEffect } from 'react';
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

const maskPhone = (value) => {
    return value
        .replace(/\D/g, "")
        .replace(/(\d{2})(\d)/, "($1) $2")
        .replace(/(\d{5})(\d)/, "$1-$2")
        .replace(/(-\d{4})\d+?$/, "$1");
};

export default function ModalUser({ onClose, googleData, userToEdit, onSuccess }){
    const navigate = useNavigate();
    const [nome, setNome] = useState('');
    const [email, setEmail] = useState('');
    const [cpf,setCpf] = useState('');
    const [telefone,setTelefone] = useState('');
    const [senha, setSenha] = useState('');
    const [error, setErro] = useState('');
    const [confirmarSenha, setConfirmarSenha] = useState('');

    const handleSaveUser = async (e) => {
        e.preventDefault();
        setErro('');

        try{
            const payload = {
                nome: nome,
                email: email,
                cpf: cpf.replace(/\D/g,""),
                telefone: telefone.replace(/\D/g, "")
            };
            if (!userToEdit) {
                payload.senha = senha;
                payload.confirmarSenha = confirmarSenha;
            }
            let response;

            if (userToEdit) {
                response = await axios.put(`http://localhost:8080/api/users/${userToEdit.idUser || userToEdit.id}`, payload, {
                    withCredentials: true
                });

                if (response.status === 200) {
                    alert("Usuário atualizado com sucesso!");
                    if (onSuccess) onSuccess();
                    onClose();
                }
            }else {
                response = await axios.post('http://localhost:8080/api/users/register-client', payload);

                if (response.data.success) {
                    alert(response.data.message);
                    navigate("/login");
                } else {
                    setErro("Dados inválidos");
                }
            }

        } catch (err) {
            if (err.response && err.response.data?.message) {
                setErro(err.response.data.message);
            } else {
                setErro("Erro inesperado ao processar usuário");
            }
        }
    }

    useEffect(() => {
        if (userToEdit) {
            setNome(userToEdit.nome || '');
            setEmail(userToEdit.email || '');
            setCpf(maskCPF(userToEdit.cpf || ''));
            setTelefone(maskPhone(userToEdit.telefone || ''));
        } else if (googleData) {
            setNome(googleData.name || '');
            setEmail(googleData.email || '');
        }
    }, [userToEdit, googleData]);

    return(
        <div className="modal active">
                <div className="modal-content modal-wide">
                    <div className="modal-header">
                        <h2>{userToEdit ? 'Editar Usuário' : 'Cadastro de Cliente'}</h2>
                        <button className="modal-close" type="button" onClick={onClose}>
                            &times;
                        </button>
                    </div>

                    <form onSubmit={handleSaveUser} className="form form-grid">

                        <div className="form-group col-span-2">
                            <label>Nome Completo *</label>
                            <input
                                type="text"
                                placeholder="Seu nome completo"
                                required
                                value={nome}
                                onChange={(e) => setNome(e.target.value)}
                                disabled={!!googleData}
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
                                onChange={(e) => setCpf(maskCPF(e.target.value))}
                            />
                        </div>

                        <div className="form-group">
                            <label>Telefone</label>
                            <input type="tel" placeholder="(00) 00000-0000" maxLength="15" value={telefone}
                                   onChange={(e) => setTelefone(maskPhone(e.target.value))}/>
                        </div>

                        <div className="form-group col-span-2">
                            <label>E-mail *</label>
                            <input type="email" placeholder="seu@email.com" required value={email}
                                   onChange={(e) => setEmail(e.target.value)}
                                   disabled={!!googleData} />
                        </div>

                        {!googleData && !userToEdit && (
                            <>
                                <div className="form-group">
                                    <label>Senha *</label>
                                    <input type="password" placeholder="••••••••" required minLength="6" value={senha}
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

                        <div className="modal-actions col-span-2">
                                {error && (
                                    <p className="error-text" style={{ color: "red" }}>
                                        {error}
                                    </p>
                                )}
                            <button type="button" className="btn-secondary" onClick={onClose}>Cancelar</button>
                            <button type="submit" className="btn-primary">
                                {userToEdit ? 'Salvar Alterações' : 'Criar Conta'}
                            </button>
                        </div>
                    </form>
                </div>
        </div>
    );

}