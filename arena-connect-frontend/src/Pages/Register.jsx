import {useState, useEffect} from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom';
import ModalUser from "../Components/ModalUser.jsx";
import ModalPartners from "../Components/ModalPartners.jsx";

import '../Styles/register.css';

export default function Register (){

    const [showModalCliente, setShowModalCliente] = useState(false);
    const [showModalParceiro, setShowModalParceiro] = useState(false);

    const location = useLocation();
    const googleData = location.state?.googleData || null;

    useEffect(() => {
        const params = new URLSearchParams(window.location.search);
        const type = params.get('type');

        if (type === 'partner') {
            setShowModalParceiro(true);
        } else if (type === 'client') {
            setShowModalCliente(true);
        } else {
            setShowModalCliente(false);
            setShowModalParceiro(false);
        }
    }, []);


    return (
        <div className="login-container">
            <div className="login-card">
                <Link to={"/landingPage"} className="login-header" style={{ textDecoration: 'none', color: 'white' }}>
                    <div className="logo">
                        <img src="/Assets/3-removebg-preview.png" alt="logo" style={{width: "80px"}}/>
                        <h1 style={{ fontFamily: "'Racing Sans One', cursive" }}>Arena Connect</h1>
                    </div>
                    <p className="subtitle">
                        {googleData ? `Falta pouco, ${googleData.name}! Escolha o tipo de cadastro` : "Escolha o tipo de cadastro"}
                    </p>
                </Link>

                <div className="register-type-container">
                    <button type="button" className="register-type-btn" onClick={() => setShowModalCliente(true)}>
                        <div className="register-type-icon">
                            <img src="/Assets/user-svgrepo-com.svg" alt="client logo" style={{width: "50px"}}/>
                        </div>
                        <h3>Sou Cliente</h3>
                        <p>Quero reservar quadras para jogar</p>
                    </button>

                    {showModalCliente && (
                        <ModalUser  onClose={() => setShowModalCliente(false)}  googleData={googleData}/>
                    )}

                    <button type="button" className="register-type-btn" onClick={() => setShowModalParceiro(true)}>
                        <div className="register-type-icon">
                            <img src="/Assets/stadium-svgrepo-com.svg" alt="arena logo" style={{width: "50px"}}/>
                        </div>
                        <h3>Sou Dono de Arena</h3>
                        <p>Quero gerenciar meu complexo</p>
                    </button>

                    {showModalParceiro && (
                        <ModalPartners  onClose={() => setShowModalParceiro(false)} googleData={googleData} />
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