import React, { useState, useEffect } from "react";
import { useNavigate, Link } from "react-router-dom";
import { loginUser } from "../api/auth";
import GoogleSignIn from "../components/GoogleSignIn";
import "./Auth.css";

export default function Login() {
  const [formData, setFormData] = useState({ 
    email: "", 
    password: "" 
  });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(false);
  const [isVisible, setIsVisible] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    setIsVisible(true);
  }, []);

  const validateForm = () => {
    const newErrors = {};
    
    if (!formData.email) {
      newErrors.email = "Email is required";
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      newErrors.email = "Please enter a valid email";
    }
    
    if (!formData.password) {
      newErrors.password = "Password is required";
    } else if (formData.password.length < 6) {
      newErrors.password = "Password must be at least 6 characters";
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    
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
      // Authenticate via backend (handles SUPER_ADMIN as well)
      const res = await loginUser(formData);
      console.log('Full login response:', JSON.stringify(res, null, 2));
      
      const { token, email, role } = res.data || {};
      
      if (!token || !email) {
        throw new Error('Invalid login response: missing token or email');
      }
      
      // Decode the JWT token to get user ID
      let userIdFromToken = null;
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        console.log('Decoded JWT payload:', payload);
        
        // The user ID is stored in the 'userId' claim
        if (payload.userId) {
          userIdFromToken = payload.userId.toString();
          console.log('Found user ID in token:', userIdFromToken);
        } else {
          console.warn('No userId found in JWT payload');
        }
      } catch (error) {
        console.error('Error decoding JWT token:', error);
      }
      
      // Use the ID from the token if available, otherwise fall back to email
      const userIdValue = userIdFromToken || email.toLowerCase().trim();
      
      // Prepare user data with defaults
      const userData = {
        id: userIdValue,
        email: email.toLowerCase().trim(),
        name: email.split('@')[0],
        role: role || 'USER'
      };
      
      console.log('Prepared user data:', userData);
      
      if (!token || !email || !userIdValue) {
        console.error('Missing required fields in login response:', { 
          token: !!token, 
          email: !!email, 
          id: !!userIdValue,
          responseData: res.data
        });
        throw new Error('Invalid login response from server');
      }
      
      // Store all data in localStorage
      try {
        console.log('Storing authentication data in localStorage');
        console.log('Token:', token ? 'Token received' : 'No token');
        
        // Store token first
        localStorage.setItem("token", token);
        
        // Store complete user data
        localStorage.setItem("user", JSON.stringify(userData));
        
        // For backward compatibility + navbar checks
        localStorage.setItem("email", userData.email);
        localStorage.setItem("role", userData.role);
        localStorage.setItem("userRole", userData.role);
        
        // Store clubId for club admins
        if (userData.clubId) {
          localStorage.setItem("clubId", userData.clubId);
        }
        
        // Verify storage
        console.log('Verifying storage:');
        console.log('- Token exists:', !!localStorage.getItem('token'));
        console.log('- User exists:', !!localStorage.getItem('user'));
        console.log('- User data:', JSON.parse(localStorage.getItem('user') || '{}'));
        
      } catch (error) {
        console.error('Error storing authentication data:', error);
        throw new Error('Failed to store authentication data');
      }
      
      if (rememberMe) {
        localStorage.setItem("rememberMe", "true");
      }
      
      // Show success message before redirecting
      setTimeout(() => {
        // Redirect based on user role
        // SUPER_ADMIN functionality enabled
        if (role === 'SUPER_ADMIN') {
          navigate('/superadmin/dashboard');
        } else if (role === 'ADMIN' || role === 'CLUB_ADMIN') {
          navigate('/admin/dashboard');
        } else {
          navigate('/');
        }
      }, 1000);
      
    } catch (err) {
      const errorMessage = err?.response?.data?.error || "Login failed. Please check your credentials.";
      setErrors({ general: errorMessage });
    } finally { 
      setLoading(false); 
    }
  };

  const handleGoogleSuccess = (response) => {
    console.log('Google login successful:', response);
    
    // Store token and user data in localStorage
    if (response.token) {
      localStorage.setItem('token', response.token);
    }
    
    if (response.user) {
      localStorage.setItem('user', JSON.stringify(response.user));
      console.log('User data stored in localStorage:', response.user);
    } else if (response.email) {
      // If user object is not provided but email is available
      const userData = {
        id: response.userId || response.email,
        email: response.email,
        name: response.name || response.email.split('@')[0],
        role: response.role || 'user'
      };
      localStorage.setItem('user', JSON.stringify(userData));
      console.log('User data created and stored:', userData);
    } else {
      console.warn('No user data received in Google login response');
    }
    
    // Show success message
    setErrors({});
    
    // Force a refresh of the auth state
    window.dispatchEvent(new Event('storage'));
    
    // Redirect to home after a short delay
    setTimeout(() => {
      // Check if we need to redirect to a protected route
      const from = new URLSearchParams(window.location.search).get('from');
      if (from) {
        navigate(from, { replace: true });
      } else {
        navigate("/");
      }
    }, 500);
  };

  const handleGoogleError = (errorMessage) => {
    setErrors({ general: errorMessage });
  };

  const handleForgotPassword = () => {
    console.log("Forgot password clicked");
    // Implement forgot password logic
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
          <p className="auth-subtitle">Welcome back! Sign in to continue</p>
        </div>

        <div className="auth-card">
          <div className="auth-tabs">
            <button className="auth-tab active">Sign In</button>
            <Link to="/register" className="auth-tab">Create Account</Link>
          </div>

          <form onSubmit={handleSubmit} className="auth-form">
            {errors.general && (
              <div className="error-message general">
                <span className="error-icon">⚠️</span>
                {errors.general}
              </div>
            )}

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
              {!errors.email && formData.email && (
                <span className="info-text" style={{display: 'block', marginTop: '4px', fontSize: '0.875rem', color: '#666'}}>
                  📚 Students must use official Chitkara email (@chitkara.edu.in)
                </span>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="password">Password</label>
              <div className="input-wrapper">
                <span className="input-icon">🔒</span>
                <input
                  id="password"
                  name="password"
                  type={showPassword ? "text" : "password"}
                  placeholder="Enter your password"
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
              {errors.password && <span className="error-text">{errors.password}</span>}
            </div>

            <div className="form-options">
              <label className="checkbox-wrapper">
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                />

                Remember me
              </label>
              <button
                type="button"
                className="forgot-password"
                onClick={handleForgotPassword}
              >
                Forgot password?
              </button>
            </div>

            <button 
              className={`auth-button ${loading ? 'loading' : ''}`} 
              type="submit" 
              disabled={loading}
            >
              {loading ? (
                <>
                  <span className="spinner"></span>
                  Signing in...
                </>
              ) : (
                'Sign In'
              )}
            </button>
          </form>

          {import.meta.env.VITE_ENABLE_GOOGLE_SIGNIN === 'true' && (
            <>
              <div className="divider">
                <span>or continue with</span>
              </div>

              <div className="social-login">
                <GoogleSignIn
                  onSuccess={handleGoogleSuccess}
                  onError={handleGoogleError}
                  loading={loading}
                  setLoading={setLoading}
                />
              </div>
            </>
          )}

          <div className="auth-footer">
            <p>Don't have an account? <Link to="/register" className="link">Sign up here</Link></p>
          </div>
        </div>
      </div>
    </div>
  );
}
