import { useEffect, useState } from 'react';
import { apiService } from './services/api';
import { websocketService } from './services/websocket';
import type { ConfigMetrics, GlobalStats, SortField, SortOrder, StatusFilter } from './types/metrics';
import GlobalStatsComponent from './components/GlobalStats';
import ConfigTable from './components/ConfigTable';
import ConnectionStatus from './components/ConnectionStatus';
import { MetricsLegend } from './components/MetricsLegend';

function App() {
  const [configs, setConfigs] = useState<ConfigMetrics[]>([]);
  const [globalStats, setGlobalStats] = useState<GlobalStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [connected, setConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  // Filter and sort state
  const [sortField, setSortField] = useState<SortField>('progress');
  const [sortOrder, setSortOrder] = useState<SortOrder>('desc');
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('all');

  // Load initial data
  useEffect(() => {
    loadInitialData();
  }, []);

  // Connect to WebSocket
  useEffect(() => {
    console.log('Connecting to WebSocket...');
    websocketService.connect();

    const unsubscribeConnect = websocketService.onConnect(() => {
      console.log('WebSocket connected');
      setConnected(true);
      setError(null);
    });

    const unsubscribeDisconnect = websocketService.onDisconnect(() => {
      console.log('WebSocket disconnected');
      setConnected(false);
    });

    const unsubscribeError = websocketService.onError((err) => {
      console.error('WebSocket error:', err);
      setError('WebSocket connection error');
    });

    const unsubscribeMetrics = websocketService.onMetricsUpdate((metrics) => {
      console.log('Received metrics update:', metrics.configName);
      updateConfigMetrics(metrics);
    });

    return () => {
      unsubscribeConnect();
      unsubscribeDisconnect();
      unsubscribeError();
      unsubscribeMetrics();
      websocketService.disconnect();
    };
  }, []);

  // Reload data when sort/filter changes
  useEffect(() => {
    loadConfigs();
  }, [sortField, sortOrder, statusFilter]);

  // Reload global stats periodically
  useEffect(() => {
    const interval = setInterval(() => {
      loadGlobalStats();
    }, 10000); // Every 10 seconds

    return () => clearInterval(interval);
  }, []);

  const loadInitialData = async () => {
    setLoading(true);
    try {
      await Promise.all([loadConfigs(), loadGlobalStats()]);
    } catch (err) {
      console.error('Failed to load initial data:', err);
      setError('Failed to load initial data');
    } finally {
      setLoading(false);
    }
  };

  const loadConfigs = async () => {
    try {
      const data = await apiService.getAllConfigs(sortField, sortOrder, statusFilter);
      setConfigs(data);
    } catch (err) {
      console.error('Failed to load configs:', err);
      setError('Failed to load configurations');
    }
  };

  const loadGlobalStats = async () => {
    try {
      const stats = await apiService.getGlobalStats();
      setGlobalStats(stats);
    } catch (err) {
      console.error('Failed to load global stats:', err);
    }
  };

  const updateConfigMetrics = (updatedMetrics: ConfigMetrics) => {
    setConfigs((prevConfigs) => {
      const index = prevConfigs.findIndex((c) => c.configName === updatedMetrics.configName);
      if (index >= 0) {
        const newConfigs = [...prevConfigs];
        newConfigs[index] = updatedMetrics;
        return newConfigs;
      } else {
        return [...prevConfigs, updatedMetrics];
      }
    });

    // Reload global stats when metrics update
    loadGlobalStats();
  };

  const handleRefresh = async () => {
    setRefreshing(true);
    setError(null);
    try {
      const result = await apiService.refreshCache();
      console.log('Cache refreshed:', result);
      // Reload all data after refresh
      await Promise.all([loadConfigs(), loadGlobalStats()]);
    } catch (err) {
      console.error('Failed to refresh cache:', err);
      setError('Failed to refresh cache');
    } finally {
      setRefreshing(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-primary-600 mx-auto mb-4"></div>
          <p className="text-gray-600 dark:text-gray-400">Loading monitoring dashboard...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      {/* Header */}
      <header className="bg-white dark:bg-gray-800 shadow-md">
        <div className="max-w-full mx-auto px-4 py-4">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
                Eternity Solver Monitor
              </h1>
              <p className="text-gray-600 dark:text-gray-400 mt-1">
                Real-time monitoring of {configs.length} configurations
              </p>
            </div>
            <div className="flex items-center gap-4">
              <button
                onClick={handleRefresh}
                disabled={refreshing}
                className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                  refreshing
                    ? 'bg-gray-400 cursor-not-allowed'
                    : 'bg-blue-600 hover:bg-blue-700 text-white'
                }`}
                title="Force rescan of save files"
              >
                <div className="flex items-center gap-2">
                  <svg
                    className={`w-5 h-5 ${refreshing ? 'animate-spin' : ''}`}
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
                    />
                  </svg>
                  <span>{refreshing ? 'Refreshing...' : 'Refresh'}</span>
                </div>
              </button>
              <ConnectionStatus connected={connected} />
            </div>
          </div>
        </div>
      </header>

      {/* Error Banner */}
      {error && (
        <div className="bg-red-50 dark:bg-red-900/20 border-l-4 border-red-500 p-4 mx-4 mt-4">
          <div className="flex">
            <div className="flex-shrink-0">
              <svg
                className="h-5 w-5 text-red-400"
                viewBox="0 0 20 20"
                fill="currentColor"
              >
                <path
                  fillRule="evenodd"
                  d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                  clipRule="evenodd"
                />
              </svg>
            </div>
            <div className="ml-3">
              <p className="text-sm text-red-700 dark:text-red-200">{error}</p>
            </div>
          </div>
        </div>
      )}

      {/* Main Content */}
      <main className="max-w-full mx-auto px-4 py-8">
        {/* Global Stats */}
        {globalStats && (
          <div className="mb-8">
            <GlobalStatsComponent stats={globalStats} />
          </div>
        )}

        {/* Metrics Legend */}
        <MetricsLegend />

        {/* Config Table */}
        <div className="card">
          <ConfigTable
            configs={configs}
            sortField={sortField}
            sortOrder={sortOrder}
            statusFilter={statusFilter}
            onSortChange={(field, order) => {
              setSortField(field);
              setSortOrder(order);
            }}
            onFilterChange={(filter) => setStatusFilter(filter)}
          />
        </div>
      </main>

      {/* Footer */}
      <footer className="bg-white dark:bg-gray-800 shadow-md mt-12">
        <div className="max-w-full mx-auto px-4 py-4">
          <p className="text-center text-gray-600 dark:text-gray-400 text-sm">
            Eternity Solver Monitoring Dashboard - Last update:{' '}
            {new Date().toLocaleTimeString()}
          </p>
        </div>
      </footer>
    </div>
  );
}

export default App;
