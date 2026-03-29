import { View, Text, Input, Button, Image } from '@tarojs/components';
import Taro, { useDidShow } from '@tarojs/taro';
import { useEffect, useMemo, useState } from 'react';
import { cancelSignupCourse, getCourseMonths, getCourses, getSignedCourses, searchCourses, signupCourse } from '../../services/courses';
import type { CourseItem, CourseSearchItem } from '../../types/api';
import { isLoggedIn } from '../../utils/session';
import { sanitizeStorageList, sanitizeStorageText } from '../../utils/sanitize';
import './index.scss';

const colleges = ['建筑工程学院', '智能制造与电梯学院', '新能源工程与汽车学院', '信息工程与物联网学院'];

const COURSE_STATUS_LABEL_MAP: Record<string, string> = {
  available: '报名中',
  closed: '已关闭',
  draft: '未发布'
};

const SEARCH_HISTORY_KEY = 'course_search_history';
const COURSE_FILTER_STATE_KEY = 'course_filter_state';

function normalizeMonthValue(value: unknown) {
  return typeof value === 'number' && Number.isInteger(value) && value > 0 ? value : null;
}

function normalizeEnrollmentValue(value: unknown) {
  return value === 'confirmed' || value === 'waitlisted' ? value : '';
}

export default function CoursesPage() {
  const [currentCollege, setCurrentCollege] = useState(colleges[0]);
  const [months, setMonths] = useState<number[]>([]);
  const [currentMonth, setCurrentMonth] = useState<number | null>(null);
  const [courses, setCourses] = useState<CourseItem[]>([]);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [activeSearchKeyword, setActiveSearchKeyword] = useState('');
  const [searchResults, setSearchResults] = useState<CourseSearchItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [signingCourseId, setSigningCourseId] = useState('');
  const [signedCourseMap, setSignedCourseMap] = useState<Record<string, 'confirmed' | 'waitlisted'>>({});
  const [filterAvailable, setFilterAvailable] = useState(false);
  const [filterSigned, setFilterSigned] = useState(false);
  const [selectedDifficulty, setSelectedDifficulty] = useState('');
  const [selectedAudience, setSelectedAudience] = useState('');
  const [selectedFee, setSelectedFee] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('');
  const [selectedEnrollment, setSelectedEnrollment] = useState('');
  const [recentSearches, setRecentSearches] = useState<string[]>([]);
  const [hasHydratedFilterState, setHasHydratedFilterState] = useState(false);

  const applyFilterState = (state?: Record<string, unknown> | null) => {
    if (!state || typeof state !== 'object') {
      setFilterAvailable(false);
      setFilterSigned(false);
      setSelectedDifficulty('');
      setSelectedAudience('');
      setSelectedFee('');
      setSelectedStatus('');
      setSelectedEnrollment('');
      return {
        currentCollege: colleges[0],
        currentMonth: null
      };
    }

    setFilterAvailable(!!state.filterAvailable);
    setFilterSigned(!!state.filterSigned);
    setSelectedDifficulty(sanitizeStorageText(state.selectedDifficulty));
    setSelectedAudience(sanitizeStorageText(state.selectedAudience));
    setSelectedFee(sanitizeStorageText(state.selectedFee));
    setSelectedStatus(sanitizeStorageText(state.selectedStatus, 20) === 'closed' ? 'closed' : sanitizeStorageText(state.selectedStatus, 20) === 'available' ? 'available' : '');
    setSelectedEnrollment(normalizeEnrollmentValue(state.selectedEnrollment));

    const savedCollege = sanitizeStorageText(state.currentCollege);

    return {
      currentCollege: colleges.includes(savedCollege) ? savedCollege : colleges[0],
      currentMonth: normalizeMonthValue(state.currentMonth)
    };
  };

  const loadSignedCourses = async () => {
    if (!isLoggedIn()) {
      setSignedCourseMap({});
      return;
    }

    try {
      const result = await getSignedCourses();
      setSignedCourseMap(
        Object.fromEntries(
          (result.courses || []).map((course) => [String(course.id), course.signupStatus || 'confirmed'])
        )
      );
    } catch {
      setSignedCourseMap({});
    }
  };

  const loadCourses = async (college: string, month?: number | null) => {
    setLoading(true);
    setError('');

    try {
      const monthResult = await getCourseMonths(college);
      const availableMonths = monthResult.months || [];
      const nextMonth = typeof month === 'number' ? month : availableMonths[0] ?? null;
      setMonths(availableMonths);
      setCurrentMonth(nextMonth);

      if (typeof nextMonth === 'number') {
        const courseResult = await getCourses(college, nextMonth);
        setCourses(courseResult.courses || []);
      } else {
        setCourses([]);
      }
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : '课程加载失败');
      setMonths([]);
      setCourses([]);
      setCurrentMonth(null);
    } finally {
      setLoading(false);
    }
  };

  const executeSearch = async (rawKeyword: string) => {
    const keyword = sanitizeStorageText(rawKeyword);
    if (!keyword) {
      setSearchKeyword('');
      setActiveSearchKeyword('');
      setSearchResults([]);
      return;
    }

    const newHistory = sanitizeStorageList([keyword, ...recentSearches.filter((item) => item !== keyword)]);
    setRecentSearches(newHistory);
    Taro.setStorageSync(SEARCH_HISTORY_KEY, newHistory);
    setSearchKeyword(keyword);
    setActiveSearchKeyword(keyword);

    setLoading(true);
    setError('');
    
    try {
      const result = await searchCourses(keyword);
      setSearchResults(result.results || []);
    } catch (searchError) {
      setError(searchError instanceof Error ? searchError.message : '搜索失败');
      setSearchResults([]);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async () => {
    await executeSearch(searchKeyword);
  };

  useDidShow(() => {
    void loadSignedCourses();

    try {
      const history = Taro.getStorageSync(SEARCH_HISTORY_KEY) || [];
      setRecentSearches(sanitizeStorageList(history));
    } catch {
      setRecentSearches([]);
    }

    let restoredCollege = currentCollege;
    let restoredMonth = currentMonth;

    try {
      const savedFilters = Taro.getStorageSync(COURSE_FILTER_STATE_KEY);
      const restoredState = applyFilterState(savedFilters && typeof savedFilters === 'object' ? savedFilters : null);
      restoredCollege = restoredState.currentCollege;
      restoredMonth = restoredState.currentMonth;
      setCurrentCollege(restoredCollege);
      setCurrentMonth(restoredMonth);
    } catch {
      const restoredState = applyFilterState(null);
      setCurrentCollege(restoredState.currentCollege);
      setCurrentMonth(restoredState.currentMonth);
      restoredCollege = restoredState.currentCollege;
      restoredMonth = restoredState.currentMonth;
    } finally {
      setHasHydratedFilterState(true);
    }

    if (!activeSearchKeyword) {
      void loadCourses(restoredCollege, restoredMonth);
    }
  });

  useEffect(() => {
    if (!hasHydratedFilterState) {
      return;
    }

    Taro.setStorageSync(COURSE_FILTER_STATE_KEY, {
      currentCollege,
      currentMonth,
      filterAvailable,
      filterSigned,
      selectedDifficulty,
      selectedAudience,
      selectedFee,
      selectedStatus,
      selectedEnrollment
    });
  }, [currentCollege, currentMonth, filterAvailable, filterSigned, hasHydratedFilterState, selectedAudience, selectedDifficulty, selectedEnrollment, selectedFee, selectedStatus]);

  const trimmedSearchKeyword = sanitizeStorageText(searchKeyword);
  const hasPendingSearchInput = !!trimmedSearchKeyword && trimmedSearchKeyword !== activeSearchKeyword;
  const showSearchResults = !!activeSearchKeyword && !hasPendingSearchInput;

  const isCourseOpenForSignup = (course: CourseItem | CourseSearchItem) => {
    if ((course.status || 'available') !== 'available') {
      return false;
    }

    if (typeof course.isSignupOpen === 'boolean') {
      return course.isSignupOpen;
    }

    return true;
  };

  const getSignupWindowLabel = (course: CourseItem | CourseSearchItem) => {
    if (!course.signupStartAt && !course.signupEndAt) {
      return '';
    }

    if (course.isSignupOpen === false) {
      if (course.signupStartAt && Date.now() < Date.parse(course.signupStartAt)) {
        return '报名未开始';
      }

      if (course.signupEndAt && Date.now() > Date.parse(course.signupEndAt)) {
        return '报名已结束';
      }

      return '当前不在报名时间内';
    }

    const startLabel = course.signupStartAt ? new Date(course.signupStartAt).toLocaleString('zh-CN', { hour12: false }) : '即刻';
    const endLabel = course.signupEndAt ? new Date(course.signupEndAt).toLocaleString('zh-CN', { hour12: false }) : '长期开放';

    return `报名时间：${startLabel} - ${endLabel}`;
  };

  const getCapacityLabel = (course: CourseItem | CourseSearchItem) => {
    if (typeof course.remainingSeats === 'number') {
      if (course.remainingSeats <= 0) {
        return '名额已满';
      }

      return `剩余 ${course.remainingSeats} 个名额`;
    }

    return '名额充足';
  };

  const getSignedCourseStatus = (courseId: string | number) => signedCourseMap[String(courseId)];

  const difficulties = useMemo(
    () => Array.from(new Set((showSearchResults ? searchResults : courses).map((course) => course.difficulty).filter(Boolean))) as string[],
    [courses, searchResults, showSearchResults],
  );

  const audiences = useMemo(
    () => Array.from(new Set((showSearchResults ? searchResults : courses).map((course) => course.audience).filter(Boolean))) as string[],
    [courses, searchResults, showSearchResults],
  );

  const fees = useMemo(
    () => Array.from(new Set((showSearchResults ? searchResults : courses).map((course) => course.fee).filter(Boolean))) as string[],
    [courses, searchResults, showSearchResults],
  );

  useEffect(() => {
    if (!hasHydratedFilterState) {
      return;
    }

    if (selectedDifficulty && !difficulties.includes(selectedDifficulty)) {
      setSelectedDifficulty('');
    }

    if (selectedAudience && !audiences.includes(selectedAudience)) {
      setSelectedAudience('');
    }

    if (selectedFee && !fees.includes(selectedFee)) {
      setSelectedFee('');
    }

    if (selectedEnrollment && !['confirmed', 'waitlisted'].includes(selectedEnrollment)) {
      setSelectedEnrollment('');
    }

    if (selectedStatus && !['available', 'closed'].includes(selectedStatus)) {
      setSelectedStatus('');
    }

    if (filterSigned && Object.keys(signedCourseMap).length === 0) {
      setFilterSigned(false);
    }

    if (currentMonth !== null && !months.includes(currentMonth)) {
      setCurrentMonth(months[0] ?? null);
    }
  }, [
    audiences,
    currentMonth,
    difficulties,
    fees,
    filterSigned,
    hasHydratedFilterState,
    months,
    selectedAudience,
    selectedDifficulty,
    selectedEnrollment,
    selectedFee,
    selectedStatus,
    signedCourseMap
  ]);

  const quickTags = useMemo(() => {
    const allTags = courses.flatMap((course) => course.tags || []);
    return Array.from(new Set(allTags)).slice(0, 8);
  }, [courses]);

  const searchSuggestions = useMemo(() => {
    if (!trimmedSearchKeyword || trimmedSearchKeyword === activeSearchKeyword) {
      return [] as string[];
    }

    const keyword = trimmedSearchKeyword.toLowerCase();
    const courseNames = courses.map((course) => course.name).filter(Boolean);
    const merged = [...recentSearches, ...quickTags, ...courseNames];

    return Array.from(new Set(merged))
      .filter((item) => item.toLowerCase().includes(keyword))
      .slice(0, 8);
  }, [activeSearchKeyword, courses, quickTags, recentSearches, trimmedSearchKeyword]);

  const renderHighlightedSuggestion = (value: string) => {
    const keyword = trimmedSearchKeyword.toLowerCase();
    const matchIndex = value.toLowerCase().indexOf(keyword);

    if (!keyword || matchIndex < 0) {
      return <Text className='search-suggestion-chip-text'>{value}</Text>;
    }

    const before = value.slice(0, matchIndex);
    const match = value.slice(matchIndex, matchIndex + trimmedSearchKeyword.length);
    const after = value.slice(matchIndex + trimmedSearchKeyword.length);

    return (
      <Text className='search-suggestion-chip-text'>
        {before}
        <Text className='search-suggestion-chip-match'>{match}</Text>
        {after}
      </Text>
    );
  };

  const resetFilters = () => {
    setFilterAvailable(false);
    setFilterSigned(false);
    setSelectedDifficulty('');
    setSelectedAudience('');
    setSelectedFee('');
    setSelectedStatus('');
    setSelectedEnrollment('');
  };

  const activeFilters = useMemo(() => {
    const items: Array<{ key: string; label: string; onClear: () => void }> = [];

    if (filterAvailable) {
      items.push({
        key: 'available',
        label: '只看可报',
        onClear: () => setFilterAvailable(false)
      });
    }

    if (filterSigned) {
      items.push({
        key: 'signed',
        label: '我的课程',
        onClear: () => setFilterSigned(false)
      });
    }

    if (selectedEnrollment) {
      items.push({
        key: `enrollment-${selectedEnrollment}`,
        label: selectedEnrollment === 'confirmed' ? '已报名' : '候补中',
        onClear: () => setSelectedEnrollment('')
      });
    }

    if (selectedStatus) {
      items.push({
        key: `status-${selectedStatus}`,
        label: COURSE_STATUS_LABEL_MAP[selectedStatus] || selectedStatus,
        onClear: () => setSelectedStatus('')
      });
    }

    if (selectedDifficulty) {
      items.push({
        key: `difficulty-${selectedDifficulty}`,
        label: `难度：${selectedDifficulty}`,
        onClear: () => setSelectedDifficulty('')
      });
    }

    if (selectedAudience) {
      items.push({
        key: `audience-${selectedAudience}`,
        label: `适合人群：${selectedAudience}`,
        onClear: () => setSelectedAudience('')
      });
    }

    if (selectedFee) {
      items.push({
        key: `fee-${selectedFee}`,
        label: `费用：${selectedFee}`,
        onClear: () => setSelectedFee('')
      });
    }

    return items;
  }, [filterAvailable, filterSigned, selectedAudience, selectedDifficulty, selectedEnrollment, selectedFee, selectedStatus]);

  const filterCourse = (course: CourseItem | CourseSearchItem) => {
      if (filterAvailable && (!isCourseOpenForSignup(course) || course.isFull)) {
        return false;
      }

      if (filterSigned && !getSignedCourseStatus(course.id)) {
        return false;
      }

      if (selectedDifficulty && course.difficulty !== selectedDifficulty) {
        return false;
      }

      if (selectedAudience && course.audience !== selectedAudience) {
        return false;
      }

      if (selectedFee && course.fee !== selectedFee) {
        return false;
      }

      if (selectedStatus && (course.status || 'available') !== selectedStatus) {
        return false;
      }

      if (selectedEnrollment && getSignedCourseStatus(course.id) !== selectedEnrollment) {
        return false;
      }

      return true;
    };

  const filteredCourseList = useMemo(
    () => courses.filter(filterCourse),
    [courses, filterAvailable, filterSigned, selectedDifficulty, selectedAudience, selectedFee, selectedStatus, selectedEnrollment, signedCourseMap],
  );

  const filteredSearchResults = useMemo(
    () => searchResults.filter(filterCourse),
    [searchResults, filterAvailable, filterSigned, selectedDifficulty, selectedAudience, selectedFee, selectedStatus, selectedEnrollment, signedCourseMap],
  );

  const resultsSummary = useMemo(() => {
    if (showSearchResults) {
      return {
        modeLabel: '搜索中',
        title: `搜索“${activeSearchKeyword}”`,
        description: `找到 ${filteredSearchResults.length} 门相关课程`
      };
    }

    if (currentMonth) {
      return {
        modeLabel: '浏览中',
        title: `${currentCollege} · ${currentMonth} 月课程`,
        description: `当前共 ${filteredCourseList.length} 门课程`
      };
    }

    return {
      modeLabel: '浏览中',
      title: `${currentCollege}`,
      description: `当前共 ${filteredCourseList.length} 门课程`
    };
  }, [activeSearchKeyword, currentCollege, currentMonth, filteredCourseList.length, filteredSearchResults.length, showSearchResults]);

  const emptyStateSummary = useMemo(() => {
    if (showSearchResults) {
      return `当前搜索“${activeSearchKeyword}”`; 
    }

    if (currentMonth) {
      return `${currentCollege} · ${currentMonth} 月`;
    }

    return currentCollege;
  }, [activeSearchKeyword, currentCollege, currentMonth, showSearchResults]);

  const getCourseStatusLabel = (status?: string) => COURSE_STATUS_LABEL_MAP[status || 'available'] || '报名中';

  const getActionLabel = (course: CourseItem | CourseSearchItem) => {
    const signedStatus = getSignedCourseStatus(course.id);

    if (signedStatus === 'confirmed') {
      return '已报名，取消报名';
    }

    if (signedStatus === 'waitlisted') {
      return '已候补，取消候补';
    }

    if (!isCourseOpenForSignup(course)) {
      return getSignupWindowLabel(course) || '暂不可报名';
    }

    return course.isFull ? '加入候补' : '立即报名';
  };

  const getActionHint = (course: CourseItem | CourseSearchItem) => {
    const signedStatus = getSignedCourseStatus(course.id);

    if (signedStatus === 'confirmed') {
      return '点击后可取消当前报名';
    }

    if (signedStatus === 'waitlisted') {
      return '点击后可取消当前候补资格';
    }

    if (!isCourseOpenForSignup(course)) {
      return getSignupWindowLabel(course) || '当前课程暂不可报名';
    }

    if (course.isFull) {
      return '提交后将进入候补名单';
    }

    return '报名成功后可在个人中心查看';
  };

  const handleSignup = async (courseId: string | number) => {
    if (!isLoggedIn()) {
      setError('请先登录后再报名');
      return;
    }

    setSigningCourseId(String(courseId));
    setError('');

    try {
      const result = await signupCourse(typeof courseId === 'number' ? courseId : undefined, typeof courseId === 'string' ? courseId : undefined);
      if (!result.success) {
        if (result.data?.isFull || result.message === '课程名额已满') {
          setError(result.message || '课程名额已满');
          void Taro.showToast({ title: result.message || '课程名额已满', icon: 'none' });

          if (showSearchResults) {
            await handleSearch();
          } else {
            await loadCourses(currentCollege, currentMonth);
          }

          return;
        }

        throw new Error(result.message || '报名失败');
      }

      void Taro.showToast({ title: result.message || '报名成功', icon: 'success' });
      await loadSignedCourses();

      if (showSearchResults) {
        await handleSearch();
      } else {
        await loadCourses(currentCollege, currentMonth);
      }
    } catch (signupError) {
      setError(signupError instanceof Error ? signupError.message : '报名失败');
    } finally {
      setSigningCourseId('');
    }
  };

  const handleCancelSignup = async (courseId: string | number) => {
    if (!isLoggedIn()) {
      setError('请先登录后再操作');
      return;
    }

    const signedStatus = getSignedCourseStatus(courseId);
    const confirmResult = await Taro.showModal({
      title: '提示',
      content: signedStatus === 'waitlisted' ? '确定取消候补吗？' : '确定取消报名吗？',
      confirmColor: '#ef4444'
    });

    if (!confirmResult.confirm) {
      return;
    }

    setSigningCourseId(String(courseId));
    setError('');

    try {
      const result = await cancelSignupCourse(typeof courseId === 'number' ? courseId : undefined, typeof courseId === 'string' ? courseId : undefined);
      if (!result.success) {
        throw new Error(result.message || '取消报名失败');
      }

      void Taro.showToast({ title: '已取消报名', icon: 'success' });
      await loadSignedCourses();

      if (showSearchResults) {
        await handleSearch();
      } else {
        await loadCourses(currentCollege, currentMonth);
      }
    } catch (cancelError) {
      setError(cancelError instanceof Error ? cancelError.message : '取消报名失败');
    } finally {
      setSigningCourseId('');
    }
  };

  const handleQuickTagClick = async (tag: string) => {
    setSearchKeyword(tag);
    await executeSearch(tag);
  };

  const clearSearch = () => {
    setSearchKeyword('');
    setActiveSearchKeyword('');
    setSearchResults([]);
  };

  const clearHistory = () => {
    Taro.removeStorageSync(SEARCH_HISTORY_KEY);
    setRecentSearches([]);
  };

  return (
    <View className='page'>
      <View className='header'>
        <Text className='title'>夜校课程</Text>
      </View>

      {!showSearchResults && (
        <>
          <View className='browse-context-bar'>
            <View>
              <Text className='browse-context-title'>当前浏览：{currentCollege}</Text>
              <Text className='browse-context-desc'>
                {currentMonth ? `聚焦 ${currentMonth} 月课程，可切换学院或月份继续查看` : '可切换学院查看不同课程安排'}
              </Text>
            </View>
            <Text className='browse-context-tag'>{currentMonth ? `${currentMonth} 月` : '待选择月份'}</Text>
          </View>
          <View className='college-list'>
            {colleges.map((college) => (
              <View
                className={`college-item ${currentCollege === college ? 'active' : ''}`}
                key={college}
                onClick={() => {
                  setCurrentCollege(college);
                  void loadCourses(college);
                }}
                hoverClass='college-item-hover'
              >
                <Text>{college}</Text>
              </View>
            ))}
          </View>
        </>
      )}

      <View className='search-box'>
        <View className='search-input-wrapper'>
          <View className='search-icon'>
            <Text>🔍</Text>
          </View>
          <Input
            className='search-input'
            placeholder='搜索课程关键词'
            placeholderClass='search-placeholder'
            value={searchKeyword}
            onInput={(event) => setSearchKeyword(sanitizeStorageText(event.detail.value))}
            onConfirm={handleSearch}
            confirmType='search'
          />
          {searchKeyword ? (
            <View 
              className='search-clear' 
              onClick={clearSearch}
            >
              <Text className='search-clear-text'>取消</Text>
            </View>
          ) : null}
        </View>
        {searchSuggestions.length > 0 ? (
          <View className='search-suggestion-panel'>
            <View className='search-suggestion-header'>
              <Text className='search-suggestion-title'>猜你想搜</Text>
              <Text className='search-suggestion-subtitle'>按关键词快速发起搜索</Text>
            </View>
            <View className='search-suggestion-list'>
              {searchSuggestions.map((item) => (
                <View className='search-suggestion-chip' key={item} onClick={() => void handleQuickTagClick(item)}>
                  <Text className='search-suggestion-chip-icon'>🔎</Text>
                  {renderHighlightedSuggestion(item)}
                </View>
              ))}
            </View>
          </View>
        ) : null}
        {!searchKeyword && recentSearches.length > 0 ? (
          <View className='quick-search-bar history-bar'>
            <View className='quick-search-header'>
              <Text className='quick-search-label'>最近搜：</Text>
              <View className='history-clear' onClick={clearHistory}>
                <Text className='history-clear-text'>清空</Text>
              </View>
            </View>
            <View className='quick-search-list'>
              {recentSearches.map((item) => (
                <View className='quick-search-chip' key={item} onClick={() => void handleQuickTagClick(item)}>
                  <Text>{item}</Text>
                </View>
              ))}
            </View>
          </View>
        ) : null}
        {!searchKeyword && quickTags.length > 0 ? (
          <View className='quick-search-bar'>
            <Text className='quick-search-label'>热门搜：</Text>
            <View className='quick-search-list'>
              {quickTags.map((tag) => (
                <View className='quick-search-chip' key={tag} onClick={() => void handleQuickTagClick(tag)}>
                  <Text>{tag}</Text>
                </View>
              ))}
            </View>
          </View>
        ) : null}
      </View>

      <View className='filter-section'>
        <Text className='filter-section-label'>快捷筛选</Text>
        <View className='filter-bar'>
          <View
            className={`filter-chip ${filterAvailable ? 'active' : ''}`}
            onClick={() => setFilterAvailable((current) => !current)}
          >
            <Text>只看可报</Text>
          </View>
          {['available', 'closed'].map((status) => (
            <View
              className={`filter-chip ${selectedStatus === status ? 'active' : ''}`}
              key={status}
              onClick={() => setSelectedStatus((current) => current === status ? '' : status)}
            >
              <Text>{COURSE_STATUS_LABEL_MAP[status] || status}</Text>
            </View>
          ))}
        </View>
      </View>

      {Object.keys(signedCourseMap).length ? (
        <View className='filter-section'>
          <Text className='filter-section-label'>报名身份</Text>
          <View className='filter-bar'>
            <View
              className={`filter-chip ${filterSigned ? 'active' : ''}`}
              onClick={() => setFilterSigned((current) => !current)}
            >
              <Text>我的课程</Text>
            </View>
            {['confirmed', 'waitlisted'].map((status) => (
              <View
                className={`filter-chip ${selectedEnrollment === status ? 'active' : ''}`}
                key={status}
                onClick={() => setSelectedEnrollment((current) => current === status ? '' : status)}
              >
                <Text>{status === 'confirmed' ? '已报名' : '候补中'}</Text>
              </View>
            ))}
          </View>
        </View>
      ) : null}

      <View className='filter-section'>
        <Text className='filter-section-label'>课程属性</Text>
        <View className='filter-bar'>
          {difficulties.map((difficulty) => (
            <View
              className={`filter-chip ${selectedDifficulty === difficulty ? 'active' : ''}`}
              key={difficulty}
              onClick={() => setSelectedDifficulty((current) => current === difficulty ? '' : difficulty)}
            >
              <Text>{difficulty}</Text>
            </View>
          ))}
          {audiences.map((audience) => (
            <View
              className={`filter-chip ${selectedAudience === audience ? 'active' : ''}`}
              key={audience}
              onClick={() => setSelectedAudience((current) => current === audience ? '' : audience)}
            >
              <Text>{audience}</Text>
            </View>
          ))}
          {fees.map((fee) => (
            <View
              className={`filter-chip ${selectedFee === fee ? 'active' : ''}`}
              key={fee}
              onClick={() => setSelectedFee((current) => current === fee ? '' : fee)}
            >
              <Text>{fee}</Text>
            </View>
          ))}
        </View>
      </View>

      {activeFilters.length > 0 ? (
        <View className='active-filter-panel'>
          <View className='active-filter-header'>
            <Text className='active-filter-title'>已选条件 {activeFilters.length}</Text>
            <View className='active-filter-reset' onClick={resetFilters}>
              <Text className='active-filter-reset-text'>一键清空</Text>
            </View>
          </View>
          <View className='active-filter-list'>
            {activeFilters.map((filter) => (
              <View className='active-filter-chip' key={filter.key} onClick={filter.onClear}>
                <Text className='active-filter-chip-text'>{filter.label}</Text>
                <Text className='active-filter-chip-close'>×</Text>
              </View>
            ))}
          </View>
        </View>
      ) : null}

      {!loading && !error ? (
        <View className='results-summary-bar'>
          <View className='results-summary-main'>
            <Text className='results-summary-mode'>{resultsSummary.modeLabel}</Text>
            <Text className='results-summary-title'>{resultsSummary.title}</Text>
            <Text className='results-summary-desc'>{resultsSummary.description}</Text>
          </View>
          <View className='results-summary-side'>
            <Text className='results-summary-count'>{activeFilters.length ? `已筛选 ${activeFilters.length}` : '未筛选'}</Text>
            {(showSearchResults || activeFilters.length > 0) ? (
              <View className='results-summary-actions'>
                {showSearchResults ? (
                  <View className='results-summary-action' onClick={clearSearch}>
                    <Text className='results-summary-action-text'>清空搜索</Text>
                  </View>
                ) : null}
                {activeFilters.length > 0 ? (
                  <View className='results-summary-action secondary' onClick={resetFilters}>
                    <Text className='results-summary-action-text secondary'>清空筛选</Text>
                  </View>
                ) : null}
              </View>
            ) : null}
          </View>
        </View>
      ) : null}

      {loading ? (
        <View className='state-container'>
          <Text className='hint'>正在加载...</Text>
        </View>
      ) : null}

      {error ? (
        <View className='state-container'>
          <Text className='hint error'>{error}</Text>
          <Button className='retry-btn' onClick={() => showSearchResults ? handleSearch() : loadCourses(currentCollege, currentMonth)}>重试</Button>
        </View>
      ) : null}

      {!showSearchResults && !loading && !error && (
        <>
          <View className='month-list'>
            {months.map((month) => (
              <View
                className={`month-item ${currentMonth === month ? 'active' : ''}`}
                key={month}
                onClick={() => void loadCourses(currentCollege, month)}
                hoverClass='month-item-hover'
              >
                <Text>{month} 月</Text>
              </View>
            ))}
          </View>

          {filteredCourseList.length > 0 ? (
            <View className='section'>
              <Text className='section-title'>课程列表</Text>
              {filteredCourseList.map((course) => (
              <View className='card' key={course.id} hoverClass='card-hover'>
                  {course.coverImage ? <Image className='course-cover' src={course.coverImage} mode='aspectFill' /> : null}
                  <View className='card-top-row'>
                    <View className='card-status-group'>
                      {getSignedCourseStatus(course.id) === 'confirmed' ? <Text className='signed-tag'>已报名</Text> : null}
                      {getSignedCourseStatus(course.id) === 'waitlisted' ? <Text className='signed-tag waitlisted'>候补中{course.waitlistPosition ? ` · 第 ${course.waitlistPosition} 位` : ''}</Text> : null}
                      <Text className={`status-tag status-${course.status === 'available' ? 'active' : 'default'}`}>
                        {getCourseStatusLabel(course.status)}
                      </Text>
                    </View>
                    <Text className={`capacity-pill ${course.isFull ? 'full' : ''}`}>{getCapacityLabel(course)}</Text>
                  </View>
                  <Text className='card-title'>{course.name}</Text>
                  <View className='course-overview-row'>
                    <Text className='meta headline'>{course.time}</Text>
                    <Text className='meta headline'>{course.signupCount || 0} 人已报名</Text>
                  </View>
                  <View className='meta-grid'>
                    <View className='meta-section'>
                      {course.teacher ? <Text className='meta meta-chip'>讲师：{course.teacher}</Text> : null}
                      {course.location ? <Text className='meta meta-chip'>地点：{course.location}</Text> : null}
                    </View>
                    <View className='meta-section'>
                      {course.difficulty ? <Text className='meta meta-chip difficulty'>难度：{course.difficulty}</Text> : null}
                      {course.audience ? <Text className='meta meta-chip audience'>适合人群：{course.audience}</Text> : null}
                    </View>
                    <View className='meta-section'>
                      {course.duration ? <Text className='meta meta-chip duration'>时长：{course.duration}</Text> : null}
                      {course.fee ? <Text className='meta meta-chip fee'>费用：{course.fee}</Text> : null}
                    </View>
                  </View>
                  <View className='course-details-wrapper'>
                    {(course.notice || course.materials) ? (
                      <View className='detail-section'>
                        <Text className='detail-section-title'>报名提醒</Text>
                        <View className='detail-blocks'>
                          {course.notice ? <Text className='meta detail-block notice'>报名须知：{course.notice}</Text> : null}
                          {course.materials ? <Text className='meta detail-block materials'>需自备：{course.materials}</Text> : null}
                        </View>
                      </View>
                    ) : null}
                    {course.tags?.length ? (
                      <View className='detail-section'>
                        <Text className='detail-section-title'>课程标签</Text>
                        <View className='tag-list'>
                          {course.tags.map((tag) => (
                            <Text className='tag-chip' key={`${course.id}-${tag}`} onClick={() => void handleQuickTagClick(tag)}>{tag}</Text>
                          ))}
                        </View>
                      </View>
                    ) : null}
                    {course.description ? (
                      <View className='detail-section'>
                        <Text className='detail-section-title'>课程简介</Text>
                        <Text className='meta description'>{course.description}</Text>
                      </View>
                    ) : null}
                  </View>
                  <View className='card-action-area'>
                    {getSignupWindowLabel(course) ? <Text className='meta signup-window'>{getSignupWindowLabel(course)}</Text> : null}
                    <Button
                      className={`signup-btn ${getSignedCourseStatus(course.id) ? 'danger' : ''}`}
                      loading={signingCourseId === String(course.id)}
                      disabled={!getSignedCourseStatus(course.id) && !isCourseOpenForSignup(course)}
                      onClick={() => void (getSignedCourseStatus(course.id) ? handleCancelSignup(course.id) : handleSignup(course.id))}
                    >
                      {getActionLabel(course)}
                    </Button>
                    <Text className='action-hint'>{getActionHint(course)}</Text>
                  </View>
                </View>
              ))}
            </View>
          ) : (
            <View className='state-container'>
              <Text className='hint'>该学院本月暂无课程</Text>
               {(filterAvailable || filterSigned || selectedDifficulty || selectedAudience || selectedFee || selectedStatus || selectedEnrollment) ? (
                 <>
                   <Text className='hint'>当前筛选条件下暂无课程</Text>
                   <Text className='hint subtle'>已定位到：{emptyStateSummary} · 已启用 {activeFilters.length} 个筛选</Text>
                     <Button
                       className='retry-btn secondary small-btn'
                       onClick={resetFilters}
                     >
                      重置筛选
                   </Button>
                </>
              ) : null}
              <Button className='retry-btn secondary' onClick={() => loadCourses(currentCollege, currentMonth)}>刷新</Button>
            </View>
          )}
        </>
      )}

      {showSearchResults && !loading && !error && (
        <View className='section'>
          <Text className='section-title'>搜索结果：{activeSearchKeyword} ({filteredSearchResults.length})</Text>
          {filteredSearchResults.length > 0 ? (
            filteredSearchResults.map((course) => (
              <View className='card search-result-card' key={`${course.id}-${course.month}`} hoverClass='card-hover'>
                {course.coverImage ? <Image className='course-cover' src={course.coverImage} mode='aspectFill' /> : null}
                <View className='card-top-row'>
                  <View className='card-status-group'>
                    {getSignedCourseStatus(course.id) === 'confirmed' ? <Text className='signed-tag'>已报名</Text> : null}
                    {getSignedCourseStatus(course.id) === 'waitlisted' ? <Text className='signed-tag waitlisted'>候补中{course.waitlistPosition ? ` · 第 ${course.waitlistPosition} 位` : ''}</Text> : null}
                    <Text className={`status-tag status-${course.status === 'available' ? 'active' : 'default'}`}>
                      {getCourseStatusLabel(course.status)}
                    </Text>
                  </View>
                  <Text className={`capacity-pill ${course.isFull ? 'full' : ''}`}>{getCapacityLabel(course)}</Text>
                </View>
                <Text className='card-title'>{course.name}</Text>
                <View className='search-result-context-row'>
                  <Text className='search-result-context-chip'>{course.college}</Text>
                  <Text className='search-result-context-chip subtle'>{course.month} 月</Text>
                </View>
                <View className='course-overview-row search'>
                  <Text className='meta headline'>{course.time}</Text>
                  <Text className='meta headline'>{course.signupCount || 0} 人已报名</Text>
                </View>
                <View className='meta-grid search-compact'>
                  <View className='meta-section'>
                    {course.teacher ? <Text className='meta meta-chip'>讲师：{course.teacher}</Text> : null}
                    {course.location ? <Text className='meta meta-chip'>地点：{course.location}</Text> : null}
                  </View>
                  <View className='meta-section'>
                    {course.difficulty ? <Text className='meta meta-chip difficulty'>难度：{course.difficulty}</Text> : null}
                    {course.audience ? <Text className='meta meta-chip audience'>适合人群：{course.audience}</Text> : null}
                  </View>
                  <View className='meta-section'>
                    {course.duration ? <Text className='meta meta-chip duration'>时长：{course.duration}</Text> : null}
                    {course.fee ? <Text className='meta meta-chip fee'>费用：{course.fee}</Text> : null}
                  </View>
                </View>
                <View className='course-details-wrapper'>
                  {(course.notice || course.materials) ? (
                    <View className='detail-section'>
                      <Text className='detail-section-title'>报名提醒</Text>
                      <View className='detail-blocks'>
                        {course.notice ? <Text className='meta detail-block notice'>报名须知：{course.notice}</Text> : null}
                        {course.materials ? <Text className='meta detail-block materials'>需自备：{course.materials}</Text> : null}
                      </View>
                    </View>
                  ) : null}
                    {course.tags?.length ? (
                      <View className='detail-section'>
                        <Text className='detail-section-title'>课程标签</Text>
                        <View className='tag-list compact'>
                          {course.tags.slice(0, 4).map((tag) => (
                            <Text className='tag-chip' key={`${course.id}-${tag}`} onClick={() => void handleQuickTagClick(tag)}>{tag}</Text>
                          ))}
                          {course.tags.length > 4 ? <Text className='tag-chip more'>+{course.tags.length - 4}</Text> : null}
                        </View>
                      </View>
                    ) : null}
                  {course.description ? (
                    <View className='detail-section'>
                      <Text className='detail-section-title'>课程简介</Text>
                      <Text className='meta description'>{course.description}</Text>
                    </View>
                  ) : null}
                </View>
                <View className='card-action-area'>
                  {getSignupWindowLabel(course) ? <Text className='meta signup-window'>{getSignupWindowLabel(course)}</Text> : null}
                  <Button
                    className={`signup-btn ${getSignedCourseStatus(course.id) ? 'danger' : ''}`}
                    loading={signingCourseId === String(course.id)}
                    disabled={!getSignedCourseStatus(course.id) && !isCourseOpenForSignup(course)}
                    onClick={() => void (getSignedCourseStatus(course.id) ? handleCancelSignup(course.id) : handleSignup(course.id))}
                  >
                    {getActionLabel(course)}
                  </Button>
                  <Text className='action-hint'>{getActionHint(course)}</Text>
                </View>
              </View>
            ))
            ) : (
              <View className='state-container'>
                <Text className='hint'>未找到相关课程</Text>
                <Text className='hint subtle'>当前搜索：{activeSearchKeyword || '未命名关键词'} · 已启用 {activeFilters.length} 个筛选</Text>
                {(filterAvailable || filterSigned || selectedDifficulty || selectedAudience || selectedFee || selectedStatus || selectedEnrollment) ? <Text className='hint'>可以尝试取消部分筛选条件</Text> : null}
                <Button
                  className='retry-btn secondary small-btn'
                  onClick={clearSearch}
                >
                  清空搜索
                </Button>
              </View>
           )}
        </View>
      )}
    </View>
  );
}
