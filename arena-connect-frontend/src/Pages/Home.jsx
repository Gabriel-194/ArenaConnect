import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import '../styles/home.css';
import axios from "axios";
import ModalCheckout from "../Components/ModalCheckout.jsx";

export default function Home(){
    const navigate = useNavigate();

    const [showCheckout, setShowCheckout] = useState(false);
    const [paymentUrl, setPaymentUrl] = useState('');

    const handleLogout = async (e) => {
        e.preventDefault();

        try {
            await axios.post(
                'http://localhost:8080/api/auth/logout',
                {},
                {
                    withCredentials: true
                }
            );
            localStorage.removeItem('userName');
        } catch (error) {
            console.error('Logout error:', error);
        } finally {
            navigate('/login');
        }
    }

    useEffect(() => {

        const validateSession = async () => {
            try {
                const response = await axios.post(
                    'http://localhost:8080/api/auth/validate',
                    {},
                    { withCredentials: true }
                );

                if (response.data.valid) {
                    if (response.data.arenaAtiva === false) {
                        setPaymentUrl(response.data.paymentUrl);
                        setShowCheckout(true);
                    } else {
                        setShowCheckout(false);
                    }
                }
            } catch (error) {
                if (error.response?.status === 401) {
                    navigate('/login');
                }
            }
        };

        validateSession();

        const interceptor = axios.interceptors.response.use(
            (response) => response,
            (error) => {
                if(error.response && error.response.status === 402) {
                    setShowCheckout(true);
                }
                return Promise.reject(error);
            }
        )

        return () => {
            axios.interceptors.response.eject(interceptor);
        }

    }, [navigate,paymentUrl]);

    return (
        <div className="home-body" style={{ minHeight: '100vh', width: '100%' }}>
            <div className="home-container">

                <header className="home-header">
                    <div className="logo-large">
                        <img src="/Assets/3-removebg-preview.png" alt="Arena Connect Logo" />
                        <h1 style={{ fontFamily: "'Racing Sans One', cursive", width: '500px' }}>
                            Arena Connect
                        </h1>
                    </div>
                    <p className="home-subtitle">Bem-vindo ao seu painel de gestão esportiva.</p>
                </header>

                <nav className="home-nav-grid">
                    <Link to="/dashboard" className="nav-card">
                        <div className="nav-card-icon">
                            <svg width="40" height="40" viewBox="0 0 20 20" fill="currentColor">
                                <path d="M2 10a8 8 0 018-8v8h8a8 8 0 11-16 0z"/>
                                <path d="M12 2.252A8.014 8.014 0 0117.748 8H12V2.252z"/>
                            </svg>
                        </div>
                        <div className="nav-card-content">
                            <h3>Dashboard</h3>
                            <p>Visão geral e relatórios</p>
                        </div>
                    </Link>

                    <Link to="/agendamentos" className="nav-card">
                        <div className="nav-card-icon">
                            <svg width="40" height="40" viewBox="0 0 20 20" fill="currentColor">
                                <path fillRule="evenodd" d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z" clipRule="evenodd"/>
                            </svg>
                        </div>
                        <div className="nav-card-content">
                            <h3>Agendamentos</h3>
                            <p>Calendário de jogos</p>
                        </div>
                    </Link>


                    <Link to="/quadras" className="nav-card">
                        <div className="nav-card-icon">
                            <svg width="40" height="40" viewBox="0 0 20 20" fill="currentColor">
                                <path fillRule="evenodd" d="M3 4a1 1 0 011-1h12a1 1 0 011 1v12a1 1 0 01-1 1H4a1 1 0 01-1-1V4zm2 2v8h10V6H5z" clipRule="evenodd"/>
                            </svg>
                        </div>
                        <div className="nav-card-content">
                            <h3>Quadras</h3>
                            <p>Gerenciar espaços</p>
                        </div>
                    </Link>

                    <button
                        id="logout-btn-home"
                        className="nav-card logout-card"
                        onClick={handleLogout}
                    >
                        <div className="nav-card-icon">
                            <svg width="40" height="40" viewBox="0 0 20 20" fill="currentColor">
                                <path fillRule="evenodd" d="M3 3a1 1 0 00-1 1v12a1 1 0 102 0V4a1 1 0 00-1-1zm10.293 9.293a1 1 0 001.414 1.414l3-3a1 1 0 000-1.414l-3-3a1 1 0 10-1.414 1.414L14.586 9H7a1 1 0 100 2h7.586l-1.293 1.293z" clipRule="evenodd"/>
                            </svg>
                        </div>
                        <div className="nav-card-content">
                            <h3>Sair</h3>
                            <p>Encerrar sessão</p>
                        </div>
                    </button>
                </nav>

                <footer className="home-footer">
                    <p>© 2025 Arena Connect. Todos os direitos reservados.</p>
                </footer>
            </div>
            {showCheckout && <ModalCheckout paymentUrl={paymentUrl} />}
        </div>
    );
};