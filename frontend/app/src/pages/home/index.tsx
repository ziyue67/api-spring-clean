import { View, Text, Navigator } from '@tarojs/components';
import { useDidShow } from '@tarojs/taro';
import { useState } from 'react';
import { getRecentArticles } from '../../services/articles';
import { getUser, isLoggedIn } from '../../utils/session';
import type { ArticleItem } from '../../types/api';
import './index.scss';

export default function HomePage() {
  const [articles, setArticles] = useState<ArticleItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [loggedIn, setLoggedIn] = useState(isLoggedIn());
  const [currentNickName, setCurrentNickName] = useState(getUser()?.nickName || '');

  useDidShow(() => {
    setLoggedIn(isLoggedIn());
    setCurrentNickName(getUser()?.nickName || '');

    const loadArticles = async () => {
      setLoading(true);

      try {
        const result = await getRecentArticles();
        setArticles(result.data || []);
      } catch {
        setArticles([]);
      } finally {
        setLoading(false);
      }
    };

    void loadArticles();
  });

  return (
    <View className='page'>
      <View className='hero'>
        <Text className='title'>青禾夜校</Text>
        <Text className='subtitle'>面向夜校课程报名、签到与运营管理的一体化数字平台。</Text>
      </View>

      <View className='status-card'>
        <View className='status-info'>
          <Text className='status-title'>
            {loggedIn ? (currentNickName || '欢迎回来') : '游客访问'}
          </Text>
          <Text className='status-desc'>
            {loggedIn ? '已登录，可报名课程与签到' : '登录后可报名课程与签到'}
          </Text>
        </View>
        <Navigator 
          className={`status-btn ${loggedIn ? 'outline' : 'primary'}`}
          url='/pages/login/index'
        >
          {loggedIn ? '切换账号' : '立即登录'}
        </Navigator>
      </View>

      <View className='card'>
        <Text className='card-title'>快速入口</Text>
        <Navigator className='nav-item' url='/pages/news/index'>查看夜校动态</Navigator>
        <Navigator className='nav-item' url='/pages/courses/index'>查看课程安排</Navigator>
        <Navigator className='nav-item' url='/pages/sign/index'>每日签到</Navigator>
        <Navigator className='nav-item' url='/pages/profile/index'>我的信息</Navigator>
      </View>

      <View className='card'>
        <Text className='card-title'>近期动态</Text>
        {loading ? <Text className='roadmap-item'>加载中...</Text> : null}
        {!loading && articles.length === 0 ? <Text className='roadmap-item'>暂无动态</Text> : null}
        {articles.map((item) => (
          <View className='roadmap-item' key={item.id}>
            <Text>{item.title}</Text>
          </View>
        ))}
      </View>
    </View>
  );
}
