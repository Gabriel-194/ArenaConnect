import { useState } from 'react';
import {Link, useNavigate} from 'react-router-dom'
import axios from "axios";

import '../styles/login.css';


export default function Login(){
    const [email,setEmail] = useState('');
    const [senha, setSenha] = useState('');
    const [erro, setErro] = useState('');
    const navigate = useNavigate();

    const handleLogin = async (e) => {
        e.preventDefault();
        setErro('');

        try{
            const response = await axios.post('http://localhost:8080/api/auth/login',
                {
                    email: email,
                    senha: senha
                },
                {
                    headers: { "Content-Type": "application/json" },
                    withCredentials: true
                }
            );

            navigate('/home');
        } catch (error) {
            console.error("Erro no login:", error);

            // AQUI ESTÁ A CORREÇÃO MÁGICA 👇
            if (error.response && error.response.data && error.response.data.message) {
                // Se o backend respondeu (ex: 401), mostramos a mensagem dele
                setErro(error.response.data.message);
            } else if (error.request) {
                // Se nem houve resposta (Backend off ou CORS)
                setErro("Sem resposta do servidor. Verifique se o Java está rodando.");
            } else {
                // Erro genérico
                setErro("Erro ao tentar fazer login.");
            }
        }
    }

    return (
        <div className="login-container">
            <div className="login-card">
                <div className="login-header">
                    <div className="logo">
                        <img src="/Assets/3-removebg-preview.png" alt="logo" style={{ width: '80px' }} />
                        <h1 style={{ fontFamily: "'Racing Sans One', cursive" }}>Arena Connect</h1>
                    </div>
                    <p className="subtitle">Gestão inteligente de arenas esportivas</p>
                </div>

                {erro && (
                    <div className="alert alert-danger" style={{
                        color: 'red', textAlign: 'center', marginBottom: '15px', padding: '10px', borderRadius: '4px'
                    }}>
                        {erro}
                    </div>
                )}

                <form onSubmit={handleLogin} className="login-form">
                    <div className="form-group">
                        <label>E-mail</label>
                        <input
                            type="email"
                            placeholder="seu@email.com"
                            required
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                        />
                    </div>

                    <div className="form-group">
                        <label>Senha</label>
                        <input
                            type="password"
                            placeholder="••••••••"
                            required
                            value={senha}
                            onChange={(e) => setSenha(e.target.value)}
                        />
                    </div>

                    <a href="#" className="forgot-password">Esqueci minha senha</a>

                    <button type="submit" className="btn-primary">Entrar</button>

                    <div className="form-footer">
                        <span className="footer-text">Não tem uma conta?</span>
                        <Link to="/Register" className="link-accent">Criar conta</Link>
                    </div>
                </form>
            </div>
        </div>
    );
}

