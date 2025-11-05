import React, { useState } from 'react';
import './IdeaApprovalModal.css';
import http from '../api/http';

const IdeaApprovalModal = ({ idea, onClose, onApprove }) => {
  const [formData, setFormData] = useState({
    status: 'APPROVED',
    pptFile: null,
    pptFileUrl: ''
  });
  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isUploadingPpt, setIsUploadingPpt] = useState(false);

  const statusOptions = [
    { value: 'SUBMITTED', label: 'Submitted', description: 'Idea is submitted and awaiting review' },
    { value: 'UNDER_REVIEW', label: 'Under Review', description: 'Idea is being reviewed by club admin' },
    { value: 'APPROVED', label: 'Approved', description: 'Idea has been approved by club admin' },
    { value: 'IMPLEMENTING', label: 'Implementing', description: 'Idea is being implemented' },
    { value: 'COMPLETED', label: 'Completed', description: 'Idea has been successfully implemented' },
    { value: 'REJECTED', label: 'Rejected', description: 'Idea has been rejected' }
  ];

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    
    // Clear error when user types
    if (errors[name]) {
      setErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }
  };

  const handlePptFileChange = async (e) => {
    const file = e.target.files[0];
    if (file) {
      // Validate file type
      const allowedTypes = ['application/vnd.ms-powerpoint', 'application/vnd.openxmlformats-officedocument.presentationml.presentation'];
      const fileExtension = file.name.toLowerCase().split('.').pop();
      
      if (!['ppt', 'pptx'].includes(fileExtension)) {
        setErrors(prev => ({
          ...prev,
          pptFile: 'Please select a valid PPT or PPTX file'
        }));
        return;
      }

      // Validate file size (max 10MB)
      if (file.size > 10 * 1024 * 1024) {
        setErrors(prev => ({
          ...prev,
          pptFile: 'File size must be less than 10MB'
        }));
        return;
      }

      setIsUploadingPpt(true);
      setErrors(prev => ({ ...prev, pptFile: '' }));

      try {
        // Upload PPT file
        const uploadFormData = new FormData();
        uploadFormData.append('file', file);
        
        const uploadResponse = await http.post('/upload/ppt', uploadFormData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        });
        
        if (uploadResponse.status === 200) {
          const uploadResult = uploadResponse.data;
          setFormData(prev => ({
            ...prev,
            pptFile: file,
            pptFileUrl: uploadResult.url
          }));
        } else {
          const errorResult = await uploadResponse.json();
          throw new Error(errorResult.error || 'Failed to upload PPT file');
        }
      } catch (error) {
        console.error('Error uploading PPT:', error);
        setErrors(prev => ({
          ...prev,
          pptFile: error.message || 'Failed to upload PPT file'
        }));
      } finally {
        setIsUploadingPpt(false);
      }
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);

    try {
      const userId = localStorage.getItem('userId');
      if (!userId) {
        throw new Error('User not authenticated');
      }

      // Call the approve with PPT API
      const params = new URLSearchParams({
        status: formData.status,
        userId: userId,
        pptFileUrl: formData.pptFileUrl || ''
      });
      
      const response = await http.put(`/ideas/${idea.id}/approve-with-ppt?${params.toString()}`);

      if (response.status === 200) {
        const updatedIdea = response.data;
        onApprove(updatedIdea);
        onClose();
      } else {
        const errorResult = response.data;
        throw new Error(errorResult.error || 'Failed to update idea status');
      }
    } catch (error) {
      console.error('Error updating idea status:', error);
      setErrors(prev => ({
        ...prev,
        submit: error.message || 'Failed to update idea status'
      }));
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!idea) return null;

  return (
    <div className="modal-overlay">
      <div className="idea-approval-modal">
        <div className="modal-header">
          <h2>Approve Idea: "{idea.title}"</h2>
          <button className="close-btn" onClick={onClose}>×</button>
        </div>

        <div className="modal-content">
          <div className="idea-info">
            <h3>Idea Details</h3>
            <div className="idea-details">
              <p><strong>Title:</strong> {idea.title}</p>
              <p><strong>Description:</strong> {idea.description}</p>
              {idea.expectedOutcome && (
                <p><strong>Expected Outcome:</strong> {idea.expectedOutcome}</p>
              )}
              <p><strong>Submitted by:</strong> {idea.submittedByName}</p>
              <p><strong>Current Status:</strong> <span className={`status-badge ${idea.status.toLowerCase()}`}>{idea.status}</span></p>
            </div>
          </div>

          <form onSubmit={handleSubmit} className="approval-form">
            <div className="form-group">
              <label htmlFor="status">New Status *</label>
              <select
                id="status"
                name="status"
                value={formData.status}
                onChange={handleInputChange}
                className={errors.status ? 'error' : ''}
                required
              >
                {statusOptions.map(option => (
                  <option key={option.value} value={option.value}>
                    {option.label} - {option.description}
                  </option>
                ))}
              </select>
              {errors.status && <span className="error-text">{errors.status}</span>}
            </div>

            <div className="form-group">
              <label htmlFor="pptFile">PPT Presentation (Optional)</label>
              <input
                type="file"
                id="pptFile"
                name="pptFile"
                onChange={handlePptFileChange}
                accept=".ppt,.pptx"
                className={errors.pptFile ? 'error' : ''}
                disabled={isUploadingPpt}
              />
              {errors.pptFile && <span className="error-text">{errors.pptFile}</span>}
              <small className="help-text">
                Upload a PPT/PPTX file (Max 10MB) to provide detailed presentation of the approved idea
              </small>
              
              {isUploadingPpt && (
                <div className="upload-status">
                  <span className="upload-spinner">⏳</span> Uploading PPT file...
                </div>
              )}
              
              {formData.pptFile && (
                <div className="file-info">
                  <span className="file-icon">📄</span>
                  <span className="file-name">{formData.pptFile.name}</span>
                  <span className="file-size">({(formData.pptFile.size / 1024 / 1024).toFixed(2)} MB)</span>
                </div>
              )}
            </div>

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
                disabled={isSubmitting || isUploadingPpt}
              >
                {isSubmitting ? 'Updating...' : 'Update Status'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default IdeaApprovalModal;

