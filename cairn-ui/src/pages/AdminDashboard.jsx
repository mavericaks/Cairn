import React, { useState, useEffect } from 'react';
import { Shield, Check, X, AlertTriangle } from 'lucide-react';
import Sidebar from '../components/Sidebar';
import { fetchApi } from '../utils/api';

export default function AdminDashboard() {
  const [approvals, setApprovals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadApprovals = async () => {
    try {
      setLoading(true);
      // NOTE: backend endpoint should ideally return a list of pending tool approvals.
      // E.g. GET /api/v1/tools/approvals/pending
      // For now we'll mock it if it doesn't exist, but let's try calling it.
      const data = await fetchApi('/tools/approvals/pending');
      setApprovals(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Failed to load approvals. You might not have admin permissions.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadApprovals();
  }, []);

  const handleAction = async (id, approve) => {
    try {
      if (approve) {
        await fetchApi(`/tools/approvals/${id}/approve`, { method: 'POST' });
      } else {
        // Implement reject if backend supports it
        // await fetchApi(`/tools/approvals/${id}/reject`, { method: 'POST' });
      }
      setApprovals(prev => prev.filter(a => a.id !== id));
    } catch (err) {
      alert(`Failed to ${approve ? 'approve' : 'reject'}: ` + err.message);
    }
  };

  return (
    <div className="chat-layout">
      <Sidebar />
      
      <main className="chat-main" style={{ background: 'var(--bg-secondary)', overflowY: 'auto', padding: '3rem 2rem' }}>
        <div style={{ maxWidth: '900px', margin: '0 auto', width: '100%' }}>
          
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '2rem' }}>
            <Shield size={32} style={{ color: 'var(--accent-secondary)' }} />
            <div>
              <h1 style={{ fontSize: '1.5rem' }}>Tool Approvals Dashboard</h1>
              <p>Review and authorize Human-in-the-Loop (HITL) execution requests.</p>
            </div>
          </div>

          {error && <div className="badge badge-warning" style={{ marginBottom: '2rem', display: 'block', padding: '1rem', fontSize: '1rem' }}>{error}</div>}

          {loading ? (
            <p>Loading pending requests...</p>
          ) : approvals.length === 0 && !error ? (
            <div className="card" style={{ textAlign: 'center', padding: '4rem 2rem' }}>
              <Check size={48} style={{ color: 'var(--success)', margin: '0 auto 1rem auto' }} />
              <h3>All clear!</h3>
              <p>No pending tool execution requests require your approval.</p>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {approvals.map(request => (
                <div key={request.id} className="card" style={{ borderColor: 'var(--warning)', position: 'relative', overflow: 'hidden' }}>
                  <div style={{ position: 'absolute', top: 0, left: 0, width: '4px', height: '100%', background: 'var(--warning)' }}></div>
                  
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <AlertTriangle size={20} style={{ color: 'var(--warning)' }} />
                      <h3 style={{ margin: 0 }}>Action Required: {request.toolName}</h3>
                    </div>
                    <span className="badge badge-warning">Pending</span>
                  </div>

                  <div style={{ background: 'var(--bg-primary)', padding: '1rem', borderRadius: 'var(--radius-md)', fontFamily: 'var(--font-mono)', fontSize: '0.875rem', marginBottom: '1.5rem', border: '1px solid var(--border-color)' }}>
                    <div style={{ color: 'var(--text-muted)', marginBottom: '0.5rem' }}>Payload:</div>
                    <pre style={{ margin: 0, whiteSpace: 'pre-wrap' }}>
                      {JSON.stringify(request.arguments, null, 2)}
                    </pre>
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem' }}>
                    <button className="btn btn-ghost" onClick={() => handleAction(request.id, false)} style={{ color: 'var(--error)' }}>
                      <X size={16} /> Reject
                    </button>
                    <button className="btn btn-primary" onClick={() => handleAction(request.id, true)}>
                      <Check size={16} /> Authorize Execution
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

        </div>
      </main>
    </div>
  );
}
