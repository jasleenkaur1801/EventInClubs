import React, { useState, useEffect } from 'react';
import { eventApi } from '../api/event';
import http from '../api/http';
import './ActiveEvents.css';

const ActiveEvents = () => {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedEvent, setSelectedEvent] = useState(null);
  const [showRegistrationModal, setShowRegistrationModal] = useState(false);
  const [registrationData, setRegistrationData] = useState({
    notes: '',
    rollNumber: ''
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [filter, setFilter] = useState('all');

  useEffect(() => {
    fetchActiveEvents();
  }, []);

  const fetchActiveEvents = async () => {
    try {
      setLoading(true);
      // Fetch active events specifically for students
      const response = await http.get('/events/active');
      const activeEvents = response.data;
      
      setEvents(activeEvents);
    } catch (error) {
      console.error('Error fetching active events:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleRegisterClick = (event) => {
    setSelectedEvent(event);
    setShowRegistrationModal(true);
    setRegistrationData({ notes: '', rollNumber: '' });
  };

  const handleRegistrationSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);

    try {
      const user = JSON.parse(localStorage.getItem('user'));
      if (!user) {
        alert('Please log in to register for events');
        return;
      }

      const params = new URLSearchParams({
        eventId: selectedEvent.id,
        userId: user.id,
        rollNumber: registrationData.rollNumber,
        notes: registrationData.notes
      });
      
      const response = await http.post(`/event-registrations/register?${params.toString()}`);

      if (response.status === 200) {
        const result = response.data;
        alert('Registration successful! You will receive a confirmation notification.');
        setShowRegistrationModal(false);
        fetchActiveEvents(); // Refresh events to update registration count
      } else {
        const error = response.data;
        alert(`Registration failed: ${error.error || 'Please try again'}`);
      }
    } catch (error) {
      console.error('Error registering for event:', error);
      alert('Registration failed. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const filteredEvents = events.filter(event => {
    if (filter === 'all') return true;
    if (filter === 'free') return event.registrationFee === 0;
    if (filter === 'paid') return event.registrationFee > 0;
    if (filter === 'workshop') return event.type === 'WORKSHOP';
    if (filter === 'hackathon') return event.type === 'HACKATHON';
    if (filter === 'seminar') return event.type === 'SEMINAR';
    return true;
  });

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const isRegistrationOpen = (event) => {
    const now = new Date();
    const startDate = new Date(event.startDate);
    const registrationDeadline = event.registrationDeadline ? new Date(event.registrationDeadline) : startDate;
    
    return now < registrationDeadline && 
           (event.maxParticipants === null || event.currentParticipants < event.maxParticipants);
  };

  const getAvailableSpots = (event) => {
    if (!event.maxParticipants) return 'Unlimited';
    return event.maxParticipants - event.currentParticipants;
  };

  if (loading) {
    return (
      <div className="active-events-container">
        <div className="loading-spinner">
          <div className="spinner"></div>
          <p>Loading active events...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="active-events-container">
      <div className="page-header">
        <h1>🎪 Active Events</h1>
        <p>Discover and register for exciting events happening around campus</p>
      </div>

      <div className="filters-section">
        <div className="filter-buttons">
          <button 
            className={filter === 'all' ? 'active' : ''}
            onClick={() => setFilter('all')}
          >
            All Events
          </button>
          <button 
            className={filter === 'free' ? 'active' : ''}
            onClick={() => setFilter('free')}
          >
            Free Events
          </button>
          <button 
            className={filter === 'paid' ? 'active' : ''}
            onClick={() => setFilter('paid')}
          >
            Paid Events
          </button>
          <button 
            className={filter === 'workshop' ? 'active' : ''}
            onClick={() => setFilter('workshop')}
          >
            Workshops
          </button>
          <button 
            className={filter === 'hackathon' ? 'active' : ''}
            onClick={() => setFilter('hackathon')}
          >
            Hackathons
          </button>
          <button 
            className={filter === 'seminar' ? 'active' : ''}
            onClick={() => setFilter('seminar')}
          >
            Seminars
          </button>
        </div>
      </div>

      {filteredEvents.length === 0 ? (
        <div className="no-events">
          <div className="no-events-icon">🎭</div>
          <h3>No Active Events</h3>
          <p>There are no active events matching your criteria at the moment. Check back soon!</p>
        </div>
      ) : (
        <div className="events-grid">
          {filteredEvents.map(event => (
            <div key={event.id} className="event-card">
              {event.imageUrl && (
                <div className="event-image">
                  <img src={event.imageUrl} alt={event.title} />
                </div>
              )}
              
              <div className="event-content">
                <div className="event-header">
                  <h3>{event.title}</h3>
                  <div className="event-badges">
                    <span className={`event-type ${event.type?.toLowerCase()}`}>
                      {event.type}
                    </span>
                    <span className={`event-fee ${event.registrationFee === 0 ? 'free' : 'paid'}`}>
                      {event.registrationFee === 0 ? 'FREE' : `₹${event.registrationFee}`}
                    </span>
                  </div>
                </div>

                <div className="event-club">
                  <span className="club-label">🏛️ Organized by:</span>
                  <span className="club-name">{event.clubName}</span>
                </div>

                <div className="event-details">
                  {event.hallName && (
                    <div className="detail-item">
                      <span className="detail-icon">🏛️</span>
                      <div className="detail-content">
                        <strong>Hall:</strong> {event.hallName}
                      </div>
                    </div>
                  )}
                  <div className="detail-item">
                    <span className="detail-icon">📅</span>
                    <div className="detail-content">
                      <strong>Start:</strong> {formatDate(event.startDate)}
                    </div>
                  </div>
                  
                  <div className="detail-item">
                    <span className="detail-icon">🏁</span>
                    <div className="detail-content">
                      <strong>End:</strong> {formatDate(event.endDate)}
                    </div>
                  </div>
                  
                  <div className="detail-item">
                    <span className="detail-icon">📍</span>
                    <div className="detail-content">
                      <strong>Location:</strong> {event.location}
                    </div>
                  </div>
                  
                  <div className="detail-item">
                    <span className="detail-icon">👥</span>
                    <div className="detail-content">
                      <strong>Capacity:</strong> {event.currentParticipants} / {event.maxParticipants || '∞'}
                      <span className="spots-available">
                        ({getAvailableSpots(event)} spots available)
                      </span>
                    </div>
                  </div>
                </div>

                {event.description && (
                  <div className="event-description">
                    <h4>About this Event</h4>
                    <p>{event.description}</p>
                  </div>
                )}

                <div className="event-actions">
                  {isRegistrationOpen(event) ? (
                    <button 
                      className="register-btn"
                      onClick={() => handleRegisterClick(event)}
                    >
                      🎫 Register Now
                    </button>
                  ) : (
                    <button className="register-btn disabled" disabled>
                      {event.currentParticipants >= event.maxParticipants ? 
                        '🚫 Event Full' : '⏰ Registration Closed'}
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Registration Modal */}
      {showRegistrationModal && (
        <div className="modal-overlay">
          <div className="registration-modal">
            <div className="modal-header">
              <h3>Register for Event</h3>
              <button 
                className="close-btn"
                onClick={() => setShowRegistrationModal(false)}
              >
                ×
              </button>
            </div>
            
            <div className="modal-content">
              <div className="event-summary">
                <h4>{selectedEvent?.title}</h4>
                <p><strong>Club:</strong> {selectedEvent?.clubName}</p>
                <p><strong>Date:</strong> {selectedEvent && formatDate(selectedEvent.startDate)}</p>
                <p><strong>Location:</strong> {selectedEvent?.location}</p>
                <p><strong>Fee:</strong> {selectedEvent?.registrationFee === 0 ? 'Free' : `₹${selectedEvent?.registrationFee}`}</p>
              </div>

              <form onSubmit={handleRegistrationSubmit}>
                <div className="form-group">
                  <label htmlFor="rollNumber">Roll Number *</label>
                  <input
                    id="rollNumber"
                    type="text"
                    value={registrationData.rollNumber}
                    onChange={(e) => setRegistrationData({...registrationData, rollNumber: e.target.value})}
                    placeholder="Enter your roll number"
                    required
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="notes">Additional Notes (Optional)</label>
                  <textarea
                    id="notes"
                    value={registrationData.notes}
                    onChange={(e) => setRegistrationData({...registrationData, notes: e.target.value})}
                    placeholder="Any special requirements, dietary restrictions, or questions..."
                    rows="4"
                  />
                </div>

                <div className="modal-actions">
                  <button 
                    type="button" 
                    className="cancel-btn"
                    onClick={() => setShowRegistrationModal(false)}
                  >
                    Cancel
                  </button>
                  <button 
                    type="submit" 
                    className="submit-btn"
                    disabled={isSubmitting}
                  >
                    {isSubmitting ? 'Registering...' : 'Confirm Registration'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ActiveEvents;
