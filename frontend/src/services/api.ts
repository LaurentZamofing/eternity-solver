import axios from 'axios';
import type {
  ConfigMetrics,
  GlobalStats,
  HistoricalMetrics,
  SortField,
  SortOrder,
  StatusFilter,
} from '../types/metrics';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * API Service for communicating with the backend
 */
export const apiService = {
  /**
   * Get all configurations with optional sorting and filtering
   */
  getAllConfigs: async (
    sort: SortField = 'progress',
    order: SortOrder = 'desc',
    status?: StatusFilter
  ): Promise<ConfigMetrics[]> => {
    const params: Record<string, string> = {
      sort,
      order,
    };
    if (status && status !== 'all') {
      params.status = status;
    }

    const response = await api.get<ConfigMetrics[]>('/configs', { params });
    return response.data;
  },

  /**
   * Get specific configuration by name
   */
  getConfig: async (configName: string): Promise<ConfigMetrics> => {
    const response = await api.get<ConfigMetrics>(`/configs/${configName}`);
    return response.data;
  },

  /**
   * Get historical data for a configuration
   */
  getConfigHistory: async (
    configName: string,
    hours: number = 24,
    limit: number = 1000
  ): Promise<HistoricalMetrics[]> => {
    const response = await api.get<HistoricalMetrics[]>(`/configs/${configName}/history`, {
      params: { hours, limit },
    });
    return response.data;
  },

  /**
   * Get global statistics
   */
  getGlobalStats: async (): Promise<GlobalStats> => {
    const response = await api.get<GlobalStats>('/stats/global');
    return response.data;
  },

  /**
   * Get recent activity
   */
  getRecentActivity: async (hours: number = 1): Promise<HistoricalMetrics[]> => {
    const response = await api.get<HistoricalMetrics[]>('/stats/recent', {
      params: { hours },
    });
    return response.data;
  },

  /**
   * Health check
   */
  healthCheck: async (): Promise<{
    status: string;
    timestamp: string;
    configsMonitored: number;
    databaseRecords: number;
  }> => {
    const response = await api.get('/health');
    return response.data;
  },

  /**
   * Get configs summary (lightweight)
   */
  getConfigsSummary: async (): Promise<{
    totalConfigs: number;
    configs: Array<{
      name: string;
      depth: number;
      progress: number;
      status: string;
    }>;
  }> => {
    const response = await api.get('/configs/summary');
    return response.data;
  },

  /**
   * Refresh cache - force rescan of all save files
   */
  refreshCache: async (): Promise<{
    success: boolean;
    message: string;
    configsFound: number;
    timestamp: string;
  }> => {
    const response = await api.post('/refresh');
    return response.data;
  },
};

export default apiService;
