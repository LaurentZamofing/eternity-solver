import React, { useState } from 'react';

interface BoardVisualizerProps {
  boardState?: string[][];
  rows: number;
  cols: number;
  depth: number;
}

/**
 * Visual representation of the puzzle board state
 * Shows pieces with their IDs and rotations
 */
export const BoardVisualizer: React.FC<BoardVisualizerProps> = ({
  boardState,
  rows,
  cols,
  depth,
}) => {
  const [zoom, setZoom] = useState<'small' | 'medium' | 'large'>('medium');

  if (!boardState || boardState.length === 0) {
    return (
      <div className="bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-8 text-center">
        <p className="text-gray-600 dark:text-gray-400">Board visualization not available</p>
        <p className="text-sm text-gray-500 dark:text-gray-500 mt-2">
          The save file may not contain board state data
        </p>
      </div>
    );
  }

  // Determine cell size based on zoom level
  const cellSizeClasses = {
    small: 'w-6 h-6 text-[6px]',
    medium: 'w-10 h-10 text-[8px]',
    large: 'w-14 h-14 text-xs',
  };

  const cellSize = cellSizeClasses[zoom];

  // Parse cell data to get piece ID and rotation
  const parseCell = (cell: string | null): { pieceId: number; rotation: number } | null => {
    if (!cell) return null;
    const parts = cell.split('_');
    if (parts.length === 2) {
      return {
        pieceId: parseInt(parts[0]),
        rotation: parseInt(parts[1]),
      };
    }
    return null;
  };

  // Generate color for piece (consistent hashing based on ID)
  const getPieceColor = (pieceId: number): string => {
    const hue = (pieceId * 137.508) % 360; // Golden angle for good distribution
    return `hsl(${hue}, 70%, 60%)`;
  };

  // Get rotation arrow based on rotation value
  const getRotationArrow = (rotation: number): string => {
    const arrows = ['↑', '→', '↓', '←'];
    return arrows[rotation % 4] || '';
  };

  // Count filled cells
  const filledCells = boardState.flat().filter(cell => cell !== null).length;

  return (
    <div className="space-y-4">
      {/* Header with controls */}
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
            Board Visualization
          </h3>
          <p className="text-sm text-gray-600 dark:text-gray-400">
            {filledCells} / {rows * cols} cells filled ({depth} pieces placed)
          </p>
        </div>

        {/* Zoom controls */}
        <div className="flex items-center space-x-2">
          <label className="text-sm text-gray-600 dark:text-gray-400">Zoom:</label>
          <button
            onClick={() => setZoom('small')}
            className={`px-3 py-1 text-sm rounded ${
              zoom === 'small'
                ? 'bg-primary-600 text-white'
                : 'bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300'
            }`}
          >
            Small
          </button>
          <button
            onClick={() => setZoom('medium')}
            className={`px-3 py-1 text-sm rounded ${
              zoom === 'medium'
                ? 'bg-primary-600 text-white'
                : 'bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300'
            }`}
          >
            Medium
          </button>
          <button
            onClick={() => setZoom('large')}
            className={`px-3 py-1 text-sm rounded ${
              zoom === 'large'
                ? 'bg-primary-600 text-white'
                : 'bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300'
            }`}
          >
            Large
          </button>
        </div>
      </div>

      {/* Board */}
      <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-4 overflow-auto">
        <div className="inline-block">
          <div className="grid gap-0.5" style={{ gridTemplateColumns: `repeat(${cols}, minmax(0, 1fr))` }}>
            {boardState.map((row, rowIndex) =>
              row.map((cell, colIndex) => {
                const parsedCell = parseCell(cell);
                const isEmpty = !parsedCell;

                if (isEmpty) {
                  // Empty cell
                  return (
                    <div
                      key={`${rowIndex}-${colIndex}`}
                      className={`${cellSize} border border-gray-300 dark:border-gray-600 bg-gray-100 dark:bg-gray-700 flex items-center justify-center`}
                      title={`Empty (${rowIndex}, ${colIndex})`}
                    >
                      <span className="text-gray-400 text-xs">·</span>
                    </div>
                  );
                }

                // Filled cell with piece
                const { pieceId, rotation } = parsedCell;
                const color = getPieceColor(pieceId);
                const arrow = getRotationArrow(rotation);

                return (
                  <div
                    key={`${rowIndex}-${colIndex}`}
                    className={`${cellSize} border-2 border-gray-800 dark:border-gray-200 flex flex-col items-center justify-center font-mono font-bold cursor-pointer hover:scale-110 transition-transform`}
                    style={{ backgroundColor: color }}
                    title={`Piece ${pieceId} (rotation ${rotation})\nPosition: (${rowIndex}, ${colIndex})`}
                  >
                    <span className="text-gray-900 leading-none">{pieceId}</span>
                    <span className="text-gray-900 text-[10px] leading-none">{arrow}</span>
                  </div>
                );
              })
            )}
          </div>
        </div>
      </div>

      {/* Legend */}
      <div className="bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-4">
        <h4 className="text-sm font-semibold text-gray-900 dark:text-white mb-2">Legend:</h4>
        <div className="grid grid-cols-2 gap-2 text-sm">
          <div className="flex items-center space-x-2">
            <div className="w-4 h-4 bg-gray-100 dark:bg-gray-700 border border-gray-300 dark:border-gray-600"></div>
            <span className="text-gray-700 dark:text-gray-300">Empty cell</span>
          </div>
          <div className="flex items-center space-x-2">
            <div
              className="w-4 h-4 border-2 border-gray-800"
              style={{ backgroundColor: getPieceColor(42) }}
            ></div>
            <span className="text-gray-700 dark:text-gray-300">Placed piece (colored by ID)</span>
          </div>
          <div className="flex items-center space-x-2">
            <span className="text-gray-700 dark:text-gray-300 font-mono">123</span>
            <span className="text-gray-700 dark:text-gray-300">Piece ID</span>
          </div>
          <div className="flex items-center space-x-2">
            <span className="text-gray-700 dark:text-gray-300">↑ → ↓ ←</span>
            <span className="text-gray-700 dark:text-gray-300">Rotation (0-3)</span>
          </div>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-3 gap-4">
        <div className="bg-blue-50 dark:bg-blue-900/20 rounded-lg p-3 border border-blue-200 dark:border-blue-800">
          <div className="text-xs text-blue-600 dark:text-blue-400 font-medium">Dimensions</div>
          <div className="text-lg font-bold text-blue-900 dark:text-blue-100 mt-1">
            {rows} &times; {cols}
          </div>
        </div>
        <div className="bg-green-50 dark:bg-green-900/20 rounded-lg p-3 border border-green-200 dark:border-green-800">
          <div className="text-xs text-green-600 dark:text-green-400 font-medium">Filled</div>
          <div className="text-lg font-bold text-green-900 dark:text-green-100 mt-1">
            {filledCells} cells
          </div>
        </div>
        <div className="bg-purple-50 dark:bg-purple-900/20 rounded-lg p-3 border border-purple-200 dark:border-purple-800">
          <div className="text-xs text-purple-600 dark:text-purple-400 font-medium">Coverage</div>
          <div className="text-lg font-bold text-purple-900 dark:text-purple-100 mt-1">
            {((filledCells / (rows * cols)) * 100).toFixed(1)}%
          </div>
        </div>
      </div>
    </div>
  );
};
