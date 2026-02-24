import { useState } from 'react';
import {Link, useNavigate} from 'react-router-dom'
import axios from "axios";
import GoogleAuthButton from '../Components/GoogleAuthButton';

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

            const nomeDoUsuario = response.data.username;
            localStorage.setItem('userName', nomeDoUsuario);

            const destination = response.data.redirectUrl || '/home';

            console.log("Redirecting to:", destination);

            if (destination === '/home') {
                navigate('/home', {
                    state: {

                        arenaAtiva: response.data.arenaAtiva,
                        paymentUrl: response.data.paymentUrl
                    }
                });
            } else {
                navigate(destination);
            }

        } catch (error) {
            console.error("Erro no login:", error);

            if (error.response && error.response.data && error.response.data.message) {
                setErro(error.response.data.message);
            } else if (error.request) {
                setErro("Sem resposta do servidor. Verifique se o servidor está rodando.");
            } else {
                setErro("Erro ao tentar fazer login.");
            }
        }
    }

    const handleRequireExtraData = (googleData) => {
        navigate('/Register', { state: { googleData: googleData } });
    };

    return (
        <div className="login-container">
            <div className="login-card">
                <Link to={"/landingPage"} className="login-header" style={{ textDecoration: 'none', color: 'white' }}>
                    <div className="logo">
                        <img src="/Assets/3-removebg-preview.png" alt="logo" style={{ width: '80px' }} />
                        <h1 style={{ fontFamily: "'Racing Sans One', cursive" }}>Arena Connect</h1>
                    </div>
                    <p className="subtitle">Gestão inteligente de arenas esportivas</p>
                </Link>

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

                    <div style={{
                        display: 'flex',
                        alignItems: 'center',
                        color: '#aaa',
                        fontSize: '14px'
                    }}>
                        <div style={{ flex: 1, height: '1px', backgroundColor: '#444' }}></div>
                        <span style={{ margin: '0 10px' }}>ou</span>
                        <div style={{ flex: 1, height: '1px', backgroundColor: '#444' }}></div>
                    </div>
                    {/*
                                       <GoogleAuthButton
                        onRequireExtraData={handleRequireExtraData}
                        setErro={setErro}
                    />

                    */}

                    <div className="form-footer">
                        <span className="footer-text">Não tem uma conta?</span>
                        <Link to="/Register" className="link-accent">Criar conta</Link>
                    </div>
                </form>
            </div>
        </div>
    );
}

