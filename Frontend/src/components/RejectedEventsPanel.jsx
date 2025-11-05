import React, { useState, useEffect } from 'react';
import EventCreationModal from './EventCreationModal';
import './RejectedEventsPanel.css';
import http from '../api/http';

const RejectedEventsPanel = ({ clubId }) => {
  const [rejectedEvents, setRejectedEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedEvent, setSelectedEvent] = useState(null);
  const [showEditModal, setShowEditModal] = useState(false);

  useEffect(() => {
    fetchRejectedEvents();
  }, [clubId]);

  const fetchRejectedEvents = async () => {
    try {
      setLoading(true);
      console.log('=== Fetching Rejected Events ===');
      console.log('Club ID:', clubId);
      
      const token = localStorage.getItem('token');
      const headers = {
        'Content-Type': 'application/json'
      };
      
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      
      const url = clubId && clubId !== 'all' ? 
        `/events/rejected/${clubId}` : 
        `/events/rejected`;
      
      console.log('Request URL:', url);
      console.log('Headers:', headers);
      
      const response = await http.get(url, {
        headers: headers
      });
      
      console.log('Response status:', response.status);
      
      if (response.status === 200) {
        const events = response.data;
        console.log('Rejected events fetched:', events);
        setRejectedEvents(events);
      } else {
        console.error('Failed to fetch rejected events. Status:', response.status);
      }
    } catch (error) {
      console.error('Error fetching rejected events:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleEditEvent = (event) => {
    setSelectedEvent(event);
    setShowEditModal(true);
  };

  const handleResubmitEvent = async (eventData) => {
    try {
      const token = localStorage.getItem('token');
      const headers = {
        'Content-Type': 'application/json'
      };
      
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      // Use the resubmit endpoint with all the updated event data
      const params = new URLSearchParams({
        eventId: selectedEvent.id.toString(),
        eventName: eventData.title,
        eventType: eventData.type,
        startDateTime: eventData.startDateTime,
        endDateTime: eventData.endDateTime,
        maxParticipants: eventData.maxParticipants.toString(),
        registrationFee: (eventData.registrationFee || 0).toString()
      });
      
      if (eventData.description) {
        params.append('description', eventData.description);
      }
      
      if (eventData.hallId) {
        params.append('hallId', eventData.hallId.toString());
      }

      const submitResponse = await http.post(`/events/resubmit-rejected?${params.toString()}`, eventData, {
        headers: headers
      });

      if (submitResponse.status === 200) {
        alert('Event resubmitted successfully! It will be reviewed by the Super Admin.');
        setShowEditModal(false);
        setSelectedEvent(null);
        fetchRejectedEvents(); // Refresh the list
      } else {
        const error = submitResponse.data || {};
        throw new Error(error.error || 'Failed to resubmit event');
      }
    } catch (error) {
      console.error('Error resubmitting event:', error);
      alert(`Failed to resubmit event: ${error.message}`);
    }
  };

  const formatDateTime = (dateTime) => {
    if (!dateTime) return 'Not specified';
    return new Date(dateTime).toLocaleString();
  };

  if (loading) {
    return (
      <div className="rejected-events-panel">
        <div className="loading">Loading rejected events...</div>
      </div>
    );
  }

  return (
    <div className="rejected-events-panel">
      <div className="panel-header">
        <h2>Rejected Events</h2>
        <p>Review feedback and resubmit your events for approval</p>
      </div>

      {rejectedEvents.length === 0 ? (
        <div className="no-events">
          <h3>No rejected events</h3>
          <p>All your submitted events are either approved or pending review.</p>
        </div>
      ) : (
        <div className="events-list">
          {rejectedEvents.map(event => (
            <div key={event.id} className="rejected-event-card">
              <div className="event-header">
                <div className="event-title-section">
                  <h3>{event.title}</h3>
                  <span className="event-type">{event.type}</span>
                </div>
                <div className="rejection-date">
                  Rejected on {formatDateTime(event.approvalDate)}
                </div>
              </div>

              <div className="event-details">
                <div className="detail-row">
                  <span className="label">Date & Time:</span>
                  <span className="value">
                    {formatDateTime(event.startDate)} - {formatDateTime(event.endDate)}
                  </span>
                </div>

                <div className="detail-row">
                  <span className="label">Participants:</span>
                  <span className="value">{event.maxParticipants}</span>
                </div>

                <div className="detail-row">
                  <span className="label">Registration Fee:</span>
                  <span className="value">₹{event.registrationFee || 0}</span>
                </div>

                {event.hallName && (
                  <div className="detail-row">
                    <span className="label">Hall:</span>
                    <span className="value">{event.hallName} (Capacity: {event.hallCapacity})</span>
                  </div>
                )}

                {event.description && (
                  <div className="detail-row">
                    <span className="label">Description:</span>
                    <span className="value description">{event.description}</span>
                  </div>
                )}
              </div>

              <div className="rejection-reason">
                <h4>Rejection Reason:</h4>
                <div className="reason-text">
                  {event.rejectionReason}
                </div>
                <div className="rejected-by">
                  - {event.approvedByName || 'Super Admin'}
                </div>
              </div>

              <div className="event-actions">
                <button 
                  className="edit-resubmit-button"
                  onClick={() => handleEditEvent(event)}
                >
                  Edit & Resubmit
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Edit and Resubmit Modal */}
      {showEditModal && selectedEvent && (
        <EventCreationModal
          onClose={() => {
            setShowEditModal(false);
            setSelectedEvent(null);
          }}
          onSubmit={handleResubmitEvent}
          clubId={clubId}
          initialData={{
            title: selectedEvent.title,
            description: selectedEvent.description,
            type: selectedEvent.type,
            startDate: selectedEvent.startDate ? selectedEvent.startDate.split('T')[0] : '',
            endDate: selectedEvent.endDate ? selectedEvent.endDate.split('T')[0] : '',
            startTime: selectedEvent.startDate ? selectedEvent.startDate.split('T')[1]?.substring(0, 5) : '',
            endTime: selectedEvent.endDate ? selectedEvent.endDate.split('T')[1]?.substring(0, 5) : '',
            maxParticipants: selectedEvent.maxParticipants,
            registrationFee: selectedEvent.registrationFee,
            selectedHall: selectedEvent.hallId || ''
          }}
          isResubmission={true}
          rejectionReason={selectedEvent.rejectionReason}
        />
      )}
    </div>
  );
};

export default RejectedEventsPanel;
