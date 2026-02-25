import React, { useState,useEffect } from 'react';
import axios from "axios";
import '../Styles/HomeClient.css';

export default function ClientHeader() {
    const username = localStorage.getItem('userName') || 'Atleta';
    const userInitial = username.charAt(0).toUpperCase();

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [notifications, setNotifications] = useState([]);

    const fetchNotifications = async () => {
        try {
            const response = await axios.get('http://localhost:8080/api/notificacoes/minhas', {
                withCredentials: true
            });
            setNotifications(response.data);
        } catch (error) {
            console.error("Erro ao buscar notificações:", error);
        }
    };

    useEffect(() => {
        fetchNotifications();

        const tempoEmMilissegundos = 10000;

        const intervalId = setInterval(() => {
            fetchNotifications();
        }, tempoEmMilissegundos);

        return () => clearInterval(intervalId);
    }, []);

    const unreadCount = notifications.filter(n => !n.lida).length;

    const HandleToggleModal = () => {
        setIsModalOpen(!isModalOpen);
        fetchNotifications();
    };

    const handleMarkAllRead = async () => {
        if (unreadCount === 0) return;

        try {
            await axios.put('http://localhost:8080/api/notificacoes/lidas', {}, {
                withCredentials: true
            });

            setNotifications(notifications.map(n => ({ ...n, lida: true })));
        } catch (error) {
            console.error("Erro ao marcar notificações como lidas:", error);
        }
    };
    return (
        <header className="client-header glass-panel">
            <div className="header-user">
                <div className="user-avatar">
                    <span>{userInitial}</span>
                </div>
                <div className="user-greeting">
                    <p>Olá, {username}</p>
                    <h3 style={{ fontFamily: 'inherit' }}>Bora jogar hoje? </h3>
                </div>
            </div>

            <div style={{ position: 'relative' }}>
                <button className="btn-icon-glass" onClick={HandleToggleModal}>
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                        <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
                    </svg>

                    {unreadCount > 0 && (
                        <span className="notification-badge">{unreadCount}</span>
                    )}
                </button>

                {isModalOpen && (
                    <div className="notifications-dropdown glass-panel">
                        <div className="notifications-header">
                            <h4>Notificações</h4>
                            <span className="mark-all-read" onClick={handleMarkAllRead}>Marcar lidas</span>
                        </div>

                        <div className="notifications-body">
                            {notifications.length === 0 ? (
                                <p className="no-notifications">Você não tem notificações.</p>
                            ) : (
                                notifications.map(notif => (
                                    <div key={notif.id} className={`notification-item ${notif.lida ? 'read' : 'unread'}`}>
                                        <div className="notif-content">
                                            <h5 className={`notif-title ${
                                                notif.tipo === 'CONFIRMADO' ? 'text-green' :
                                                    notif.tipo === 'PENDENTE' ? 'text-yellow' :
                                                        notif.tipo === 'CANCELADO' ? 'text-red' : 
                                                            notif.tipo === 'FINALIZADO' ? 'text-blue' : ''
                                            }`}>
                                                {notif.titulo}
                                            </h5>
                                            <p className="notif-desc">{notif.mensagem}</p>

                                            <span className="notif-time">
                                                {new Date(notif.dataCriacao).toLocaleDateString('pt-BR')} às {new Date(notif.dataCriacao).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}
                                            </span>
                                        </div>
                                        {!notif.lida && <div className="unread-dot"></div>}
                                    </div>
                                ))
                            )}
                        </div>
                    </div>
                )}
            </div>
        </header>
    );
}