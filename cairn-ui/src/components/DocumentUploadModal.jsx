import React, { useState, useRef } from 'react';
import { X, UploadCloud, FileText } from 'lucide-react';
import { fetchApi } from '../utils/api';

export default function DocumentUploadModal({ onClose }) {
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const fileInputRef = useRef(null);

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      setFile(e.target.files[0]);
      setResult(null);
      setError('');
    }
  };

  const handleUpload = async () => {
    if (!file) return;
    
    setUploading(true);
    setError('');
    
    const formData = new FormData();
    formData.append('file', file);
    
    try {
      // fetchApi handles FormData automatically
      const res = await fetchApi('/documents/upload', {
        method: 'POST',
        body: formData
      });
      setResult(res);
    } catch (err) {
      setError(err.message || 'Failed to upload document');
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
          <h3 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <UploadCloud size={24} className="logo-icon" />
            Upload Document for RAG
          </h3>
          <button className="btn-ghost" onClick={onClose} style={{ padding: '0.25rem', border: 'none', background: 'transparent', cursor: 'pointer', color: 'var(--text-secondary)' }}>
            <X size={24} />
          </button>
        </div>
        
        <p style={{ marginBottom: '1.5rem' }}>Upload PDFs, TXT, or DOCX files to ground the Discovery Agent with your custom knowledge.</p>

        {!file ? (
          <div 
            style={{ 
              border: '2px dashed var(--border-color)', 
              borderRadius: 'var(--radius-lg)', 
              padding: '3rem 2rem', 
              textAlign: 'center',
              cursor: 'pointer',
              background: 'var(--bg-tertiary)',
              transition: 'all var(--transition-fast)'
            }}
            onClick={() => fileInputRef.current?.click()}
          >
            <UploadCloud size={48} style={{ color: 'var(--text-muted)', marginBottom: '1rem' }} />
            <p>Click to browse or drag and drop a file</p>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.5rem' }}>Supported: PDF, TXT, DOCX</p>
          </div>
        ) : (
          <div className="card" style={{ display: 'flex', alignItems: 'center', gap: '1rem', background: 'var(--bg-tertiary)', borderColor: 'var(--accent-primary)' }}>
            <FileText size={32} style={{ color: 'var(--accent-primary)' }} />
            <div style={{ flexGrow: 1, overflow: 'hidden' }}>
              <p style={{ fontWeight: 500, whiteSpace: 'nowrap', textOverflow: 'ellipsis', overflow: 'hidden' }}>{file.name}</p>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{(file.size / 1024 / 1024).toFixed(2)} MB</p>
            </div>
            <button className="btn-ghost" onClick={() => setFile(null)} disabled={uploading}>
              <X size={20} />
            </button>
          </div>
        )}

        <input 
          type="file" 
          ref={fileInputRef} 
          style={{ display: 'none' }} 
          onChange={handleFileChange}
          accept=".pdf,.txt,.docx,.md,.csv,.sql"
        />

        {error && <div className="badge badge-warning" style={{ marginTop: '1.5rem', display: 'block' }}>{error}</div>}
        {result && <div className="badge badge-success" style={{ marginTop: '1.5rem', display: 'block' }}>{result}</div>}

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem', marginTop: '2rem' }}>
          <button className="btn btn-secondary" onClick={onClose} disabled={uploading}>Cancel</button>
          <button className="btn btn-primary" onClick={handleUpload} disabled={!file || uploading}>
            {uploading ? 'Ingesting...' : 'Upload & Process'}
          </button>
        </div>
      </div>
    </div>
  );
}
