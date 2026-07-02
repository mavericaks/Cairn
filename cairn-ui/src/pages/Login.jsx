import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { setAuthToken, fetchApi } from '../utils/api';
import { Hexagon } from 'lucide-react';

export default function Login() {
  const [username, setUsername] = useState('dev_admin');
  const [isAdmin, setIsAdmin] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const response = await fetchApi(`/dev/login?username=${username}&isAdmin=${isAdmin}`, {
        method: 'POST'
      });
      
      if (response.token) {
        setAuthToken(response.token);
        navigate('/');
      }
    } catch (err) {
      setError('Failed to login. Ensure backend is running.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', height: '100vh', alignItems: 'center', justifyContent: 'center', background: 'var(--bg-primary)' }}>
      <div className="card" style={{ width: '400px', textAlign: 'center' }}>
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '1.5rem' }}>
          <Hexagon size={48} className="logo-icon" />
        </div>
        <h2 style={{ marginBottom: '0.5rem' }}>Welcome to Cairn</h2>
        <p style={{ marginBottom: '2rem' }}>Sign in to access the AI orchestrator</p>

        {error && <div className="badge badge-warning" style={{ marginBottom: '1rem', display: 'block' }}>{error}</div>}

        <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <input
            type="text"
            className="input-field"
            placeholder="Username (e.g. dev_admin)"
            value={username}
            onChange={e => setUsername(e.target.value)}
            required
          />
          
          <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', alignSelf: 'flex-start' }}>
            <input 
              type="checkbox" 
              checked={isAdmin} 
              onChange={e => setIsAdmin(e.target.checked)} 
            />
            <span style={{ fontSize: '0.875rem' }}>Enable Admin Tools (HITL)</span>
          </label>

          <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: '1rem' }} disabled={loading}>
            {loading ? 'Authenticating...' : 'Enter Cairn'}
          </button>
        </form>
      </div>
    </div>
  );
}
