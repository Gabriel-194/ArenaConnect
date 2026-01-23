'use client';

import '../Styles/landingPage.css';
import { Link } from 'react-router-dom';
import { useRef, useEffect } from 'react';


const Icons = {
    Lightning: () => (
        <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
        </svg>
    ),
    Building: () => (
        <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
        </svg>
    ),
    User: () => (
        <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
        </svg>
    ),
    Dollar: () => (
        <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
    ),
    Calendar: () => (
        <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
    ),
    Chart: () => (
        <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
        </svg>
    ),
    Users: () => (
        <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
        </svg>
    ),
    Mobile: () => (
        <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
        </svg>
    ),
    Check: () => (
        <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
        </svg>
    ),
    Clock: () => (
        <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
    ),
    Wrench: () => (
        <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
            <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
        </svg>
    ),
    Bell: () => (
        <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
        </svg>
    ),
    Twitter: () => (
        <svg fill="currentColor" viewBox="0 0 24 24">
            <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z" />
        </svg>
    ),
    Instagram: () => (
        <svg fill="currentColor" viewBox="0 0 24 24">
            <path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zm0-2.163c-3.259 0-3.667.014-4.947.072-4.358.2-6.78 2.618-6.98 6.98-.059 1.281-.073 1.689-.073 4.948 0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98 1.281.058 1.689.072 4.948.072 3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98-1.281-.059-1.69-.073-4.949-.073zm0 5.838c-3.403 0-6.162 2.759-6.162 6.162s2.759 6.163 6.162 6.163 6.162-2.759 6.162-6.163c0-3.403-2.759-6.162-6.162-6.162zm0 10.162c-2.209 0-4-1.79-4-4 0-2.209 1.791-4 4-4s4 1.791 4 4c0 2.21-1.791 4-4 4zm6.406-11.845c-.796 0-1.441.645-1.441 1.44s.645 1.44 1.441 1.44c.795 0 1.439-.645 1.439-1.44s-.644-1.44-1.439-1.44z" />
        </svg>
    ),
    LinkedIn: () => (
        <svg fill="currentColor" viewBox="0 0 24 24">
            <path d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433c-1.144 0-2.063-.926-2.063-2.065 0-1.138.92-2.063 2.063-2.063 1.14 0 2.064.925 2.064 2.063 0 1.139-.925 2.065-2.064 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z" />
        </svg>
    ),
};


const LiquidBackground = () => (
    <div className="liquid-background">
        <div className="liquid-blob blob-1" />
        <div className="liquid-blob blob-2" />
        <div className="liquid-blob blob-3" />
        <div className="liquid-blob blob-4" />
        <div className="noise-overlay" />
    </div>
);


const Navbar = () => (
    <nav className="navbar">
        <div className="navbar-inner">
            <div className="navbar-content">
                <a href="/" className="logo">
                        <img src="/Assets/3-removebg-preview.png" alt="Arena Connect Logo" style={{ height: '40px', width: 'auto' }} />
                    <span className="logo-text" style={{ fontFamily: "'Racing Sans One', cursive" }}>ArenaConnect</span>
                </a>

                <div className="nav-links">
                    <a href="#features" className="nav-link">Recursos</a>
                    <a href="#pricing" className="nav-link">Precos</a>
                </div>

                <div className="nav-cta">
                    <Link to={"/login"} className="btn-ghost">Entrar</Link>
                    <Link to="/register" className="btn-primary">Comecar Agora</Link>
                </div>
            </div>
        </div>
    </nav>
);


const HeroSection = () => {
    return (
        <section className="hero">
            <div className="hero-content">

                <h1 className="hero-title text-balance">
                    Gerencie sua Arena.
                    <br />
                    <span className="hero-title-gradient">Domine o Jogo.</span>
                </h1>

                <p className="hero-subtitle text-pretty">
                    Conectamos donos de quadras esportivas com atletas. Controle de status em tempo real,
                    gestao financeira completa e agendamentos simplificados em uma unica plataforma.
                </p>

                <div className="hero-cta">
                    <Link to="/register?type=partner" className="cta-card">
                        <div className="cta-card-overlay" />
                        <div className="cta-card-content">
                            <div className="cta-icon">
                                <Icons.Building />
                            </div>
                            <div className="cta-text">
                                <div className="cta-title">Sou Dono de arena</div>
                                <div className="cta-subtitle">Gerencie sua arena</div>
                            </div>
                        </div>
                    </Link>

                    <Link to="/register?type=client" className="cta-card cta-card-primary">
                        <div className="cta-card-overlay" />
                        <div className="cta-card-content">
                            <div className="cta-icon">
                                <Icons.User />
                            </div>
                            <div className="cta-text">
                                <div className="cta-title">Sou Atleta</div>
                                <div className="cta-subtitle">Reserve sua quadra</div>
                            </div>
                        </div>
                    </Link>
                </div>
            </div>
        </section>
    );
};

const FeaturesSection = () => {
    const features = [
        {
            icon: <Icons.Lightning />,
            title: "Controle de Status em Tempo Real",
            description: "Monitore todas as suas quadras em tempo real. Status atualizado instantaneamente para donos e atletas.",
            gradient: "gradient-1",
        },
        {
            icon: <Icons.Dollar />,
            title: "Gestao Financeira Completa",
            description: "Dashboard financeiro detalhado. Acompanhe receitas, despesas e lucros com graficos intuitivos.",
            gradient: "gradient-2",
        },
        {
            icon: <Icons.Calendar />,
            title: "Agendamento Simplificado",
            description: "Atletas reservam em segundos. Sistema inteligente evita conflitos e otimiza sua grade de horarios.",
            gradient: "gradient-3",
        },
        {
            icon: <Icons.Chart />,
            title: "Relatorios Inteligentes",
            description: "Analise de dados avancada. Descubra tendencias e tome decisoes baseadas em dados reais.",
            gradient: "gradient-4",
        },
        {
            icon: <Icons.Users />,
            title: "Gestao de Times",
            description: "Organize campeonatos e gerencie times. Sistema completo para torneios e competicoes.",
            gradient: "gradient-5",
        },
        {
            icon: <Icons.Mobile />,
            title: "App Mobile Nativo",
            description: "Acesse de qualquer lugar. Aplicativo nativo para iOS e Android com notificacoes em tempo real.",
            gradient: "gradient-6",
        },
    ];

    return (
        <section id="features" className="features">
            <div className="container">
                <div className="features-header">
                    <span className="section-label">Recursos</span>
                    <h2 className="section-title text-balance">
                        Tudo que voce precisa para
                        <br />
                        <span className="hero-title-gradient">dominar o jogo</span>
                    </h2>
                    <p className="section-subtitle text-pretty">
                        Uma plataforma completa que conecta donos de arenas com atletas,
                        simplificando a gestao e maximizando seus resultados.
                    </p>
                </div>

                <div className="features-grid">
                    {features.map((feature, index) => (
                        <div key={index} className="feature-card">
                            <div className={`feature-card-glow ${feature.gradient}`} />
                            <div className={`feature-icon ${feature.gradient}`}>
                                {feature.icon}
                            </div>
                            <h3 className="feature-title">{feature.title}</h3>
                            <p className="feature-description">{feature.description}</p>
                        </div>
                    ))}
                </div>
            </div>
        </section>
    );
};

const Footer = () => {
    const links = {
        suporte: ["Contato", "arennaConnect@gmal.com", "(41) 98489-0734"],
    };

    return (
        <footer className="footer">
            <div className="container">
                <div className="footer-grid">
                    <div className="footer-brand">
                        <a href="/" className="logo">
                            <img src="/Assets/3-removebg-preview.png" alt="logo" style={{width: "80px"}}/>
                            <span className="logo-text">ArenaConnect</span>
                        </a>
                        <p>
                            Conectando donos de arenas com atletas.
                            A plataforma completa para gestao esportiva.
                        </p>
                        <div className="social-links">
                            <a href="https://twitter.com" target="_blank" rel="noopener noreferrer" className="social-link">
                                <Icons.Twitter />
                            </a>
                            <a href="https://instagram.com" target="_blank" rel="noopener noreferrer" className="social-link">
                                <Icons.Instagram />
                            </a>
                            <a href="https://linkedin.com" target="_blank" rel="noopener noreferrer" className="social-link">
                                <Icons.LinkedIn />
                            </a>
                        </div>
                    </div>

                    <div className="footer-links">
                        <h4>Suporte</h4>
                        <ul>
                            {links.suporte.map((item) => (
                                <li key={item}><a href="#">{item}</a></li>
                            ))}
                        </ul>
                    </div>
                </div>

                <div className="footer-bottom">
                    <p className="footer-copyright">
                        2025 ArenaConnect. Todos os direitos reservados.
                    </p>
                    <div className="footer-legal">
                        <a href="#">Termos de Uso</a>
                        <a href="#">Privacidade</a>
                        <a href="#">Cookies</a>
                    </div>
                </div>
            </div>
        </footer>
    );
};

const isMobile = () => {
    window.matchMedia('(pointer: coarse)').matches;
}


const LandingPage = () => {
    const scrollRef = useRef(null);

    useEffect(() => {
        if (isMobile()) return;

        const container = scrollRef.current;
        if (!container) return;

        const handleScroll = (e) => {

            if (Math.abs(e.deltaX) > Math.abs(e.deltaY)) return;

            e.preventDefault();

            container.scrollLeft += e.deltaY * 1;
        };
        window.addEventListener('wheel', handleScroll, { passive: false });

        return () => {
            window.removeEventListener('wheel', handleScroll);
        };
    }, []);

    return (
        <div style={{ position: 'relative', width: '100vw', height: '100vh', overflow: 'hidden' }}>
            <LiquidBackground />
            <Navbar />

            <div ref={scrollRef} className="horizontal-outer-wrapper">
                <HeroSection />
                <FeaturesSection />
                <Footer />
            </div>
        </div>
    );
};

export default LandingPage;