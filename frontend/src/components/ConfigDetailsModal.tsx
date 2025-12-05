import type { ConfigMetrics } from '../types/metrics';
import { HistoricalChart } from './HistoricalChart';
import { BoardVisualizerEnhanced } from './BoardVisualizerEnhanced';

interface ConfigDetailsModalProps {
  config: ConfigMetrics;
  onClose: () => void;
}

function ConfigDetailsModal({ config, onClose }: ConfigDetailsModalProps) {

  const formatETA = (ms: number) => {
    if (ms <= 0) return 'N/A';

    const seconds = Math.floor(ms / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);

    if (days > 0) {
      return `${days}d ${hours % 24}h`;
    } else if (hours > 0) {
      return `${hours}h ${minutes % 60}m`;
    } else if (minutes > 0) {
      return `${minutes}m`;
    } else {
      return `${seconds}s`;
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-[95vw] w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200 dark:border-gray-700">
          <div className="flex items-center space-x-3">
            <h2 className="text-2xl font-bold text-gray-900 dark:text-white">
              {config.configName}
            </h2>
            {config.status === 'best_record' && (
              <span className="px-3 py-1 text-sm font-semibold rounded-full bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200">
                🏆 Best Record
              </span>
            )}
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-200"
          >
            <svg
              className="w-6 h-6"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        {/* Content */}
        <div className="p-6 space-y-6">
          {/* Metrics Grid */}
          <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
            <div className="bg-gray-50 dark:bg-gray-700 p-4 rounded-lg">
              <p className="text-sm text-gray-600 dark:text-gray-400">Depth</p>
              <p className="text-2xl font-bold text-gray-900 dark:text-white">
                {config.depth} / {config.totalPieces}
              </p>
            </div>
            <div className="bg-blue-50 dark:bg-blue-900/20 p-4 rounded-lg border border-blue-200 dark:border-blue-800">
              <p className="text-sm text-blue-600 dark:text-blue-400">Physical %</p>
              <p className="text-2xl font-bold text-blue-900 dark:text-blue-100">
                {(config.physicalProgressPercentage || 0).toFixed(2)}%
              </p>
            </div>
            <div className="bg-green-50 dark:bg-green-900/20 p-4 rounded-lg border border-green-200 dark:border-green-800">
              <p className="text-sm text-green-600 dark:text-green-400">Search Space</p>
              <p className="text-2xl font-bold text-green-900 dark:text-green-100">
                {config.progressPercentage.toFixed(2)}%
              </p>
            </div>
            <div className="bg-gray-50 dark:bg-gray-700 p-4 rounded-lg">
              <p className="text-sm text-gray-600 dark:text-gray-400">Compute Time</p>
              <p className="text-2xl font-bold text-gray-900 dark:text-white">
                {config.computeTimeFormatted}
              </p>
            </div>
            <div className="bg-gray-50 dark:bg-gray-700 p-4 rounded-lg">
              <p className="text-sm text-gray-600 dark:text-gray-400">Speed</p>
              <p className="text-2xl font-bold text-gray-900 dark:text-white">
                {config.piecesPerSecond > 0
                  ? `${config.piecesPerSecond.toFixed(2)} p/s`
                  : 'N/A'}
              </p>
            </div>
          </div>

          {/* Additional Info */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <p className="text-sm text-gray-600 dark:text-gray-400">Puzzle Dimensions</p>
              <p className="text-lg font-medium text-gray-900 dark:text-white">
                {config.rows} × {config.cols}
              </p>
            </div>
            <div>
              <p className="text-sm text-gray-600 dark:text-gray-400">Estimated Time Remaining</p>
              <p className="text-lg font-medium text-gray-900 dark:text-white">
                {formatETA(config.estimatedTimeRemainingMs)}
              </p>
            </div>
            <div>
              <p className="text-sm text-gray-600 dark:text-gray-400">Status</p>
              <p className="text-lg font-medium text-gray-900 dark:text-white capitalize">
                {config.status}
              </p>
            </div>
            <div>
              <p className="text-sm text-gray-600 dark:text-gray-400">Last Update</p>
              <p className="text-lg font-medium text-gray-900 dark:text-white">
                {config.lastUpdate
                  ? new Date(config.lastUpdate).toLocaleString()
                  : 'N/A'}
              </p>
            </div>
          </div>

          {/* Board Visualization - Enhanced with triangles */}
          <BoardVisualizerEnhanced
            boardState={config.boardState}
            rows={config.rows}
            cols={config.cols}
            depth={config.depth}
            puzzleName={config.configName}
            configName={config.configName}
            positionToSequence={config.positionToSequence}
            numFixedPieces={
              config.boardState && config.positionToSequence
                ? config.boardState.flat().filter((cell) => cell !== null).length -
                  Object.keys(config.positionToSequence).length
                : 0
            }
          />

          {/* Original simple visualization (fallback) */}
          {/* <BoardVisualizer
            boardState={config.boardState}
            rows={config.rows}
            cols={config.cols}
            depth={config.depth}
          /> */}

          {/* Historical Chart */}
          <HistoricalChart configName={config.configName} hours={24} />
        </div>

        {/* Footer */}
        <div className="flex justify-end p-6 border-t border-gray-200 dark:border-gray-700">
          <button onClick={onClose} className="btn-secondary">
            Close
          </button>
        </div>
      </div>
    </div>
  );
}

export default ConfigDetailsModal;
