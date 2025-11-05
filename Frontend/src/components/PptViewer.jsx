import React, { useState, useEffect } from 'react';
import './PptViewer.css';

const PptViewer = ({ pptUrl, ideaTitle, item, onClose }) => {
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [useOnlineViewer, setUseOnlineViewer] = useState(false);

  // Determine the actual PPT URL and title from the item or props
  const actualPptUrl = pptUrl || (item?.pptFileUrl);
  const actualTitle = ideaTitle || item?.title || 'Presentation';

  // Get API base URL from environment variable
  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

  // Detect if we're on localhost or production
  const isLocalhost = window.location.hostname === 'localhost' || 
                      window.location.hostname === '127.0.0.1' ||
                      window.location.hostname.includes('192.168.');

  // Get the full URL for the PPT file
  const fullPptUrl = `${API_BASE_URL}${actualPptUrl}`;

  useEffect(() => {
    // If not localhost, try to use Microsoft Office Online Viewer
    if (!isLocalhost && actualPptUrl) {
      setUseOnlineViewer(true);
      setIsLoading(true);
    } else {
      // On localhost, skip online viewer and show download interface
      setUseOnlineViewer(false);
      setIsLoading(false);
    }
  }, [actualPptUrl, isLocalhost]);

  const handleLoad = () => {
    setIsLoading(false);
  };

  const handleError = () => {
    setIsLoading(false);
    setError('Failed to load PPT file. Please try downloading it instead.');
    setUseOnlineViewer(false); // Fallback to download interface
  };

  const handleDownload = () => {
    const link = document.createElement('a');
    link.href = fullPptUrl;
    link.download = `${actualTitle.replace(/[^a-z0-9]/gi, '_').toLowerCase()}.ppt`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  if (!actualPptUrl) {
    return (
      <div className="modal-overlay">
        <div className="ppt-viewer-modal">
          <div className="modal-header">
            <h2>No PPT Available</h2>
            <button className="close-btn" onClick={onClose}>×</button>
          </div>
          <div className="modal-content">
            <div className="no-ppt">
              <div className="no-ppt-icon">📄</div>
              <p>No PPT presentation has been uploaded for this idea yet.</p>
            </div>
            <div className="modal-actions">
              <button className="btn-secondary" onClick={onClose}>
                Close
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="modal-overlay">
      <div className="ppt-viewer-modal">
        <div className="modal-header">
          <h2>PPT Presentation: {actualTitle}</h2>
          <button className="close-btn" onClick={onClose}>×</button>
        </div>
        <div className="modal-content">
          {isLoading && (
            <div className="loading-container">
              <div className="loading-spinner">⏳</div>
              <p>Loading PPT presentation...</p>
            </div>
          )}
          
          {error && (
            <div className="error-container">
              <div className="error-icon">❌</div>
              <p>{error}</p>
              <button className="btn-primary" onClick={handleDownload}>
                📥 Download PPT
              </button>
            </div>
          )}
          
          {!error && useOnlineViewer && (
            <div className="ppt-container">
              {/* Online Viewer for Production */}
              <div className="ppt-preview">
                <iframe
                  src={`https://view.officeapps.live.com/op/embed.aspx?src=${encodeURIComponent(fullPptUrl)}`}
                  width="100%"
                  height="600px"
                  frameBorder="0"
                  title={actualTitle}
                  style={{ border: 'none', borderRadius: '8px' }}
                  onLoad={handleLoad}
                  onError={handleError}
                >
                  This is an embedded Microsoft Office document, powered by Office Online.
                </iframe>
                <div className="preview-content" style={{ marginTop: '20px' }}>
                  <div className="ppt-actions">
                    <a 
                      href={`https://view.officeapps.live.com/op/view.aspx?src=${encodeURIComponent(fullPptUrl)}`}
                      target="_blank" 
                      rel="noopener noreferrer"
                      className="btn-primary"
                    >
                      🔗 Open in Full Screen
                    </a>
                    <button className="btn-secondary" onClick={handleDownload}>
                      📥 Download PPT File
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}

          {!error && !useOnlineViewer && (
            <div className="ppt-container">
              {/* Download Interface for Localhost */}
              <div className="ppt-preview">
                <div className="preview-content">
                  <div className="preview-icon">📊</div>
                  <h3>PowerPoint Presentation</h3>
                  <p className="ppt-filename">{actualTitle}</p>
                  <p className="ppt-info">
                    {isLocalhost 
                      ? "You're on localhost. PowerPoint files cannot be previewed online in development mode. Please download the file to view it."
                      : "PowerPoint files cannot be previewed directly in the browser. Please download the file to view it in Microsoft PowerPoint, Google Slides, or any compatible application."
                    }
                  </p>
                  <div className="ppt-actions">
                    <button className="btn-primary btn-large" onClick={handleDownload}>
                      📥 Download & View PPT
                    </button>
                    <a 
                      href={fullPptUrl} 
                      download
                      className="btn-secondary"
                    >
                      💾 Save to Computer
                    </a>
                  </div>
                  <div className="ppt-suggestions">
                    <p><strong>Suggested apps to open PPT files:</strong></p>
                    <ul>
                      <li>🖥️ Microsoft PowerPoint</li>
                      <li>📊 Google Slides (upload to Google Drive)</li>
                      <li>🍎 Apple Keynote</li>
                      <li>📂 LibreOffice Impress</li>
                    </ul>
                  </div>
                </div>
              </div>
            </div>
          )}
          
          <div className="modal-actions">
            <button className="btn-secondary" onClick={handleDownload}>
              📥 Download
            </button>
            <button className="btn-primary" onClick={onClose}>
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default PptViewer;
