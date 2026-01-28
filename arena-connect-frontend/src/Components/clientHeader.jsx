import '../Styles/HomeClient.css';

export default function clientHeader (){
    const username = localStorage.getItem('userName') || 'Atleta';
    const userInitial = username.charAt(0).toUpperCase();

    return(
        <header className="client-header glass-panel">
            <div className="header-user">
                <div className="user-avatar">
                    <span>{userInitial}</span>
                </div>
                <div className="user-greeting">
                    <p>Olá, {username}</p>
                    <h3 style={{ fontFamily: 'inherit'}}>Bora jogar hoje? </h3>
                </div>
            </div>
            <button className="btn-icon-glass">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                    <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
                </svg>
            </button>
        </header>
    );
}