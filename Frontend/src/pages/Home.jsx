import React, { useState, useEffect } from "react";
import "./Home.css";
import { useNavigate } from "react-router-dom";

export default function Home() {
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    setIsVisible(true);
  }, []);

  const navigate = useNavigate();

  const handleGetStarted = () => {
    const token = localStorage.getItem('token');
    if (token) {
      navigate('/clubs') ;
    } else {
      navigate("/login");
      
    }
  };

  return (
    <main className="home-main">
      <section className="hero full-height">
        <div className="hero-inner">
          <div className="hero-left">
            <h1 className={isVisible ? 'fade-in-up' : ''}>Event Idea Marketplace</h1>
            <p className={isVisible ? 'fade-in-up delay-1' : ''}>
              Clubs post event problems. Students submit creative proposals. Community votes for the best ideas.
            </p>
            <div className={`hero-cta ${isVisible ? 'fade-in-up delay-2' : ''}`}>
              <button onClick={handleGetStarted} className="btn-primary large">
                Get Started
              </button>
            </div>
            <div className={`hero-stats ${isVisible ? 'fade-in-up delay-3' : ''}`}>
              <div className="stat-item">
                <span className="stat-number">500+</span>
                <span className="stat-label">Active Students</span>
              </div>
              <div className="stat-item">
                <span className="stat-number">25+</span>
                <span className="stat-label">Clubs</span>
              </div>
              <div className="stat-item">
                <span className="stat-number">150+</span>
                <span className="stat-label">Events Created</span>
              </div>
            </div>
          </div>
          <div className="hero-right">
            <div className={`hero-box ${isVisible ? 'fade-in-up delay-2' : ''}`}>
              <h3>🎉 Share Your Ideas</h3>
              <p>Submit your best event ideas and win recognition from top clubs!</p>
              <div className="idea-counter">
                <span className="counter-number">1,247</span>
                <span className="counter-label">Ideas Shared Today</span>
              </div>
            </div>
          </div>
        </div>
        <div className="hero-bg-elements">
          <div className="floating-shape shape-1"></div>
          <div className="floating-shape shape-2"></div>
          <div className="floating-shape shape-3"></div>
        </div>
      </section>

      <section className="features-section">
        <div className="section-header">
          <h2>How It Works</h2>
          <p>Three simple steps to make a difference</p>
        </div>
        <div className="features-grid">
          <div className="feature-card">
            <div className="feature-icon">📝</div>
            <h3>Submit Ideas</h3>
            <p>Share your creative event proposals with the community</p>
            <div className="feature-step">Step 1</div>
          </div>
          <div className="feature-card">
            <div className="feature-icon">🗳️</div>
            <h3>Vote & Discuss</h3>
            <p>Vote on the best ideas and participate in discussions</p>
            <div className="feature-step">Step 2</div>
          </div>
          <div className="feature-card">
            <div className="feature-icon">🏆</div>
            <h3>Get Recognition</h3>
            <p>Winning ideas get implemented and you earn recognition</p>
            <div className="feature-step">Step 3</div>
          </div>
        </div>
      </section>

      <section className="cta-section">
        <div className="cta-content">
          <h2>Ready to Make a Difference?</h2>
          <p>Join thousands of students and clubs creating amazing events together</p>
          <div className="cta-buttons">
            <button className="btn-primary large">Join Now</button>
            <button className="btn-outline">Learn More</button>
          </div>
        </div>
      </section>
    </main>
  );
}
