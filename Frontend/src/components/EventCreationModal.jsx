import React, { useState, useEffect } from 'react';
import { hallApi } from '../api/hall';
import './EventCreationModal.css';

const EventCreationModal = ({ onClose, onSubmit, clubId, initialData = null, isResubmission = false, rejectionReason = null }) => {
  const [formData, setFormData] = useState(initialData || {
    title: '',
    description: '',
    type: 'WORKSHOP',
    startDate: '',
    endDate: '',
    startTime: '',
    endTime: '',
    maxParticipants: '',
    registrationFee: 0,
    selectedHall: '',
    isTeamEvent: false,
    minTeamMembers: '',
    maxTeamMembers: ''
  });

  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [availableHalls, setAvailableHalls] = useState([]);
  const [suggestedHall, setSuggestedHall] = useState(null);
  const [loadingHalls, setLoadingHalls] = useState(false);
  const [hallsMessage, setHallsMessage] = useState('');

  const eventTypes = [
    'WORKSHOP', 'SEMINAR', 'COMPETITION', 'HACKATHON', 'CONFERENCE',
    'NETWORKING', 'SOCIAL', 'SPORTS', 'CULTURAL', 'TECHNICAL', 'OTHER'
  ];

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));

    // Clear specific error when user starts typing
    if (errors[name]) {
      setErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }

    // Fetch halls when relevant fields change
    if (['maxParticipants', 'startDate', 'endDate', 'startTime', 'endTime'].includes(name)) {
      // Debounce the hall fetching
      setTimeout(() => {
        fetchAvailableHalls();
      }, 500);
    }
  };

  const fetchAvailableHalls = async () => {
    const { maxParticipants, startDate, endDate, startTime, endTime } = formData;
    
    // Only fetch if we have all required data
    if (!maxParticipants || !startDate || !endDate || !startTime || !endTime) {
      setAvailableHalls([]);
      setSuggestedHall(null);
      setHallsMessage('');
      return;
    }

    const participants = parseInt(maxParticipants);
    if (isNaN(participants) || participants <= 0) {
      return;
    }

    setLoadingHalls(true);
    setHallsMessage('');

    try {
      const startDateTime = new Date(`${startDate}T${startTime}`);
      const endDateTime = new Date(`${endDate}T${endTime}`);

      // Validate date/time
      if (startDateTime >= endDateTime) {
        setHallsMessage('Please set valid start and end times');
        setAvailableHalls([]);
        setSuggestedHall(null);
        return;
      }

      const halls = await hallApi.getAvailableHalls(participants, startDateTime, endDateTime);
      
      console.log('API returned halls:', halls);
      console.log('Current participants:', participants);
      
      // Filter halls to ensure they have sufficient capacity
      const suitableHalls = halls.filter(hall => {
        const hasCapacity = hall.seatingCapacity >= participants;
        console.log(`Hall ${hall.name} capacity ${hall.seatingCapacity} >= ${participants}? ${hasCapacity}`);
        return hasCapacity;
      });
      
      console.log('Suitable halls after filtering:', suitableHalls);
      
      if (suitableHalls.length === 0) {
        setHallsMessage(`No halls available with capacity for ${participants} participants at the selected time`);
        setAvailableHalls([]);
        setSuggestedHall(null);
        setFormData(prev => ({ ...prev, selectedHall: '' }));
      } else {
        setAvailableHalls(suitableHalls);
        
        // Find the best fit hall - smallest hall that can accommodate all participants
        // Sort by capacity first to ensure we get the smallest suitable hall
        const sortedHalls = [...suitableHalls].sort((a, b) => a.seatingCapacity - b.seatingCapacity);
        const bestFit = sortedHalls[0];
        
        console.log('Best fit hall selected:', bestFit);
        
        setSuggestedHall(bestFit);
        const excessCapacity = bestFit.seatingCapacity - participants;
        const efficiencyMessage = excessCapacity <= 20 ? ' (Optimal fit)' : ' (Large hall)';
        setHallsMessage(`${suitableHalls.length} suitable hall(s) available. Suggested: ${bestFit.name} (Capacity: ${bestFit.seatingCapacity})${efficiencyMessage}`);
        
        console.log('Setting suggested hall:', bestFit.name, 'with capacity:', bestFit.seatingCapacity);
        
        // Auto-select the suggested hall if no hall is currently selected
        if (!formData.selectedHall) {
          setFormData(prev => ({ ...prev, selectedHall: bestFit.id.toString() }));
        }
      }
    } catch (error) {
      console.error('Error fetching available halls:', error);
      setHallsMessage('Error fetching available halls');
      setAvailableHalls([]);
      setSuggestedHall(null);
    } finally {
      setLoadingHalls(false);
    }
  };

  const validateForm = () => {
    const newErrors = {};

    if (!formData.title.trim()) {
      newErrors.title = 'Event title is required';
    }

    if (!formData.startDate) {
      newErrors.startDate = 'Start date is required';
    }

    if (!formData.endDate) {
      newErrors.endDate = 'End date is required';
    }

    if (!formData.startTime) {
      newErrors.startTime = 'Start time is required';
    }

    if (!formData.endTime) {
      newErrors.endTime = 'End time is required';
    }

    if (formData.startDate && formData.endDate && formData.startTime && formData.endTime) {
      const startDateTime = new Date(`${formData.startDate}T${formData.startTime}`);
      const endDateTime = new Date(`${formData.endDate}T${formData.endTime}`);

      if (startDateTime >= endDateTime) {
        newErrors.endDate = 'End date/time must be after start date/time';
      }

      // Check if dates are in the future
      const now = new Date();
      if (startDateTime <= now) {
        newErrors.startDate = 'Start date/time must be in the future';
      }
    }

    if (!formData.maxParticipants) {
      newErrors.maxParticipants = 'Max participants is required';
    } else if (isNaN(formData.maxParticipants) || formData.maxParticipants <= 0) {
      newErrors.maxParticipants = 'Please enter a valid number of participants';
    }

    if (formData.registrationFee && (isNaN(formData.registrationFee) || formData.registrationFee < 0)) {
      newErrors.registrationFee = 'Please enter a valid registration fee';
    }

    if (!formData.selectedHall) {
      newErrors.selectedHall = 'Please select a hall for the event';
    }

    // Validate team event fields
    if (formData.isTeamEvent) {
      if (!formData.minTeamMembers) {
        newErrors.minTeamMembers = 'Minimum team members is required for team events';
      } else if (isNaN(formData.minTeamMembers) || formData.minTeamMembers < 1) {
        newErrors.minTeamMembers = 'Minimum team members must be at least 1';
      }

      if (!formData.maxTeamMembers) {
        newErrors.maxTeamMembers = 'Maximum team members is required for team events';
      } else if (isNaN(formData.maxTeamMembers) || formData.maxTeamMembers < 1) {
        newErrors.maxTeamMembers = 'Maximum team members must be at least 1';
      }

      if (formData.minTeamMembers && formData.maxTeamMembers && 
          parseInt(formData.minTeamMembers) > parseInt(formData.maxTeamMembers)) {
        newErrors.maxTeamMembers = 'Maximum team members must be greater than or equal to minimum';
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    setIsSubmitting(true);

    try {
      const eventData = {
        ...formData,
        clubId,
        startDateTime: `${formData.startDate}T${formData.startTime}`,
        endDateTime: `${formData.endDate}T${formData.endTime}`,
        hallId: formData.selectedHall
      };

      await onSubmit(eventData);
      onClose();
    } catch (error) {
      console.error('Error creating event:', error);
      setErrors({ submit: 'Failed to create event. Please try again.' });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal-content event-creation-modal">
        <div className="modal-header">
          <h2>{isResubmission ? 'Edit & Resubmit Event' : 'Create New Event'}</h2>
          <button className="close-button" onClick={onClose}>&times;</button>
        </div>

        {isResubmission && rejectionReason && (
          <div className="rejection-notice">
            <h4>Previous Rejection Reason:</h4>
            <p>{rejectionReason}</p>
          </div>
        )}

        <form onSubmit={handleSubmit} className="event-creation-form">
          <div className="form-group">
            <label htmlFor="title">Event Title *</label>
            <input
              type="text"
              id="title"
              name="title"
              value={formData.title}
              onChange={handleInputChange}
              className={errors.title ? 'error' : ''}
              placeholder="Enter event title"
            />
            {errors.title && <span className="error-text">{errors.title}</span>}
          </div>

          <div className="form-group">
            <label htmlFor="description">Description</label>
            <textarea
              id="description"
              name="description"
              value={formData.description}
              onChange={handleInputChange}
              placeholder="Enter event description"
              rows="3"
            />
          </div>

          <div className="form-row">
            <div className="form-group">
              <label htmlFor="type">Event Type</label>
              <select
                id="type"
                name="type"
                value={formData.type}
                onChange={handleInputChange}
              >
                {eventTypes.map(type => (
                  <option key={type} value={type}>
                    {type.charAt(0) + type.slice(1).toLowerCase()}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label htmlFor="maxParticipants">Max Participants *</label>
              <input
                type="number"
                id="maxParticipants"
                name="maxParticipants"
                value={formData.maxParticipants}
                onChange={handleInputChange}
                className={errors.maxParticipants ? 'error' : ''}
                placeholder="Maximum participants"
                min="1"
              />
              {errors.maxParticipants && <span className="error-text">{errors.maxParticipants}</span>}
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label htmlFor="startDate">Start Date *</label>
              <input
                type="date"
                id="startDate"
                name="startDate"
                value={formData.startDate}
                onChange={handleInputChange}
                className={errors.startDate ? 'error' : ''}
              />
              {errors.startDate && <span className="error-text">{errors.startDate}</span>}
            </div>

            <div className="form-group">
              <label htmlFor="startTime">Start Time *</label>
              <input
                type="time"
                id="startTime"
                name="startTime"
                value={formData.startTime}
                onChange={handleInputChange}
                className={errors.startTime ? 'error' : ''}
              />
              {errors.startTime && <span className="error-text">{errors.startTime}</span>}
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label htmlFor="endDate">End Date *</label>
              <input
                type="date"
                id="endDate"
                name="endDate"
                value={formData.endDate}
                onChange={handleInputChange}
                className={errors.endDate ? 'error' : ''}
              />
              {errors.endDate && <span className="error-text">{errors.endDate}</span>}
            </div>

            <div className="form-group">
              <label htmlFor="endTime">End Time *</label>
              <input
                type="time"
                id="endTime"
                name="endTime"
                value={formData.endTime}
                onChange={handleInputChange}
                className={errors.endTime ? 'error' : ''}
              />
              {errors.endTime && <span className="error-text">{errors.endTime}</span>}
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="registrationFee">Registration Fee (₹)</label>
            <input
              type="number"
              id="registrationFee"
              name="registrationFee"
              value={formData.registrationFee}
              onChange={handleInputChange}
              className={errors.registrationFee ? 'error' : ''}
              placeholder="0"
              min="0"
              step="0.01"
            />
            {errors.registrationFee && <span className="error-text">{errors.registrationFee}</span>}
          </div>

          <div className="form-group">
            <label className="checkbox-label">
              <input
                type="checkbox"
                name="isTeamEvent"
                checked={formData.isTeamEvent}
                onChange={handleInputChange}
              />
              <span>This is a team event</span>
            </label>
          </div>

          {formData.isTeamEvent && (
            <div className="form-row">
              <div className="form-group">
                <label htmlFor="minTeamMembers">Minimum Team Members *</label>
                <input
                  type="number"
                  id="minTeamMembers"
                  name="minTeamMembers"
                  value={formData.minTeamMembers}
                  onChange={handleInputChange}
                  className={errors.minTeamMembers ? 'error' : ''}
                  placeholder="Minimum team size"
                  min="1"
                />
                {errors.minTeamMembers && <span className="error-text">{errors.minTeamMembers}</span>}
              </div>

              <div className="form-group">
                <label htmlFor="maxTeamMembers">Maximum Team Members *</label>
                <input
                  type="number"
                  id="maxTeamMembers"
                  name="maxTeamMembers"
                  value={formData.maxTeamMembers}
                  onChange={handleInputChange}
                  className={errors.maxTeamMembers ? 'error' : ''}
                  placeholder="Maximum team size"
                  min="1"
                />
                {errors.maxTeamMembers && <span className="error-text">{errors.maxTeamMembers}</span>}
              </div>
            </div>
          )}

          <div className="form-group">
            <label htmlFor="selectedHall">Hall Selection *</label>
              <select
                id="selectedHall"
                name="selectedHall"
                value={formData.selectedHall}
                onChange={handleInputChange}
                className={errors.selectedHall ? 'error' : ''}
                disabled={loadingHalls || availableHalls.length === 0}
              >
                <option value="">
                  {loadingHalls ? 'Loading halls...' : 
                   availableHalls.length === 0 ? 'No halls available' : 'Select a hall'}
                </option>
                {availableHalls.map(hall => (
                  <option key={hall.id} value={hall.id}>
                    {hall.name} (Capacity: {hall.seatingCapacity}) - {hall.location}
                    {suggestedHall && hall.id === suggestedHall.id ? ' - Recommended' : ''}
                  </option>
                ))}
              </select>
              {errors.selectedHall && <span className="error-text">{errors.selectedHall}</span>}
              {hallsMessage && (
                <small className={`hall-message ${availableHalls.length === 0 ? 'error-text' : 'success-text'}`}>
                  {hallsMessage}
                </small>
              )}
          </div>

          {errors.submit && <div className="error-text">{errors.submit}</div>}

          <div className="modal-actions">
            <button type="button" onClick={onClose} className="cancel-button">
              Cancel
            </button>
            <button 
              type="submit" 
              className="submit-button"
              disabled={isSubmitting || availableHalls.length === 0}
            >
              {isSubmitting ? 
                (isResubmission ? 'Resubmitting...' : 'Creating...') : 
                (isResubmission ? 'Resubmit Event' : 'Create Event')
              }
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default EventCreationModal;
