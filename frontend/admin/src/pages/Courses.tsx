import { useEffect, useState } from 'react';
import { fetchCourseStats, fetchCourseSignups } from '../services/api';
import type { CourseStatsData, SignupListData } from '../services/api';

export function Courses() {
  const [courses, setCourses] = useState<CourseStatsData[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedCourse, setSelectedCourse] = useState<number | null>(null);
  const [signups, setSignups] = useState<SignupListData[]>([]);

  useEffect(() => {
    fetchCourseStats()
      .then(setCourses)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const handleViewSignups = async (courseId: number) => {
    setSelectedCourse(courseId);
    try {
      const data = await fetchCourseSignups(courseId);
      setSignups(data);
    } catch {
      setSignups([]);
    }
  };

  if (loading) {
    return <div style={{ textAlign: 'center', padding: 60, color: '#999' }}>加载中...</div>;
  }

  return (
    <div>
      <h1 style={{ fontSize: 20, marginBottom: 24, color: '#1a1a2b' }}>课程管理</h1>

      <div style={{ background: '#fff', borderRadius: 8, overflow: 'hidden' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
          <thead>
            <tr style={{ background: '#f9fafb', textAlign: 'left' }}>
              <th style={{ padding: '10px 12px', color: '#666' }}>课程名称</th>
              <th style={{ padding: '10px 12px', color: '#666' }}>学院</th>
              <th style={{ padding: '10px 12px', color: '#666' }}>教师</th>
              <th style={{ padding: '10px 12px', color: '#666' }}>时间</th>
              <th style={{ padding: '10px 12px', color: '#666' }}>难度</th>
              <th style={{ padding: '10px 12px', color: '#666' }}>报名/容量</th>
              <th style={{ padding: '10px 12px', color: '#666' }}>候补</th>
              <th style={{ padding: '10px 12px', color: '#666' }}>满座率</th>
              <th style={{ padding: '10px 12px', color: '#666' }}>状态</th>
              <th style={{ padding: '10px 12px', color: '#666' }}>操作</th>
            </tr>
          </thead>
          <tbody>
            {courses.map((c) => (
              <tr key={c.id} style={{ borderBottom: '1px solid #f0f0f0' }}>
                <td style={{ padding: '10px 12px', maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {c.title}
                </td>
                <td style={{ padding: '10px 12px' }}>{c.college}</td>
                <td style={{ padding: '10px 12px' }}>{c.teacher}</td>
                <td style={{ padding: '10px 12px' }}>
                  {c.month}月 {c.week} {c.timeStart}-{c.timeEnd}
                </td>
                <td style={{ padding: '10px 12px' }}>{c.difficulty || '-'}</td>
                <td style={{ padding: '10px 12px' }}>{c.confirmedCount}/{c.maxSeats}</td>
                <td style={{ padding: '10px 12px' }}>{c.waitlistedCount}</td>
                <td style={{ padding: '10px 12px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <div style={{ flex: 1, background: '#e5e7eb', borderRadius: 4, height: 8, overflow: 'hidden' }}>
                      <div style={{
                        width: `${c.fillRate}%`,
                        background: c.fillRate > 80 ? '#22c55e' : c.fillRate > 50 ? '#f59e0b' : '#60a5fa',
                        height: '100%'
                      }} />
                    </div>
                    <span style={{ fontSize: 12, color: '#666' }}>{c.fillRate}%</span>
                  </div>
                </td>
                <td style={{ padding: '10px 12px' }}>
                  <span style={{
                    padding: '2px 8px',
                    borderRadius: 10,
                    fontSize: 12,
                    background: c.status === 'available' ? '#dcfce7' : '#fee2e2',
                    color: c.status === 'available' ? '#166534' : '#991b1b'
                  }}>
                    {c.status === 'available' ? '开放' : '关闭'}
                  </span>
                </td>
                <td style={{ padding: '10px 12px' }}>
                  <button
                    onClick={() => handleViewSignups(c.id)}
                    style={{
                      padding: '4px 8px',
                      background: selectedCourse === c.id ? '#1a1a2b' : '#f3f4f6',
                      color: selectedCourse === c.id ? '#fff' : '#333',
                      border: 'none',
                      borderRadius: 4,
                      cursor: 'pointer',
                      fontSize: 12
                    }}
                  >
                    名单
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Signup List Modal */}
      {selectedCourse !== null && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0,0,0,0.5)',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          zIndex: 100
        }}>
          <div style={{
            background: '#fff',
            borderRadius: 8,
            width: 600,
            maxWidth: '90vw',
            maxHeight: '80vh',
            overflow: 'auto',
            padding: 24
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
              <h3 style={{ fontSize: 15, margin: 0, color: '#333' }}>报名名单</h3>
              <button
                onClick={() => { setSelectedCourse(null); setSignups([]); }}
                style={{ background: 'none', border: 'none', fontSize: 18, cursor: 'pointer', color: '#999' }}
              >
                &times;
              </button>
            </div>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
              <thead>
                <tr style={{ borderBottom: '2px solid #e5e7eb', textAlign: 'left' }}>
                  <th style={{ padding: '8px', color: '#666' }}>昵称</th>
                  <th style={{ padding: '8px', color: '#666' }}>手机号</th>
                  <th style={{ padding: '8px', color: '#666' }}>状态</th>
                  <th style={{ padding: '8px', color: '#666' }}>报名时间</th>
                </tr>
              </thead>
              <tbody>
                {signups.map((s) => (
                  <tr key={s.id} style={{ borderBottom: '1px solid #f0f0f0' }}>
                    <td style={{ padding: '8px' }}>{s.nickName}</td>
                    <td style={{ padding: '8px' }}>{s.phone || '-'}</td>
                    <td style={{ padding: '8px' }}>
                      <span style={{
                        padding: '2px 6px',
                        borderRadius: 10,
                        fontSize: 12,
                        background: s.status === 'confirmed' ? '#dcfce7' : '#fef3c7',
                        color: s.status === 'confirmed' ? '#166534' : '#92400e'
                      }}>
                        {s.status === 'confirmed' ? '已确认' : '候补'}
                      </span>
                    </td>
                    <td style={{ padding: '8px', color: '#999' }}>
                      {new Date(s.createdAt).toLocaleString('zh-CN')}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
