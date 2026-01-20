import {useState} from "react";
import axios from 'axios';
import '../Styles/components.css'



export default function ModalCourts({onClose}) {
    const [nome, setNome] = useState('');
    const [tipo,setTipo] = useState('');
    const [valorHora,setValorHora] = useState('');
    const [error, setErro] = useState('');

    const handleCreateCourt = async (e) =>{
        try{
            const response = await axios.post("http://localhost:8080/quadra/createQuadra",{
                nome:nome,
                tipo_quadra: tipo,
                valor_hora: valorHora
            });

            if (response.data.success) {
                alert(response.data.message);
                alert("Quadra cadastrada com sucesso!");
            } else {
                alert(setErro("Dados invalidos"));
            }
        } catch (error) {
            if (err.response && err.response.data?.message) {
                setErro(err.response.data.message);
            } else {
                setErro("Erro inesperado ao registrar usuário");
            }
        }
    }

    return (
        <div className="modal">
            <div className="modal-content">
                <div className="modal-header">
                    <h3>Nova Quadra</h3>
                    <button type="button" className="modal-close" onClick={onClose}>
                        &times;
                    </button>
                </div>

                <form className="form" onSubmit={handleCreateCourt}>
                    <div className="form-group">
                        <label>Nome da Quadra</label>
                        <input type="text" placeholder="Ex: Quadra 1" value={nome}
                               onChange={(e) => setNome(e.target.value)} required
                        />
                    </div>

                    <div className="form-group">
                        <label>Tipo da quadra</label>
                        <input type="text" placeholder="Ex: futsal" value={tipo}
                            onChange={(e) => setTipo(e.target.value)} required
                        />
                    </div>

                    <div className="form-group">
                        <label>Valor por Hora (R$)</label>
                        <input type="number" placeholder="150.00" step="0.01" value={valorHora}
                            onChange={(e) => setValorHora(e.target.value)} required
                        />
                    </div>

                    {error && <p className="error-text">{error}</p>}

                    <div className="form-actions">
                        <button type="button" className="btn-secondary" onClick={onClose}>
                            Cancelar
                        </button>

                        <button type="submit" className="btn-primary">
                            Cadastrar
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );

}