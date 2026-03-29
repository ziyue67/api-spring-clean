import { View, Text, Input, Button } from '@tarojs/components';
import Taro from '@tarojs/taro';
import { useState } from 'react';
import { loginWithWechatCode } from '../../services/wechat-auth';
import type { UserItem } from '../../types/api';
import { setSession } from '../../utils/session';
import { sanitizeTextInput } from '../../utils/sanitize';
import './index.scss';

type LoginSuccessResult = {
  token: string;
  data: UserItem;
  message: string;
};

export default function LoginPage() {
  const [nickName, setNickName] = useState('微信用户');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  const finishLogin = (result: LoginSuccessResult) => {
    setSession(result.token, result.data);
    setMessage(result.message);
    Taro.showToast({ title: '登录成功', icon: 'success' });
    setTimeout(() => {
      void Taro.switchTab({ url: '/pages/home/index' }).catch(() => {
        void Taro.navigateTo({ url: '/pages/home/index' });
      });
    }, 300);
  };

  const ensureLoginSuccess = (result: {
    success?: boolean;
    token?: string;
    data?: UserItem | null;
    message?: string;
  }) => {
    if (!result?.success || !result?.token || !result?.data) {
      throw new Error(result?.message || '登录失败');
    }

    return result as LoginSuccessResult;
  };

  const handleWechatLogin = async () => {
    setLoading(true);
    setMessage('');

    try {
      const loginResult = await Taro.login();
      const code = loginResult.code;

      if (!code) {
        throw new Error('未获取到微信登录凭证，请重试');
      }

      const result = await loginWithWechatCode(code, sanitizeTextInput(nickName, 100) || '微信用户');
      finishLogin(ensureLoginSuccess(result));
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '微信登录失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <View className='page'>
      <Text className='title'>登录 / 切换身份</Text>

      <View className='hero-card'>
        <Text className='hero-title'>微信一键登录</Text>
        <Text className='hero-desc'>使用当前微信身份登录，自动获取你的夜校账号信息。</Text>
        <Button className='button secondary primary-login' loading={loading} onClick={handleWechatLogin}>
          微信一键登录
        </Button>
      </View>
      <Input
        className='input'
        placeholder='昵称（可选）'
        value={nickName}
        maxlength={100}
        onInput={(event) => setNickName(sanitizeTextInput(event.detail.value, 100))}
      />
      <Text className='message helper'>如需找回或合并历史档案，请联系管理员协助处理。</Text>

      {message ? <Text className='message'>{message}</Text> : null}
    </View>
  );

}
