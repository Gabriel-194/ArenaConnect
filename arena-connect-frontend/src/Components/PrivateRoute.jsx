import React, {useEffect, useState} from 'react';
import { Navigate } from 'react-router-dom';
import axios from 'axios';
import Loader from './Loader';

const PrivateRoute = ({ children }) => {
    const [isAuthenticated, setIsAuthenticated] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const verifyToken = async () => {
            try{
                const response = await axios.post('http://localhost:8080/api/auth/validate',{},{
                    withCredentials:true
                });

                setIsAuthenticated(response.data.valid);
            }catch (error) {
                setIsAuthenticated(false);
            }finally {
                setLoading(false);
        }
    };
    verifyToken();
}, []);

    if (loading) {
        return <Loader />;
    }
    return isAuthenticated ? children : <Navigate to="/login" />;
};

export default PrivateRoute;