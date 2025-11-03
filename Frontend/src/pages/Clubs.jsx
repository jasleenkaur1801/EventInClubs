import React, { useState, useEffect } from "react";
import ClubCard from "../components/ClubCard";
import { clubApi } from "../api/club";
import "./Clubs.css";

export default function Clubs() {
  const [activeTab, setActiveTab] = useState('all');
  const [searchTerm, setSearchTerm] = useState('');
  const [clubs, setClubs] = useState([]);
  const [filteredClubs, setFilteredClubs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [categories, setCategories] = useState(['all', 'technology', 'design', 'engineering']);

  useEffect(() => {
    // Fetch real data from database
    fetchClubs();
    fetchCategories();

    // Set up real-time polling every 30 seconds
    const pollInterval = setInterval(() => {
      fetchClubs(true); // Silent fetch to avoid console spam
      fetchCategories();
    }, 30000);

    // Cleanup interval on component unmount
    return () => clearInterval(pollInterval);
  }, []);

  const fetchClubs = async (silent = false) => {
    try {
      const clubsData = await clubApi.getAllClubs();
      // Always update with database data, even if empty
      setClubs(clubsData || []);
      applyFilters(clubsData || []);
      setError(null);
      setLoading(false);
      if (!silent) {
        console.log('Clubs data updated from database:', clubsData?.length || 0, 'clubs found');
      }
    } catch (err) {
      console.error('Error fetching clubs:', err);
      // If API fails, show empty state instead of sample data
      setClubs([]);
      setFilteredClubs([]);
      setLoading(false);
      if (!silent) {
        console.log('Failed to fetch clubs from database');
      }
    }
  };

  const fetchCategories = async () => {
    try {
      const categoriesData = await clubApi.getCategories();
      if (categoriesData && categoriesData.length > 0) {
        setCategories(['all', ...categoriesData.map(cat => cat.toLowerCase())]);
      }
    } catch (err) {
      console.error('Error fetching categories:', err);
      // Keep default categories
      console.log('Using default categories');
    }
  };

  const getSampleClubs = () => [
    {
      id: 1,
      name: "Coding Ninjas CUIET",
      description: "A coding community to enhance problem solving and interview skills.",
      shortName: "CN",
      memberCount: 150,
      eventCount: 12,
      category: "Technology",
      rating: 4.8
    },
    {
      id: 2,
      name: "Google Developer Groups (GDG)",
      description: "Learn Google tech and build real-world projects with peers.",
      shortName: "GD",
      memberCount: 89,
      eventCount: 8,
      category: "Technology",
      rating: 4.6
    },
    {
      id: 3,
      name: "Design Thinking Club CUIET",
      description: "Workshops and events focused on creative problem-solving.",
      shortName: "DT",
      memberCount: 67,
      eventCount: 6,
      category: "Design",
      rating: 4.7
    },
    {
      id: 4,
      name: "IEEE-CIET",
      description: "Professional chapter for research, workshops and competitions.",
      shortName: "IE",
      memberCount: 120,
      eventCount: 15,
      category: "Engineering",
      rating: 4.9
    },
    {
      id: 5,
      name: "Robotics Club CUIET",
      description: "Build and program robots for competitions and exhibitions.",
      shortName: "RC",
      memberCount: 95,
      eventCount: 10,
      category: "Engineering",
      rating: 4.5
    },
    {
      id: 6,
      name: "UI/UX Design Society",
      description: "Learn modern design principles and create amazing user experiences.",
      shortName: "UX",
      memberCount: 78,
      eventCount: 7,
      category: "Design",
      rating: 4.6
    }
  ];

  // Helper function to apply current filters to club data
  const applyFilters = (clubsData) => {
    let filtered = clubsData;
    
    // Apply category filter
    if (activeTab !== 'all') {
      filtered = filtered.filter(club => 
        club.category && club.category.toLowerCase() === activeTab
      );
    }
    
    // Apply search filter
    if (searchTerm.trim() !== '') {
      filtered = filtered.filter(club =>
        (club.name && club.name.toLowerCase().includes(searchTerm.toLowerCase())) ||
        (club.description && club.description.toLowerCase().includes(searchTerm.toLowerCase())) ||
        (club.category && club.category.toLowerCase().includes(searchTerm.toLowerCase()))
      );
    }
    
    setFilteredClubs(filtered);
  };

  useEffect(() => {
    applyFilters(clubs);
  }, [searchTerm, activeTab, clubs]);

  const handleTabChange = (tab) => {
    setActiveTab(tab);
    setSearchTerm(''); // Clear search when changing tabs
  };

  const handleSearch = async () => {
    if (searchTerm.trim() === '') {
      if (activeTab === 'all') {
        setFilteredClubs(clubs);
      } else {
        setFilteredClubs(clubs.filter(club => 
          club.category && club.category.toLowerCase() === activeTab
        ));
      }
      return;
    }

    try {
      const searchResults = await clubApi.searchClubs(searchTerm);
      if (searchResults && searchResults.length > 0) {
        setFilteredClubs(searchResults);
      } else {
        // If no search results, show filtered local results
        const filtered = clubs.filter(club =>
          (club.name && club.name.toLowerCase().includes(searchTerm.toLowerCase())) ||
          (club.description && club.description.toLowerCase().includes(searchTerm.toLowerCase())) ||
          (club.category && club.category.toLowerCase().includes(searchTerm.toLowerCase()))
        );
        setFilteredClubs(filtered);
      }
    } catch (err) {
      console.error('Error searching clubs:', err);
      // Fallback to local filtering
      const filtered = clubs.filter(club =>
        (club.name && club.name.toLowerCase().includes(searchTerm.toLowerCase())) ||
        (club.description && club.description.toLowerCase().includes(searchTerm.toLowerCase())) ||
        (club.category && club.category.toLowerCase().includes(searchTerm.toLowerCase()))
      );
      setFilteredClubs(filtered);
    }
  };

  return (
    <main className="clubs-main">
      <div className="clubs-header">
        <div className="clubs-header-content">
          <h1>Discover Amazing Clubs</h1>
          <p>Find your perfect community and start contributing today</p>
        </div>
      </div>

      <div className="clubs-container">
        <section className="search-section">
          <div className="search-header">
            <h2>Find Your Perfect Club</h2>
            <p>Discover communities that match your interests and passions</p>
          </div>
          <div className="search-container">
            <input
              type="text"
              placeholder="Search by club name, category, or description..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
              className="search-input"
            />
            <button onClick={handleSearch} className="search-btn">
              Search
            </button>
          </div>
        </section>

        <section className="clubs-content">
          <div className="category-tabs">
            {categories.map(category => (
              <button
                key={category}
                className={`category-tab ${activeTab === category ? 'active' : ''}`}
                onClick={() => handleTabChange(category)}
              >
                {category.charAt(0).toUpperCase() + category.slice(1)}
              </button>
            ))}
          </div>

          {loading ? (
            <div className="loading-container">
              <div className="spinner"></div>
              <p>Loading clubs...</p>
            </div>
          ) : (
            <div className="clubs-grid">
              {filteredClubs.map((club) => (
                <ClubCard
                  key={club.id}
                  name={club.name}
                  desc={club.description}
                  shortName={club.shortName}
                  events={club.eventCount}
                  category={club.category}
                  rating={club.rating}
                  logoUrl={club.logoUrl}
                />
              ))}
            </div>
          )}

          {!loading && filteredClubs.length === 0 && (
            <div className="no-results">
              <h3>No clubs found</h3>
              <p>Try adjusting your search or category filter</p>
            </div>
          )}
        </section>
      </div>
    </main>
  );
}
