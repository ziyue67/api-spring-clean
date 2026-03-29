import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { setToken } from '../config';

export function Login() {
  const navigate = useNavigate();
  const [token, setTokenInput] = useState('');
  const [error, setError] = useState('');

  const handleLogin = () => {
    if (!token.trim()) {
      setError('请输入管理员 Token');
      return;
    }
    // Admin token is a JWT issued by the API's wechat-login endpoint
    // For admin dashboard, we accept the JWT directly
    setToken(token.trim());
    navigate('/');
  };

  return (
    <div style={{
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      minHeight: '100vh',
      background: '#f5f5f5'
    }}>
      <div style={{
        background: '#fff',
        padding: 40,
      borderRadius: 8,
        boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
        width: 400,
        maxWidth: '90vw'
      }}>
        <h1 style={{ fontSize: 22, marginBottom: 8, color: '#1a1a2b' }}>青禾夜校</h1>
        <p style={{ fontSize: 14, color: '#666', marginBottom: 24 }}>管理后台登录</p>
        <div style={{ marginBottom: 16 }}>
          <label style={{ display: 'block', fontSize: 13, marginBottom: 6, color: '#333' }}>
            JWT Token
          </label>
          <textarea
            value={token}
            onChange={(e) => { setTokenInput(e.target.value); setError(''); }}
            placeholder="粘贴从 API 获取的管理员 JWT Token"
            rows={4}
            style={{
              width: '100%',
              padding: 10,
              border: '1px solid #d9d9d9',
              borderRadius: 4,
              fontSize: 13,
              fontFamily: 'monospace',
              boxSizing: 'border-box',
              resize: 'vertical'
            }}
          />
        </div>
        {error && <p style={{ color: '#e53e3e', fontSize: 13, marginBottom: 12 }}>{error}</p>}
        <button
          onClick={handleLogin}
          style={{
            width: '100%',
            padding: '10px',
            background: '#1a1a2b',
            color: '#fff',
            border: 'none',
            borderRadius: 4,
            fontSize: 14,
            cursor: 'pointer'
          }}
        >
          登录
        </button>
      </div>
    </div>
  );
}
