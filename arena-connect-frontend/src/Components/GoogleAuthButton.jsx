import React from "react";
import { GoogleLogin } from '@react-oauth/google';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';

const GoogleAuthButton = ({ onRequireExtraData, setErro }) => {
    const navigate = useNavigate();
    const onSuccess = async (credentialResponse) => {
        const tokenGoogle = credentialResponse.credential;

        try {
            const response = await axios.post('http://localhost:8080/api/auth/google',
                { token: tokenGoogle },
                {
                    headers: { "Content-Type": "application/json" },
                    withCredentials: true
                }
            );

            if (response.data.isNewUser) {
                onRequireExtraData(response.data.googleData);
            }
            else {
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
            }

        } catch (error) {
            console.error("Erro no login com Google:", error);

            if (error.response && error.response.data && error.response.data.message) {
                setErro(error.response.data.message);
            } else if (error.request) {
                setErro("Sem resposta do servidor. Verifique se o servidor está rodando.");
            } else {
                setErro("Erro ao tentar fazer login com o Google.");
            }
        }
    };

    const onError = () => {
        console.log("Falha no Login do Google");
    };

    return (
        <div style={{ display: 'flex', justifyContent: 'center', margin: '10px 0' }}>
            <GoogleLogin
                onSuccess={onSuccess}
                onError={onError}
                theme="filled_black"
                shape="pill"
                text="continue_with"
            />
        </div>
    );
};

export default GoogleAuthButton;