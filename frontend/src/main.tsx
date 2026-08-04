import React from 'react';
import ReactDOM from 'react-dom/client';
import { RouterWithAuth } from './routes';
import { AppThemeProvider } from './contexts/ThemeContext';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <AppThemeProvider>
      <RouterWithAuth />
    </AppThemeProvider>
  </React.StrictMode>
);
