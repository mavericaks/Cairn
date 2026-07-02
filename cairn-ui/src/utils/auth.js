import { getAuthToken } from './api';
import { jwtDecode } from 'jwt-decode';

export const isAuthenticated = () => {
  const token = getAuthToken();
  if (!token) return false;
  
  try {
    const decoded = jwtDecode(token);
    // Check expiration
    if (decoded.exp * 1000 < Date.now()) {
      return false;
    }
    return true;
  } catch (e) {
    return false;
  }
};

export const getUser = () => {
  const token = getAuthToken();
  if (!token) return null;
  
  try {
    return jwtDecode(token);
  } catch (e) {
    return null;
  }
};

export const isAdmin = () => {
  const user = getUser();
  return user && user.role === 'ROLE_ADMIN';
};
