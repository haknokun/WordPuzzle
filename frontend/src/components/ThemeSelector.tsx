import { useState, useEffect } from 'react';
import './ThemeSelector.css';

interface ThemeSelectorProps {
  onCategoryChange: (category: string | null) => void;
  onWordTypeChange: (wordType: string | null) => void;
  disabled?: boolean;
}

interface WordType {
  code: string;
  name: string;
  description: string;
}

const API_BASE = 'http://localhost:8080/api';

export default function ThemeSelector({
  onCategoryChange,
  onWordTypeChange,
  disabled = false,
}: ThemeSelectorProps) {
  const [categories, setCategories] = useState<string[]>([]);
  const [wordTypes, setWordTypes] = useState<WordType[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string>('');
  const [selectedWordType, setSelectedWordType] = useState<string>('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [categoriesRes, wordTypesRes] = await Promise.all([
          fetch(`${API_BASE}/categories`),
          fetch(`${API_BASE}/word-types`),
        ]);

        if (categoriesRes.ok) {
          const data = await categoriesRes.json();
          setCategories(data);
        }

        if (wordTypesRes.ok) {
          const data = await wordTypesRes.json();
          setWordTypes(data);
        }
      } catch (error) {
        console.error('Failed to fetch theme data:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  const handleCategoryChange = (value: string) => {
    setSelectedCategory(value);
    setSelectedWordType(''); // 카테고리 선택 시 단어유형 초기화
    onCategoryChange(value || null);
    onWordTypeChange(null);
  };

  const handleWordTypeChange = (value: string) => {
    setSelectedWordType(value);
    setSelectedCategory(''); // 단어유형 선택 시 카테고리 초기화
    onWordTypeChange(value || null);
    onCategoryChange(null);
  };

  const handleClear = () => {
    setSelectedCategory('');
    setSelectedWordType('');
    onCategoryChange(null);
    onWordTypeChange(null);
  };

  if (loading) {
    return <div className="theme-selector loading">테마 로딩 중...</div>;
  }

  return (
    <div className="theme-selector">
      <div className="theme-group">
        <label>
          전문 분야:
          <select
            value={selectedCategory}
            onChange={(e) => handleCategoryChange(e.target.value)}
            disabled={disabled || selectedWordType !== ''}
          >
            <option value="">전체</option>
            {categories.map((cat) => (
              <option key={cat} value={cat}>
                {cat}
              </option>
            ))}
          </select>
        </label>
      </div>

      <div className="theme-group">
        <label>
          단어 유형:
          <select
            value={selectedWordType}
            onChange={(e) => handleWordTypeChange(e.target.value)}
            disabled={disabled || selectedCategory !== ''}
          >
            <option value="">전체</option>
            {wordTypes.map((wt) => (
              <option key={wt.code} value={wt.code} title={wt.description}>
                {wt.name}
              </option>
            ))}
          </select>
        </label>
      </div>

      {(selectedCategory || selectedWordType) && (
        <button
          className="clear-theme"
          onClick={handleClear}
          disabled={disabled}
          type="button"
        >
          테마 해제
        </button>
      )}
    </div>
  );
}
