import type { GlobalStats } from '../types/metrics';

interface GlobalStatsProps {
  stats: GlobalStats;
}

function GlobalStatsComponent({ stats }: GlobalStatsProps) {
  const statCards = [
    {
      label: 'Total Configs',
      value: stats.totalConfigs,
      color: 'blue',
      icon: '📊',
    },
    {
      label: 'Running',
      value: stats.runningConfigs,
      color: 'green',
      icon: '▶️',
    },
    {
      label: 'Solved',
      value: stats.solvedConfigs,
      color: 'purple',
      icon: '✅',
    },
    {
      label: 'Stuck',
      value: stats.stuckConfigs,
      color: 'red',
      icon: '⚠️',
    },
  ];

  const progressCards = [
    {
      label: 'Best Progress',
      value: `${stats.bestProgressPercentage.toFixed(2)}%`,
      subtitle: stats.bestProgressConfigName,
      color: 'indigo',
    },
    {
      label: 'Average Progress',
      value: `${stats.averageProgressPercentage.toFixed(2)}%`,
      subtitle: 'Across all configs',
      color: 'blue',
    },
    {
      label: 'Max Depth',
      value: stats.maxDepth,
      subtitle: stats.maxDepthConfigName,
      color: 'green',
    },
    {
      label: 'Total Time',
      value: stats.totalComputeTimeFormatted,
      subtitle: 'Cumulative compute time',
      color: 'orange',
    },
  ];

  return (
    <div className="space-y-6">
      {/* Status Cards */}
      <div>
        <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">
          System Status
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {statCards.map((card) => (
            <div
              key={card.label}
              className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6 border-l-4"
              style={{
                borderLeftColor:
                  card.color === 'blue'
                    ? '#3b82f6'
                    : card.color === 'green'
                    ? '#10b981'
                    : card.color === 'purple'
                    ? '#8b5cf6'
                    : '#ef4444',
              }}
            >
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-gray-600 dark:text-gray-400">{card.label}</p>
                  <p className="text-3xl font-bold text-gray-900 dark:text-white mt-1">
                    {card.value}
                  </p>
                </div>
                <div className="text-4xl">{card.icon}</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Progress Cards */}
      <div>
        <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">
          Performance Metrics
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {progressCards.map((card) => (
            <div
              key={card.label}
              className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6"
            >
              <p className="text-sm text-gray-600 dark:text-gray-400">{card.label}</p>
              <p className="text-2xl font-bold text-gray-900 dark:text-white mt-2">
                {card.value}
              </p>
              <p className="text-xs text-gray-500 dark:text-gray-500 mt-1 truncate">
                {card.subtitle}
              </p>
            </div>
          ))}
        </div>
      </div>

      {/* Top Performers */}
      {stats.topConfigs && stats.topConfigs.length > 0 && (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
            🏆 Top 5 Performers
          </h3>
          <div className="space-y-2">
            {stats.topConfigs.slice(0, 5).map((config, index) => (
              <div
                key={config.configName}
                className="flex items-center justify-between py-2 px-3 bg-gray-50 dark:bg-gray-700 rounded"
              >
                <div className="flex items-center space-x-3">
                  <span className="text-lg font-bold text-gray-500 dark:text-gray-400">
                    #{index + 1}
                  </span>
                  <span className="text-sm font-medium text-gray-900 dark:text-white truncate">
                    {config.configName}
                  </span>
                </div>
                <div className="flex items-center space-x-4">
                  <span className="text-sm text-gray-600 dark:text-gray-400">
                    Depth: {config.depth}
                  </span>
                  <span className="text-sm font-semibold text-primary-600 dark:text-primary-400">
                    {config.progressPercentage.toFixed(2)}%
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

export default GlobalStatsComponent;
