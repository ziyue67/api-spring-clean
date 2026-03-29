import { useEffect, useState } from 'react';
import { fetchOverview, fetchSignTrends, fetchCollegeStats } from '../services/api';
import type { OverviewData, SignTrendsData, CollegeStatsData } from '../services/api';

export function Dashboard() {
  const [overview, setOverview] = useState<OverviewData | null>(null);
  const [signTrends, setSignTrends] = useState<SignTrendsData[]>([]);
  const [colleges, setColleges] = useState<CollegeStatsData[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([fetchOverview(), fetchSignTrends(30), fetchCollegeStats()])
      .then(([ov, trends, col]) => {
        setOverview(ov);
        setSignTrends(trends);
        setColleges(col);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return <div style={{ textAlign: 'center', padding: 60, color: '#999' }}>加载中...</div>;
  }

  if (error) {
    return <div style={{ textAlign: 'center', padding: 60, color: '#e53e3e' }}>加载失败: {error}</div>;
  }

  if (!overview) return null;

  return (
    <div>
      <h1 style={{ fontSize: 20, marginBottom: 24, color: '#1a1a2b' }}>数据概览</h1>

      {/* Stat Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 32 }}>
        <StatCard title="总用户数" value={overview.totalUsers} />
        <StatCard title="总课程数" value={overview.totalCourses} />
        <StatCard title="确认报名" value={overview.totalConfirmedSignups} />
        <StatCard title="签到次数" value={overview.totalSignLogs} />
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 32 }}>
        {/* Sign Trends */}
        <div style={{ background: '#fff', padding: 20, borderRadius: 8 }}>
          <h3 style={{ fontSize: 15, marginBottom: 16, color: '#333' }}>签到趋势（近30天）</h3>
          <div style={{ maxHeight: 200, overflowY: 'auto' }}>
            {signTrends.length === 0 ? (
              <p style={{ color: '#999', fontSize: 13 }}>暂无数据</p>
            ) : (
              <div style={{ display: 'flex', alignItems: 'flex-end', gap: 2, height: 160 }}>
                {signTrends.map((item) => {
                  const maxCount = Math.max(...signTrends.map((t) => t.count), 1);
                  const height = (item.count / maxCount) * 140;
                  return (
                    <div key={item.date} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                      <div style={{
                        width: '100%',
                        height,
                        background: '#60a5fa',
                        borderRadius: '2px 2px 0 0',
                        minWidth: 8
                      }} />
                      <span style={{ fontSize: 10, color: '#666', marginTop: 4 }}>
                        {item.date.slice(5)}
                      </span>
                      <span style={{ fontSize: 10, color: '#333', fontWeight: 600 }}>
                        {item.count}
                      </span>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {/* College Distribution */}
        <div style={{ background: '#fff', padding: 20, borderRadius: 8 }}>
          <h3 style={{ fontSize: 15, marginBottom: 16, color: '#333' }}>学院课程分布</h3>
          {colleges.length === 0 ? (
            <p style={{ color: '#999', fontSize: 13 }}>暂无数据</p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {colleges.map((item) => (
                <div key={item.college} style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <span style={{ fontSize: 13, color: '#333', width: 120, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {item.college}
                  </span>
                  <div style={{ flex: 1, background: '#e5e7eb', borderRadius: 4, height: 20, overflow: 'hidden' }}>
                    <div style={{
                      width: `${(item.courseCount / Math.max(...colleges.map((c) => c.courseCount), 1)) * 100}%`,
                      background: '#818cf8',
                      height: '100%',
                      borderRadius: 4
                    }} />
                  </div>
                  <span style={{ fontSize: 13, color: '#666', width: 40, textAlign: 'right' }}>
                    {item.courseCount}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Recent Users */}
      <div style={{ background: '#fff', padding: 20, borderRadius: 8, marginBottom: 24 }}>
        <h3 style={{ fontSize: 15, marginBottom: 16, color: '#333' }}>最近注册用户</h3>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
          <thead>
            <tr style={{ borderBottom: '2px solid #e5e7eb', textAlign: 'left' }}>
              <th style={{ padding: '8px 12px', color: '#666' }}>昵称</th>
              <th style={{ padding: '8px 12px', color: '#666' }}>手机号</th>
              <th style={{ padding: '8px 12px', color: '#666' }}>积分</th>
              <th style={{ padding: '8px 12px', color: '#666' }}>注册时间</th>
            </tr>
          </thead>
          <tbody>
            {overview.recentUsers.map((user) => (
              <tr key={user.id} style={{ borderBottom: '1px solid #f0f0f0' }}>
                <td style={{ padding: '8px 12px' }}>{user.nickName || '未设置'}</td>
                <td style={{ padding: '8px 12px' }}>{user.phone || '-'}</td>
                <td style={{ padding: '8px 12px' }}>{user.points}</td>
                <td style={{ padding: '8px 12px', color: '#999' }}>
                  {new Date(user.createdAt).toLocaleDateString('zh-CN')}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Top Courses */}
      <div style={{ background: '#fff', padding: 20, borderRadius: 8 }}>
        <h3 style={{ fontSize: 15, marginBottom: 16, color: '#333' }}>热门课程</h3>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
          <thead>
            <tr style={{ borderBottom: '2px solid #e5e7eb', textAlign: 'left' }}>
              <th style={{ padding: '8px 12px', color: '#666' }}>课程名称</th>
              <th style={{ padding: '8px 12px', color: '#666' }}>学院</th>
              <th style={{ padding: '8px 12px', color: '#666' }}>教师</th>
              <th style={{ padding: '8px 12px', color: '#666' }}>报名/容量</th>
              <th style={{ padding: '8px 12px', color: '#666' }}>状态</th>
            </tr>
          </thead>
          <tbody>
            {overview.topCourses.map((course) => (
              <tr key={course.id} style={{ borderBottom: '1px solid #f0f0f0' }}>
                <td style={{ padding: '8px 12px' }}>{course.title}</td>
                <td style={{ padding: '8px 12px' }}>{course.college}</td>
                <td style={{ padding: '8px 12px' }}>{course.teacher}</td>
                <td style={{ padding: '8px 12px' }}>{course.signupCount}/{course.maxSeats}</td>
                <td style={{ padding: '8px 12px' }}>
                  <span style={{
                    padding: '2px 8px',
                    borderRadius: 10,
                    fontSize: 12,
                    background: course.status === 'available' ? '#dcfce7' : '#fee2e2',
                    color: course.status === 'available' ? '#166534' : '#991b1b'
                  }}>
                    {course.status === 'available' ? '开放' : '关闭'}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function StatCard({ title, value }: { title: string; value: number }) {
  return (
    <div style={{
      background: '#fff',
      padding: 20,
      borderRadius: 8,
      textAlign: 'center'
    }}>
      <p style={{ fontSize: 13, color: '#666', margin: '0 0 8px' }}>{title}</p>
      <p style={{ fontSize: 28, fontWeight: 700, color: '#1a1a2b', margin: 0 }}>{value.toLocaleString()}</p>
    </div>
  );
}
