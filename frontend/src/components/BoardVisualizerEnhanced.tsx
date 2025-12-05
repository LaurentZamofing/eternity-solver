import React, { useState, useEffect } from 'react';
import type { PieceDefinition } from '../types/metrics';
import { CellDetailsModal } from './CellDetailsModal';

interface BoardVisualizerEnhancedProps {
  boardState?: string[][];
  rows: number;
  cols: number;
  depth: number;
  puzzleName: string;
  configName: string;
  positionToSequence?: { [key: string]: number }; // Map from "row,col" to sequence number
  numFixedPieces?: number; // Number of fixed pieces (corners + hints)
}

// Color mapping for edge patterns
const getEdgeColor = (pattern: number): string => {
  if (pattern === 0) return '#808080'; // Gray for borders

  const colors = [
    '#FF0000', // 1: Red
    '#00FF00', // 2: Green
    '#0000FF', // 3: Blue
    '#FFFF00', // 4: Yellow
    '#FF00FF', // 5: Magenta
    '#00FFFF', // 6: Cyan
    '#FFA500', // 7: Orange
    '#800080', // 8: Purple
    '#FFC0CB', // 9: Pink
    '#A52A2A', // 10: Brown
    '#FFD700', // 11: Gold
    '#4B0082', // 12: Indigo
    '#FF1493', // 13: Deep Pink
    '#00CED1', // 14: Dark Turquoise
    '#DC143C', // 15: Crimson
    '#FF6347', // 16: Tomato
    '#4682B4', // 17: Steel Blue
    '#32CD32', // 18: Lime Green
    '#FF4500', // 19: Orange Red
    '#DA70D6', // 20: Orchid
    '#8B4513', // 21: Saddle Brown
  ];

  return colors[(pattern - 1) % colors.length] || '#CCCCCC';
};

// Get label for pattern number
const getPatternLabel = (pattern: number): string => {
  if (pattern === 0) return '0';
  // Use letters A-Z for patterns 1-26
  if (pattern <= 26) {
    return String.fromCharCode(64 + pattern); // A=1, B=2, etc.
  }
  return pattern.toString();
};

/**
 * Enhanced board visualizer with diagonal triangles showing edge colors.
 * Each piece is divided into 4 triangles (N, E, S, W) with colors and labels.
 */
export const BoardVisualizerEnhanced: React.FC<BoardVisualizerEnhancedProps> = ({
  boardState,
  rows,
  cols,
  depth,
  puzzleName,
  configName,
  positionToSequence,
  numFixedPieces = 0,
}) => {
  const [zoom, setZoom] = useState<'small' | 'medium' | 'large'>('medium');
  const [pieceDefinitions, setPieceDefinitions] = useState<Record<number, PieceDefinition>>({});
  const [loading, setLoading] = useState(true);
  const [selectedCell, setSelectedCell] = useState<{ row: number; col: number } | null>(null);
  const [deadEndCells, setDeadEndCells] = useState<Set<string>>(new Set()); // Set of "row,col" with 0 valid options
  const [validCountByCell, setValidCountByCell] = useState<Map<string, number>>(new Map()); // Map "row,col" -> valid piece count
  const [loadingDeadEnds, setLoadingDeadEnds] = useState(false);

  // Cell sizes for different zoom levels
  const cellSizes = {
    small: 40,
    medium: 60,
    large: 80,
  };

  const cellSize = cellSizes[zoom];

  // Load piece definitions from backend
  useEffect(() => {
    const loadPieceDefinitions = async () => {
      try {
        setLoading(true);
        const response = await fetch(`http://localhost:8080/api/pieces/${puzzleName}`);
        if (response.ok) {
          const data = await response.json();
          setPieceDefinitions(data);
        } else {
          console.error('Failed to load piece definitions:', response.statusText);
        }
      } catch (error) {
        console.error('Error loading piece definitions:', error);
      } finally {
        setLoading(false);
      }
    };

    if (puzzleName) {
      loadPieceDefinitions();
    }
  }, [puzzleName]);

  // Load dead end cells and valid piece counts (empty cells)
  useEffect(() => {
    const loadCellInfo = async () => {
      if (!boardState || !configName || !positionToSequence) return;

      setLoadingDeadEnds(true);
      const deadEnds = new Set<string>();
      const validCounts = new Map<string, number>();

      try {
        // Find the last placed piece position
        let lastPlacedRow = -1;
        let lastPlacedCol = -1;
        let maxSeq = 0;

        Object.entries(positionToSequence).forEach(([pos, seq]) => {
          if (seq > maxSeq) {
            maxSeq = seq;
            const [r, c] = pos.split(',').map(Number);
            lastPlacedRow = r;
            lastPlacedCol = c;
          }
        });

        // Find all empty cells
        const emptyCells: Array<[number, number]> = [];
        boardState.forEach((row, rowIndex) => {
          row.forEach((cell, colIndex) => {
            if (cell === null) {
              emptyCells.push([rowIndex, colIndex]);
            }
          });
        });

        // Check each empty cell for valid options
        await Promise.all(
          emptyCells.map(async ([row, col]) => {
            try {
              const response = await fetch(
                `http://localhost:8080/api/configs/${configName}/cell/${row}/${col}/details`
              );
              if (response.ok) {
                const data = await response.json();
                // Count valid options
                const validCount = data.possiblePieces?.filter((p: any) => p.valid).length || 0;
                const cellKey = `${row},${col}`;
                validCounts.set(cellKey, validCount);

                // Only mark as dead end if it has 0 options AND is adjacent to the last placed piece
                if (validCount === 0 && lastPlacedRow >= 0) {
                  const isAdjacent =
                    (Math.abs(row - lastPlacedRow) === 1 && col === lastPlacedCol) ||
                    (Math.abs(col - lastPlacedCol) === 1 && row === lastPlacedRow);

                  if (isAdjacent) {
                    deadEnds.add(cellKey);
                  }
                }
              }
            } catch (error) {
              console.debug(`Could not check cell (${row},${col}):`, error);
            }
          })
        );

        setDeadEndCells(deadEnds);
        setValidCountByCell(validCounts);
      } catch (error) {
        console.error('Error loading cell info:', error);
      } finally {
        setLoadingDeadEnds(false);
      }
    };

    loadCellInfo();
  }, [boardState, configName, positionToSequence]);

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

  if (loading) {
    return (
      <div className="bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-8 text-center">
        <p className="text-gray-600 dark:text-gray-400">Loading piece definitions...</p>
      </div>
    );
  }

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

  // Get edge value after rotation
  // Rotation is CLOCKWISE: 0=0°, 1=90°cw, 2=180°, 3=270°cw
  // After clockwise rotation by 270°, what's at North came from East (rotated left)
  // So what's at position i now was originally at position (i - rotation + 4) % 4
  const getRotatedEdge = (piece: PieceDefinition, direction: 'north' | 'east' | 'south' | 'west', rotation: number): number => {
    const edges = [piece.north, piece.east, piece.south, piece.west];
    const directionMap = { north: 0, east: 1, south: 2, west: 3 };
    const currentIndex = directionMap[direction];
    // Find which original edge is now at this position after clockwise rotation
    const originalIndex = (currentIndex - rotation + 4) % 4;
    return edges[originalIndex];
  };

  // Find the maximum sequence number (last piece placed)
  const maxSequenceNum = positionToSequence
    ? Math.max(...Object.values(positionToSequence))
    : 0;

  // Render a single piece as SVG with 4 triangles
  const renderPiece = (cell: string | null, rowIndex: number, colIndex: number) => {
    const parsedCell = parseCell(cell);
    const sequenceNum = positionToSequence?.[`${rowIndex},${colIndex}`];
    const cellKey = `${rowIndex},${colIndex}`;
    const isDeadEnd = deadEndCells.has(cellKey);
    const isFixedPiece = parsedCell && !sequenceNum; // Has piece but no sequence number = fixed
    const isLastPlaced = sequenceNum === maxSequenceNum && maxSequenceNum > 0; // Last piece placed

    if (!parsedCell) {
      // Empty cell - mark red if it's a dead end
      const backgroundColor = isDeadEnd ? '#ff4444' : '#f5f5f5';
      const borderColor = isDeadEnd ? '#cc0000' : 'gray-300';

      return (
        <svg
          key={`${rowIndex}-${colIndex}`}
          width={cellSize}
          height={cellSize}
          className={`border-2 ${isDeadEnd ? 'border-red-600' : 'border-gray-300 dark:border-gray-600'} cursor-pointer hover:opacity-80 transition-opacity`}
          onClick={() => setSelectedCell({ row: rowIndex, col: colIndex })}
        >
          <title>
            {isDeadEnd
              ? 'Dead End (0 valid options) - Click for details'
              : `Empty cell (${validCountByCell.get(cellKey) ?? '?'} valid options) - Click for details`}
          </title>
          <rect width={cellSize} height={cellSize} fill={backgroundColor} />
          {isDeadEnd && (
            <>
              {/* X mark for dead end */}
              <line x1="5" y1="5" x2={cellSize - 5} y2={cellSize - 5} stroke="#fff" strokeWidth="3" />
              <line x1={cellSize - 5} y1="5" x2="5" y2={cellSize - 5} stroke="#fff" strokeWidth="3" />
              <text
                x={cellSize / 2}
                y={cellSize / 2 + cellSize / 3}
                textAnchor="middle"
                dominantBaseline="middle"
                fontSize={Math.max(8, cellSize / 6)}
                fill="#fff"
                fontWeight="bold"
              >
                Dead End
              </text>
            </>
          )}
          {!isDeadEnd && (
            <text
              x={cellSize / 2}
              y={cellSize / 2}
              textAnchor="middle"
              dominantBaseline="middle"
              fontSize={Math.max(10, cellSize / 4)}
              fill="#333"
              fontWeight="bold"
            >
              {validCountByCell.get(cellKey) ?? '?'}
            </text>
          )}
        </svg>
      );
    }

    const { pieceId, rotation } = parsedCell;
    const piece = pieceDefinitions[pieceId];

    if (!piece) {
      // Piece definition not found - show piece ID only
      return (
        <svg
          key={`${rowIndex}-${colIndex}`}
          width={cellSize}
          height={cellSize}
          className="border-2 border-gray-800"
        >
          <rect width={cellSize} height={cellSize} fill="#ddd" />
          <text
            x={cellSize / 2}
            y={cellSize / 2}
            textAnchor="middle"
            dominantBaseline="middle"
            fontSize="10"
            fill="#333"
            fontWeight="bold"
          >
            {pieceId}
          </text>
        </svg>
      );
    }

    // Get rotated edge values
    const northEdge = getRotatedEdge(piece, 'north', rotation);
    const eastEdge = getRotatedEdge(piece, 'east', rotation);
    const southEdge = getRotatedEdge(piece, 'south', rotation);
    const westEdge = getRotatedEdge(piece, 'west', rotation);

    // Get colors
    const northColor = getEdgeColor(northEdge);
    const eastColor = getEdgeColor(eastEdge);
    const southColor = getEdgeColor(southEdge);
    const westColor = getEdgeColor(westEdge);

    // Get labels
    const northLabel = getPatternLabel(northEdge);
    const eastLabel = getPatternLabel(eastEdge);
    const southLabel = getPatternLabel(southEdge);
    const westLabel = getPatternLabel(westEdge);

    const center = cellSize / 2;
    const fontSize = Math.max(8, cellSize / 8);

    return (
      <svg
        key={`${rowIndex}-${colIndex}`}
        width={cellSize}
        height={cellSize}
        className={`cursor-pointer hover:opacity-80 transition-opacity ${
          isLastPlaced
            ? 'border-4 border-green-500'
            : 'border border-gray-800 dark:border-gray-200'
        }`}
        onClick={() => setSelectedCell({ row: rowIndex, col: colIndex })}
      >
        <title>{`Piece ${pieceId} (rotation ${rotation})${
          isLastPlaced ? ' - LAST PLACED' : ''
        }\nN:${northEdge} E:${eastEdge} S:${southEdge} W:${westEdge}\nClick for details`}</title>
        {/* North triangle */}
        <polygon
          points={`${center},${center} 0,0 ${cellSize},0`}
          fill={northColor}
          stroke="#333"
          strokeWidth="0.5"
        />

        {/* East triangle */}
        <polygon
          points={`${center},${center} ${cellSize},0 ${cellSize},${cellSize}`}
          fill={eastColor}
          stroke="#333"
          strokeWidth="0.5"
        />

        {/* South triangle */}
        <polygon
          points={`${center},${center} ${cellSize},${cellSize} 0,${cellSize}`}
          fill={southColor}
          stroke="#333"
          strokeWidth="0.5"
        />

        {/* West triangle */}
        <polygon
          points={`${center},${center} 0,${cellSize} 0,0`}
          fill={westColor}
          stroke="#333"
          strokeWidth="0.5"
        />

        {/* Labels */}
        <text
          x={center}
          y={center * 0.4}
          textAnchor="middle"
          dominantBaseline="middle"
          fontSize={fontSize}
          fill="#fff"
          fontWeight="bold"
          stroke="#000"
          strokeWidth="0.5"
        >
          {northLabel}
        </text>

        <text
          x={center + center * 0.5}
          y={center}
          textAnchor="middle"
          dominantBaseline="middle"
          fontSize={fontSize}
          fill="#fff"
          fontWeight="bold"
          stroke="#000"
          strokeWidth="0.5"
        >
          {eastLabel}
        </text>

        <text
          x={center}
          y={center + center * 0.6}
          textAnchor="middle"
          dominantBaseline="middle"
          fontSize={fontSize}
          fill="#fff"
          fontWeight="bold"
          stroke="#000"
          strokeWidth="0.5"
        >
          {southLabel}
        </text>

        <text
          x={center * 0.5}
          y={center}
          textAnchor="middle"
          dominantBaseline="middle"
          fontSize={fontSize}
          fill="#fff"
          fontWeight="bold"
          stroke="#000"
          strokeWidth="0.5"
        >
          {westLabel}
        </text>

        {/* Center piece ID */}
        <circle cx={center} cy={center} r={center * 0.2} fill="white" opacity="0.9" />
        <text
          x={center}
          y={center}
          textAnchor="middle"
          dominantBaseline="middle"
          fontSize={fontSize * 0.7}
          fill="#000"
          fontWeight="bold"
        >
          {pieceId}
        </text>

        {/* Sequence number badge (top-left corner) for backtracking pieces */}
        {sequenceNum && (
          <>
            <circle cx={cellSize * 0.15} cy={cellSize * 0.15} r={cellSize * 0.12} fill="#4F46E5" opacity="0.9" />
            <text
              x={cellSize * 0.15}
              y={cellSize * 0.15}
              textAnchor="middle"
              dominantBaseline="middle"
              fontSize={Math.max(6, fontSize * 0.6)}
              fill="#fff"
              fontWeight="bold"
            >
              {sequenceNum}
            </text>
          </>
        )}

        {/* Fixed piece badge (top-left corner) - golden star for pre-placed pieces */}
        {isFixedPiece && (
          <>
            <circle cx={cellSize * 0.15} cy={cellSize * 0.15} r={cellSize * 0.14} fill="#FFD700" opacity="0.95" stroke="#B8860B" strokeWidth="1" />
            <text
              x={cellSize * 0.15}
              y={cellSize * 0.15}
              textAnchor="middle"
              dominantBaseline="middle"
              fontSize={Math.max(8, fontSize * 0.8)}
              fill="#8B4513"
              fontWeight="bold"
            >
              ★
            </text>
          </>
        )}
      </svg>
    );
  };

  // Count filled cells
  const filledCells = boardState.flat().filter(cell => cell !== null).length;

  return (
    <div className="space-y-4">
      {/* Header with controls */}
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
            Board Visualization (Enhanced)
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
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: `repeat(${cols}, ${cellSize}px)`,
              gap: '1px',
            }}
          >
            {boardState.map((row, rowIndex) =>
              row.map((cell, colIndex) => renderPiece(cell, rowIndex, colIndex))
            )}
          </div>
        </div>
      </div>

      {/* Legend */}
      <div className="bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-4">
        <h4 className="text-sm font-semibold text-gray-900 dark:text-white mb-2">Legend:</h4>
        <div className="grid grid-cols-2 gap-2 text-sm">
          <div className="flex items-center space-x-2">
            <div className="w-8 h-8">
              <svg width="32" height="32">
                <polygon points="16,16 0,0 32,0" fill={getEdgeColor(1)} stroke="#333" />
                <polygon points="16,16 32,0 32,32" fill={getEdgeColor(2)} stroke="#333" />
                <polygon points="16,16 32,32 0,32" fill={getEdgeColor(3)} stroke="#333" />
                <polygon points="16,16 0,32 0,0" fill={getEdgeColor(4)} stroke="#333" />
              </svg>
            </div>
            <span className="text-gray-700 dark:text-gray-300">4 triangles = 4 edges</span>
          </div>
          <div className="flex items-center space-x-2">
            <div className="w-8 h-8 bg-gray-500"></div>
            <span className="text-gray-700 dark:text-gray-300">Gray = Border (0)</span>
          </div>
          <div className="flex items-center space-x-2">
            <span className="text-gray-700 dark:text-gray-300 font-mono">A-Z</span>
            <span className="text-gray-700 dark:text-gray-300">Edge pattern letters</span>
          </div>
          <div className="flex items-center space-x-2">
            <span className="text-gray-700 dark:text-gray-300 font-mono text-xs">123</span>
            <span className="text-gray-700 dark:text-gray-300">Piece ID (center)</span>
          </div>
          {positionToSequence && (
            <div className="flex items-center space-x-2">
              <div className="w-6 h-6 rounded-full bg-indigo-600 flex items-center justify-center">
                <span className="text-white text-xs font-bold">1</span>
              </div>
              <span className="text-gray-700 dark:text-gray-300">Placement order (backtracking)</span>
            </div>
          )}
          <div className="flex items-center space-x-2">
            <div className="w-6 h-6 rounded-full bg-yellow-400 flex items-center justify-center border-2 border-yellow-600">
              <span className="text-yellow-900 text-xs font-bold">★</span>
            </div>
            <span className="text-gray-700 dark:text-gray-300">Fixed piece (pre-placed)</span>
          </div>
          <div className="flex items-center space-x-2">
            <div className="w-6 h-6 bg-red-500 flex items-center justify-center">
              <span className="text-white text-xs font-bold">×</span>
            </div>
            <span className="text-gray-700 dark:text-gray-300">Dead end (0 valid options)</span>
          </div>
          <div className="flex items-center space-x-2">
            <div className="w-6 h-6 border-4 border-green-500 bg-gray-300 flex items-center justify-center">
              <span className="text-gray-700 text-xs font-bold">■</span>
            </div>
            <span className="text-gray-700 dark:text-gray-300">Last piece placed</span>
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

      {/* Cell Details Modal */}
      {selectedCell && (
        <CellDetailsModal
          configName={configName}
          row={selectedCell.row}
          col={selectedCell.col}
          onClose={() => setSelectedCell(null)}
        />
      )}
    </div>
  );
};
