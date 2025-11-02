import React, { useState, useEffect } from 'react';
import { clubApi } from '../api/club';
import { eventApi } from '../api/event.js';
import { userApi } from '../api/user';
import { analyticsApi } from '../api/analytics';
import { superAdminRequestApi } from '../api/superAdminRequests';
import { clubAdminRequestApi } from '../api/clubAdminRequests';
import SuperAdminRequests from './SuperAdminRequests';
import ClubAdminRequests from './ClubAdminRequests';
import SuperAdminApprovalDashboard from '../components/SuperAdminApprovalDashboard';
import './SuperAdminDashboard.css';

export default function SuperAdminDashboard() {
  const [activeTab, setActiveTab] = useState('overview');
  const [clubs, setClubs] = useState([]);
  const [events, setEvents] = useState([]);
  const [users, setUsers] = useState([]);
  const [analytics, setAnalytics] = useState({});
  const [pendingSuperAdminRequests, setPendingSuperAdminRequests] = useState(0);
  const [pendingClubAdminRequests, setPendingClubAdminRequests] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchAllData();
  }, []);

  const fetchAllData = async () => {
    try {
      await Promise.all([
        fetchClubs(),
        fetchEvents(),
        fetchUsers(),
        fetchAnalytics(),
        fetchPendingSuperAdminRequests(),
        fetchPendingClubAdminRequests()
      ]);
      setLoading(false);
    } catch (error) {
      console.error('Error fetching data:', error);
      setLoading(false);
    }
  };

  const fetchClubs = async () => {
    try {
      const clubsData = await clubApi.getAllClubs();
      setClubs(clubsData || []);
    } catch (error) {
      console.error('Error fetching clubs:', error);
      setClubs([]);
    }
  };

  const fetchEvents = async () => {
    try {
      const eventsData = await eventApi.getEventsAcceptingIdeas();
      setEvents(eventsData || []);
    } catch (error) {
      console.error('Error fetching events:', error);
      setEvents([]);
    }
  };

  const fetchUsers = async () => {
    try {
      const usersData = await userApi.getAllUsers();
      setUsers(usersData || []);
    } catch (error) {
      console.error('Error fetching users:', error);
      setUsers([]);
    }
  };

  const fetchAnalytics = async () => {
    try {
      const analyticsData = await analyticsApi.getSystemAnalytics();
      setAnalytics(analyticsData || {});
    } catch (error) {
      console.error('Error fetching analytics:', error);
      setAnalytics({});
    }
  };

  const fetchPendingSuperAdminRequests = async () => {
    try {
      const response = await superAdminRequestApi.getPendingRequestsCount();
      setPendingSuperAdminRequests(response.data.pendingRequests || 0);
    } catch (error) {
      console.error('Error fetching pending super admin requests:', error);
      setPendingSuperAdminRequests(0);
    }
  };

  const fetchPendingClubAdminRequests = async () => {
    try {
      const response = await clubAdminRequestApi.getPendingRequestsCount();
      setPendingClubAdminRequests(response.data.pendingRequests || 0);
    } catch (error) {
      console.error('Error fetching pending club admin requests:', error);
      setPendingClubAdminRequests(0);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('superAdminToken');
    localStorage.removeItem('userRole');
    localStorage.removeItem('userId');
    localStorage.removeItem('email');
    window.location.href = '/login';
  };

  const renderOverview = () => (
    <div className="overview-section">
      <h2>Super Admin Overview</h2>
      
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon">🏢</div>
          <div className="stat-content">
            <h3>{clubs.length}</h3>
            <p>Active Clubs</p>
          </div>
        </div>
        
        <div className="stat-card">
          <div className="stat-icon">📅</div>
          <div className="stat-content">
            <h3>{events.length}</h3>
            <p>Active Events</p>
          </div>
        </div>
        
        <div className="stat-card">
          <div className="stat-icon">👥</div>
          <div className="stat-content">
            <h3>{users.length}</h3>
            <p>Total Users</p>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon">👑</div>
          <div className="stat-content">
            <h3>{pendingSuperAdminRequests}</h3>
            <p>Super Admin Requests</p>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon">🏢</div>
          <div className="stat-content">
            <h3>{pendingClubAdminRequests}</h3>
            <p>Club Admin Requests</p>
          </div>
        </div>
      </div>

      <div className="recent-activity">
        <h3>Recent Activity</h3>
        <div className="activity-list">
          <div className="activity-item">
            <div className="activity-icon">📄</div>
            <div className="activity-content">
              <p>New club registration: Coding Ninjas CUIET</p>
              <span>2 hours ago</span>
            </div>
          </div>
          <div className="activity-item">
            <div className="activity-icon">✅</div>
            <div className="activity-content">
              <p>Club approved: Tech Innovation Club</p>
              <span>1 day ago</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );

  const renderSystemManagement = () => (
    <div className="system-management-section">
      <h2>System Management</h2>
      
      <div className="management-grid">
        <div className="management-card">
          <h3>🏢 All Clubs</h3>
          <p>{clubs.length} active clubs</p>
          <div className="clubs-list">
            {clubs.slice(0, 5).map(club => (
              <div key={club.id} className="club-item">
                <span>{club.name}</span>
                <span className={`status ${club.isActive ? 'active' : 'inactive'}`}>
                  {club.isActive ? 'Active' : 'Inactive'}
                </span>
              </div>
            ))}
          </div>
        </div>
        
        <div className="management-card">
          <h3>👥 User Management</h3>
          <p>{users.length} total users</p>
          <div className="users-list">
            {users.slice(0, 5).map(user => (
              <div key={user.id} className="user-item">
                <span>{user.name}</span>
                <span className="user-role">{user.role}</span>
              </div>
            ))}
          </div>
        </div>
        
        <div className="management-card">
          <h3>📊 Analytics</h3>
          <div className="analytics-stats">
            <div className="stat-row">
              <span>Active Events:</span>
              <span>{analytics.activeEvents || 0}</span>
            </div>
            <div className="stat-row">
              <span>Club Admins:</span>
              <span>{analytics.clubAdmins || 0}</span>
            </div>
            <div className="stat-row">
              <span>Students:</span>
              <span>{analytics.students || 0}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );

  const renderContent = () => {
    switch (activeTab) {
      case 'overview':
        return renderOverview();
      case 'event-approvals':
        return <SuperAdminApprovalDashboard />;
      case 'super-admin-requests':
        return <SuperAdminRequests />;
      case 'club-admin-requests':
        return <ClubAdminRequests />;
      case 'system':
        return renderSystemManagement();
      default:
        return renderOverview();
    }
  };

  return (
    <div className="super-admin-dashboard">
      <div className="dashboard-sidebar">
        <div className="sidebar-header">
          <h2>🔐 Super Admin</h2>
          <p>EventInClubs System</p>
        </div>
        <nav className="sidebar-nav">
          <button
            className={`nav-item ${activeTab === 'overview' ? 'active' : ''}`}
            onClick={() => setActiveTab('overview')}
          >
            <span className="nav-icon">📊</span>
            Overview
          </button>
          <button
            className={`nav-item ${activeTab === 'event-approvals' ? 'active' : ''}`}
            onClick={() => setActiveTab('event-approvals')}
          >
            <span className="nav-icon">📅</span>
            Event Approvals
          </button>
          <button
            className={`nav-item ${activeTab === 'super-admin-requests' ? 'active' : ''}`}
            onClick={() => setActiveTab('super-admin-requests')}
          >
            <span className="nav-icon">👑</span>
            Super Admin Requests
            {pendingSuperAdminRequests > 0 && (
              <span className="nav-badge">{pendingSuperAdminRequests}</span>
            )}
          </button>
          <button
            className={`nav-item ${activeTab === 'club-admin-requests' ? 'active' : ''}`}
            onClick={() => setActiveTab('club-admin-requests')}
          >
            <span className="nav-icon">🏢</span>
            Club Admin Requests
            {pendingClubAdminRequests > 0 && (
              <span className="nav-badge">{pendingClubAdminRequests}</span>
            )}
          </button>
          <button
            className={`nav-item ${activeTab === 'system' ? 'active' : ''}`}
            onClick={() => setActiveTab('system')}
          >
            <span className="nav-icon">⚙️</span>
            System Management
          </button>
        </nav>
        
        <div className="sidebar-footer">
          <button className="logout-btn" onClick={handleLogout}>
            🚪 Logout
          </button>
        </div>
      </div>

      <div className="dashboard-main">
        <div className="dashboard-header">
          <h1>Super Admin Dashboard</h1>
          <div className="header-actions">
            <div className="admin-info">
              <span>👤 Super Administrator</span>
            </div>
          </div>
        </div>

        <div className="dashboard-content">
          {loading ? (
            <div className="loading-spinner">
              <div className="spinner"></div>
              <p>Loading dashboard...</p>
            </div>
          ) : (
            renderContent()
          )}
        </div>
      </div>
    </div>
  );
}
