import { createRoot } from 'react-dom/client'
import React from 'react'
import Login from './Pages/Login';

import './Styles/base.css'
import './Styles/components.css'
import './Styles/layout.css'

import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Register from "./Pages/Register.jsx";
import Home from './Pages/Home';
import PrivateRoute from "./Components/PrivateRoute.jsx";
import Quadras from "./Pages/Quadras.jsx";
import LandingPage from "./Pages/landingPage.jsx";
import HomeCLient from "./Pages/HomeClient.jsx";
import Agendamentos from "./Pages/Agendamentos.jsx";
import ClientsAgendamentos from "./Pages/ClientsAgendamentos.jsx";


createRoot(document.getElementById('root')).render(
    <React.StrictMode>

        <BrowserRouter>
            <Routes>
                <Route path="/" element={<LandingPage />} />
                <Route path="/landingPage" element={<LandingPage />} />
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register/>} />
                <Route path="/home" element={<PrivateRoute adminOnly={true}><Home /></PrivateRoute>}/>
                <Route path="/quadras" element={<PrivateRoute adminOnly={true}><Quadras /></PrivateRoute>}/>
                <Route path="/agendamentos" element={<PrivateRoute adminOnly={true}> <Agendamentos /> </PrivateRoute>}/>

                <Route path="/homeClient" element={<PrivateRoute clientOnly={true}> <HomeCLient /> </PrivateRoute>}/>
                <Route path="/clientsAgendamentos" element={<PrivateRoute clientOnly={true}> <ClientsAgendamentos /> </PrivateRoute>}/>
            </Routes>
        </BrowserRouter>
    </React.StrictMode>,
)