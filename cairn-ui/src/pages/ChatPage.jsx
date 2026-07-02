import React, { useState, useRef, useEffect } from 'react';
import { Send, Cpu } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import Sidebar from '../components/Sidebar';
import DocumentUploadModal from '../components/DocumentUploadModal';
import { streamChat } from '../utils/api';

export default function ChatPage() {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [isStreaming, setIsStreaming] = useState(false);
  const [showUpload, setShowUpload] = useState(false);
  const messagesEndRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const handleSend = () => {
    if (!input.trim() || isStreaming) return;

    const userMessage = { role: 'user', content: input };
    setMessages(prev => [...prev, userMessage, { role: 'ai', content: '', routeInfo: null }]);
    setInput('');
    setIsStreaming(true);

    streamChat(
      userMessage.content,
      (event) => {
        setMessages(prev => {
          const newMessages = [...prev];
          const lastMsg = newMessages[newMessages.length - 1];
          
          if (event.type === 'ROUTING') {
            lastMsg.routeInfo = event.data;
          } else if (event.type === 'TOKEN') {
            lastMsg.content += (lastMsg.content ? ' ' : '') + event.data;
          }
          return newMessages;
        });
      },
      (error) => {
        console.error("Chat error:", error);
        setMessages(prev => {
          const newMessages = [...prev];
          const lastMsg = newMessages[newMessages.length - 1];
          lastMsg.content += '\n\n**Error:** ' + (error.message || 'Connection failed.');
          return newMessages;
        });
        setIsStreaming(false);
      },
      () => {
        setIsStreaming(false);
      }
    );
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className="chat-layout">
      <Sidebar onUploadClick={() => setShowUpload(true)} />
      
      <main className="chat-main">
        <header className="chat-header">
          <h1 style={{ fontSize: '1.1rem', fontWeight: 500 }}>Cairn Workspace</h1>
          <div className="badge badge-info" style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
            <span style={{ width: '8px', height: '8px', background: 'var(--accent-primary)', borderRadius: '50%', display: 'inline-block', boxShadow: '0 0 8px var(--accent-primary)' }}></span>
            System Online
          </div>
        </header>

        <div className="chat-messages">
          {messages.length === 0 ? (
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', color: 'var(--text-muted)' }}>
              <Cpu size={64} style={{ marginBottom: '1rem', opacity: 0.2 }} />
              <h2>How can Cairn help you today?</h2>
              <p style={{ marginTop: '0.5rem' }}>Ask a question, upload a document, or generate code.</p>
            </div>
          ) : (
            messages.map((msg, idx) => (
              <div key={idx} className={`message-wrapper ${msg.role}`}>
                <div className={`avatar ${msg.role}`}>
                  {msg.role === 'ai' ? <Cpu size={24} /> : <span>U</span>}
                </div>
                <div className="message-content">
                  <div className="message-bubble">
                    {msg.role === 'ai' ? (
                      <div className="markdown-body">
                        <ReactMarkdown>{msg.content}</ReactMarkdown>
                        {msg.content === '' && isStreaming && <span className="animate-pulse">...</span>}
                      </div>
                    ) : (
                      msg.content
                    )}
                  </div>
                  {msg.routeInfo && (
                    <div className="message-metadata animate-fade-in">
                      <span className="badge badge-info">{msg.routeInfo}</span>
                    </div>
                  )}
                </div>
              </div>
            ))
          )}
          <div ref={messagesEndRef} />
        </div>

        <div className="chat-input-container">
          <div className="chat-input-wrapper">
            <textarea
              className="chat-input"
              placeholder="Message Cairn... (Shift+Enter for new line)"
              rows={1}
              value={input}
              onChange={e => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              onInput={(e) => {
                e.target.style.height = 'auto';
                e.target.style.height = (e.target.scrollHeight) + 'px';
              }}
            />
            <button 
              className="chat-send-btn" 
              onClick={handleSend}
              disabled={!input.trim() || isStreaming}
            >
              <Send size={18} />
            </button>
          </div>
        </div>
      </main>

      {showUpload && <DocumentUploadModal onClose={() => setShowUpload(false)} />}
    </div>
  );
}
