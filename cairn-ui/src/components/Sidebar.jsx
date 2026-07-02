import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { Hexagon, MessageSquare, Upload, Shield, LogOut } from 'lucide-react';
import { removeAuthToken, isAdmin } from '../utils/auth';

export default function Sidebar({ onUploadClick }) {
  const navigate = useNavigate();
  const hasAdmin = isAdmin();

  const handleLogout = () => {
    removeAuthToken();
    navigate('/login');
  };

  return (
    <div className="sidebar">
      <div className="sidebar-header">
        <Hexagon size={32} className="logo-icon" />
        <h2 style={{ fontSize: '1.25rem', letterSpacing: '0.05em' }}>Cairn AI</h2>
      </div>

      <button className="btn btn-primary" style={{ marginBottom: '2rem' }} onClick={() => navigate('/')}>
        <MessageSquare size={16} />
        New Chat
      </button>

      <div className="sidebar-nav">
        <NavLink to="/" end className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
          <MessageSquare size={18} />
          <span>Conversations</span>
        </NavLink>
        
        <div className="nav-item" onClick={onUploadClick}>
          <Upload size={18} />
          <span>Upload Document</span>
        </div>

        {hasAdmin && (
          <NavLink to="/admin" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
            <Shield size={18} />
            <span>Tool Approvals</span>
          </NavLink>
        )}
      </div>

      <div style={{ marginTop: 'auto', borderTop: '1px solid var(--border-color)', paddingTop: '1rem' }}>
        <div className="nav-item" onClick={handleLogout} style={{ color: 'var(--error)' }}>
          <LogOut size={18} />
          <span>Sign Out</span>
        </div>
      </div>
    </div>
  );
}
