import React from 'react';
import './PieceVisualizer.css';

export interface PieceVisualizerProps {
  north: number;
  east: number;
  south: number;
  west: number;
  rotation?: number;
  size?: number;
  pieceId?: number;
  showLabel?: boolean;
  className?: string;
  onClick?: () => void;
}

export const PieceVisualizer: React.FC<PieceVisualizerProps> = ({
  north,
  east,
  south,
  west,
  rotation = 0,
  size = 100,
  pieceId,
  showLabel = false,
  className = '',
  onClick
}) => {
  const getRotatedEdges = () => {
    const rot = rotation % 4;
    switch (rot) {
      case 0:
        return { vN: north, vE: east, vS: south, vW: west };
      case 1:
        return { vN: west, vE: north, vS: east, vW: south };
      case 2:
        return { vN: south, vE: west, vS: north, vW: east };
      case 3:
        return { vN: east, vE: south, vS: west, vW: north };
      default:
        return { vN: north, vE: east, vS: south, vW: west };
    }
  };

  const { vN, vE, vS, vW } = getRotatedEdges();

  const getPatternUrl = (patternId: number): string | null => {
    if (patternId === 0) return null;
    if (patternId < 1 || patternId > 22) return null;
    return `/patterns/pattern${patternId}.png`;
  };

  const quadrantSize = size / 2;

  return (
    <div
      className={`piece-visualizer ${className}`}
      style={{
        width: `${size}px`,
        height: `${size}px`,
        position: 'relative',
        cursor: onClick ? 'pointer' : 'default'
      }}
      onClick={onClick}
      title={pieceId ? `Piece ${pieceId}: N=${north} E=${east} S=${south} W=${west}` : undefined}
    >
      <div
        className="piece-quadrant"
        style={{
          width: `${quadrantSize}px`,
          height: `${quadrantSize}px`,
          position: 'absolute',
          top: 0,
          left: 0,
          backgroundImage: getPatternUrl(vN) ? `url(${getPatternUrl(vN)})` : 'none',
          backgroundColor: getPatternUrl(vN) ? 'transparent' : '#999',
          backgroundSize: '200% 200%',
          backgroundPosition: '0% 0%'
        }}
      />
      <div
        className="piece-quadrant"
        style={{
          width: `${quadrantSize}px`,
          height: `${quadrantSize}px`,
          position: 'absolute',
          top: 0,
          right: 0,
          backgroundImage: getPatternUrl(vE) ? `url(${getPatternUrl(vE)})` : 'none',
          backgroundColor: getPatternUrl(vE) ? 'transparent' : '#999',
          backgroundSize: '200% 200%',
          backgroundPosition: '0% 100%',
          transform: 'rotate(-90deg)'
        }}
      />
      <div
        className="piece-quadrant"
        style={{
          width: `${quadrantSize}px`,
          height: `${quadrantSize}px`,
          position: 'absolute',
          bottom: 0,
          left: 0,
          backgroundImage: getPatternUrl(vW) ? `url(${getPatternUrl(vW)})` : 'none',
          backgroundColor: getPatternUrl(vW) ? 'transparent' : '#999',
          backgroundSize: '200% 200%',
          backgroundPosition: '100% 0%',
          transform: 'rotate(90deg)'
        }}
      />
      <div
        className="piece-quadrant"
        style={{
          width: `${quadrantSize}px`,
          height: `${quadrantSize}px`,
          position: 'absolute',
          bottom: 0,
          right: 0,
          backgroundImage: getPatternUrl(vS) ? `url(${getPatternUrl(vS)})` : 'none',
          backgroundColor: getPatternUrl(vS) ? 'transparent' : '#999',
          backgroundSize: '200% 200%',
          backgroundPosition: '100% 100%',
          transform: 'rotate(180deg)'
        }}
      />
      {showLabel && pieceId !== undefined && (
        <div
          className="piece-label"
          style={{
            position: 'absolute',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            backgroundColor: 'rgba(0, 0, 0, 0.7)',
            color: 'white',
            padding: '2px 6px',
            borderRadius: '3px',
            fontSize: '11px',
            fontWeight: 'bold',
            pointerEvents: 'none',
            userSelect: 'none'
          }}
        >
          {pieceId}
        </div>
      )}
    </div>
  );
};

export default PieceVisualizer;
