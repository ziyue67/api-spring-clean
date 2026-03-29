import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { clearToken, isAuthenticated } from '../config';

export function Layout() {
  const navigate = useNavigate();

  if (!isAuthenticated()) {
    navigate('/login');
    return null;
  }

  const handleLogout = () => {
    clearToken();
    navigate('/login');
  };

  const navLinkStyle = (isActive: boolean) => ({
    display: 'block',
    padding: '10px 20px',
    color: isActive ? '#60a5fa' : '#ccc',
    textDecoration: 'none',
    background: isActive ? 'rgba(255,255,255,0.1)' : 'transparent',
    fontSize: 14
  });

  return (
    <div style={{ display: 'flex', minHeight: '100vh' }}>
      <nav style={{
        width: 220,
        background: '#1a1a2b',
        color: '#fff',
        padding: '20px 0',
        flexShrink: 0,
        display: 'flex',
        flexDirection: 'column'
      }}>
        <div style={{ padding: '0 20px', marginBottom: 30 }}>
          <h2 style={{ fontSize: 16, margin: 0 }}>青禾夜校</h2>
          <p style={{ fontSize: 12, opacity: 0.6, margin: '4px 0 0' }}>管理后台</p>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <NavLink to="/" end style={({ isActive }) => navLinkStyle(isActive)}>
            数据概览
          </NavLink>
          <NavLink to="/courses" end style={({ isActive }) => navLinkStyle(isActive)}>
            课程管理
          </NavLink>
          <NavLink to="/users" end style={({ isActive }) => navLinkStyle(isActive)}>
            用户管理
          </NavLink>
        </div>
        <div style={{ marginTop: 'auto', padding: '0 20px' }}>
          <button
            onClick={handleLogout}
            style={{
              background: 'none',
              border: '1px solid rgba(255,255,255,0.3)',
              color: '#ccc',
              padding: '8px 16px',
              borderRadius: 4,
              cursor: 'pointer',
              width: '100%',
              fontSize: 13
            }}
          >
            退出登录
          </button>
        </div>
      </nav>
      <main style={{ flex: 1, padding: 24, background: '#f5f5f5', overflowY: 'auto' }}>
        <Outlet />
      </main>
    </div>
  );
}
