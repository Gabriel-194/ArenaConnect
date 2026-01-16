import { useState } from 'react';
import { Link } from 'react-router-dom'
import axios from "axios";

import '../styles/login.css';


export default function Login(){
    const [email,setEmail] = useState('');
    const [senha, setSenha] = useState('');
    const [erro, setErro] = useState('');

    const handleLogin = async (e) => {
        e.preventDefault();
        setErro('');

        try{
            const response = await axios.post('http://localhost:8080/api/auth/login', {
                login: email,
                senha: senha
            });

            const token = response.data.token;
            localStorage.setItem('token', token);
            alert("Login realizado com sucesso! (Token salvo)");
        } catch (error) {
            console.error(error);
            setErro("Falha no login. Verifique se o Java está rodando e o CORS configurado.");
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
                        color: 'red', textAlign: 'center', marginBottom: '15px',
                        background: '#ffe6e6', padding: '10px', borderRadius: '4px'
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

