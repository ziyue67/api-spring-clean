import { useEffect, useState } from 'react';
import { fetchUserList } from '../services/api';
import type { UserListData } from '../services/api';

export function Users() {
  const [data, setData] = useState<UserListData | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [keyword, setKeyword] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const pageSize = 20;

  const load = async (p: number, kw?: string) => {
    setLoading(true);
    try {
      const result = await fetchUserList(p, pageSize, kw || undefined);
      setData(result);
      setPage(p);
    } catch {
      setData(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load(1);
  }, []);

  const handleSearch = () => {
    setKeyword(searchInput);
    load(1, searchInput);
  };

  if (loading && !data) {
    return <div style={{ textAlign: 'center', padding: 60, color: '#999' }}>加载中...</div>;
  }

  if (!data) {
    return <div style={{ textAlign: 'center', padding: 60, color: '#e53e3e' }}>加载失败</div>;
  }

  return (
    <div>
      <h1 style={{ fontSize: 20, marginBottom: 24, color: '#1a1a2b' }}>用户管理</h1>

      {/* Search */}
      <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
        <input
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
          placeholder="搜索昵称、手机号或 openid"
          style={{
            flex: 1,
            padding: '8px 12px',
            border: '1px solid #d9d9d9',
            borderRadius: 4,
            fontSize: 13,
            outline: 'none'
          }}
        />
        <button
          onClick={handleSearch}
          style={{
            padding: '8px 16px',
            background: '#1a1a2b',
            color: '#fff',
            border: 'none',
            borderRadius: 4,
            cursor: 'pointer',
            fontSize: 13
          }}
        >
          搜索
        </button>
        {keyword && (
          <button
            onClick={() => { setSearchInput(''); setKeyword(''); load(1); }}
            style={{
              padding: '8px 16px',
              background: '#f3f4f6',
              border: 'none',
              borderRadius: 4,
              cursor: 'pointer',
              fontSize: 13
            }}
          >
            清除
          </button>
        )}
      </div>

      {/* Table */}
      <div style={{ background: '#fff', borderRadius: 8, overflow: 'hidden' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
          <thead>
            <tr style={{ background: '#f9fafb', textAlign: 'left' }}>
              <th style={{ padding: '10px 12px', color: '#666' }}>ID</th>
              <th style={{ padding: '10px 12px', color: '#666' }}>昵称</th>
              <th style={{ padding: '10px 12px', color: '#666' }}>手机号</th>
              <th style={{ padding: '10px 12px', color: '#666' }}>积分</th>
              <th style={{ padding: '10px 12px', color: '#666' }}>角色</th>
              <th style={{ padding: '10px 12px', color: '#666' }}>报名数</th>
              <th style={{ padding: '10px 12px', color: '#666' }}>签到数</th>
              <th style={{ padding: '10px 12px', color: '#666' }}>最近登录</th>
              <th style={{ padding: '10px 12px', color: '#666' }}>注册时间</th>
            </tr>
          </thead>
          <tbody>
            {data.data.map((user) => (
              <tr key={user.id} style={{ borderBottom: '1px solid #f0f0f0' }}>
                <td style={{ padding: '8px 12px', color: '#999' }}>{user.id}</td>
                <td style={{ padding: '8px 12px' }}>{user.nickName || '未设置'}</td>
                <td style={{ padding: '8px 12px' }}>{user.phone || '-'}</td>
                <td style={{ padding: '8px 12px' }}>{user.points}</td>
                <td style={{ padding: '8px 12px' }}>
                  <span style={{
                    padding: '2px 6px',
                    borderRadius: 10,
                    fontSize: 12,
                    background: user.roles.includes('admin') ? '#fef3c7' : '#f3f4f6',
                    color: user.roles.includes('admin') ? '#92400e' : '#666'
                  }}>
                    {user.roles.join(', ')}
                  </span>
                </td>
                <td style={{ padding: '8px 12px' }}>{user.signupCount}</td>
                <td style={{ padding: '8px 12px' }}>{user.signCount}</td>
                <td style={{ padding: '8px 12px', color: '#999' }}>
                  {new Date(user.lastLoginAt).toLocaleDateString('zh-CN')}
                </td>
                <td style={{ padding: '8px 12px', color: '#999' }}>
                  {new Date(user.createdAt).toLocaleDateString('zh-CN')}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 16 }}>
        <span style={{ fontSize: 13, color: '#666' }}>
          共 {data.total} 条， 第 {data.page}/{data.totalPages} 页
        </span>
        <div style={{ display: 'flex', gap: 8 }}>
          <button
            disabled={page <= 1}
            onClick={() => load(page - 1, keyword)}
            style={{
              padding: '6px 12px',
              border: '1px solid #d9d9d9',
              borderRadius: 4,
              background: page <= 1 ? '#f9fafb' : '#fff',
              cursor: page <= 1 ? 'not-allowed' : 'pointer',
              fontSize: 13
            }}
          >
            上一页
          </button>
          <button
            disabled={page >= data.totalPages}
            onClick={() => load(page + 1, keyword)}
            style={{
              padding: '6px 12px',
              border: '1px solid #d9d9d9',
              borderRadius: 4,
              background: page >= data.totalPages ? '#f9fafb' : '#fff',
              cursor: page >= data.totalPages ? 'not-allowed' : 'pointer',
              fontSize: 13
            }}
          >
            下一页
          </button>
        </div>
      </div>
    </div>
  );
}
