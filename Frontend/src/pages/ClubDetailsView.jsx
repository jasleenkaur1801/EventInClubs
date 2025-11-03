import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { clubApi } from '../api/club';
import { eventApi } from '../api/event';
import './ClubDetailsView.css';

export default function ClubDetailsView() {
  const { clubId } = useParams();
  const navigate = useNavigate();
  const [club, setClub] = useState(null);
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [eventFilter, setEventFilter] = useState('all'); // 'all', 'active', 'past'

  useEffect(() => {
    fetchClubDetails();
    fetchClubEvents();
  }, [clubId]);

  const fetchClubDetails = async () => {
    try {
      const clubs = await clubApi.getAllClubs();
      const foundClub = clubs.find(c => c.id === parseInt(clubId));
      if (foundClub) {
        setClub(foundClub);
      } else {
        setError('Club not found');
      }
    } catch (err) {
      console.error('Error fetching club details:', err);
      setError('Failed to load club details');
    } finally {
      setLoading(false);
    }
  };

  const fetchClubEvents = async () => {
    try {
      const allEvents = await eventApi.getAllEvents();
      const clubEvents = allEvents.filter(event => event.clubId === parseInt(clubId));
      setEvents(clubEvents);
    } catch (err) {
      console.error('Error fetching club events:', err);
    }
  };

  const getCategoryColor = (category) => {
    const colors = {
      'technology': '#3b82f6',
      'design': '#8b5cf6',
      'engineering': '#10b981',
      'business': '#f59e0b',
      'arts': '#ec4899',
      'sports': '#ef4444',
      'science': '#06b6d4'
    };
    return colors[category?.toLowerCase()] || '#6b7280';
  };

  const getEventStatus = (event) => {
    const now = new Date();
    const eventDate = new Date(event.endDate || event.eventDate);
    
    // Check event status field if available
    if (event.status === 'COMPLETED' || event.status === 'CANCELLED') {
      return 'past';
    }
    
    // Check if event date has passed
    if (eventDate && !isNaN(eventDate) && now > eventDate) {
      return 'past';
    }
    
    // Check registration deadline (only if it exists)
    const registrationDeadlineValue = event.registrationEndDate || event.registrationDeadline;
    if (registrationDeadlineValue) {
      const registrationEnd = new Date(registrationDeadlineValue);
      if (!isNaN(registrationEnd) && now > registrationEnd && now < eventDate) {
        return 'registration-closed';
      }
    }
    
    return 'active';
  };

  const categorizeEvents = () => {
    const activeEvents = events.filter(e => getEventStatus(e) === 'active' || getEventStatus(e) === 'registration-closed');
    const pastEvents = events.filter(e => getEventStatus(e) === 'past');
    
    return { activeEvents, pastEvents, totalEvents: events.length };
  };

  const getFilteredEvents = () => {
    const { activeEvents, pastEvents } = categorizeEvents();
    
    switch(eventFilter) {
      case 'active':
        return activeEvents;
      case 'past':
        return pastEvents;
      case 'all':
      default:
        return events;
    }
  };

  const getStatusLabel = (status) => {
    switch(status) {
      case 'past': return 'Completed';
      case 'registration-closed': return 'Registration Closed';
      case 'active': return 'Active';
      default: return status;
    }
  };

  if (loading) {
    return (
      <div className="club-details-container">
        <div className="loading-spinner">
          <div className="spinner"></div>
          <p>Loading club details...</p>
        </div>
      </div>
    );
  }

  if (error || !club) {
    return (
      <div className="club-details-container">
        <div className="error-container">
          <h2>⚠️ {error || 'Club not found'}</h2>
          <button className="btn-primary" onClick={() => navigate('/admin/dashboard')}>
            Back to Dashboard
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="club-details-container">
      <div className="club-details-header">
        <button className="back-button" onClick={() => navigate('/admin/dashboard')}>
          ← Back to Dashboard
        </button>
      </div>

      <div className="club-details-content">
        {/* Club Header Section */}
        <div className="club-header-section">
          <div className="club-logo-large">
            {club.logoUrl ? (
              <img src={`http://localhost:8080${club.logoUrl}`} alt={`${club.name} logo`} />
            ) : (
              <div className="logo-placeholder">
                {club.shortName || club.name.split(' ').slice(0, 2).map(w => w[0]).join('')}
              </div>
            )}
          </div>
          
          <div className="club-header-info">
            <div className="club-title-section">
              <h1>{club.name}</h1>
              <span 
                className="club-category-badge" 
                style={{ backgroundColor: getCategoryColor(club.category) }}
              >
                {club.category}
              </span>
            </div>
            
            <p className="club-short-name">Short Name: {club.shortName}</p>
            
            <div className="club-status-badges">
              <span className={`status-badge ${club.isActive ? 'active' : 'inactive'}`}>
                {club.isActive ? '✓ Active' : '✗ Inactive'}
              </span>
              <span className="status-badge approval">
                {club.approvalStatus}
              </span>
            </div>
          </div>
        </div>

        {/* Club Description */}
        <div className="club-section">
          <h2>About This Club</h2>
          <p className="club-description">{club.description}</p>
        </div>

        {/* Club Statistics */}
        <div className="club-section">
          <h2>Statistics</h2>
          <div className="stats-grid">
            <div className="stat-box">
              <div className="stat-icon">👥</div>
              <div className="stat-info">
                <h3>{club.memberCount || 0}</h3>
                <p>Members</p>
              </div>
            </div>
            
            <div className="stat-box">
              <div className="stat-icon">📅</div>
              <div className="stat-info">
                <h3>{club.eventCount || 0}</h3>
                <p>Total Events</p>
              </div>
            </div>
            
            <div className="stat-box">
              <div className="stat-icon">📈</div>
              <div className="stat-info">
                <h3>
                  {(() => {
                    // Calculate average attendance rate: how full are events on average
                    if (events.length === 0) return 0;
                    const eventsWithCapacity = events.filter(e => e.maxParticipants > 0);
                    if (eventsWithCapacity.length === 0) return 0;
                    const totalAttendanceRate = eventsWithCapacity.reduce((sum, event) => {
                      const rate = (event.currentParticipants || 0) / event.maxParticipants;
                      return sum + rate;
                    }, 0);
                    const avgAttendanceRate = (totalAttendanceRate / eventsWithCapacity.length) * 100;
                    return Math.min(100, Math.round(avgAttendanceRate));
                  })()}%
                </h3>
                <p>Engagement Rate</p>
              </div>
            </div>
            
            <div className="stat-box">
              <div className="stat-icon">🎯</div>
              <div className="stat-info">
                <h3>{events.filter(e => getEventStatus(e) === 'active').length}</h3>
                <p>Active Events</p>
              </div>
            </div>
          </div>
        </div>

        {/* Events with Filters */}
        <div className="club-section">
          <div className="events-header">
            <h2>📅 Events</h2>
            <div className="event-filters">
              <button 
                className={`filter-btn ${eventFilter === 'all' ? 'active' : ''}`}
                onClick={() => setEventFilter('all')}
              >
                All Events ({categorizeEvents().totalEvents})
              </button>
              <button 
                className={`filter-btn ${eventFilter === 'active' ? 'active' : ''}`}
                onClick={() => setEventFilter('active')}
              >
                Active ({categorizeEvents().activeEvents.length})
              </button>
              <button 
                className={`filter-btn ${eventFilter === 'past' ? 'active' : ''}`}
                onClick={() => setEventFilter('past')}
              >
                Past ({categorizeEvents().pastEvents.length})
              </button>
            </div>
          </div>

          {getFilteredEvents().length > 0 ? (
            <div className="events-list">
              {getFilteredEvents().map(event => (
                <div key={event.id} className={`event-card ${getEventStatus(event) === 'past' ? 'past-event' : ''}`}>
                  <div className="event-header">
                    <h3>{event.title}</h3>
                    <span className={`event-status-badge ${getEventStatus(event)}`}>
                      {getStatusLabel(getEventStatus(event))}
                    </span>
                  </div>
                  <p className="event-description">{event.description}</p>
                  <div className="event-details">
                    <div className="event-detail-item">
                      <span className="detail-icon">📅</span>
                      <span>{new Date(event.startDate || event.eventDate).toLocaleDateString()}</span>
                    </div>
                    <div className="event-detail-item">
                      <span className="detail-icon">📍</span>
                      <span>{event.location}</span>
                    </div>
                    <div className="event-detail-item">
                      <span className="detail-icon">👥</span>
                      <span>
                        {getEventStatus(event) === 'past' 
                          ? `${event.currentParticipants || 0} participants`
                          : `${event.maxParticipants} max participants`
                        }
                      </span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="no-events">
              <p>
                {eventFilter === 'active' ? 'No active events at the moment' :
                 eventFilter === 'past' ? 'No past events yet' :
                 'No events created yet'}
              </p>
            </div>
          )}
        </div>

        {/* Admin Information */}
        <div className="club-section">
          <h2>Administration</h2>
          <div className="admin-info">
            <p><strong>Admin:</strong> {club.adminUserName || 'Not assigned'}</p>
            <p><strong>Created:</strong> {new Date(club.createdAt).toLocaleDateString()}</p>
            <p><strong>Last Updated:</strong> {new Date(club.updatedAt).toLocaleDateString()}</p>
          </div>
        </div>
      </div>
    </div>
  );
}
