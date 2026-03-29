import { View, Text, Button } from '@tarojs/components';
import Taro, { useDidShow } from '@tarojs/taro';
import { useState } from 'react';
import { getArticles } from '../../services/articles';
import type { ArticleItem } from '../../types/api';
import './index.scss';

export default function NewsPage() {
  const [articles, setArticles] = useState<ArticleItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const loadArticles = async () => {
    setLoading(true);
    setError('');

    try {
      const result = await getArticles();
      setArticles(result.data || []);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : '加载失败');
      setArticles([]);
    } finally {
      setLoading(false);
    }
  };

  const isValidArticleLink = (link: string): boolean => {
    const matched = link.match(/^https?:\/\/([^\/?#]+)(?:[\/?#]|$)/i);
    const hostname = matched?.[1]?.toLowerCase();

    if (!hostname) {
      return false;
    }

    const allowedDomains = ['mp.weixin.qq.com'];
    return allowedDomains.some((domain) => hostname === domain || hostname.endsWith(`.${domain}`));
  };

  const handleCopy = (link: string) => {
    if (!isValidArticleLink(link)) {
      void Taro.showToast({
        title: '链接不安全',
        icon: 'none',
      });
      return;
    }

    void Taro.setClipboardData({
      data: link,
      success: () => {
        void Taro.showToast({
          title: '链接已复制',
          icon: 'success',
        });
      },
    });
  };

  useDidShow(() => {
    void loadArticles();
  });

  return (
    <View className='page'>
      <View className='header'>
        <Text className='title'>夜校动态</Text>
      </View>

      {loading && !articles.length ? (
        <View className='state-container'>
          <Text className='hint'>正在加载动态...</Text>
        </View>
      ) : null}

      {error ? (
        <View className='state-container'>
          <Text className='hint error'>{error}</Text>
          <Button className='retry-btn' onClick={loadArticles}>重试</Button>
        </View>
      ) : null}

      {!loading && !error && articles.length === 0 ? (
        <View className='state-container'>
          <Text className='hint'>暂无最新动态</Text>
          <Button className='retry-btn secondary' onClick={loadArticles}>刷新</Button>
        </View>
      ) : null}

      <View className='list'>
        {articles.map((article) => (
          <View className='card' key={article.id} hoverClass='card-hover'>
            <Text className='card-title'>{article.title}</Text>
            <Text className='meta'>{article.publish_time || '暂无发布时间'}</Text>
            {article.link && isValidArticleLink(article.link) ? (
              <View 
                className='link-container' 
                onClick={() => handleCopy(article.link)}
                hoverClass='link-hover'
              >
                <Text className='link'>微信文章链接</Text>
                <Text className='link-hint'>点击复制链接</Text>
              </View>
            ) : null}
          </View>
        ))}
      </View>
    </View>
  );
}
