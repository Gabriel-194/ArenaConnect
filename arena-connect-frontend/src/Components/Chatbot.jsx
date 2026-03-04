import { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import '../Styles/Chatbot.css';

export default function Chatbot({ onOpenPartnerModal }) {
    const [isOpen, setIsOpen] = useState(false);
    const [messages, setMessages] = useState([{
        role: 'assistant',
        text: 'Olá! Sou o assistente virtual do Arena Connect. Como posso ajudá-lo hoje?'
    }
    ]);
    const [inputMessage, setInputMessage] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const messagesEndRef = useRef(null);

    const actionsRef = useRef(null);

    const scrollActions = (direction) => {
        if (actionsRef.current) {
            const scrollAmount = 150;
            actionsRef.current.scrollBy({
                left: direction === 'left' ? -scrollAmount : scrollAmount,
                behavior: 'smooth'
            });
        }
    };

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({behavior: "smooth"});
    };

    useEffect(() => {
        scrollToBottom();
    }, [messages, isLoading]);

    const quickActions = [
        {id: 'como_cadastrar_cliente', label: ' Cadastro como cliente'},
        {id: 'como_cadastrar_admin', label: ' Cadastrar minha arena'},
        {id: 'funcionalidades', label: 'O que o sistema faz?'},
    ];

    const sendMessage = async (message) => {
        if(!message.trim()) return;

        const newUserMessage = { role: 'user', text: message };
        setMessages(prev => [...prev, newUserMessage]);
        setInputMessage('');
        setIsLoading(true);

        try {
            const history = messages.map(msg => ({
                role: msg.role,
                text: msg.text
            }));

            const response = await axios.post('http://localhost:8080/api/chatbot/message', {
                message: message,
                history: history
            });

            if (response.data.success) {
                const botText = response.data.response;
                setMessages(prev => [...prev, { role: 'assistant', text: botText }]);

                if (response.data.action === 'OPEN_PARTNER_MODAL') {
                    if (onOpenPartnerModal) {
                        setTimeout(() => {
                            onOpenPartnerModal();
                        }, 5000);
                    }
                }
            }
        } catch (error) {
            console.error('Erro ao enviar mensagem:', error);
            setMessages(prev => [...prev, {
                role: 'assistant',
                text: 'Ops! O servidor está a descansar um pouco. Tente novamente em instantes.'
            }]);
        } finally {
            setIsLoading(false);
        }
    };

    const handleQuickAction = async (actionId) => {
        setIsLoading(true);
        try {
            const response = await axios.get(`http://localhost:8080/api/chatbot/quick-response/${actionId}`);
            if (response.data.success) {
                setMessages(prev => [...prev, {
                    role: 'assistant',
                    text: response.data.response
                }]);

                if (response.data.action === 'OPEN_PARTNER_MODAL') {
                    if (onOpenPartnerModal) {
                        setTimeout(() => {
                            onOpenPartnerModal();
                        }, 7000);
                    }
                }
            }
        } catch (error) {
            console.error('Erro na ação rápida:', error);
        } finally {
            setIsLoading(false);
        }
    };

    return(
        <div className="chatbot-container">
            {!isOpen && (
                <button
                    onClick={() => setIsOpen(true)}
                    className="chatbot-button"
                >
                    <img
                        src="/Assets/chat-ai.png"
                        alt="Chat AI"
                        className="chatbot-icon-img"
                    />
                </button>
            )}

            {/* Janela do Chat */}
            {isOpen && (
                <div className="chatbot-window">

                    {/* Header */}
                    <div className="chatbot-header">
                        <div>
                            <div className="chatbot-title">Arena Connect AI</div>
                            <div className="chatbot-status">Resposta instantânea</div>
                        </div>
                        <button onClick={() => setIsOpen(false)} className="chatbot-close">
                            ×
                        </button>
                    </div>

                    {/* Mensagens */}
                    <div className="chatbot-messages">
                        {messages.map((msg, idx) => (
                            <div key={idx} className={`message ${msg.role === 'user' ? 'user-message' : 'bot-message'}`}>
                                <div className="message-bubble">
                                    {msg.text}
                                </div>
                            </div>
                        ))}
                        {isLoading && (
                            <div className="message bot-message">
                                <div className="message-bubble">Processando...</div>
                            </div>
                        )}
                        <div ref={messagesEndRef} />
                    </div>

                    {/* Sugestões Rápidas */}
                    <div className="chatbot-actions-wrapper">
                        <button className="scroll-arrow left" onClick={() => scrollActions('left')}>‹</button>

                        <div className="chatbot-quick-actions" ref={actionsRef}>
                            {quickActions.map(action => (
                                <button
                                    key={action.id}
                                    onClick={() => handleQuickAction(action.id)}
                                    className="quick-action-btn"
                                >
                                    {action.label}
                                </button>
                            ))}
                        </div>

                        <button className="scroll-arrow right" onClick={() => scrollActions('right')}>›</button>
                    </div>

                    {/* Input */}
                    <div className="chatbot-input-area">
                        <input
                            type="text"
                            value={inputMessage}
                            onChange={(e) => setInputMessage(e.target.value)}
                            onKeyPress={(e) => e.key === 'Enter' && sendMessage(inputMessage)}
                            placeholder="Pergunte algo..."
                            className="chatbot-input"
                        />
                        <button
                            onClick={() => sendMessage(inputMessage)}
                            disabled={!inputMessage.trim() || isLoading}
                            className="chatbot-send-btn"
                        >
                            ➔
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
}