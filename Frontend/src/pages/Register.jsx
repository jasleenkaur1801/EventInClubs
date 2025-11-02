import React, { useState, useEffect } from "react";
import { registerUser } from "../api/auth";
import { useNavigate, Link } from "react-router-dom";
import "./Auth.css";

export default function Register() {
  const [formData, setFormData] = useState({ 
    name: "", 
    email: "", 
    password: "", 
    confirmPassword: "",
    role: "STUDENT" 
  });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [passwordStrength, setPasswordStrength] = useState(0);
  const [isVisible, setIsVisible] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    setIsVisible(true);
  }, []);

  const roles = [
    {
      value: "STUDENT",
      label: "Student",
      description: "Join clubs, submit ideas, and vote on events",
      icon: "🎓"
    },
    {
      value: "CLUB_ADMIN",
      label: "Club Admin",
      description: "Manage your club, create events, and review ideas",
      icon: "🏢"
    }
    // SUPER_ADMIN role disabled
    // {
    //   value: "SUPER_ADMIN",
    //   label: "Super Admin",
    //   description: "Platform administration and oversight",
    //   icon: "👑"
    // }
  ];

  const checkPasswordStrength = (password) => {
    let strength = 0;
    if (password.length >= 8) strength++;
    if (/[a-z]/.test(password)) strength++;
    if (/[A-Z]/.test(password)) strength++;
    if (/[0-9]/.test(password)) strength++;
    if (/[^A-Za-z0-9]/.test(password)) strength++;
    return strength;
  };

  const validateForm = () => {
    const newErrors = {};
    
    if (!formData.name.trim()) {
      newErrors.name = "Full name is required";
    } else if (formData.name.trim().length < 2) {
      newErrors.name = "Name must be at least 2 characters";
    }
    
    if (!formData.email) {
      newErrors.email = "Email is required";
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      newErrors.email = "Please enter a valid email";
    } else if (formData.role === "STUDENT" && !formData.email.toLowerCase().endsWith("@chitkara.edu.in")) {
      newErrors.email = "Students must use official Chitkara email ID (@chitkara.edu.in)";
    }
    
    if (!formData.password) {
      newErrors.password = "Password is required";
    } else if (formData.password.length < 8) {
      newErrors.password = "Password must be at least 8 characters";
    } else if (passwordStrength < 3) {
      newErrors.password = "Password is too weak";
    }
    
    if (!formData.confirmPassword) {
      newErrors.confirmPassword = "Please confirm your password";
    } else if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = "Passwords do not match";
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    
    // Check password strength
    if (name === 'password') {
      setPasswordStrength(checkPasswordStrength(value));
    }
    
    // Clear error when user starts typing
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: "" }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }
    
    setLoading(true);
    try {
      const { confirmPassword, ...registrationData } = formData;
      const response = await registerUser(registrationData);
      
      // Check if this is an approval request (super admin or club admin)
      if (response.data?.type === 'super_admin_request') {
        setErrors({ 
          success: "Super admin request submitted successfully! Please wait for approval from an existing super admin. You will be notified once your request is reviewed." 
        });
        
        setTimeout(() => {
          navigate("/login");
        }, 5000);
      } else if (response.data?.type === 'club_admin_request') {
        setErrors({ 
          success: "Club admin request submitted successfully! Please wait for approval from a super admin. You will be able to login once approved." 
        });
        
        setTimeout(() => {
          navigate("/login");
        }, 5000);
      } else {
        // Regular registration success
        setErrors({ success: "Account created successfully! Redirecting to login..." });
        
        setTimeout(() => {
          navigate("/login");
        }, 2000);
      }
      
    } catch (err) {
      const errorMessage = err?.response?.data?.error || "Registration failed. Please try again.";
      setErrors({ general: errorMessage });
    } finally { 
      setLoading(false); 
    }
  };

  const getPasswordStrengthText = () => {
    const texts = ["Very Weak", "Weak", "Fair", "Good", "Strong"];
    const colors = ["#ff4444", "#ff8800", "#ffaa00", "#00aa00", "#00ff00"];
    return { text: texts[passwordStrength - 1] || "Very Weak", color: colors[passwordStrength - 1] || "#ff4444" };
  };

  const handleGoogleRegister = () => {
    /* global google */
    const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;
    if (!window.google || !clientId) {
      setErrors({ general: "Google signup not configured" });
      return;
    }
    try {
      google.accounts.id.initialize({
        client_id: clientId,
        callback: (response) => {
          // We'll just reuse the login flow on backend to create/sign-in
          fetch(`${import.meta.env.VITE_API_BASE || ''}/api/auth/google`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ idToken: response.credential })
          }).then(res => res.json()).then(data => {
            if (data.token) {
              localStorage.setItem('token', data.token);
              localStorage.setItem('email', data.email);
              localStorage.setItem('role', data.role);
              navigate('/');
            } else {
              setErrors({ general: data.error || 'Google signup failed' });
            }
          }).catch(() => setErrors({ general: 'Google signup failed' }));
        }
      });
      google.accounts.id.prompt();
    } catch (e) {
      setErrors({ general: 'Google signup failed to initialize' });
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-background">
        <div className="auth-pattern"></div>
      </div>
      
      <div className={`auth-content ${isVisible ? 'fade-in' : ''}`}>
        <div className="auth-header">
          <div className="auth-logo">
            <div className="logo-icon">🎪</div>
            <h1>Event Idea Marketplace</h1>
          </div>
          <p className="auth-subtitle">Join our community and start creating amazing events</p>
        </div>

        <div className="auth-card">
          <div className="auth-tabs">
            <Link to="/login" className="auth-tab">Sign In</Link>
            <button className="auth-tab active">Create Account</button>
          </div>

          <form onSubmit={handleSubmit} className="auth-form">
            {errors.general && (
              <div className="error-message general">
                <span className="error-icon">⚠️</span>
                {errors.general}
              </div>
            )}

            {errors.success && (
              <div className="success-message">
                <span className="success-icon">✅</span>
                {errors.success}
              </div>
            )}

            <div className="form-group">
              <label htmlFor="name">Full Name</label>
              <div className="input-wrapper">
                <span className="input-icon">👤</span>
                <input
                  id="name"
                  name="name"
                  type="text"
                  placeholder="Enter your full name"
                  value={formData.name}
                  onChange={handleChange}
                  className={errors.name ? 'error' : ''}
                  required
                />
              </div>
              {errors.name && <span className="error-text">{errors.name}</span>}
            </div>

            <div className="form-group">
              <label htmlFor="email">Email Address</label>
              <div className="input-wrapper">
                <span className="input-icon">📧</span>
                <input
                  id="email"
                  name="email"
                  type="email"
                  placeholder="Enter your email"
                  value={formData.email}
                  onChange={handleChange}
                  className={errors.email ? 'error' : ''}
                  required
                />
              </div>
              {errors.email && <span className="error-text">{errors.email}</span>}
              {formData.role === "STUDENT" && !errors.email && (
                <span className="info-text" style={{display: 'block', marginTop: '4px', fontSize: '0.875rem', color: '#666'}}>
                  📚 Students must use official Chitkara email (@chitkara.edu.in)
                </span>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="role">Account Type</label>
              <div className="role-selector">
                {roles.map((role) => (
                  <label key={role.value} className={`role-option ${formData.role === role.value ? 'selected' : ''}`}>
                    <input
                      type="radio"
                      name="role"
                      value={role.value}
                      checked={formData.role === role.value}
                      onChange={handleChange}
                    />
                    <div className="role-content">
                      <span className="role-icon">{role.icon}</span>
                      <div className="role-info">
                        <span className="role-label">{role.label}</span>
                        <span className="role-description">{role.description}</span>
                      </div>
                    </div>
                  </label>
                ))}
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="password">Password</label>
              <div className="input-wrapper">
                <span className="input-icon">🔒</span>
                <input
                  id="password"
                  name="password"
                  type={showPassword ? "text" : "password"}
                  placeholder="Create a strong password"
                  value={formData.password}
                  onChange={handleChange}
                  className={errors.password ? 'error' : ''}
                  required
                />
                <button
                  type="button"
                  className="password-toggle"
                  onClick={() => setShowPassword(!showPassword)}
                >
                  {showPassword ? "🙈" : "👁️"}
                </button>
              </div>
              
              {formData.password && (
                <div className="password-strength">
                  <div className="strength-bars">
                    {[1, 2, 3, 4, 5].map((level) => (
                      <div
                        key={level}
                        className={`strength-bar ${level <= passwordStrength ? 'active' : ''}`}
                        style={{ backgroundColor: level <= passwordStrength ? getPasswordStrengthText().color : '#e5e7eb' }}
                      ></div>
                    ))}
                  </div>
                  <span className="strength-text" style={{ color: getPasswordStrengthText().color }}>
                    {getPasswordStrengthText().text}
                  </span>
                </div>
              )}
              
              {errors.password && <span className="error-text">{errors.password}</span>}
            </div>

            <div className="form-group">
              <label htmlFor="confirmPassword">Confirm Password</label>
              <div className="input-wrapper">
                <span className="input-icon">🔐</span>
                <input
                  id="confirmPassword"
                  name="confirmPassword"
                  type={showConfirmPassword ? "text" : "password"}
                  placeholder="Confirm your password"
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  className={errors.confirmPassword ? 'error' : ''}
                  required
                />
                <button
                  type="button"
                  className="password-toggle"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                >
                  {showConfirmPassword ? "🙈" : "👁️"}
                </button>
              </div>
              {errors.confirmPassword && <span className="error-text">{errors.confirmPassword}</span>}
            </div>

            <button 
              className={`auth-button ${loading ? 'loading' : ''}`} 
              type="submit" 
              disabled={loading}
            >
              {loading ? (
                <>
                  <span className="spinner"></span>
                  Creating Account...
                </>
              ) : (
                'Create Account'
              )}
            </button>
          </form>

          <div className="divider">
            <span>or sign up with</span>
          </div>

          <div className="social-login">
            <button
              type="button"
              className="social-button google"
              onClick={handleGoogleRegister}
            >
              <span className="social-icon">G</span>
              Continue with Google
            </button>
          </div>

          <div className="auth-footer">
            <p>Already have an account? <Link to="/login" className="link">Sign in here</Link></p>
          </div>
        </div>
      </div>
    </div>
  );
}
