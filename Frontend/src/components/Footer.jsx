import React from "react";
import "./Footer.css";

export default function Footer() {
  return (
    <footer className="site-footer">
      <div className="container">
        <div className="footer-grid">
          <div>
            <h4>Event Idea Marketplace</h4>
            <p>Connecting clubs and students to create better events.</p>
          </div>
          <div>
            <h5>Contact</h5>
            <p>email: eventinclubs@gmail.com</p>
          </div>
        </div>

        <div className="footer-bottom">© 2025 Event Idea Marketplace — All rights reserved.</div>
      </div>
    </footer>
  );
}
