import { useState } from 'react';
import type { ConfigMetrics, SortField, SortOrder, StatusFilter } from '../types/metrics';
import ConfigDetailsModal from './ConfigDetailsModal';

interface ConfigTableProps {
  configs: ConfigMetrics[];
  sortField: SortField;
  sortOrder: SortOrder;
  statusFilter: StatusFilter;
  onSortChange: (field: SortField, order: SortOrder) => void;
  onFilterChange: (filter: StatusFilter) => void;
}

function ConfigTable({
  configs,
  sortField,
  sortOrder,
  statusFilter,
  onSortChange,
  onFilterChange,
}: ConfigTableProps) {
  const [selectedConfig, setSelectedConfig] = useState<ConfigMetrics | null>(null);
  const [selectedBestConfig, setSelectedBestConfig] = useState<ConfigMetrics | null>(null);

  const handleSort = (field: SortField) => {
    if (sortField === field) {
      // Toggle order
      onSortChange(field, sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      onSortChange(field, 'desc');
    }
  };

  const handleViewBest = async (configName: string) => {
    try {
      const response = await fetch(`http://localhost:8080/api/configs/${configName}/best`);
      if (response.ok) {
        const bestConfig = await response.json();
        setSelectedBestConfig(bestConfig);
      } else {
        console.error('Failed to load best result:', response.statusText);
      }
    } catch (error) {
      console.error('Error loading best result:', error);
    }
  };

  const getStatusBadge = (status: string, solved: boolean) => {
    if (solved) {
      return (
        <span className="px-2 py-1 text-xs font-semibold rounded-full bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200">
          Solved
        </span>
      );
    }

    switch (status) {
      case 'running':
      case 'active':
        return (
          <span className="px-2 py-1 text-xs font-semibold rounded-full bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200">
            Running
          </span>
        );
      case 'stuck':
        return (
          <span className="px-2 py-1 text-xs font-semibold rounded-full bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200">
            Stuck
          </span>
        );
      case 'idle':
        return (
          <span className="px-2 py-1 text-xs font-semibold rounded-full bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-200">
            Idle
          </span>
        );
      default:
        return (
          <span className="px-2 py-1 text-xs font-semibold rounded-full bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-200">
            {status}
          </span>
        );
    }
  };

  const getSortIcon = (field: SortField) => {
    if (sortField !== field) {
      return '↕️';
    }
    return sortOrder === 'asc' ? '↑' : '↓';
  };

  return (
    <div>
      {/* Table Header with Controls */}
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
          Configuration Status ({configs.length})
        </h2>

        {/* Status Filter */}
        <div className="flex items-center space-x-2">
          <label className="text-sm text-gray-600 dark:text-gray-400">Filter:</label>
          <select
            value={statusFilter}
            onChange={(e) => onFilterChange(e.target.value as StatusFilter)}
            className="px-3 py-1 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm"
          >
            <option value="all">All</option>
            <option value="running">Running</option>
            <option value="idle">Idle</option>
            <option value="solved">Solved</option>
            <option value="stuck">Stuck</option>
          </select>
        </div>
      </div>

      {/* Table */}
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
          <thead className="bg-gray-50 dark:bg-gray-700">
            <tr>
              <th
                className="px-3 py-2 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-600"
                onClick={() => handleSort('name')}
              >
                Configuration {getSortIcon('name')}
              </th>
              <th
                className="px-2 py-2 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-600"
                onClick={() => handleSort('depth')}
              >
                Depth {getSortIcon('depth')}
                <div className="text-xs font-normal text-gray-400">Current</div>
              </th>
              <th
                className="px-2 py-2 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-600"
                onClick={() => handleSort('bestDepthEver')}
              >
                Best {getSortIcon('bestDepthEver')}
                <div className="text-xs font-normal text-gray-400">Record</div>
              </th>
              <th className="px-2 py-2 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                Physical %
                <div className="text-xs font-normal text-gray-400">Placed</div>
              </th>
              <th
                className="px-2 py-2 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-600"
                onClick={() => handleSort('progress')}
              >
                Search % {getSortIcon('progress')}
                <div className="text-xs font-normal text-gray-400">Estimated</div>
              </th>
              <th className="px-2 py-2 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                Δ
                <div className="text-xs font-normal text-gray-400">Delta</div>
              </th>
              <th
                className="px-2 py-2 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-600"
                onClick={() => handleSort('time')}
              >
                Time {getSortIcon('time')}
              </th>
              <th className="px-2 py-2 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                Last Save
                <div className="text-xs font-normal text-gray-400">Ago</div>
              </th>
              <th className="px-2 py-2 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                Speed
              </th>
              <th className="px-2 py-2 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                Status
              </th>
              <th className="px-2 py-2 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
            {configs.map((config) => (
              <tr
                key={config.configName}
                className="hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
              >
                <td className="px-3 py-3 whitespace-nowrap text-sm font-medium text-gray-900 dark:text-white">
                  {config.configName}
                </td>
                <td className="px-2 py-3 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">
                  {config.depth} / {config.totalPieces}
                </td>
                <td className="px-2 py-3 whitespace-nowrap">
                  <div className="flex items-center space-x-1">
                    <span className="text-sm font-semibold text-green-600 dark:text-green-400">
                      {config.bestDepthEver || config.depth}
                    </span>
                    {config.bestDepthEver > config.depth && (
                      <span className="text-xs text-gray-500 dark:text-gray-400" title="Solver backtracked from this record">
                        (-{config.bestDepthEver - config.depth})
                      </span>
                    )}
                  </div>
                </td>
                <td className="px-2 py-3 whitespace-nowrap">
                  <div className="flex items-center">
                    <div className="w-16 bg-gray-200 dark:bg-gray-700 rounded-full h-2 mr-1">
                      <div
                        className="bg-blue-600 h-2 rounded-full"
                        style={{ width: `${Math.min(config.physicalProgressPercentage || 0, 100)}%` }}
                      ></div>
                    </div>
                    <span className="text-xs text-gray-900 dark:text-white">
                      {(config.physicalProgressPercentage || 0).toFixed(1)}%
                    </span>
                  </div>
                </td>
                <td className="px-2 py-3 whitespace-nowrap">
                  <div className="flex items-center">
                    <div className="w-16 bg-gray-200 dark:bg-gray-700 rounded-full h-2 mr-1">
                      <div
                        className={`h-2 rounded-full ${
                          config.progressPercentage >= 100 ? 'bg-red-600' : 'bg-green-600'
                        }`}
                        style={{ width: `${Math.min(config.progressPercentage, 100)}%` }}
                      ></div>
                    </div>
                    <span className="text-xs text-gray-900 dark:text-white">
                      {config.progressPercentage.toFixed(1)}%
                    </span>
                  </div>
                </td>
                <td className="px-2 py-3 whitespace-nowrap text-sm">
                  {(() => {
                    const delta = config.progressPercentage - (config.physicalProgressPercentage || 0);
                    const isHigh = Math.abs(delta) > 30;
                    const colorClass = delta > 0
                      ? (isHigh ? 'text-red-600 font-semibold' : 'text-orange-600')
                      : 'text-gray-500 dark:text-gray-400';
                    return (
                      <span className={colorClass} title={`Search space explored ${delta > 0 ? 'more' : 'less'} than pieces placed`}>
                        {delta > 0 ? '+' : ''}{delta.toFixed(1)}%
                        {isHigh && delta > 0 && ' ⚠️'}
                      </span>
                    );
                  })()}
                </td>
                <td className="px-2 py-3 whitespace-nowrap text-xs text-gray-500 dark:text-gray-400">
                  {config.computeTimeFormatted}
                </td>
                <td className="px-2 py-3 whitespace-nowrap text-xs text-gray-500 dark:text-gray-400" title={config.lastSaveDate || 'N/A'}>
                  {config.lastSaveRelative || 'N/A'}
                </td>
                <td className="px-2 py-3 whitespace-nowrap text-xs text-gray-500 dark:text-gray-400">
                  {config.piecesPerSecond > 0
                    ? `${config.piecesPerSecond.toFixed(2)}`
                    : '-'}
                </td>
                <td className="px-2 py-3 whitespace-nowrap">
                  {getStatusBadge(config.status, config.solved)}
                </td>
                <td className="px-2 py-3 whitespace-nowrap text-sm">
                  <div className="flex space-x-2">
                    <button
                      onClick={() => setSelectedConfig(config)}
                      className="text-primary-600 hover:text-primary-900 dark:text-primary-400 dark:hover:text-primary-300"
                    >
                      View Details
                    </button>
                    {config.bestDepthEver > config.depth && (
                      <button
                        onClick={() => handleViewBest(config.configName)}
                        className="text-green-600 hover:text-green-900 dark:text-green-400 dark:hover:text-green-300 font-semibold"
                        title={`View best record (${config.bestDepthEver} pieces)`}
                      >
                        View Best
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Empty State */}
      {configs.length === 0 && (
        <div className="text-center py-12">
          <p className="text-gray-500 dark:text-gray-400">No configurations found.</p>
        </div>
      )}

      {/* Config Details Modal */}
      {selectedConfig && (
        <ConfigDetailsModal
          config={selectedConfig}
          onClose={() => setSelectedConfig(null)}
        />
      )}

      {/* Best Result Modal */}
      {selectedBestConfig && (
        <ConfigDetailsModal
          config={selectedBestConfig}
          onClose={() => setSelectedBestConfig(null)}
        />
      )}
    </div>
  );
}

export default ConfigTable;
