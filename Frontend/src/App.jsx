import React from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import Navbar from "./components/Navbar";
import Footer from "./components/Footer";
import Home from "./pages/Home";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Clubs from "./pages/Clubs";
import Problems from './pages/Problems';
import ClubTopics from './pages/ClubTopics';
import ClubAdminDashboard from './pages/ClubAdminDashboard';
import ActiveEvents from './pages/ActiveEvents';
// SUPER_ADMIN functionality enabled
import SuperAdminDashboard from './pages/SuperAdminDashboard';
import ViewIdeas from "./pages/ViewIdeas";
import Leaderboard from "./pages/Leaderboard";
import ProtectedRoute from "./components/ProtectedRoute";
import NotificationBell from "./components/NotificationBell";
import AchievementBadge from "./components/AchievementBadge";
import "./App.css";

export default function App() {
  return (
    <BrowserRouter>
      <Navbar />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/clubs" element={<ProtectedRoute><Clubs /></ProtectedRoute>} />
        <Route path="/topics" element={<ProtectedRoute><ClubTopics /></ProtectedRoute>} />
        <Route path="/leaderboard" element={<ProtectedRoute><Leaderboard /></ProtectedRoute>} />
        <Route path="/events/:eventId/ideas" element={<ProtectedRoute><ViewIdeas /></ProtectedRoute>} />
        <Route path="/admin/dashboard" element={<ProtectedRoute><ClubAdminDashboard /></ProtectedRoute>} />
        {/* SUPER_ADMIN functionality enabled */}
        <Route path="/superadmin/dashboard" element={<ProtectedRoute><SuperAdminDashboard /></ProtectedRoute>} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/problems" element={<Problems />} />
        <Route path="/club-topics" element={<ClubTopics />} />
        <Route path="/active-events" element={<ActiveEvents />} />
        <Route path="/view-ideas/:eventId" element={<ViewIdeas />} />
        {/* Example protected page */}
        <Route path="/dashboard" element={<ProtectedRoute><div style={{ padding: 24 }}>Dashboard (protected)</div></ProtectedRoute>} />
      </Routes>
      <Footer />
    </BrowserRouter>
  );
}
