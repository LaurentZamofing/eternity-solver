import React, { useEffect, useState } from 'react';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  TimeScale,
} from 'chart.js';
import { Line } from 'react-chartjs-2';
import 'chartjs-adapter-date-fns';
import { apiService } from '../services/api';
import type { HistoricalMetrics } from '../types/metrics';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  TimeScale
);

interface HistoricalChartProps {
  configName: string;
  hours?: number;
}

/**
 * Historical progress chart showing depth and progress over time
 */
export const HistoricalChart: React.FC<HistoricalChartProps> = ({
  configName,
  hours = 24
}) => {
  const [history, setHistory] = useState<HistoricalMetrics[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [timeRange, setTimeRange] = useState(hours);

  useEffect(() => {
    loadHistory();
  }, [configName, timeRange]);

  const loadHistory = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await apiService.getConfigHistory(configName, timeRange);
      setHistory(data);
    } catch (err) {
      console.error('Failed to load history:', err);
      setError('Failed to load historical data');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600 mx-auto mb-4"></div>
          <p className="text-gray-600 dark:text-gray-400">Loading history...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
        <p className="text-red-700 dark:text-red-200">{error}</p>
      </div>
    );
  }

  if (history.length === 0) {
    return (
      <div className="bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-8 text-center">
        <p className="text-gray-600 dark:text-gray-400">No historical data available yet.</p>
        <p className="text-sm text-gray-500 dark:text-gray-500 mt-2">
          Data will appear as the solver makes progress.
        </p>
      </div>
    );
  }

  // Prepare data for Chart.js
  const timestamps = history.map(h => new Date(h.timestamp));
  const depths = history.map(h => h.depth);
  const progressPercentages = history.map(h => h.progressPercentage);
  const speeds = history.map(h => h.piecesPerSecond);

  const chartData = {
    labels: timestamps,
    datasets: [
      {
        label: 'Depth (pieces)',
        data: depths,
        borderColor: 'rgb(59, 130, 246)',
        backgroundColor: 'rgba(59, 130, 246, 0.1)',
        yAxisID: 'y',
        tension: 0.3,
      },
      {
        label: 'Progress (%)',
        data: progressPercentages,
        borderColor: 'rgb(16, 185, 129)',
        backgroundColor: 'rgba(16, 185, 129, 0.1)',
        yAxisID: 'y1',
        tension: 0.3,
      },
      {
        label: 'Speed (pieces/sec)',
        data: speeds,
        borderColor: 'rgb(251, 146, 60)',
        backgroundColor: 'rgba(251, 146, 60, 0.1)',
        yAxisID: 'y2',
        tension: 0.3,
        hidden: true, // Hidden by default
      },
    ],
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: {
      mode: 'index' as const,
      intersect: false,
    },
    plugins: {
      legend: {
        position: 'top' as const,
        labels: {
          color: 'rgb(156, 163, 175)',
        },
      },
      title: {
        display: true,
        text: `Historical Progress - ${configName}`,
        color: 'rgb(156, 163, 175)',
      },
      tooltip: {
        callbacks: {
          label: function(context: any) {
            let label = context.dataset.label || '';
            if (label) {
              label += ': ';
            }
            if (context.parsed.y !== null) {
              if (context.dataset.label === 'Progress (%)') {
                label += context.parsed.y.toFixed(2) + '%';
              } else if (context.dataset.label === 'Speed (pieces/sec)') {
                label += context.parsed.y.toFixed(2) + ' p/s';
              } else {
                label += context.parsed.y;
              }
            }
            return label;
          }
        }
      }
    },
    scales: {
      x: {
        type: 'time' as const,
        time: {
          unit: timeRange <= 1 ? 'minute' : timeRange <= 6 ? 'hour' : 'day',
          displayFormats: {
            minute: 'HH:mm',
            hour: 'HH:mm',
            day: 'MMM dd',
          },
        },
        title: {
          display: true,
          text: 'Time',
          color: 'rgb(156, 163, 175)',
        },
        ticks: {
          color: 'rgb(156, 163, 175)',
        },
        grid: {
          color: 'rgba(156, 163, 175, 0.1)',
        },
      },
      y: {
        type: 'linear' as const,
        display: true,
        position: 'left' as const,
        title: {
          display: true,
          text: 'Depth (pieces)',
          color: 'rgb(59, 130, 246)',
        },
        ticks: {
          color: 'rgb(59, 130, 246)',
        },
        grid: {
          color: 'rgba(156, 163, 175, 0.1)',
        },
      },
      y1: {
        type: 'linear' as const,
        display: true,
        position: 'right' as const,
        title: {
          display: true,
          text: 'Progress (%)',
          color: 'rgb(16, 185, 129)',
        },
        ticks: {
          color: 'rgb(16, 185, 129)',
        },
        grid: {
          drawOnChartArea: false,
        },
        max: 100,
      },
      y2: {
        type: 'linear' as const,
        display: false,
        position: 'right' as const,
        title: {
          display: true,
          text: 'Speed (p/s)',
          color: 'rgb(251, 146, 60)',
        },
      },
    },
  };

  return (
    <div className="space-y-4">
      {/* Time Range Selector */}
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
          Progress History
        </h3>
        <div className="flex items-center space-x-2">
          <label className="text-sm text-gray-600 dark:text-gray-400">Time Range:</label>
          <select
            value={timeRange}
            onChange={(e) => setTimeRange(Number(e.target.value))}
            className="px-3 py-1 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm"
          >
            <option value={1}>Last hour</option>
            <option value={6}>Last 6 hours</option>
            <option value={24}>Last 24 hours</option>
            <option value={72}>Last 3 days</option>
            <option value={168}>Last week</option>
          </select>
        </div>
      </div>

      {/* Chart */}
      <div className="bg-white dark:bg-gray-800 rounded-lg p-4 border border-gray-200 dark:border-gray-700">
        <div style={{ height: '400px' }}>
          <Line data={chartData} options={options} />
        </div>
      </div>

      {/* Stats Summary */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-blue-50 dark:bg-blue-900/20 rounded-lg p-3 border border-blue-200 dark:border-blue-800">
          <div className="text-xs text-blue-600 dark:text-blue-400 font-medium">Max Depth</div>
          <div className="text-xl font-bold text-blue-900 dark:text-blue-100 mt-1">
            {Math.max(...depths)}
          </div>
        </div>
        <div className="bg-green-50 dark:bg-green-900/20 rounded-lg p-3 border border-green-200 dark:border-green-800">
          <div className="text-xs text-green-600 dark:text-green-400 font-medium">Max Progress</div>
          <div className="text-xl font-bold text-green-900 dark:text-green-100 mt-1">
            {Math.max(...progressPercentages).toFixed(2)}%
          </div>
        </div>
        <div className="bg-orange-50 dark:bg-orange-900/20 rounded-lg p-3 border border-orange-200 dark:border-orange-800">
          <div className="text-xs text-orange-600 dark:text-orange-400 font-medium">Avg Speed</div>
          <div className="text-xl font-bold text-orange-900 dark:text-orange-100 mt-1">
            {(speeds.reduce((a, b) => a + b, 0) / speeds.length).toFixed(2)} p/s
          </div>
        </div>
        <div className="bg-purple-50 dark:bg-purple-900/20 rounded-lg p-3 border border-purple-200 dark:border-purple-800">
          <div className="text-xs text-purple-600 dark:text-purple-400 font-medium">Data Points</div>
          <div className="text-xl font-bold text-purple-900 dark:text-purple-100 mt-1">
            {history.length}
          </div>
        </div>
      </div>
    </div>
  );
};
