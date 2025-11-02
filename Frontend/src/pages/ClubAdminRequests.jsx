import React, { useState, useEffect } from 'react';
import { clubAdminRequestApi } from '../api/clubAdminRequests';
import './SuperAdminRequests.css'; // Reuse the same styles

export default function ClubAdminRequests() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('all'); // all, pending, approved, rejected
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [rejectionReason, setRejectionReason] = useState('');
  const [showRejectModal, setShowRejectModal] = useState(false);

  useEffect(() => {
    fetchRequests();
  }, []);

  const fetchRequests = async () => {
    try {
      setLoading(true);
      const data = await clubAdminRequestApi.getAllRequests();
      setRequests(data || []);
    } catch (error) {
      console.error('Error fetching club admin requests:', error);
      setRequests([]);
    } finally {
      setLoading(false);
    }
  };


  const handleApproveClick = async (request) => {
    if (!window.confirm(`Approve club admin request for ${request.name}?`)) {
      return;
    }

    try {
      // Approve without requiring club assignment
      await clubAdminRequestApi.approveRequest(request.id, null);
      alert('Club admin request approved successfully!');
      fetchRequests(); // Refresh the list
    } catch (error) {
      console.error('Error approving request:', error);
      alert('Failed to approve request: ' + (error.response?.data?.error || error.message));
    }
  };

  const handleRejectClick = (request) => {
    setSelectedRequest(request);
    setShowRejectModal(true);
    setRejectionReason('');
  };

  const handleRejectSubmit = async () => {
    if (!rejectionReason.trim()) {
      alert('Please provide a reason for rejection');
      return;
    }

    try {
      await clubAdminRequestApi.rejectRequest(selectedRequest.id, rejectionReason);
      alert('Club admin request rejected successfully!');
      setShowRejectModal(false);
      setSelectedRequest(null);
      setRejectionReason('');
      fetchRequests(); // Refresh the list
    } catch (error) {
      console.error('Error rejecting request:', error);
      alert('Failed to reject request: ' + (error.response?.data?.error || error.message));
    }
  };

  const filteredRequests = requests.filter(req => {
    if (filter === 'all') return true;
    return req.status.toLowerCase() === filter.toLowerCase();
  });

  const getStatusBadgeClass = (status) => {
    switch (status.toLowerCase()) {
      case 'pending':
        return 'status-badge status-pending';
      case 'approved':
        return 'status-badge status-approved';
      case 'rejected':
        return 'status-badge status-rejected';
      default:
        return 'status-badge';
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  return (
    <div className="super-admin-requests-container">
      <div className="requests-header">
        <h2>🏢 Club Admin Requests</h2>
        <p>Review and manage club admin registration requests</p>
      </div>

      <div className="requests-filters">
        <button
          className={`filter-btn ${filter === 'all' ? 'active' : ''}`}
          onClick={() => setFilter('all')}
        >
          All Requests ({requests.length})
        </button>
        <button
          className={`filter-btn ${filter === 'pending' ? 'active' : ''}`}
          onClick={() => setFilter('pending')}
        >
          Pending ({requests.filter(r => r.status === 'PENDING').length})
        </button>
        <button
          className={`filter-btn ${filter === 'approved' ? 'active' : ''}`}
          onClick={() => setFilter('approved')}
        >
          Approved ({requests.filter(r => r.status === 'APPROVED').length})
        </button>
        <button
          className={`filter-btn ${filter === 'rejected' ? 'active' : ''}`}
          onClick={() => setFilter('rejected')}
        >
          Rejected ({requests.filter(r => r.status === 'REJECTED').length})
        </button>
      </div>

      {loading ? (
        <div className="loading-container">
          <div className="spinner"></div>
          <p>Loading club admin requests...</p>
        </div>
      ) : filteredRequests.length === 0 ? (
        <div className="empty-state">
          <div className="empty-icon">📭</div>
          <h3>No requests found</h3>
          <p>There are no club admin requests matching the selected filter.</p>
        </div>
      ) : (
        <div className="requests-table">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Status</th>
                <th>Requested At</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredRequests.map(request => (
                <tr key={request.id}>
                  <td>{request.name}</td>
                  <td>{request.email}</td>
                  <td>
                    <span className={getStatusBadgeClass(request.status)}>
                      {request.status}
                    </span>
                  </td>
                  <td>{formatDate(request.requestedAt)}</td>
                  <td>
                    {request.status === 'PENDING' && (
                      <div className="action-buttons">
                        <button
                          className="btn-approve"
                          onClick={() => handleApproveClick(request)}
                          title="Approve request"
                        >
                          ✓ Approve
                        </button>
                        <button
                          className="btn-reject"
                          onClick={() => handleRejectClick(request)}
                          title="Reject request"
                        >
                          ✗ Reject
                        </button>
                      </div>
                    )}
                    {request.status === 'APPROVED' && (
                      <span className="approved-text">✓ Approved on {formatDate(request.approvedAt)}</span>
                    )}
                    {request.status === 'REJECTED' && (
                      <span className="rejected-text" title={request.rejectionReason}>
                        ✗ Rejected
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Rejection Modal */}
      {showRejectModal && (
        <div className="modal-overlay" onClick={() => setShowRejectModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Reject Club Admin Request</h3>
              <button
                className="modal-close"
                onClick={() => setShowRejectModal(false)}
              >
                ×
              </button>
            </div>
            <div className="modal-body">
              <p><strong>Name:</strong> {selectedRequest?.name}</p>
              <p><strong>Email:</strong> {selectedRequest?.email}</p>
              <div className="form-group">
                <label>Rejection Reason:</label>
                <textarea
                  value={rejectionReason}
                  onChange={(e) => setRejectionReason(e.target.value)}
                  placeholder="Please provide a reason for rejecting this request..."
                  rows="4"
                />
              </div>
            </div>
            <div className="modal-footer">
              <button
                className="btn-cancel"
                onClick={() => setShowRejectModal(false)}
              >
                Cancel
              </button>
              <button
                className="btn-reject"
                onClick={handleRejectSubmit}
              >
                Reject Request
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
