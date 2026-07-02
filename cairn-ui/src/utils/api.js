export const API_BASE_URL = 'http://localhost:8080/api/v1';

export const getAuthToken = () => localStorage.getItem('cairn_token');
export const setAuthToken = (token) => localStorage.setItem('cairn_token', token);
export const removeAuthToken = () => localStorage.removeItem('cairn_token');

export const fetchApi = async (endpoint, options = {}) => {
  const token = getAuthToken();
  
  const headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  // Handle multipart form data
  if (options.body instanceof FormData) {
    delete headers['Content-Type'];
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if (response.status === 401 || response.status === 403) {
    if (window.location.pathname !== '/login') {
      window.location.href = '/login';
    }
  }

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `API Error: ${response.status}`);
  }

  // Handle empty responses
  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    return response.json();
  }
  return response.text();
};

export const streamChat = (message, onEvent, onError, onComplete) => {
  const token = getAuthToken();
  const controller = new AbortController();

  const connect = async () => {
    try {
      // Use fetch to initiate the SSE stream manually since standard EventSource 
      // doesn't support adding custom Authorization headers easily
      const response = await fetch(`${API_BASE_URL}/chat`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ message }),
        signal: controller.signal
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder('utf-8');
      let buffer = '';

      while (true) {
        const { value, done } = await reader.read();
        if (done) break;
        
        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n\n');
        buffer = lines.pop(); // Keep the last incomplete chunk

        for (const line of lines) {
          if (line.trim() === '') continue;
          
          // Basic SSE parsing
          let eventType = 'message';
          let data = '';
          
          const eventLines = line.split('\n');
          for (const evLine of eventLines) {
            if (evLine.startsWith('event:')) {
              eventType = evLine.substring(6).trim();
            } else if (evLine.startsWith('data:')) {
              data = evLine.substring(5).trim();
            }
          }

          if (data) {
            try {
              const parsedData = JSON.parse(data);
              onEvent(parsedData);
            } catch (e) {
              onEvent(data); // If not JSON
            }
          }
        }
      }
      onComplete();
    } catch (err) {
      if (err.name !== 'AbortError') {
        onError(err);
      }
    }
  };

  connect();
  return () => controller.abort();
};
