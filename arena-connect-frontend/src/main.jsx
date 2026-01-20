import { createRoot } from 'react-dom/client'
import React from 'react'
import Login from './Pages/Login';

import './Styles/base.css'
import './Styles/components.css'
import './Styles/layout.css'

import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Register from "./Pages/Register.jsx";
import Home from './Pages/Home';
import PrivateRoute from "./Components/PrivateRoute.jsx";
import Quadras from "./Pages/Quadras.jsx";


createRoot(document.getElementById('root')).render(
    <React.StrictMode>
        <BrowserRouter>
            <Routes>
                <Route path="/login" element={<Login />} />
                <Route path="/" element={<Navigate to="/login" />} />
                <Route path="/register" element={<Register/>} />
                <Route path="/home" element={<PrivateRoute> <Home /> </PrivateRoute>}/>
                <Route path="/quadras" element={<PrivateRoute> <Quadras /> </PrivateRoute>}/>
            </Routes>
        </BrowserRouter>
    </React.StrictMode>,
)