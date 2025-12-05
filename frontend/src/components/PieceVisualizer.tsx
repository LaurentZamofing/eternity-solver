import React from 'react';

interface PieceVisualizerProps {
  pieceId: number;
  rotation: number;
  edges: number[]; // [N, E, S, W] after rotation
  size?: number;
  showLabels?: boolean;
  className?: string;
  isCurrent?: boolean;
  isValid?: boolean;
  alreadyTried?: boolean;
  onClick?: () => void;
}

// Color mapping for edge patterns (same as BoardVisualizerEnhanced)
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

// Get label for pattern number (same as BoardVisualizerEnhanced)
const getPatternLabel = (pattern: number): string => {
  if (pattern === 0) return '0';
  // Use letters A-Z for patterns 1-26
  if (pattern <= 26) {
    return String.fromCharCode(64 + pattern); // A=1, B=2, etc.
  }
  return pattern.toString();
};

/**
 * Component to visualize a single puzzle piece with its edges.
 * Renders a square divided into 4 triangles (N, E, S, W) with colors and labels.
 */
export const PieceVisualizer: React.FC<PieceVisualizerProps> = ({
  pieceId,
  rotation,
  edges,
  size = 60,
  showLabels = true,
  className = '',
  isCurrent = false,
  isValid = true,
  alreadyTried = false,
  onClick,
}) => {
  const [northEdge, eastEdge, southEdge, westEdge] = edges;

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

  const center = size / 2;
  const fontSize = Math.max(8, size / 8);
  const pieceFontSize = Math.max(9, size / 7);

  // Determine border style based on status
  let borderClass = 'border border-gray-400';
  if (isCurrent) {
    borderClass = 'border-4 border-blue-500';
  } else if (!isValid) {
    borderClass = 'border border-red-400 opacity-50';
  } else if (alreadyTried) {
    borderClass = 'border border-yellow-500';
  }

  return (
    <div className={`relative inline-block ${className}`}>
      <svg
        width={size}
        height={size}
        className={`${borderClass} ${onClick ? 'cursor-pointer hover:opacity-80' : ''} transition-opacity`}
        onClick={onClick}
      >
        <title>{`Piece ${pieceId} r${rotation}\nN:${northEdge} E:${eastEdge} S:${southEdge} W:${westEdge}`}</title>

        {/* North triangle */}
        <polygon
          points={`${center},${center} 0,0 ${size},0`}
          fill={northColor}
          stroke="#333"
          strokeWidth="0.5"
        />

        {/* East triangle */}
        <polygon
          points={`${center},${center} ${size},0 ${size},${size}`}
          fill={eastColor}
          stroke="#333"
          strokeWidth="0.5"
        />

        {/* South triangle */}
        <polygon
          points={`${center},${center} ${size},${size} 0,${size}`}
          fill={southColor}
          stroke="#333"
          strokeWidth="0.5"
        />

        {/* West triangle */}
        <polygon
          points={`${center},${center} 0,${size} 0,0`}
          fill={westColor}
          stroke="#333"
          strokeWidth="0.5"
        />

        {/* Edge labels */}
        {showLabels && (
          <>
            <text
              x={center}
              y={fontSize + 2}
              textAnchor="middle"
              fontSize={fontSize}
              fill="#fff"
              fontWeight="bold"
              stroke="#000"
              strokeWidth="0.3"
            >
              {northLabel}
            </text>

            <text
              x={size - fontSize}
              y={center + fontSize / 3}
              textAnchor="middle"
              fontSize={fontSize}
              fill="#fff"
              fontWeight="bold"
              stroke="#000"
              strokeWidth="0.3"
            >
              {eastLabel}
            </text>

            <text
              x={center}
              y={size - 4}
              textAnchor="middle"
              fontSize={fontSize}
              fill="#fff"
              fontWeight="bold"
              stroke="#000"
              strokeWidth="0.3"
            >
              {southLabel}
            </text>

            <text
              x={fontSize}
              y={center + fontSize / 3}
              textAnchor="middle"
              fontSize={fontSize}
              fill="#fff"
              fontWeight="bold"
              stroke="#000"
              strokeWidth="0.3"
            >
              {westLabel}
            </text>
          </>
        )}

        {/* Piece ID in center */}
        <text
          x={center}
          y={center + pieceFontSize / 3}
          textAnchor="middle"
          fontSize={pieceFontSize}
          fill="#000"
          fontWeight="bold"
          opacity="0.7"
        >
          {pieceId}
        </text>
      </svg>

      {/* Status badges */}
      {isCurrent && (
        <div className="absolute -top-1 -right-1 bg-blue-500 text-white text-xs px-1 rounded">
          Current
        </div>
      )}
      {alreadyTried && !isCurrent && (
        <div className="absolute -top-1 -right-1 bg-yellow-500 text-white text-xs px-1 rounded">
          Tried
        </div>
      )}
      {!isValid && (
        <div className="absolute -top-1 -right-1 bg-red-500 text-white text-xs px-1 rounded">
          Invalid
        </div>
      )}
    </div>
  );
};
