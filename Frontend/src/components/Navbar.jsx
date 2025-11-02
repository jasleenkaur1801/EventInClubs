import React, { useState, useEffect } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import NotificationBell from "./NotificationBell";
import AchievementBadge from "./AchievementBadge";
import "./Navbar.css";

export default function Navbar() {
  const [forceUpdate, setForceUpdate] = useState(0);
  const navigate = useNavigate();
  const location = useLocation();
  
  // Force component to re-render when needed
  const triggerUpdate = () => setForceUpdate(prev => prev + 1);
  
  // Get fresh auth data on every render
  const token = localStorage.getItem("token");
  const role = localStorage.getItem("role");
  // SUPER_ADMIN functionality enabled
  
  const isAuthenticated = token;
  const isSuperAdmin = role === 'SUPER_ADMIN';
  const isAdmin = role === 'ADMIN' || role === 'CLUB_ADMIN';
  
  // Update when location changes
  useEffect(() => {
    triggerUpdate();
  }, [location]);
  
  // Listen for custom auth events
  useEffect(() => {
    const handleAuthChange = () => triggerUpdate();
    window.addEventListener('authStateChanged', handleAuthChange);
    return () => window.removeEventListener('authStateChanged', handleAuthChange);
  }, []);

  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("role");
    localStorage.removeItem("email");
    localStorage.removeItem("userRole");
    localStorage.removeItem("userId");
    localStorage.removeItem("user");
    localStorage.removeItem("rememberMe");
    triggerUpdate();
    navigate("/login");
  };

  return (
    <header className="topbar">
      <div className="container topbar-inner">
        <div className="brand" onClick={() => navigate("/")}>
          <span className="logo-emoji">🎪</span>
          <div>
            <div className="brand-title">Event Idea Marketplace</div>
            <div className="brand-sub">Clubs • Ideas • Votes</div>
          </div>
        </div>

        <nav className="navlinks">
          {!isAdmin && !isSuperAdmin && <Link to="/">Home</Link>}
          {token && !isAdmin && !isSuperAdmin && <Link to="/clubs">Clubs</Link>}
          {token && !isAdmin && !isSuperAdmin && <Link to="/club-topics" className="nav-link">Club Topics</Link>}
          {token && !isAdmin && !isSuperAdmin && <Link to="/active-events" className="nav-link">🎪 Active Events</Link>}
          {token && !isAdmin && !isSuperAdmin && <Link to="/leaderboard">🏆 Leaderboard</Link>}
          {!isAuthenticated ? (
            <>
              <Link to="/login">Login</Link>
              <Link to="/register" className="btn-outline">Register</Link>
            </>
          ) : (
            <div className="user-actions">
              {/* SUPER_ADMIN functionality enabled */}
              {isSuperAdmin && <span className="admin-badge">🔐 Super Admin</span>}
              {!isAdmin && !isSuperAdmin && <AchievementBadge />}
              {!isAdmin && !isSuperAdmin && <NotificationBell />}
              <button className="btn-ghost" onClick={logout}>Logout</button>
            </div>
          )}
        </nav>
      </div>
    </header>
  );
}
