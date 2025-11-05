import React, { useState, useEffect } from 'react';
import { hallApi } from '../api/hall';
import { httpClient } from '../api/http';
import './EventApprovalModal.css';

const EventApprovalModal = ({ proposal, onClose, onApprove }) => {
  const [formData, setFormData] = useState({
    eventName: proposal?.title || '',
    eventType: proposal?.type || 'WORKSHOP',
    startDate: '',
    endDate: '',
    startTime: '',
    endTime: '',
    maxParticipants: '',
    registrationFee: 0,
    description: proposal?.description || '',
    poster: null,
    selectedHall: '',
    pptFileUrl: ''
  });

  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [posterPreview, setPosterPreview] = useState(null);
  const [allHalls, setAllHalls] = useState([]);

  const eventTypes = [
    'WORKSHOP',
    'SEMINAR', 
    'HACKATHON',
    'COMPETITION',
    'CONFERENCE',
    'MEETUP',
    'WEBINAR',
    'CULTURAL',
    'SPORTS',
    'OTHER'
  ];

  // Fetch all halls on component mount for manual selection
  useEffect(() => {
    const fetchAllHalls = async () => {
      try {
        const halls = await hallApi.getAllHalls();
        setAllHalls(halls || []);
      } catch (error) {
        console.error('Error fetching all halls:', error);
      }
    };
    fetchAllHalls();
  }, []);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    console.log(`Input changed: ${name} = "${value}"`);
    
    setFormData(prev => {
      const newData = {
        ...prev,
        [name]: value
      };
      console.log('Updated formData:', newData);
      return newData;
    });

    // Clear error when user starts typing
    if (errors[name]) {
      setErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }

    // Automated hall recommendation disabled - manual selection only
  };

  // Fetch available halls with specific data (to avoid closure issues)
  const fetchAvailableHallsWithData = async (data) => {
    const { maxParticipants, startDate, endDate, startTime, endTime } = data;
    
    console.log('fetchAvailableHallsWithData called with data.maxParticipants:', maxParticipants);
    
    // Only fetch if we have all required data
    if (!maxParticipants || !startDate || !endDate || !startTime || !endTime) {
      setAvailableHalls([]);
      setSuggestedHall(null);
      setHallsMessage('');
      return;
    }

    const participants = parseInt(maxParticipants);
    console.log('Parsed participants:', participants);
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
      
      console.log('Available halls for', participants, 'participants:', halls);
      
      // Backend should already filter, but double-check for safety
      const suitableHalls = halls.filter(hall => hall.seatingCapacity >= participants);
      
      console.log('Suitable halls after filtering:', suitableHalls);
      
      if (suitableHalls.length === 0) {
        setHallsMessage(`No halls available with capacity for ${participants} participants at the selected time`);
        setAvailableHalls([]);
        setSuggestedHall(null);
        // Clear selected hall if it's no longer available
        setFormData(prev => ({ ...prev, selectedHall: '' }));
      } else {
        // Only show suitable halls in the dropdown
        setAvailableHalls(suitableHalls);
        
        // Find the best fit hall: smallest hall that can accommodate all participants
        // Sort by capacity to get the smallest suitable hall
        const sortedHalls = [...suitableHalls].sort((a, b) => a.seatingCapacity - b.seatingCapacity);
        const bestFit = sortedHalls[0];
        
        console.log('Best fit hall selected:', bestFit);
        
        setSuggestedHall(bestFit);
        const excessCapacity = bestFit.seatingCapacity - participants;
        const efficiencyMessage = excessCapacity <= 20 ? ' (Optimal fit)' : ' (Large hall)';
        setHallsMessage(`Recommended: ${bestFit.name} (Capacity: ${bestFit.seatingCapacity})${efficiencyMessage}. ${suitableHalls.length} suitable hall(s) available.`);
        
        // Don't auto-select - let user manually choose
        // Automated code still runs in background to provide recommendations
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

  // Fetch available halls based on current form data
  const fetchAvailableHalls = async () => {
    const { maxParticipants, startDate, endDate, startTime, endTime } = formData;
    
    console.log('fetchAvailableHalls called with formData.maxParticipants:', maxParticipants);
    
    // Only fetch if we have all required data
    if (!maxParticipants || !startDate || !endDate || !startTime || !endTime) {
      setAvailableHalls([]);
      setSuggestedHall(null);
      setHallsMessage('');
      return;
    }

    const participants = parseInt(maxParticipants);
    console.log('Parsed participants:', participants);
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
      
      console.log('Available halls for', participants, 'participants:', halls);
      
      // Backend should already filter, but double-check for safety
      const suitableHalls = halls.filter(hall => hall.seatingCapacity >= participants);
      
      console.log('Suitable halls after filtering:', suitableHalls);
      
      if (suitableHalls.length === 0) {
        setHallsMessage(`No halls available with capacity for ${participants} participants at the selected time`);
        setAvailableHalls([]);
        setSuggestedHall(null);
        // Clear selected hall if it's no longer available
        setFormData(prev => ({ ...prev, selectedHall: '' }));
      } else {
        // Only show suitable halls in the dropdown
        setAvailableHalls(suitableHalls);
        
        // Find the best fit hall: smallest hall that can accommodate all participants
        // Sort by capacity to get the smallest suitable hall
        const sortedHalls = [...suitableHalls].sort((a, b) => a.seatingCapacity - b.seatingCapacity);
        const bestFit = sortedHalls[0];
        
        console.log('Best fit hall selected:', bestFit);
        
        setSuggestedHall(bestFit);
        const excessCapacity = bestFit.seatingCapacity - participants;
        const efficiencyMessage = excessCapacity <= 20 ? ' (Optimal fit)' : ' (Large hall)';
        setHallsMessage(`Recommended: ${bestFit.name} (Capacity: ${bestFit.seatingCapacity})${efficiencyMessage}. ${suitableHalls.length} suitable hall(s) available.`);
        
        // Don't auto-select - let user manually choose
        // Automated code still runs in background to provide recommendations
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

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      // Validate file type
      const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif'];
      if (!allowedTypes.includes(file.type)) {
        setErrors(prev => ({
          ...prev,
          poster: 'Please select a valid image file (JPEG, PNG, GIF)'
        }));
        return;
      }

      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        setErrors(prev => ({
          ...prev,
          poster: 'File size must be less than 5MB'
        }));
        return;
      }

      setFormData(prev => ({
        ...prev,
        poster: file
      }));

      // Create preview
      const reader = new FileReader();
      reader.onload = (e) => {
        setPosterPreview(e.target.result);
      };
      reader.readAsDataURL(file);

      // Clear error
      setErrors(prev => ({
        ...prev,
        poster: ''
      }));
    }
  };

  // PPT upload removed from form

  const validateForm = () => {
    const newErrors = {};

    if (!formData.eventName.trim()) {
      newErrors.eventName = 'Event name is required';
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

    // Validate date logic
    if (formData.startDate && formData.endDate) {
      const startDateTime = new Date(`${formData.startDate}T${formData.startTime || '00:00'}`);
      const endDateTime = new Date(`${formData.endDate}T${formData.endTime || '23:59'}`);
      
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

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const isValidUrl = (string) => {
    try {
      new URL(string);
      return true;
    } catch (_) {
      return false;
    }
  };

  const handleSubmit = async (e) => {
    console.log('handleSubmit called');
    e.preventDefault();
    console.log('preventDefault called');
    
    console.log('Form submitted, formData:', formData);
    
    // Temporarily bypass validation for testing
    // if (!validateForm()) {
    //   return;
    // }

    setIsSubmitting(true);

    try {
      // Prepare minimal event data for testing - NO DATES for now
      const eventData = {
        title: 'Test Event',
        description: 'Test Description',
        type: 'WORKSHOP',
        clubId: 1,
        maxParticipants: 50,
        registrationFee: 0
        // Temporarily remove dates to test basic creation
      };

      console.log('Sending event data:', eventData);
      console.log('Proposal data:', proposal);
      console.log('Form data:', formData);

      // Validate form data
      if (!formData.startDate || !formData.startTime || !formData.endDate || !formData.endTime) {
        setErrors({ submit: 'Please fill in all date and time fields' });
        return;
      }

      // Combine date and time into datetime strings
      const startDateTime = new Date(`${formData.startDate}T${formData.startTime}`);
      const endDateTime = new Date(`${formData.endDate}T${formData.endTime}`);
      
      // Validate the dates are valid
      if (isNaN(startDateTime.getTime()) || isNaN(endDateTime.getTime())) {
        setErrors({ submit: 'Invalid date or time values' });
        return;
      }
      
      console.log('Start Date String:', `${formData.startDate}T${formData.startTime}`);
      console.log('End Date String:', `${formData.endDate}T${formData.endTime}`);
      console.log('Start DateTime:', startDateTime.toISOString());
      console.log('End DateTime:', endDateTime.toISOString());

      // Try using the existing approve-proposal endpoint instead
      const approveData = new URLSearchParams({
        proposalId: proposal.id,
        eventName: formData.eventName,
        eventType: formData.eventType,
        startDateTime: startDateTime.toISOString(),
        endDateTime: endDateTime.toISOString(),
        maxParticipants: formData.maxParticipants,
        registrationFee: formData.registrationFee || '0',
        hallId: formData.selectedHall,
        description: formData.description || proposal.description || '',
        pptFileUrl: formData.pptFileUrl || ''
      });

      const createResponse = await httpClient.post('/events/approve-proposal', approveData, {
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
        }
      });
      const createdEvent = createResponse.data;

      console.log('Event created:', createdEvent);
      
      // Call onApprove to refresh the lists before closing
      if (onApprove) {
        await onApprove(eventData);
      }
      
      // Event is now in PENDING_APPROVAL status, waiting for Super Admin
      alert('Event created and submitted for Super Admin approval!\n\nStatus: Pending Approval\n\nYou will be notified once the Super Admin reviews your event.');
      onClose();
    } catch (error) {
      console.error('Error approving event:', error);
      console.error('Error response:', error.response?.data);
      console.error('Error status:', error.response?.status);
      
      let errorMessage = 'Failed to approve event. Please try again.';
      if (error.response?.data?.message) {
        errorMessage = error.response.data.message;
      } else if (error.response?.data?.error) {
        errorMessage = error.response.data.error;
      } else if (error.message) {
        errorMessage = error.message;
      }
      
      setErrors({ submit: errorMessage });
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!proposal) return null;

  return (
    <div className="modal-overlay">
      <div className="event-approval-modal">
        <div className="modal-header">
          <h2>{proposal.approvalStatus === 'REJECTED' ? 'Fix & Reapply for Approval' : 'Create Event & Submit for Approval'}</h2>
          <button className="close-btn" onClick={onClose}>×</button>
        </div>

        <div className="modal-content">
          <div className="proposal-info">
            <h3>Original Proposal: "{proposal.title}"</h3>
            <p className="proposal-description">{proposal.description}</p>
            {proposal.rejectionReason && (
              <div style={{
                background: '#fff5f5',
                border: '1px solid #fed7d7',
                color: '#8a1621',
                borderRadius: 8,
                padding: '10px 12px',
                marginTop: 10
              }}>
                <strong>Rejected Reason:</strong> {proposal.rejectionReason}
              </div>
            )}
          </div>

          <form onSubmit={handleSubmit} className="event-approval-form">
            <div className="form-row">
              <div className="form-group">
                <label htmlFor="eventName">Event Name *</label>
                <input
                  type="text"
                  id="eventName"
                  name="eventName"
                  value={formData.eventName}
                  onChange={handleInputChange}
                  className={errors.eventName ? 'error' : ''}
                  placeholder="Enter event name"
                />
                {errors.eventName && <span className="error-text">{errors.eventName}</span>}
              </div>

              <div className="form-group">
                <label htmlFor="eventType">Event Type *</label>
                <select
                  id="eventType"
                  name="eventType"
                  value={formData.eventType}
                  onChange={handleInputChange}
                  className={errors.eventType ? 'error' : ''}
                >
                  {eventTypes.map(type => (
                    <option key={type} value={type}>{type}</option>
                  ))}
                </select>
                {errors.eventType && <span className="error-text">{errors.eventType}</span>}
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
                  min={new Date().toISOString().split('T')[0]}
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
                  min={formData.startDate || new Date().toISOString().split('T')[0]}
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

            <div className="form-row">
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
                <label htmlFor="selectedHall">Hall Selection *</label>
                <select
                  id="selectedHall"
                  name="selectedHall"
                  value={formData.selectedHall}
                  onChange={handleInputChange}
                  className={errors.selectedHall ? 'error' : ''}
                >
                  <option value="">Select a hall</option>
                  {allHalls.map(hall => (
                    <option key={hall.id} value={hall.id}>
                      {hall.name} (Capacity: {hall.seatingCapacity}) - {hall.location}
                    </option>
                  ))}
                </select>
                {errors.selectedHall && <span className="error-text">{errors.selectedHall}</span>}
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="description">Event Description</label>
              <textarea
                id="description"
                name="description"
                value={formData.description}
                onChange={handleInputChange}
                rows="4"
                placeholder="Detailed event description"
              />
            </div>

            <div className="form-group">
              <label htmlFor="poster">Event Poster</label>
              <input
                type="file"
                id="poster"
                name="poster"
                onChange={handleFileChange}
                accept="image/*"
                className={errors.poster ? 'error' : ''}
              />
              {errors.poster && <span className="error-text">{errors.poster}</span>}
              <small className="help-text">Upload event poster (JPEG, PNG, GIF - Max 5MB)</small>
              
              {posterPreview && (
                <div className="poster-preview">
                  <img src={posterPreview} alt="Poster preview" />
                </div>
              )}
            </div>

            {/* PPT Upload Section Removed */}

            {errors.submit && <div className="error-text">{errors.submit}</div>}

            <div className="form-actions">
              <button
                type="button"
                onClick={onClose}
                className="btn-secondary"
                disabled={isSubmitting}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn-primary"
                disabled={isSubmitting}
              >
                {isSubmitting ? 'Submitting...' : 'Create & Submit for Approval'}
              </button>
            </div>
          </form>
        </div>
      </div>
      
      {/* PPT Viewer Modal Removed */}
    </div>
  );
};

export default EventApprovalModal;
