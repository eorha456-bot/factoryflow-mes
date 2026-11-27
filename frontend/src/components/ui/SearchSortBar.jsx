import { IconSearch } from './Icons';

export default function SearchSortBar({
  search,
  onSearchChange,
  searchPlaceholder = '코드 또는 이름 검색 (바코드 스캔 가능)',
  sortValue,
  onSortChange,
  sortOptions = [],
  children,
}) {
  return (
    <div className="filter-bar">
      <div className="search-input-wrap">
        <span className="search-icon">
          <IconSearch size={15} />
        </span>
        <input
          type="text"
          className="search-input"
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
          placeholder={searchPlaceholder}
        />
        {search && (
          <button
            type="button"
            className="search-clear"
            onClick={() => onSearchChange('')}
            aria-label="검색어 지우기"
          >
            ×
          </button>
        )}
      </div>

      {children}

      {sortOptions.length > 0 && (
        <select
          aria-label="정렬 기준"
          value={sortValue}
          onChange={(e) => onSortChange(e.target.value)}
        >
          {sortOptions.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      )}
    </div>
  );
}
