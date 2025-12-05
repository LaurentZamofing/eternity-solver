import React, { useState } from 'react';

/**
 * Legend component explaining the metrics
 */
export const MetricsLegend: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <div className="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-4 mb-6">
      <div className="flex items-start justify-between">
        <div className="flex items-start space-x-3 flex-1">
          <div className="text-blue-600 dark:text-blue-400 text-xl">ℹ️</div>
          <div className="flex-1">
            <h3 className="text-sm font-semibold text-blue-900 dark:text-blue-100 mb-1">
              Understanding Progress Metrics
            </h3>
            <p className="text-xs text-blue-800 dark:text-blue-200">
              Two types of progress are tracked: physical pieces placed vs. search space explored.
            </p>

            {isOpen && (
              <div className="mt-3 space-y-2 text-xs text-blue-800 dark:text-blue-200">
                <div className="flex items-start space-x-2">
                  <span className="font-semibold min-w-[100px]">Physical %:</span>
                  <span>Actual pieces placed on the board (depth / totalPieces × 100)</span>
                </div>
                <div className="flex items-start space-x-2">
                  <span className="font-semibold min-w-[100px]">Search Space:</span>
                  <span>Solver's estimation of explored search space based on pruning and constraints</span>
                </div>
                <div className="flex items-start space-x-2">
                  <span className="font-semibold min-w-[100px]">Δ Delta:</span>
                  <span>Difference between search space and physical progress</span>
                </div>
                <div className="mt-2 p-2 bg-white dark:bg-gray-800 rounded border border-blue-200 dark:border-blue-700">
                  <p className="font-semibold mb-1">What Delta Tells You:</p>
                  <ul className="space-y-1 ml-4 list-disc">
                    <li><span className="text-red-600 font-semibold">High positive (+45%+)</span>: Search space exhausted, configuration likely impossible</li>
                    <li><span className="text-orange-600">Moderate (+10-45%)</span>: Heavy pruning, progress slowing</li>
                    <li><span className="text-gray-600">Low (±10%)</span>: Healthy progress, search space aligns with placement</li>
                  </ul>
                </div>
              </div>
            )}
          </div>
        </div>
        <button
          onClick={() => setIsOpen(!isOpen)}
          className="text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-200 text-sm font-medium ml-4"
        >
          {isOpen ? 'Hide' : 'Learn More'}
        </button>
      </div>
    </div>
  );
};
