import { createRoot } from 'react-dom/client'
import React from 'react'
import Login from './pages/Login';


import './Styles/base.css'
import './Styles/components.css'
import './Styles/layout.css'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Register from "./Pages/Register.jsx";

createRoot(document.getElementById('root')).render(
    <React.StrictMode>
        <BrowserRouter>
            <Routes>
                <Route path="/login" element={<Login />} />
                <Route path="/" element={<Navigate to="/login" />} />
                <Route path="/register" element={<Register/>} />
            </Routes>
        </BrowserRouter>
    </React.StrictMode>,
)