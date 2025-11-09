import React, { useState } from 'react';
import './ClubLogoUpdateModal.css';
import { clubApi } from '../api/club';

export default function ClubLogoUpdateModal({ isOpen, onClose, club, onSuccess }) {
  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
  const [logoFile, setLogoFile] = useState(null);
  const [logoPreview, setLogoPreview] = useState(
    club?.logoUrl 
      ? (club.logoUrl.startsWith('http') ? club.logoUrl : `${API_BASE_URL}${club.logoUrl}`)
      : null
  );
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleLogoChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      // Validate file type
      const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'];
      if (!validTypes.includes(file.type)) {
        setError('Please select a valid image file (jpg, png, gif, webp)');
        return;
      }
      
      // Validate file size (5MB max)
      if (file.size > 5 * 1024 * 1024) {
        setError('Image size must be less than 5MB');
        return;
      }
      
      setLogoFile(file);
      
      // Create preview
      const reader = new FileReader();
      reader.onloadend = () => {
        setLogoPreview(reader.result);
      };
      reader.readAsDataURL(file);
      
      setError('');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!logoFile) {
      setError('Please select a logo to upload');
      return;
    }
    
    setLoading(true);
    try {
      console.log('Uploading logo for club:', club.name, '(ID:', club.id, ')');
      
      // Upload logo
      const uploadResponse = await clubApi.uploadClubLogo(logoFile);
      console.log('Logo uploaded successfully:', uploadResponse);
      
      // Update club with new logo URL
      const user = JSON.parse(localStorage.getItem('user') || '{}');
      console.log('Updating club with logoUrl:', uploadResponse.fileUrl, 'for user:', user.id);
      
      const updateResult = await clubApi.updateClub(club.id, { logoUrl: uploadResponse.fileUrl }, user.id);
      console.log('Club updated successfully:', updateResult);
      
      onSuccess();
      onClose();
    } catch (err) {
      console.error('Error updating logo:', err);
      console.error('Error details:', err.response?.data || err.message);
      setError('Failed to update logo. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setLogoFile(null);
    setLogoPreview(
      club?.logoUrl 
        ? (club.logoUrl.startsWith('http') ? club.logoUrl : `${API_BASE_URL}${club.logoUrl}`)
        : null
    );
    setError('');
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay">
      <div className="modal-content logo-update-modal">
        <div className="modal-header">
          <h2>Update Club Logo</h2>
          <button className="modal-close" onClick={handleClose}>
            ✕
          </button>
        </div>

        <form onSubmit={handleSubmit} className="logo-update-form">
          {error && (
            <div className="error-message">
              {error}
            </div>
          )}

          <div className="club-info">
            <h3>{club?.name}</h3>
            <p>{club?.shortName}</p>
          </div>

          <div className="logo-upload-section">
            {logoPreview ? (
              <div className="logo-preview">
                <img src={logoPreview} alt="Club logo preview" />
              </div>
            ) : (
              <div className="no-logo">
                <span>No logo uploaded yet</span>
              </div>
            )}

            <div className="logo-upload-area">
              <input
                id="logo-update"
                type="file"
                accept="image/jpeg,image/jpg,image/png,image/gif,image/webp"
                onChange={handleLogoChange}
                className="logo-input"
              />
              <label htmlFor="logo-update" className="logo-upload-label">
                <div className="upload-icon">📷</div>
                <div className="upload-text">{logoPreview ? 'Change Logo' : 'Upload Logo'}</div>
                <div className="upload-hint">JPG, PNG, GIF or WebP (max 5MB)</div>
              </label>
            </div>
          </div>

          <div className="form-actions">
            <button type="button" className="btn-secondary" onClick={handleClose}>
              Cancel
            </button>
            <button type="submit" className="btn-primary" disabled={loading || !logoFile}>
              {loading ? 'Uploading...' : 'Update Logo'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
