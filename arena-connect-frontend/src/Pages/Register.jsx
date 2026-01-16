import {useState} from 'react'
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';
import ModalUser from "../Components/ModalUser.jsx";
import ModalPartners from "../Components/ModalPartners.jsx";

import '../Styles/register.css';

export default function Register (){

    const [showModalCliente, setShowModalCliente] = useState(false);
    const [showModalParceiro, setShowModalParceiro] = useState(false);
    const navigate = useNavigate();


    return (
        <div className="login-container">
            <div className="login-card">
                <div className="login-header">
                    <div className="logo">
                        <img src="/Assets/3-removebg-preview.png" alt="logo" style={{width: "80px"}}/>
                        <h1 style={{ fontFamily: "'Racing Sans One', cursive" }}>Arena Connect</h1>
                    </div>
                    <p className="subtitle">Escolha o tipo de cadastro</p>
                </div>

                <div className="register-type-container">
                    <button type="button" className="register-type-btn" onClick={() => setShowModalCliente(true)}>
                        <div className="register-type-icon">
                            <img src="/Assets/user-svgrepo-com.svg" alt="client logo" style={{width: "50px"}}/>
                        </div>
                        <h3>Sou Cliente</h3>
                        <p>Quero reservar quadras para jogar</p>
                    </button>

                    {showModalCliente && (
                        <ModalUser  onClose={() => setShowModalCliente(false)} />
                    )}

                    <button type="button" className="register-type-btn" onClick={() => setShowModalParceiro(true)}>
                        <div className="register-type-icon">
                            <img src="/Assets/stadium-svgrepo-com.svg" alt="arena logo" style={{width: "50px"}}/>
                        </div>
                        <h3>Sou Dono de Arena</h3>
                        <p>Quero gerenciar meu complexo</p>
                    </button>

                    {showModalParceiro && (
                        <ModalPartners  onClose={() => setShowModalParceiro(false)} />
                    )}
                </div>

                <div className="form-footer">
                    <span className="footer-text">Já tem uma conta?</span>
                    <Link to="/login" className="link-accent">Fazer Login</Link>
                </div>
            </div>
        </div>
    );
}