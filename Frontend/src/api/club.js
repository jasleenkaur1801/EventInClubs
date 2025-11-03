import http from './http';

const CLUB_API_BASE = '/clubs';

export const clubApi = {
  // Get all active clubs
  getAllClubs: async () => {
    try {
      const response = await http.get(CLUB_API_BASE);
      return response.data;
    } catch (error) {
      console.error('Error fetching clubs:', error);
      throw error;
    }
  },

  // Get clubs by admin user ID
  getClubsByAdminUser: async (adminUserId) => {
    try {
      const response = await http.get(`${CLUB_API_BASE}/admin/${adminUserId}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching clubs by admin user:', error);
      throw error;
    }
  },

  // Get top clubs
  getTopClubs: async () => {
    try {
      const response = await http.get(`${CLUB_API_BASE}/top`);
      return response.data;
    } catch (error) {
      console.error('Error fetching top clubs:', error);
      throw error;
    }
  },

  // Get clubs by category
  getClubsByCategory: async (category) => {
    try {
      const response = await http.get(`${CLUB_API_BASE}/category/${category}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching clubs by category:', error);
      throw error;
    }
  },

  // Get top clubs by category
  getTopClubsByCategory: async (category) => {
    try {
      const response = await http.get(`${CLUB_API_BASE}/top/category/${category}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching top clubs by category:', error);
      throw error;
    }
  },

  // Search clubs
  searchClubs: async (query) => {
    try {
      const response = await http.get(`${CLUB_API_BASE}/search?q=${encodeURIComponent(query)}`);
      return response.data;
    } catch (error) {
      console.error('Error searching clubs:', error);
      throw error;
    }
  },

  // Get club by ID
  getClubById: async (id) => {
    try {
      const response = await http.get(`${CLUB_API_BASE}/${id}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching club by ID:', error);
      throw error;
    }
  },

  // Get club by name
  getClubByName: async (name) => {
    try {
      const response = await http.get(`${CLUB_API_BASE}/name/${encodeURIComponent(name)}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching club by name:', error);
      throw error;
    }
  },

  // Create new club
  createClub: async (clubData, adminUserId) => {
    try {
      const response = await http.post(`${CLUB_API_BASE}?adminUserId=${adminUserId}`, clubData);
      return response.data;
    } catch (error) {
      console.error('Error creating club:', error);
      throw error;
    }
  },

  // Update club
  updateClub: async (id, clubData, userId) => {
    try {
      const response = await http.put(`${CLUB_API_BASE}/${id}?userId=${userId}`, clubData);
      return response.data;
    } catch (error) {
      console.error('Error updating club:', error);
      throw error;
    }
  },

  // Delete club
  deleteClub: async (id, userId) => {
    try {
      await http.delete(`${CLUB_API_BASE}/${id}?userId=${userId}`);
      return true;
    } catch (error) {
      console.error('Error deleting club:', error);
      throw error;
    }
  },

  // Get available categories
  getCategories: async () => {
    try {
      const response = await http.get(`${CLUB_API_BASE}/categories`);
      return response.data;
    } catch (error) {
      console.error('Error fetching categories:', error);
      throw error;
    }
  },

  // Super Admin endpoints for club approval
  getPendingClubs: async () => {
    try {
      const response = await http.get(`${CLUB_API_BASE}/pending`);
      return response.data;
    } catch (error) {
      console.error('Error fetching pending clubs:', error);
      throw error;
    }
  },

  approveClub: async (clubId) => {
    try {
      const response = await http.post(`${CLUB_API_BASE}/${clubId}/approve`);
      return response.data;
    } catch (error) {
      console.error('Error approving club:', error);
      throw error;
    }
  },

  rejectClub: async (clubId) => {
    try {
      const response = await http.post(`${CLUB_API_BASE}/${clubId}/reject`);
      return response.data;
    } catch (error) {
      console.error('Error rejecting club:', error);
      throw error;
    }
  },

  // Upload club logo
  uploadClubLogo: async (file) => {
    try {
      const formData = new FormData();
      formData.append('file', file);
      
      const response = await http.post('/files/upload-club-logo', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      return response.data;
    } catch (error) {
      console.error('Error uploading club logo:', error);
      throw error;
    }
  }
};
