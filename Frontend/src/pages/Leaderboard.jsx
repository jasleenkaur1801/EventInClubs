import React, { useState, useEffect } from 'react';
import { Trophy, Medal, Award, Users, Star, Crown } from 'lucide-react';
import http from '../api/http';
import '../styles/Leaderboard.css';

const Leaderboard = () => {
  const [leaderboardData, setLeaderboardData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchLeaderboard();
  }, []);

  const fetchLeaderboard = async () => {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        setError('Please log in to view the leaderboard');
        setLoading(false);
        return;
      }

      const response = await http.get('/achievements/leaderboard?limit=50', {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (response.status !== 200) {
        throw new Error('Failed to fetch leaderboard data');
      }

      const data = response.data;
      setLeaderboardData(data);
      setLoading(false);
    } catch (err) {
      console.error('Error fetching leaderboard:', err);
      setError('Failed to load leaderboard data');
      setLoading(false);
    }
  };

  const getRankIcon = (rank) => {
    switch (rank) {
      case 1:
        return <Crown className="rank-icon gold" size={24} />;
      case 2:
        return <Trophy className="rank-icon silver" size={24} />;
      case 3:
        return <Medal className="rank-icon bronze" size={24} />;
      default:
        return <span className="rank-number">#{rank}</span>;
    }
  };

  const getLevelColor = (level) => {
    switch (level) {
      case 'Diamond':
        return 'diamond';
      case 'Platinum':
        return 'platinum';
      case 'Gold':
        return 'gold';
      case 'Silver':
        return 'silver';
      case 'Bronze':
        return 'bronze';
      default:
        return 'beginner';
    }
  };

  if (loading) {
    return (
      <div className="leaderboard-container">
        <div className="loading-state">
          <Trophy size={48} className="loading-icon" />
          <p>Loading leaderboard...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="leaderboard-container">
        <div className="error-state">
          <Award size={48} className="error-icon" />
          <p>{error}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="leaderboard-container">
      <div className="leaderboard-header">
        <div className="header-content">
          <Trophy size={40} className="header-icon" />
          <div>
            <h1>🏆 Leaderboard</h1>
            <p>Top performers ranked by achievements and points</p>
          </div>
        </div>
        <div className="stats-summary">
          <div className="stat-item">
            <Users size={20} />
            <span>{leaderboardData.length} Users</span>
          </div>
        </div>
      </div>

      {leaderboardData.length === 0 ? (
        <div className="empty-state">
          <Star size={48} className="empty-icon" />
          <h3>No users found</h3>
          <p>Start earning achievements to appear on the leaderboard!</p>
        </div>
      ) : (
        <div className="leaderboard-list">
          {leaderboardData.map((user, index) => {
            const rank = index + 1;
            const isTopThree = rank <= 3;
            
            return (
              <div 
                key={user.userId} 
                className={`leaderboard-item ${isTopThree ? 'top-three' : ''} ${getLevelColor(user.level)}`}
              >
                <div className="rank-section">
                  {getRankIcon(rank)}
                </div>
                
                <div className="user-info">
                  <div className="user-details">
                    <h3 className="user-name">{user.name}</h3>
                    <p className="user-email">{user.email}</p>
                  </div>
                  
                  <div className="user-level">
                    <span className="trophy-emoji">{user.trophy}</span>
                    <span className={`level-badge ${getLevelColor(user.level)}`}>
                      {user.level}
                    </span>
                  </div>
                </div>
                
                <div className="stats-section">
                  <div className="stat">
                    <Star size={16} />
                    <span className="stat-value">{user.totalPoints}</span>
                    <span className="stat-label">Points</span>
                  </div>
                  
                  <div className="stat">
                    <Award size={16} />
                    <span className="stat-value">{user.achievementCount}</span>
                    <span className="stat-label">Achievements</span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      <div className="leaderboard-footer">
        <div className="level-guide">
          <h4>Level Guide</h4>
          <div className="level-items">
            <div className="level-item">
              <span className="level-trophy">💎</span>
              <span>Diamond (1000+ pts)</span>
            </div>
            <div className="level-item">
              <span className="level-trophy">🏆</span>
              <span>Platinum (500+ pts)</span>
            </div>
            <div className="level-item">
              <span className="level-trophy">🥇</span>
              <span>Gold (250+ pts)</span>
            </div>
            <div className="level-item">
              <span className="level-trophy">🥈</span>
              <span>Silver (100+ pts)</span>
            </div>
            <div className="level-item">
              <span className="level-trophy">🥉</span>
              <span>Bronze (25+ pts)</span>
            </div>
            <div className="level-item">
              <span className="level-trophy">🏅</span>
              <span>Beginner (0+ pts)</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Leaderboard;
