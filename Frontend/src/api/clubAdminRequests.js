import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api';

const getAuthHeader = () => {
  const token = localStorage.getItem('token');
  return {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  };
};

export const clubAdminRequestApi = {
  // Get all club admin requests
  getAllRequests: async () => {
    const response = await axios.get(
      `${API_BASE_URL}/api/club-admin-requests`,
      getAuthHeader()
    );
    return response.data;
  },

  // Get pending club admin requests
  getPendingRequests: async () => {
    const response = await axios.get(
      `${API_BASE_URL}/api/club-admin-requests/pending`,
      getAuthHeader()
    );
    return response.data;
  },

  // Get pending requests count
  getPendingRequestsCount: async () => {
    const response = await axios.get(
      `${API_BASE_URL}/api/club-admin-requests/pending`,
      getAuthHeader()
    );
    return { data: { pendingRequests: response.data.length } };
  },

  // Approve a club admin request
  approveRequest: async (requestId, clubId) => {
    const response = await axios.post(
      `${API_BASE_URL}/api/club-admin-requests/${requestId}/approve`,
      clubId ? { clubId: parseInt(clubId) } : {},
      getAuthHeader()
    );
    return response.data;
  },

  // Reject a club admin request
  rejectRequest: async (requestId, reason) => {
    const response = await axios.post(
      `${API_BASE_URL}/api/club-admin-requests/${requestId}/reject`,
      { reason },
      getAuthHeader()
    );
    return response.data;
  }
};
