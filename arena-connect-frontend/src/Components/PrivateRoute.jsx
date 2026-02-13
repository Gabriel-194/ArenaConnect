import React, {useEffect, useState} from 'react';
import { Navigate } from 'react-router-dom';
import axios from 'axios';
import Loader from './Loader';

const PrivateRoute = ({ children, adminOnly = false, clientOnly = false, superAdminOnly = false }) => {
    const [isAuthenticated, setIsAuthenticated] = useState(null);
    const [userHomeUrl, setUserHomeUrl] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const verifyToken = async () => {
            try{
                const response = await axios.post('http://localhost:8080/api/auth/validate',{},{
                    withCredentials:true
                });

                if (response.data.valid) {
                    setIsAuthenticated(true);
                    setUserHomeUrl(response.data.redirectUrl);
                } else {
                    setIsAuthenticated(false);
                }
            }catch (error) {
                setIsAuthenticated(false);
            }finally {
                setLoading(false);
            }
        };
        verifyToken();
    }, []);

    if (loading) return <Loader />;

    if (!isAuthenticated) return <Navigate to="/login" />;

    if (adminOnly && userHomeUrl !== '/home') {
        return <Navigate to={userHomeUrl} replace />;
    }

    if (clientOnly && userHomeUrl !== '/homeClient') {
        return <Navigate to={userHomeUrl} replace />;
    }

    if (superAdminOnly && userHomeUrl !== '/homeSuperAdmin') {
        return <Navigate to={userHomeUrl} replace />;
    }

    return children;
};

export default PrivateRoute;