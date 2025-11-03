import React from "react";
import "./ClubCard.css";

export default function ClubCard({ name, desc, shortName, events, category, rating, logoUrl }) {
  const handleViewIdeas = () => {
    console.log(`Viewing ideas for ${name}`);
    // Navigate to club ideas page or show modal
  };

  const handleFollow = () => {
    console.log(`Following ${name}`);
    // Handle follow functionality
  };

  const getCategoryColor = (cat) => {
    const colors = {
      'technology': '#3b82f6',
      'design': '#8b5cf6',
      'engineering': '#10b981',
      'arts': '#f59e0b',
      'sports': '#ef4444'
    };
    return colors[cat.toLowerCase()] || '#6b7280';
  };

  return (
    <div className="club-card">
      <div className="club-header">
        <div className="club-avatar">
          {logoUrl ? (
            <img src={`http://localhost:8080${logoUrl}`} alt={`${name} logo`} className="club-logo-img" />
          ) : (
            <span className="club-initials">
              {shortName || name.split(" ").slice(0,2).map(w=>w[0]).join("")}
            </span>
          )}
        </div>
        <div className="club-meta">
          <span className="club-category" style={{ backgroundColor: getCategoryColor(category) }}>
            {category}
          </span>
          <div className="club-rating">
            <span className="rating-stars">{'⭐'.repeat(Math.floor(rating))}</span>
            <span className="rating-number">{rating}</span>
          </div>
        </div>
      </div>
      <div className="club-body">
        <h3>{name}</h3>
        <p>{desc}</p>
        <div className="club-stats">
          <span className="stat">
            <span className="stat-number">{events}</span>
            <span className="stat-label">events</span>
          </span>
        </div>
        <div className="club-actions">
          <button onClick={handleViewIdeas} className="btn-primary">
            View Ideas
          </button>
        </div>
      </div>
    </div>
  );
}
